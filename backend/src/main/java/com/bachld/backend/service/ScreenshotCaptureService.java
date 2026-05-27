package com.bachld.backend.service;

import com.bachld.backend.dto.response.ScreenshotCaptureResponse;
import com.bachld.backend.dto.response.ScreenshotContextOptionResponse;
import com.bachld.backend.dto.response.ScreenshotHistoryItemResponse;
import com.bachld.backend.dto.response.ScreenshotStudentOptionResponse;
import com.bachld.backend.dto.websocket.ScreenshotReadyMessage;
import com.bachld.backend.model.Classes;
import com.bachld.backend.model.ExamRoom;
import com.bachld.backend.model.ScreenshotCapture;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.StudentClass;
import com.bachld.backend.model.StudentExamRoom;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.ExamRoomRepository;
import com.bachld.backend.repository.ScreenshotCaptureRepository;
import com.bachld.backend.repository.StudentClassRepository;
import com.bachld.backend.repository.StudentExamRoomRepository;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Role;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScreenshotCaptureService {

    static final String SCREENSHOT_READY = "SCREENSHOT_READY";

    final RemoteCommandService remoteCommandService;
    final ScreenshotCaptureRepository screenshotCaptureRepository;
    final StudentRepository studentRepository;
    final StudentClassRepository studentClassRepository;
    final StudentExamRoomRepository studentExamRoomRepository;
    final ClassRepository classRepository;
    final ExamRoomRepository examRoomRepository;
    final TeacherRepository teacherRepository;
    final SimpMessagingTemplate messagingTemplate;
    final Util util;
    final EntityManager entityManager;

    @Value("${storage.screenshot-dir:uploads/screenshots}")
    String screenshotDir;

    @Value("${storage.screenshot-url-path:/resources/images/screenshots}")
    String screenshotUrlPath;

    @Value("${app.public-base-url:http://localhost:8080}")
    String publicBaseUrl;

    @Transactional
    public ScreenshotCaptureResponse requestClassScreenshot(Integer classId, Integer studentUserId) {
        verifyCanAccessClass(classId);
        return requestClassScreenshotInternal(classId, studentUserId);
    }

    @Transactional
    public ScreenshotCaptureResponse requestClassViolationScreenshot(Integer classId, Integer studentUserId) {
        return requestClassScreenshotInternal(classId, studentUserId);
    }

    private ScreenshotCaptureResponse requestClassScreenshotInternal(Integer classId, Integer studentUserId) {
        Student student = getStudentByUserId(studentUserId);
        StudentClass studentClass = studentClassRepository.findByStudentIdAndClassId(student.getId(), classId)
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên không thuộc lớp học này"));

        ScreenshotCapture capture = new ScreenshotCapture();
        capture.setStudentClassId(studentClass.getId());
        capture.setStatus(Status.INACTIVE.getValue());
        ScreenshotCapture saved = screenshotCaptureRepository.save(capture);

        remoteCommandService.sendScreenshotCommand(studentUserId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ScreenshotCaptureResponse requestExamRoomScreenshot(Integer examRoomId, Integer studentUserId) {
        verifyCanAccessExamRoom(examRoomId);
        return requestExamRoomScreenshotInternal(examRoomId, studentUserId);
    }

    @Transactional
    public ScreenshotCaptureResponse requestExamRoomViolationScreenshot(Integer examRoomId, Integer studentUserId) {
        return requestExamRoomScreenshotInternal(examRoomId, studentUserId);
    }

    private ScreenshotCaptureResponse requestExamRoomScreenshotInternal(Integer examRoomId, Integer studentUserId) {
        Student student = getStudentByUserId(studentUserId);
        StudentExamRoom studentExamRoom = studentExamRoomRepository.findByStudentIdAndExamRoomId(student.getId(), examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên không thuộc phòng thi này"));

        ScreenshotCapture capture = new ScreenshotCapture();
        capture.setStudentExamRoomId(studentExamRoom.getId());
        capture.setStatus(Status.INACTIVE.getValue());
        ScreenshotCapture saved = screenshotCaptureRepository.save(capture);

        remoteCommandService.sendScreenshotCommand(studentUserId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ScreenshotCaptureResponse uploadScreenshot(Integer screenshotId, MultipartFile file) {
        User currentUser = util.getCurrentUser();
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên hiện tại"));

        ScreenshotCapture capture = screenshotCaptureRepository.findById(screenshotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh chụp màn hình"));

        Context context = resolveContext(capture);
        if (!context.studentId().equals(student.getId())) {
            throw new IllegalArgumentException("Không được phép upload ảnh cho sinh viên khác");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không hợp lệ");
        }

        String relativePath = buildRelativePath(screenshotId);
        Path target = Path.of(screenshotDir).resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh chụp màn hình: " + e.getMessage());
        }

        capture.setImagePath(buildPublicImageUrl(relativePath));
        capture.setStatus(Status.ACTIVE.getValue());
        ScreenshotCapture saved = screenshotCaptureRepository.save(capture);

        notifyReady(saved, context);
        return toResponse(saved);
    }

    public Resource getImage(Integer screenshotId) {
        ScreenshotCapture capture = getAuthorizedCapture(screenshotId);
        if (capture.getImagePath() == null || capture.getImagePath().isBlank()) {
            throw new IllegalArgumentException("Ảnh chụp màn hình chưa sẵn sàng");
        }

        Path path = Path.of(screenshotDir).resolve(toStoredRelativePath(capture.getImagePath())).normalize();
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Không tìm thấy file ảnh chụp màn hình");
        }
        return resource;
    }

    public ScreenshotCaptureResponse getMetadata(Integer screenshotId) {
        return toResponse(getAuthorizedCapture(screenshotId));
    }

    public Page<ScreenshotHistoryItemResponse> getHistory(
            String contextType,
            Integer contextId,
            Integer studentId,
            LocalDate date,
            Pageable pageable
    ) {
        String normalizedContextType = normalizeContextType(contextType);
        Integer teacherId = getCurrentTeacherIdOrNull();
        if ("EXAM_ROOM".equals(normalizedContextType)) {
            return getExamRoomHistory(contextId, studentId, date, teacherId, pageable);
        }
        return getClassHistory(contextId, studentId, date, teacherId, pageable);
    }

    public List<ScreenshotContextOptionResponse> getContextOptions(String contextType) {
        String normalizedContextType = normalizeContextType(contextType);
        Integer teacherId = getCurrentTeacherIdOrNull();
        String sql;
        if ("EXAM_ROOM".equals(normalizedContextType)) {
            sql = """
                    SELECT er.id, er.code
                    FROM exam_room er
                    WHERE (:teacherId IS NULL OR er.teacher1_id = :teacherId OR er.teacher2_id = :teacherId)
                    ORDER BY er.exam_date DESC, er.start_time DESC
                    """;
        } else {
            sql = """
                    SELECT c.id, c.name
                    FROM classes c
                    WHERE (:teacherId IS NULL OR c.teacher_id = :teacherId)
                    ORDER BY c.name ASC
                    """;
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("teacherId", teacherId);
        return query.getResultList().stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new ScreenshotContextOptionResponse(asInteger(values[0]), asString(values[1]));
                })
                .toList();
    }

    public List<ScreenshotStudentOptionResponse> getStudentOptions(String contextType, Integer contextId) {
        if (contextId == null) {
            return List.of();
        }

        String normalizedContextType = normalizeContextType(contextType);
        Integer teacherId = getCurrentTeacherIdOrNull();
        String sql;
        if ("EXAM_ROOM".equals(normalizedContextType)) {
            sql = """
                    SELECT s.id, u.full_name, s.code
                    FROM student_exam_room ser
                    JOIN exam_room er ON er.id = ser.exam_room_id
                    JOIN students s ON s.id = ser.student_id
                    JOIN users u ON u.id = s.user_id
                    WHERE er.id = :contextId
                      AND (:teacherId IS NULL OR er.teacher1_id = :teacherId OR er.teacher2_id = :teacherId)
                    ORDER BY u.full_name ASC, s.code ASC
                    """;
        } else {
            sql = """
                    SELECT s.id, u.full_name, s.code
                    FROM student_class sc
                    JOIN classes c ON c.id = sc.class_id
                    JOIN students s ON s.id = sc.student_id
                    JOIN users u ON u.id = s.user_id
                    WHERE c.id = :contextId
                      AND (:teacherId IS NULL OR c.teacher_id = :teacherId)
                    ORDER BY u.full_name ASC, s.code ASC
                    """;
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("contextId", contextId);
        query.setParameter("teacherId", teacherId);
        return query.getResultList().stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new ScreenshotStudentOptionResponse(asInteger(values[0]), asString(values[1]), asString(values[2]));
                })
                .toList();
    }

    private Page<ScreenshotHistoryItemResponse> getClassHistory(
            Integer contextId,
            Integer studentId,
            LocalDate date,
            Integer teacherId,
            Pageable pageable
    ) {
        String selectSql = """
                SELECT scp.id,
                       scp.created_at,
                       s.id AS student_id,
                       u.full_name,
                       s.code,
                       c.id AS context_id,
                       c.name AS context_name,
                       (
                           SELECT sci.application_name
                           FROM student_class_info sci
                           WHERE sci.student_class_id = sc.id
                             AND sci.created_at <= scp.created_at
                             AND (sci.connection_type IS NULL OR sci.connection_type NOT IN ('CONNECT', 'DISCONNECT'))
                           ORDER BY sci.created_at DESC, sci.id DESC
                           LIMIT 1
                       ) AS application_name,
                       scp.image_path
                FROM screenshot_capture scp
                JOIN student_class sc ON sc.id = scp.student_class_id
                JOIN classes c ON c.id = sc.class_id
                JOIN students s ON s.id = sc.student_id
                JOIN users u ON u.id = s.user_id
                WHERE scp.student_class_id IS NOT NULL
                  AND scp.status = 1
                  AND (:contextId IS NULL OR c.id = :contextId)
                  AND (:studentId IS NULL OR s.id = :studentId)
                  AND (:captureDate IS NULL OR DATE(scp.created_at) = :captureDate)
                  AND (:teacherId IS NULL OR c.teacher_id = :teacherId)
                ORDER BY scp.created_at DESC, scp.id DESC
                """;
        String countSql = """
                SELECT COUNT(*)
                FROM screenshot_capture scp
                JOIN student_class sc ON sc.id = scp.student_class_id
                JOIN classes c ON c.id = sc.class_id
                JOIN students s ON s.id = sc.student_id
                WHERE scp.student_class_id IS NOT NULL
                  AND scp.status = 1
                  AND (:contextId IS NULL OR c.id = :contextId)
                  AND (:studentId IS NULL OR s.id = :studentId)
                  AND (:captureDate IS NULL OR DATE(scp.created_at) = :captureDate)
                  AND (:teacherId IS NULL OR c.teacher_id = :teacherId)
                """;

        Query query = createHistoryQuery(selectSql, contextId, studentId, date, teacherId, pageable);
        Query countQuery = createHistoryCountQuery(countSql, contextId, studentId, date, teacherId);
        List<ScreenshotHistoryItemResponse> items = query.getResultList().stream()
                .map(row -> mapHistoryRow((Object[]) row, "CLASS"))
                .toList();
        return new PageImpl<>(items, pageable, asLong(countQuery.getSingleResult()));
    }

    private Page<ScreenshotHistoryItemResponse> getExamRoomHistory(
            Integer contextId,
            Integer studentId,
            LocalDate date,
            Integer teacherId,
            Pageable pageable
    ) {
        String selectSql = """
                SELECT scp.id,
                       scp.created_at,
                       s.id AS student_id,
                       u.full_name,
                       s.code,
                       er.id AS context_id,
                       er.code AS context_name,
                       (
                           SELECT seri.application_name
                           FROM student_exam_room_info seri
                           WHERE seri.student_exam_room_id = ser.id
                             AND seri.created_at <= scp.created_at
                             AND (seri.connection_type IS NULL OR seri.connection_type NOT IN ('CONNECT', 'DISCONNECT'))
                           ORDER BY seri.created_at DESC, seri.id DESC
                           LIMIT 1
                       ) AS application_name,
                       scp.image_path
                FROM screenshot_capture scp
                JOIN student_exam_room ser ON ser.id = scp.student_exam_room_id
                JOIN exam_room er ON er.id = ser.exam_room_id
                JOIN students s ON s.id = ser.student_id
                JOIN users u ON u.id = s.user_id
                WHERE scp.student_exam_room_id IS NOT NULL
                  AND scp.status = 1
                  AND (:contextId IS NULL OR er.id = :contextId)
                  AND (:studentId IS NULL OR s.id = :studentId)
                  AND (:captureDate IS NULL OR DATE(scp.created_at) = :captureDate)
                  AND (:teacherId IS NULL OR er.teacher1_id = :teacherId OR er.teacher2_id = :teacherId)
                ORDER BY scp.created_at DESC, scp.id DESC
                """;
        String countSql = """
                SELECT COUNT(*)
                FROM screenshot_capture scp
                JOIN student_exam_room ser ON ser.id = scp.student_exam_room_id
                JOIN exam_room er ON er.id = ser.exam_room_id
                JOIN students s ON s.id = ser.student_id
                WHERE scp.student_exam_room_id IS NOT NULL
                  AND scp.status = 1
                  AND (:contextId IS NULL OR er.id = :contextId)
                  AND (:studentId IS NULL OR s.id = :studentId)
                  AND (:captureDate IS NULL OR DATE(scp.created_at) = :captureDate)
                  AND (:teacherId IS NULL OR er.teacher1_id = :teacherId OR er.teacher2_id = :teacherId)
                """;

        Query query = createHistoryQuery(selectSql, contextId, studentId, date, teacherId, pageable);
        Query countQuery = createHistoryCountQuery(countSql, contextId, studentId, date, teacherId);
        List<ScreenshotHistoryItemResponse> items = query.getResultList().stream()
                .map(row -> mapHistoryRow((Object[]) row, "EXAM_ROOM"))
                .toList();
        return new PageImpl<>(items, pageable, asLong(countQuery.getSingleResult()));
    }

    private Query createHistoryQuery(String sql, Integer contextId, Integer studentId, LocalDate date, Integer teacherId, Pageable pageable) {
        Query query = createHistoryCountQuery(sql, contextId, studentId, date, teacherId);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return query;
    }

    private Query createHistoryCountQuery(String sql, Integer contextId, Integer studentId, LocalDate date, Integer teacherId) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("contextId", contextId);
        query.setParameter("studentId", studentId);
        query.setParameter("captureDate", date);
        query.setParameter("teacherId", teacherId);
        return query;
    }

    private ScreenshotCapture getAuthorizedCapture(Integer screenshotId) {
        ScreenshotCapture capture = screenshotCaptureRepository.findById(screenshotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh chụp màn hình"));

        User currentUser = util.getCurrentUser();
        if (currentUser.getRoleId() == Role.ADMIN.getValue()) {
            return capture;
        }
        if (currentUser.getRoleId() != Role.TEACHER.getValue()) {
            throw new IllegalArgumentException("Không có quyền xem ảnh chụp màn hình");
        }

        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên hiện tại"));
        Context context = resolveContext(capture);
        if (context.classId() != null) {
            Classes classes = classRepository.findById(context.classId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
            if (!Objects.equals(teacher.getId(), classes.getTeacherId())) {
                throw new IllegalArgumentException("Không có quyền xem ảnh chụp màn hình");
            }
        } else {
            ExamRoom examRoom = examRoomRepository.findById(context.examRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi"));
            if (!Objects.equals(teacher.getId(), examRoom.getTeacher1Id())
                    && !Objects.equals(teacher.getId(), examRoom.getTeacher2Id())) {
                throw new IllegalArgumentException("Không có quyền xem ảnh chụp màn hình");
            }
        }
        return capture;
    }

    private Context resolveContext(ScreenshotCapture capture) {
        if (capture.getStudentClassId() != null) {
            StudentClass studentClass = studentClassRepository.findById(capture.getStudentClassId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin sinh viên lớp học"));
            Student student = studentRepository.findById(studentClass.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));
            return new Context(student.getId(), student.getUserId(), studentClass.getClassId(), null);
        }

        StudentExamRoom studentExamRoom = studentExamRoomRepository.findById(capture.getStudentExamRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin sinh viên phòng thi"));
        Student student = studentRepository.findById(studentExamRoom.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));
        return new Context(student.getId(), student.getUserId(), null, studentExamRoom.getExamRoomId());
    }

    private void notifyReady(ScreenshotCapture capture, Context context) {
        ScreenshotReadyMessage message = ScreenshotReadyMessage.builder()
                .type(SCREENSHOT_READY)
                .screenshotId(capture.getId())
                .studentId(context.studentId())
                .studentUserId(context.studentUserId())
                .imageUrl(capture.getImagePath())
                .createdAt(capture.getCreatedAt())
                .build();

        if (context.classId() != null) {
            messagingTemplate.convertAndSend("/topic/class/" + context.classId(), message);
        } else {
            messagingTemplate.convertAndSend("/topic/exam/" + context.examRoomId(), message);
        }
    }

    private Student getStudentByUserId(Integer studentUserId) {
        return studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));
    }

    private void verifyCanAccessClass(Integer classId) {
        User currentUser = util.getCurrentUser();
        if (currentUser.getRoleId() == Role.ADMIN.getValue()) {
            return;
        }
        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên hiện tại"));
        Classes classes = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        if (!Objects.equals(teacher.getId(), classes.getTeacherId())) {
            throw new IllegalArgumentException("Không có quyền chụp màn hình lớp học này");
        }
    }

    private void verifyCanAccessExamRoom(Integer examRoomId) {
        User currentUser = util.getCurrentUser();
        if (currentUser.getRoleId() == Role.ADMIN.getValue()) {
            return;
        }
        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên hiện tại"));
        ExamRoom examRoom = examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi"));
        if (!Objects.equals(teacher.getId(), examRoom.getTeacher1Id())
                && !Objects.equals(teacher.getId(), examRoom.getTeacher2Id())) {
            throw new IllegalArgumentException("Không có quyền chụp màn hình phòng thi này");
        }
    }

    private Integer getCurrentTeacherIdOrNull() {
        User currentUser = util.getCurrentUser();
        if (currentUser.getRoleId() == Role.ADMIN.getValue()) {
            return null;
        }
        if (currentUser.getRoleId() != Role.TEACHER.getValue()) {
            throw new IllegalArgumentException("Không có quyền xem lịch sử ảnh màn hình");
        }
        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên hiện tại"));
        return teacher.getId();
    }

    private String normalizeContextType(String contextType) {
        if ("EXAM_ROOM".equalsIgnoreCase(contextType) || "EXAM".equalsIgnoreCase(contextType)) {
            return "EXAM_ROOM";
        }
        return "CLASS";
    }

    private ScreenshotHistoryItemResponse mapHistoryRow(Object[] row, String contextType) {
        return ScreenshotHistoryItemResponse.builder()
                .id(asInteger(row[0]))
                .createdAt(asLocalDateTime(row[1]))
                .studentId(asInteger(row[2]))
                .studentName(asString(row[3]))
                .studentCode(asString(row[4]))
                .contextType(contextType)
                .contextId(asInteger(row[5]))
                .contextName(asString(row[6]))
                .applicationName(asString(row[7]))
                .imageUrl(asString(row[8]))
                .build();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    private ScreenshotCaptureResponse toResponse(ScreenshotCapture capture) {
        return ScreenshotCaptureResponse.builder()
                .id(capture.getId())
                .imageUrl(capture.getImagePath())
                .build();
    }

    private String buildPublicImageUrl(String relativePath) {
        return normalizeBaseUrl(publicBaseUrl) + normalizeUrlPath(screenshotUrlPath) + "/" + relativePath.replace('\\', '/');
    }

    private String toStoredRelativePath(String imagePath) {
        String urlPrefix = normalizeBaseUrl(publicBaseUrl) + normalizeUrlPath(screenshotUrlPath) + "/";
        if (imagePath.startsWith(urlPrefix)) {
            return imagePath.substring(urlPrefix.length());
        }

        String pathPrefix = normalizeUrlPath(screenshotUrlPath) + "/";
        if (imagePath.startsWith(pathPrefix)) {
            return imagePath.substring(pathPrefix.length());
        }
        int pathIndex = imagePath.indexOf(pathPrefix);
        if (pathIndex >= 0) {
            return imagePath.substring(pathIndex + pathPrefix.length());
        }

        return imagePath;
    }

    private String buildRelativePath(Integer screenshotId) {
        LocalDate today = LocalDate.now();
        return Path.of(
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                screenshotId + "-" + UUID.randomUUID() + ".jpg"
        ).toString();
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:8080" : value;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String normalizeUrlPath(String value) {
        String normalized = value == null || value.isBlank() ? "/resources/images/screenshots" : value;
        normalized = normalized.startsWith("/") ? normalized : "/" + normalized;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private record Context(Integer studentId, Integer studentUserId, Integer classId, Integer examRoomId) {}
}
