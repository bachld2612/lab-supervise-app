package com.bachld.backend.config;

import com.bachld.backend.dto.response.StudentClassInfoResponse;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.enums.Role;
import com.bachld.backend.util.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final StudentRepository studentRepository;
    private final StudentClassRepository studentClassRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ConnectedStudentRegistry connectedStudentRegistry;
    private final ConnectedExamStudentRegistry connectedExamStudentRegistry;
    private final StudentClassInfoRepository studentClassInfoRepository;
    private final ExamRoomRepository examRoomRepository;
    private final StudentExamRoomRepository studentExamRoomRepository;
    private final StudentExamRoomInfoRepository studentExamRoomInfoRepository;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        notifyClassIfStudent(event.getUser(), "CONNECT");
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        notifyClassIfStudent(event.getUser(), "DISCONNECT");
    }

    private void notifyClassIfStudent(Principal principal, String type) {
        if (principal == null) return;

        Integer userId;
        try {
            userId = Integer.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleId() != Role.STUDENT.getValue()) return;

        Student student = studentRepository.findByUserId(userId).orElse(null);
        if (student == null) return;

            // Exam room takes priority over regular class: if an exam is active,
        // only notify the exam topic and skip the class entirely.
        boolean examHandled = notifyExamRoomIfActive(student, user, type);
        if (examHandled) return;

        Classes activeClass = findActiveClass(student.getId());
        if (activeClass != null) {
            notifyClass(student, user, activeClass, type);
        }
    }

    private void notifyClass(Student student, User user, Classes activeClass, String type) {
        StudentClassInfoResponse response = StudentClassInfoResponse.builder()
                .classId(activeClass.getId())
                .studentId(student.getId())
                .studentName(user.getFullName())
                .studentCode(student.getCode())
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();

        if ("CONNECT".equals(type)) {
            connectedStudentRegistry.register(activeClass.getId(), student.getId());
        } else if ("DISCONNECT".equals(type)) {
            connectedStudentRegistry.unregister(activeClass.getId(), student.getId());
        }

        studentClassRepository.findByStudentIdAndClassId(student.getId(), activeClass.getId()).ifPresent(sc -> {
            StudentClassInfo log = new StudentClassInfo();
            log.setStudentClassId(sc.getId());
            log.setConnectionType(type);
            log.setBanApplication(false);
            log.setStatus(Status.ACTIVE.getValue());
            studentClassInfoRepository.save(log);
        });

        messagingTemplate.convertAndSend("/topic/class/" + activeClass.getId(), response);
        log.info("Student {} ({}) [{}] class {}", student.getCode(), user.getFullName(), type, activeClass.getId());
    }

    private Classes findActiveClass(Integer studentId) {
        LocalDate today = LocalDate.now();
        List<Classes> potentialClasses = studentClassRepository.findActiveClassesByStudentId(studentId, today);
        if (potentialClasses.isEmpty()) return null;

        List<Schedule> allSchedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = allSchedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        LocalTime nowTime = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        for (Classes clazz : potentialClasses) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());
            if (schedule == null || schedule.getDaysOfWeek() == null) continue;
            boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                    .contains(String.valueOf(dayOfWeek));
            boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime())
                    && !nowTime.isAfter(schedule.getEndTime());
            if (isToday && isActiveTime) return clazz;
        }
        return null;
    }

    private ExamRoom findActiveExamRoom(Integer studentId) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        List<ExamRoom> exams = examRoomRepository.findActiveByStudentId(studentId, today);
        return exams.stream()
                .filter(er -> !nowTime.isBefore(er.getStartTime()) && !nowTime.isAfter(er.getEndTime()))
                .findFirst()
                .orElse(null);
    }

    private boolean notifyExamRoomIfActive(Student student, User user, String type) {
        ExamRoom examRoom = findActiveExamRoom(student.getId());

        if (examRoom == null && "DISCONNECT".equals(type)) {
            // Exam may have ended — look up from registry so we still send the disconnect
            Integer registeredRoomId = connectedExamStudentRegistry.getExamRoomForStudent(student.getId());
            if (registeredRoomId != null) {
                examRoom = examRoomRepository.findById(registeredRoomId).orElse(null);
            }
        }

        if (examRoom == null) return false;

        studentExamRoomRepository.findByStudentIdAndExamRoomId(student.getId(), examRoom.getId()).ifPresent(ser -> {
            StudentExamRoomInfo info = new StudentExamRoomInfo();
            info.setStudentExamRoomId(ser.getId());
            info.setConnectionType(type);
            info.setViolation(false);
            info.setStatus(Status.ACTIVE.getValue());
            studentExamRoomInfoRepository.save(info);
        });

        if ("CONNECT".equals(type)) {
            connectedExamStudentRegistry.register(examRoom.getId(), student.getId());
        } else if ("DISCONNECT".equals(type)) {
            connectedExamStudentRegistry.unregister(examRoom.getId(), student.getId());
        }

        StudentClassInfoResponse response = StudentClassInfoResponse.builder()
                .classId(examRoom.getId())
                .studentId(student.getId())
                .studentName(user.getFullName())
                .studentCode(student.getCode())
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/exam/" + examRoom.getId(), response);
        log.info("Student {} ({}) [{}] exam {}", student.getCode(), user.getFullName(), type, examRoom.getId());
        return true;
    }
}
