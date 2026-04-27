package com.bachld.backend.service;

import com.bachld.backend.dto.request.ClassCreateRequest;
import com.bachld.backend.dto.request.ClassUpdateRequest;
import com.bachld.backend.dto.response.*;
import com.bachld.backend.model.*;
import com.bachld.backend.repository.*;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassService {

    ClassRepository classRepository;

    SubjectRepository subjectRepository;

    TeacherRepository teacherRepository;

    ScheduleRepository scheduleRepository;

    SemesterRepository semesterRepository;

    StudentRepository studentRepository;

    StudentClassInfoRepository studentClassInfoRepository;

    Util util;

    public Page<ClassResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }

        return classRepository.findByKeyword(pageable, keyword, status);
    }

    public ClassResponse getById(Integer id) {
        return classRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    @Transactional
    public void create(ClassCreateRequest request) {
        SubjectResponse subject = subjectRepository.findByIdAndStatus(request.getSubjectId(), Status.ACTIVE.getValue());
        if (subject == null) {
            throw new IllegalArgumentException("Không tìm thấy môn học có id: " + request.getSubjectId());
        }

        TeacherResponse teacher = teacherRepository.findTeacherByIdAndStatus(request.getTeacherId(), Status.ACTIVE.getValue());
        if (teacher == null) {
            throw new IllegalArgumentException("Không tìm thấy giảng viên có id: " + request.getTeacherId());
        }

        ScheduleResponse schedule = scheduleRepository.findByIdAndStatus(request.getScheduleId(), Status.ACTIVE.getValue());
        if (schedule == null) {
            throw new IllegalArgumentException("Không tìm thấy lịch học có id: " + request.getScheduleId());
        }

        SemesterResponse semester = semesterRepository.findByIdAndStatus(request.getSemesterId(), Status.ACTIVE.getValue());
        if (semester == null) {
            throw new IllegalArgumentException("Không tìm thấy học kì có id: " +  request.getSemesterId());
        }

        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn ngày kết thúc");
        }

        Classes classes = new Classes();
        classes.setName(request.getName());
        classes.setMaxStudent(request.getMaxStudent());
        classes.setSubjectId(request.getSubjectId());
        classes.setTeacherId(request.getTeacherId());
        classes.setScheduleId(request.getScheduleId());
        classes.setStartDate(startDate);
        classes.setEndDate(endDate);
        classes.setSemesterId(request.getSemesterId());
        classes.setStatus(Status.ACTIVE.getValue());

        int sessionCount = schedule.getPeriods().split(",").length;
        int sessionNumber = (subject.getCreditNumber() * 15) / sessionCount;
        classes.setSessionNumber(sessionNumber);

        classRepository.save(classes);
    }

    public List<ClassResponse> getListByStudentUserId() {
        User currentUser = util.getCurrentUser();
        LocalDate today = LocalDate.now();
        List<ClassResponse> response = classRepository.findActiveClassByStudentUserId(currentUser.getId(),  today);

        List<Schedule> schedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        LocalTime nowTime = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        // studyStatus = 1 --> studying
        // studyStatus = 0 --> Not study
        for (ClassResponse clazz : response) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());

            if (schedule != null && schedule.getDaysOfWeek() != null) {
                boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                        .contains(String.valueOf(dayOfWeek));

                boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime()) && !nowTime.isAfter(schedule.getEndTime());

                if (isToday && isActiveTime) {
                    clazz.setStudyStatus(1);
                    break;
                }
            }
        }

        for (ClassResponse clazz : response) {
            if (clazz.getStudyStatus() == null || clazz.getStudyStatus() != 1 ) {
                clazz.setStudyStatus(0);
            }
        }

        return response.stream()
                .sorted(Comparator.comparing(ClassResponse::getStudyStatus).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(ClassUpdateRequest request, int id) {
        Classes classes = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            classes.setName(request.getName());
        }

        if (request.getMaxStudent() != null) {
            classes.setMaxStudent(request.getMaxStudent());
        }

        if (request.getTeacherId() != null) {
            TeacherResponse teacher = teacherRepository.findTeacherByIdAndStatus(request.getTeacherId(), Status.ACTIVE.getValue());
            if (teacher == null) {
                throw new IllegalArgumentException("Không tìm thấy giảng viên có id: " + request.getTeacherId());
            }
            classes.setTeacherId(request.getTeacherId());
        }

        if (request.getSubjectId() != null) {
            SubjectResponse subject = subjectRepository.findByIdAndStatus(request.getSubjectId(), Status.ACTIVE.getValue());
            if (subject == null) {
                throw new IllegalArgumentException("Không tìm thấy môn học có id: " + request.getSubjectId());
            }
            classes.setSubjectId(request.getSubjectId());
        }

        if (request.getScheduleId() != null) {
            ScheduleResponse schedule = scheduleRepository.findByIdAndStatus(request.getScheduleId(), Status.ACTIVE.getValue());
            if (schedule == null) {
                throw new IllegalArgumentException("Không tìm thấy lịch học có id: " + request.getScheduleId());
            }
            classes.setScheduleId(request.getScheduleId());
        }

        if (request.getSemesterId() != null) {
            SemesterResponse semester = semesterRepository.findByIdAndStatus(request.getSemesterId(), Status.ACTIVE.getValue());
            if (semester == null) {
                throw new IllegalArgumentException("Không tìm thấy học kì có id: " +  request.getSemesterId());
            }
            classes.setSemesterId(request.getSemesterId());
        }

        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            classes.setStartDate(LocalDate.parse(request.getStartDate()));
        }

        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            classes.setEndDate(LocalDate.parse(request.getEndDate()));
        }

        if (classes.getStartDate().isAfter(classes.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải sớm hơn ngày kết thúc");
        }

        SubjectResponse currentSubject = subjectRepository.findByIdAndStatus(classes.getSubjectId(), Status.ACTIVE.getValue());
        ScheduleResponse currentSchedule = scheduleRepository.findByIdAndStatus(classes.getScheduleId(), Status.ACTIVE.getValue());
        
        if (currentSubject != null && currentSchedule != null) {
            int currentSessionCount = currentSchedule.getPeriods().split(",").length;
            int newSessionNumber = (currentSubject.getCreditNumber() * 15) / currentSessionCount;
            classes.setSessionNumber(newSessionNumber);
        }

        classRepository.save(classes);
    }

    public List<ClassResponse> getListByTeacherUserId() {
        User currentUser = util.getCurrentUser();
        LocalDate today = LocalDate.now();
        List<ClassResponse> response = classRepository.findActiveClassByTeacherUserId(currentUser.getId(), today);

        List<Schedule> schedules = scheduleRepository.findAll();
        Map<Integer, Schedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(BaseEntity::getId, s -> s));

        LocalTime nowTime = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        for (ClassResponse clazz : response) {
            Schedule schedule = scheduleMap.get(clazz.getScheduleId());
            if (schedule != null && schedule.getDaysOfWeek() != null) {
                boolean isToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                        .contains(String.valueOf(dayOfWeek));
                boolean isActiveTime = !nowTime.isBefore(schedule.getStartTime()) && !nowTime.isAfter(schedule.getEndTime());
                clazz.setStudyStatus((isToday && isActiveTime) ? 1 : 0);
            } else {
                clazz.setStudyStatus(0);
            }
        }

        return response.stream()
                .sorted(Comparator.comparing(ClassResponse::getStudyStatus).reversed())
                .collect(Collectors.toList());
    }

    public List<ClassStudentTrackingResponse> getTrackingByClassId(Integer classId) {
        return studentClassInfoRepository.findStudentsWithLatestTracking(classId);
    }

    public Page<StudentResponse> getStudentsByClassId(Integer classId, Pageable pageable, String keyword) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }
        return studentRepository.findByClassId(pageable, classId, keyword);
    }

    public void delete(Integer id) {
        Classes cs = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần có id: " + id));

        cs.setStatus(Status.INACTIVE.getValue());
        classRepository.save(cs);
    }
}
