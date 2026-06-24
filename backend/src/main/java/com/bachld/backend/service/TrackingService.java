package com.bachld.backend.service;

import com.bachld.backend.dto.request.ClipboardEventRequest;
import com.bachld.backend.dto.request.StudentClassInfoCreateRequest;
import com.bachld.backend.dto.response.StudentClassInfoResponse;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.enums.Status;
import com.bachld.backend.util.enums.TrackingAction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrackingService {

    static final int MAX_CLIPBOARD_TEXT_BYTES = 10 * 1024;

    StudentRepository studentRepository;

    StudentClassRepository studentClassRepository;

    ScheduleRepository scheduleRepository;

    StudentClassInfoRepository studentClassInfoRepository;

    UserRepository userRepository;

    BanApplicationRepository banApplicationRepository;

    ExamRoomRepository examRoomRepository;

    StudentExamRoomRepository studentExamRoomRepository;

    StudentExamRoomInfoRepository studentExamRoomInfoRepository;

    AllowedApplicationRepository allowedApplicationRepository;

    ClipboardTextCryptoService clipboardTextCryptoService;

    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}", "")
                .toLowerCase();
    }

    private static String normalizeToken(String s) {
        return normalize(s).trim();
    }

    @Transactional
    public StudentClassInfoResponse processTracking(Integer userId, StudentClassInfoCreateRequest request) {
        Student student = studentRepository.findByUserId(userId).orElse(null);
        if (student == null) {
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
        String studentName = (user != null) ? user.getFullName() : "Unknown";

        // --- Exam room check (priority over regular class) ---
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        String appName = request.getApplicationName();

        List<ExamRoom> activeExams = examRoomRepository.findActiveByStudentId(student.getId(), today);
        Optional<ExamRoom> activeExam = activeExams.stream()
                .filter(er -> !nowTime.isBefore(er.getStartTime()) && !nowTime.isAfter(er.getEndTime()))
                .findFirst();

        if (activeExam.isPresent()) {
            ExamRoom exam = activeExam.get();
            StudentExamRoom ser = studentExamRoomRepository
                    .findByStudentIdAndExamRoomId(student.getId(), exam.getId()).orElse(null);
            if (ser == null) return null;

            boolean trackingEnabled = Boolean.TRUE.equals(exam.getTrackingEnabled());
            boolean isViolation = false;
            if (trackingEnabled) {
                List<String> allowedApps = allowedApplicationRepository.findActiveAppNamesByExamRoomId(exam.getId());
                String normalizedApp = normalizeToken(appName);
                if (!normalizedApp.isEmpty()) {
                    isViolation = allowedApps.stream()
                            .map(TrackingService::normalizeToken)
                            .filter(allowed -> !allowed.isEmpty())
                            .noneMatch(normalizedApp::contains);
                }
            }

            StudentExamRoomInfo info = new StudentExamRoomInfo();
            info.setStudentExamRoomId(ser.getId());
            info.setApplicationName(appName);
            info.setAction(TrackingAction.NORMAL);
            info.setViolation(isViolation);
            info.setStatus(Status.ACTIVE.getValue());
            studentExamRoomInfoRepository.save(info);

            return StudentClassInfoResponse.builder()
                    .classId(exam.getId())
                    .studentId(student.getId())
                    .studentName(studentName)
                    .studentCode(student.getCode())
                    .applicationName(appName)
                    .action(TrackingAction.NORMAL.getValue())
                    .createdAt(LocalDateTime.now())
                    .isBanApplication(isViolation)
                    .type("EXAM")
                    .build();
        }
        // --- End exam room check ---

        List<Classes> potentialClasses = studentClassRepository.findActiveClassesByStudentId(student.getId(), today);

        if (potentialClasses.isEmpty()) {
            return null;
        }

        List<Schedule> allSchedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = allSchedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        Classes activeClass = null;
        int dayOfWeek = today.getDayOfWeek().getValue();

        for (Classes clazz : potentialClasses) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());
            if (schedule != null && schedule.getDaysOfWeek() != null) {
                boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                        .contains(String.valueOf(dayOfWeek));
                
                boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime()) && !nowTime.isAfter(schedule.getEndTime());

                if (isToday && isActiveTime) {
                    activeClass = clazz;
                    break;
                }
            }
        }

        if (activeClass == null) {
            return null;
        }

        StudentClass studentClass = studentClassRepository.findByStudentIdAndClassId(student.getId(), activeClass.getId())
                .orElse(null);
        
        if (studentClass == null) {
            return null;
        }

        boolean trackingEnabled = Boolean.TRUE.equals(activeClass.getTrackingEnabled());
        boolean isBanApplication = trackingEnabled && isBannedApplication(activeClass.getId(), appName);

        StudentClassInfo info = new StudentClassInfo();
        info.setStudentClassId(studentClass.getId());
        info.setApplicationName(appName);
        info.setAction(TrackingAction.NORMAL);
        info.setBanApplication(isBanApplication);
        info.setStatus(Status.ACTIVE.getValue());
        studentClassInfoRepository.save(info);

        return StudentClassInfoResponse.builder()
                .classId(activeClass.getId())
                .studentId(student.getId())
                .studentName(studentName)
                .studentCode(student.getCode())
                .applicationName(appName)
                .action(TrackingAction.NORMAL.getValue())
                .createdAt(LocalDateTime.now())
                .isBanApplication(isBanApplication)
                .build();
    }

    @Transactional
    public StudentClassInfoResponse processClipboardEvent(Integer userId, ClipboardEventRequest request) {
        Student student = studentRepository.findByUserId(userId).orElse(null);
        if (student == null) {
            return null;
        }

        TrackingAction action = TrackingAction.fromValue(request.getAction());
        if (action == TrackingAction.NORMAL) {
            return null;
        }

        String clipboardText = clipboardTextCryptoService.decrypt(
                request.getClipboardTextEncrypted(),
                request.getClipboardKeyEncrypted(),
                request.getClipboardIv()
        );
        if (clipboardText == null) {
            return null;
        }
        if (clipboardText.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CLIPBOARD_TEXT_BYTES) {
            throw new IllegalArgumentException("Clipboard text exceeds " + MAX_CLIPBOARD_TEXT_BYTES + " bytes");
        }

        User user = userRepository.findById(userId).orElse(null);
        String studentName = (user != null) ? user.getFullName() : "Unknown";
        String appName = request.getApplicationName();

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<ExamRoom> activeExams = examRoomRepository.findActiveByStudentId(student.getId(), today);
        Optional<ExamRoom> activeExam = activeExams.stream()
                .filter(er -> !nowTime.isBefore(er.getStartTime()) && !nowTime.isAfter(er.getEndTime()))
                .findFirst();

        if (activeExam.isPresent()) {
            ExamRoom exam = activeExam.get();
            StudentExamRoom ser = studentExamRoomRepository
                    .findByStudentIdAndExamRoomId(student.getId(), exam.getId()).orElse(null);
            if (ser == null) return null;

            StudentExamRoomInfo info = new StudentExamRoomInfo();
            info.setStudentExamRoomId(ser.getId());
            info.setApplicationName(appName);
            info.setAction(action);
            info.setClipboardTextEncrypted(request.getClipboardTextEncrypted());
            info.setClipboardKeyEncrypted(request.getClipboardKeyEncrypted());
            info.setClipboardIv(request.getClipboardIv());
            info.setViolation(false);
            info.setStatus(Status.ACTIVE.getValue());
            studentExamRoomInfoRepository.save(info);

            return buildClipboardResponse(exam.getId(), student, studentName, appName, action, clipboardText, "EXAM");
        }

        List<Classes> potentialClasses = studentClassRepository.findActiveClassesByStudentId(student.getId(), today);
        if (potentialClasses.isEmpty()) {
            return null;
        }

        List<Schedule> allSchedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = allSchedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        Classes activeClass = null;
        int dayOfWeek = today.getDayOfWeek().getValue();
        for (Classes clazz : potentialClasses) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());
            if (schedule != null && schedule.getDaysOfWeek() != null) {
                boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                        .contains(String.valueOf(dayOfWeek));
                boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime()) && !nowTime.isAfter(schedule.getEndTime());
                if (isToday && isActiveTime) {
                    activeClass = clazz;
                    break;
                }
            }
        }

        if (activeClass == null) {
            return null;
        }

        StudentClass studentClass = studentClassRepository.findByStudentIdAndClassId(student.getId(), activeClass.getId())
                .orElse(null);
        if (studentClass == null) {
            return null;
        }

        StudentClassInfo info = new StudentClassInfo();
        info.setStudentClassId(studentClass.getId());
        info.setApplicationName(appName);
        info.setAction(action);
        info.setClipboardTextEncrypted(request.getClipboardTextEncrypted());
        info.setClipboardKeyEncrypted(request.getClipboardKeyEncrypted());
        info.setClipboardIv(request.getClipboardIv());
        info.setBanApplication(false);
        info.setStatus(Status.ACTIVE.getValue());
        studentClassInfoRepository.save(info);

        return buildClipboardResponse(activeClass.getId(), student, studentName, appName, action, clipboardText, null);
    }

    private StudentClassInfoResponse buildClipboardResponse(Integer contextId, Student student, String studentName,
                                                            String appName, TrackingAction action,
                                                            String clipboardText, String type) {
        return StudentClassInfoResponse.builder()
                .classId(contextId)
                .studentId(student.getId())
                .studentName(studentName)
                .studentCode(student.getCode())
                .applicationName(appName)
                .action(action.getValue())
                .clipboardText(clipboardText)
                .createdAt(LocalDateTime.now())
                .isBanApplication(false)
                .type(type)
                .build();
    }

    private boolean isBannedApplication(Integer classId, String appName) {
        List<String> bannedApps = banApplicationRepository.findActiveAppNamesByClassId(classId);
        String normalizedApp = normalize(appName);
        return bannedApps.stream()
                .anyMatch(banned -> normalizedApp.contains(normalize(banned)));
    }
}
