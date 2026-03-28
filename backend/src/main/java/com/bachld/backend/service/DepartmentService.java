package com.bachld.backend.service;

import com.bachld.backend.dto.request.DepartmentCreateRequest;
import com.bachld.backend.dto.request.DepartmentUpdateRequest;
import com.bachld.backend.dto.response.DepartmentResponse;
import com.bachld.backend.model.Department;
import com.bachld.backend.repository.DepartmentRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepartmentService {

    DepartmentRepository departmentRepository;

    public Page<DepartmentResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return departmentRepository.findByKeyword(pageable, keyword, status);
    }

    public DepartmentResponse getById(Integer id) {
        return departmentRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    public void create(DepartmentCreateRequest request) {
        Department department = new Department();

        department.setName(request.getName());
        department.setStatus(Status.ACTIVE.getValue());

        departmentRepository.save(department);
    }

    public void update(DepartmentUpdateRequest request, int id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoa có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            department.setName(request.getName());
        }

        departmentRepository.save(department);
    }
}
