package com.bachld.backend.service;

import com.bachld.backend.dto.request.StudentLogCreateRequest;
import com.bachld.backend.dto.response.StudentLogResponse;
import com.bachld.backend.model.StudentLog;
import com.bachld.backend.repository.StudentLogRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentLogService {

    StudentLogRepository studentLogRepository;

    Util util;

    public void create(List<StudentLogCreateRequest> requestList) {
        List<StudentLog> studentLogs = new ArrayList<>();

        for (StudentLogCreateRequest request : requestList) {
            StudentLog studentLog = new StudentLog();
            studentLog.setStudentId(request.getStudentId());
            studentLog.setAppLog(request.getAppLog());
            studentLog.setStatus(Status.ACTIVE.getValue());
            studentLogs.add(studentLog);
        }
        studentLogRepository.saveAll(studentLogs);
    }

    public Page<StudentLogResponse> getListByStudentId(Pageable pageable, Integer studentId, String keyword) {
        util.validateStudent(studentId);

        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return studentLogRepository.findByStudentIdAndKeyword(pageable, studentId, keyword);
    }

}
