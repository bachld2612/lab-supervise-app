package com.bachld.backend.service;

import com.bachld.backend.dto.request.ClassCreateRequest;
import com.bachld.backend.dto.request.ClassUpdateRequest;
import com.bachld.backend.dto.response.ClassResponse;
import com.bachld.backend.dto.response.ScheduleResponse;
import com.bachld.backend.dto.response.SubjectResponse;
import com.bachld.backend.dto.response.TeacherResponse;
import com.bachld.backend.model.Classes;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.ScheduleRepository;
import com.bachld.backend.repository.SubjectRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassService {

    ClassRepository classRepository;

    SubjectRepository subjectRepository;

    TeacherRepository teacherRepository;

    ScheduleRepository scheduleRepository;

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

        Classes classes = new Classes();
        classes.setName(request.getName());
        classes.setMaxStudent(request.getMaxStudent());
        classes.setSubjectId(request.getSubjectId());
        classes.setTeacherId(request.getTeacherId());
        classes.setScheduleId(request.getScheduleId());
        classes.setStatus(Status.ACTIVE.getValue());

        int sessionCount = schedule.getPeriods().split(",").length;
        int sessionNumber = (subject.getCreditNumber() * 15) / sessionCount;
        classes.setSessionNumber(sessionNumber);

        classRepository.save(classes);
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

        SubjectResponse currentSubject = subjectRepository.findByIdAndStatus(classes.getSubjectId(), Status.ACTIVE.getValue());
        ScheduleResponse currentSchedule = scheduleRepository.findByIdAndStatus(classes.getScheduleId(), Status.ACTIVE.getValue());
        
        if (currentSubject != null && currentSchedule != null) {
            int currentSessionCount = currentSchedule.getPeriods().split(",").length;
            int newSessionNumber = (currentSubject.getCreditNumber() * 15) / currentSessionCount;
            classes.setSessionNumber(newSessionNumber);
        }

        classRepository.save(classes);
    }

    public void delete(Integer id) {
        Classes cs = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần có id: " + id));

        cs.setStatus(Status.INACTIVE.getValue());
        classRepository.save(cs);
    }
}
