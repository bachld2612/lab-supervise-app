package com.bachld.backend.service;

import com.bachld.backend.dto.request.BanApplicationCreateRequest;
import com.bachld.backend.dto.request.BanApplicationUpdateRequest;
import com.bachld.backend.dto.response.BanApplicationResponse;
import com.bachld.backend.model.BanApplication;
import com.bachld.backend.model.Teacher;
import com.bachld.backend.model.User;
import com.bachld.backend.repository.BanApplicationRepository;
import com.bachld.backend.repository.TeacherRepository;
import com.bachld.backend.util.Util;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BanApplicationService {

    BanApplicationRepository banApplicationRepository;

    TeacherRepository teacherRepository;

    Util util;

    public Page<BanApplicationResponse> getList(Pageable pageable, String keyword, Integer status) {
        Teacher teacher = getCurrentTeacher();
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        } else {
            keyword = "%%";
        }
        return banApplicationRepository.findByKeywordAndTeacherId(pageable, keyword, teacher.getId(), status);
    }

    public BanApplicationResponse getById(int id) {
        Teacher teacher = getCurrentTeacher();
        BanApplicationResponse response = banApplicationRepository.findByIdAndTeacherId(id, teacher.getId());
        if (response == null) {
            throw new IllegalArgumentException("Không tìm thấy ứng dụng cấm có id: " + id);
        }
        return response;
    }

    public void create(BanApplicationCreateRequest request) {
        Teacher teacher = getCurrentTeacher();
        BanApplication entity = new BanApplication();
        entity.setTeacherId(teacher.getId());
        entity.setApplicationName(request.getApplicationName());
        entity.setImageUrl(request.getImageUrl());
        entity.setStatus(Status.ACTIVE.getValue());
        banApplicationRepository.save(entity);
    }

    public void update(BanApplicationUpdateRequest request, int id) {
        Teacher teacher = getCurrentTeacher();
        BanApplication entity = banApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng dụng cấm có id: " + id));

        if (!entity.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa ứng dụng này");
        }

        if (request.getApplicationName() != null && !request.getApplicationName().isEmpty()) {
            entity.setApplicationName(request.getApplicationName());
        }
        if (request.getImageUrl() != null) {
            entity.setImageUrl(request.getImageUrl());
        }
        banApplicationRepository.save(entity);
    }

    public void delete(int id) {
        Teacher teacher = getCurrentTeacher();
        BanApplication entity = banApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng dụng cấm có id: " + id));

        if (!entity.getTeacherId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xoá ứng dụng này");
        }

        entity.setStatus(Status.INACTIVE.getValue());
        banApplicationRepository.save(entity);
    }

    private Teacher getCurrentTeacher() {
        User currentUser = util.getCurrentUser();
        return teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin giảng viên"));
    }
}