package com.bachld.backend.service;

import com.bachld.backend.dto.request.StudentClassInfoCreateRequest;
import com.bachld.backend.dto.response.StudentClassInfoResponse;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrackingService {

    StudentRepository studentRepository;

    StudentClassRepository studentClassRepository;

    ScheduleRepository scheduleRepository;

    StudentClassInfoRepository studentClassInfoRepository;

    UserRepository userRepository;

    BanApplicationRepository banApplicationRepository;

    @Transactional
    public StudentClassInfoResponse processTracking(Integer userId, StudentClassInfoCreateRequest request) {
        Student student = studentRepository.findByUserId(userId).orElse(null);
        if (student == null) {
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
        String studentName = (user != null) ? user.getFullName() : "Unknown";

        LocalDate today = LocalDate.now();
        List<Classes> potentialClasses = studentClassRepository.findActiveClassesByStudentId(student.getId(), today);

        if (potentialClasses.isEmpty()) {
            return null;
        }

        List<Schedule> allSchedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = allSchedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        Classes activeClass = null;
        LocalTime nowTime = LocalTime.now();
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

        List<String> bannedApps = banApplicationRepository.findActiveAppNamesByClassId(activeClass.getId());
        String appName = request.getApplicationName();
        boolean isBanApplication = bannedApps.stream()
                .anyMatch(banned -> appName.toLowerCase().contains(banned.toLowerCase()));

        StudentClassInfo info = new StudentClassInfo();
        info.setStudentClassId(studentClass.getId());
        info.setApplicationName(appName);
        info.setBanApplication(isBanApplication);
        info.setStatus(Status.ACTIVE.getValue());
        studentClassInfoRepository.save(info);

        return StudentClassInfoResponse.builder()
                .classId(activeClass.getId())
                .studentId(student.getId())
                .studentName(studentName)
                .studentCode(student.getCode())
                .applicationName(appName)
                .createdAt(LocalDateTime.now())
                .isBanApplication(isBanApplication)
                .build();
    }
}
