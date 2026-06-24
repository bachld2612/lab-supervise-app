package com.bachld.backend.service;

import com.bachld.backend.dto.request.AllowedApplicationCreateRequest;
import com.bachld.backend.dto.request.AllowedApplicationUpdateRequest;
import com.bachld.backend.dto.response.AllowedApplicationResponse;
import com.bachld.backend.dto.response.WhitelistUpdateMessage;
import com.bachld.backend.model.AllowedApplication;
import com.bachld.backend.model.ExamRoom;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.AllowedApplicationRepository;
import com.bachld.backend.repository.ExamRoomRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AllowedApplicationService {

    AllowedApplicationRepository allowedApplicationRepository;

    ExamRoomRepository examRoomRepository;

    TeacherRepository teacherRepository;

    SimpMessagingTemplate messagingTemplate;

    Util util;

    public Page<AllowedApplicationResponse> getList(Integer examRoomId, Pageable pageable, String keyword) {
        String kw = keyword != null ? "%" + keyword.trim().toLowerCase() + "%" : null;
        return allowedApplicationRepository.findByExamRoomAndKeyword(pageable, examRoomId, kw, null);
    }

    public void create(AllowedApplicationCreateRequest request) {
        Teacher teacher = getCurrentTeacher();
        getExamRoomAndCheckTeacher(request.getExamRoomId(), teacher.getId());

        AllowedApplication entity = new AllowedApplication();
        entity.setExamRoomId(request.getExamRoomId());
        entity.setApplicationName(request.getApplicationName().trim());
        entity.setImageUrl(request.getImageUrl());
        entity.setStatus(Status.ACTIVE.getValue());
        allowedApplicationRepository.save(entity);

        broadcastWhitelistUpdate(request.getExamRoomId());
    }

    public void update(AllowedApplicationUpdateRequest request, int id) {
        Teacher teacher = getCurrentTeacher();
        AllowedApplication entity = allowedApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng dụng có id: " + id));

        getExamRoomAndCheckTeacher(entity.getExamRoomId(), teacher.getId());

        if (request.getApplicationName() != null && !request.getApplicationName().isBlank()) {
            entity.setApplicationName(request.getApplicationName().trim());
        }
        if (request.getImageUrl() != null) {
            entity.setImageUrl(request.getImageUrl());
        }
        allowedApplicationRepository.save(entity);

        broadcastWhitelistUpdate(entity.getExamRoomId());
    }

    public void delete(int id) {
        Teacher teacher = getCurrentTeacher();
        AllowedApplication entity = allowedApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng dụng có id: " + id));

        getExamRoomAndCheckTeacher(entity.getExamRoomId(), teacher.getId());

        entity.setStatus(Status.INACTIVE.getValue());
        allowedApplicationRepository.save(entity);

        broadcastWhitelistUpdate(entity.getExamRoomId());
    }

    private void broadcastWhitelistUpdate(Integer examRoomId) {
        List<AllowedApplicationResponse> updated = allowedApplicationRepository
                .findByExamRoomIdAndStatus(examRoomId, Status.ACTIVE.getValue())
                .stream()
                .map(a -> new AllowedApplicationResponse(a.getId(), a.getExamRoomId(), a.getApplicationName(), a.getImageUrl(), a.getStatus()))
                .collect(Collectors.toList());

        WhitelistUpdateMessage msg = new WhitelistUpdateMessage("WHITELIST_UPDATE", examRoomId, updated);
        messagingTemplate.convertAndSend("/topic/exam/" + examRoomId, msg);
    }

    private void getExamRoomAndCheckTeacher(Integer examRoomId, Integer teacherId) {
        ExamRoom examRoom = examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng thi có id: " + examRoomId));

        if (!teacherId.equals(examRoom.getTeacher1Id()) && !teacherId.equals(examRoom.getTeacher2Id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác với phòng thi này");
        }
    }

    private Teacher getCurrentTeacher() {
        User currentUser = util.getCurrentUser();
        return teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin giảng viên"));
    }
}
