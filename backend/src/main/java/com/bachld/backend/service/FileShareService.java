package com.bachld.backend.service;

import com.bachld.backend.model.Student;
import com.bachld.backend.model.StudentClass;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.StudentClassRepository;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.util.Util;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileShareService {

    private static final long TEMP_FILE_TTL_SECONDS = 300;

    StudentClassRepository studentClassRepository;
    StudentRepository studentRepository;
    RemoteCommandService remoteCommandService;
    Util util;
    ConcurrentHashMap<String, TempSharedFile> tempFiles = new ConcurrentHashMap<>();
    ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(new CleanupThreadFactory());

    @NonFinal
    @Value("${storage.shared-file-dir:uploads/shared-files-temp}")
    String sharedFileDir;

    public void sendFileToClass(Integer classId, MultipartFile file) {
        List<Integer> studentUserIds = studentClassRepository.findByClassId(classId).stream()
                .map(StudentClass::getStudentId)
                .map(studentId -> studentRepository.findById(studentId).orElse(null))
                .filter(student -> student != null && student.getUserId() != null)
                .map(Student::getUserId)
                .distinct()
                .toList();

        TempSharedFile sharedFile = saveTempSharedFile(file, studentUserIds);
        sendFileAvailable(sharedFile);
    }

    public void sendFileToStudent(Integer studentId, MultipartFile file) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + studentId));
        if (student.getUserId() == null) {
            throw new IllegalArgumentException("Sinh viên chưa có tài khoản người dùng");
        }

        TempSharedFile sharedFile = saveTempSharedFile(file, List.of(student.getUserId()));
        sendFileAvailable(sharedFile);
    }

    public ResponseEntity<Resource> downloadSharedFile(String fileToken) {
        cleanupExpiredFiles();

        User currentUser = util.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Không xác định được người dùng");
        }

        TempSharedFile sharedFile = tempFiles.get(fileToken);
        if (sharedFile == null || sharedFile.isExpired()) {
            removeTempFile(fileToken, sharedFile);
            throw new IllegalArgumentException("File không còn tồn tại hoặc đã hết hạn");
        }
        if (!sharedFile.allowedStudentUserIds().contains(currentUser.getId())) {
            throw new IllegalArgumentException("Không có quyền tải file này");
        }
        if (!Files.exists(sharedFile.path())) {
            tempFiles.remove(fileToken);
            throw new IllegalArgumentException("File không còn tồn tại");
        }

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(sharedFile.path());
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file tạm: " + e.getMessage());
        }

        sharedFile.pendingStudentUserIds().remove(currentUser.getId());
        Resource resource = new ByteArrayResource(fileBytes);
        String contentType = sharedFile.contentType() == null || sharedFile.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : sharedFile.contentType();

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(sharedFile.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentLength(fileBytes.length)
                .body(resource);

        if (sharedFile.pendingStudentUserIds().isEmpty()) {
            removeTempFile(fileToken, sharedFile);
        }
        return response;
    }

    private TempSharedFile saveTempSharedFile(MultipartFile file, List<Integer> studentUserIds) {
        cleanupExpiredFiles();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            throw new IllegalArgumentException("Không có sinh viên nhận file");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "download"
                : file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        String token = UUID.randomUUID().toString();
        String extension = StringUtils.getFilenameExtension(originalFileName);
        String storedFileName = extension == null || extension.isBlank()
                ? token
                : token + "." + extension;

        Path storageDir = getStorageDir();
        Path target = storageDir.resolve(storedFileName).normalize();
        try {
            Files.createDirectories(storageDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file tạm: " + e.getMessage());
        }

        Set<Integer> recipients = ConcurrentHashMap.newKeySet();
        recipients.addAll(studentUserIds);
        Set<Integer> pendingRecipients = ConcurrentHashMap.newKeySet();
        pendingRecipients.addAll(studentUserIds);

        TempSharedFile sharedFile = new TempSharedFile(
                token,
                originalFileName,
                file.getContentType(),
                file.getSize(),
                target,
                Instant.now().plusSeconds(TEMP_FILE_TTL_SECONDS),
                recipients,
                pendingRecipients
        );
        tempFiles.put(token, sharedFile);
        scheduleTempFileRemoval(token, sharedFile);
        return sharedFile;
    }

    private void scheduleTempFileRemoval(String token, TempSharedFile sharedFile) {
        cleanupExecutor.schedule(
                () -> removeTempFile(token, sharedFile),
                TEMP_FILE_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void sendFileAvailable(TempSharedFile sharedFile) {
        for (Integer studentUserId : sharedFile.allowedStudentUserIds()) {
            if (studentUserId == null) continue;
            remoteCommandService.sendFileAvailableCommand(
                    studentUserId,
                    sharedFile.token(),
                    sharedFile.fileName(),
                    sharedFile.fileSize()
            );
        }
    }

    private void cleanupExpiredFiles() {
        Instant now = Instant.now();
        tempFiles.forEach((token, file) -> {
            if (file.expiresAt().isBefore(now)) {
                removeTempFile(token, file);
            }
        });
    }

    private void removeTempFile(String token, TempSharedFile file) {
        if (file == null) {
            return;
        }
        if (tempFiles.remove(token, file)) {
            try {
                Files.deleteIfExists(file.path());
            } catch (IOException ignored) {
            }
        }
    }

    private Path getStorageDir() {
        return Path.of(sharedFileDir).toAbsolutePath().normalize();
    }

    @PreDestroy
    public void shutdownCleanupExecutor() {
        cleanupExecutor.shutdownNow();
        tempFiles.forEach(this::removeTempFile);
    }

    private record TempSharedFile(
            String token,
            String fileName,
            String contentType,
            long fileSize,
            Path path,
            Instant expiresAt,
            Set<Integer> allowedStudentUserIds,
            Set<Integer> pendingStudentUserIds
    ) {
        boolean isExpired() {
            return expiresAt.isBefore(Instant.now());
        }
    }

    private static class CleanupThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "file-share-cleanup-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
