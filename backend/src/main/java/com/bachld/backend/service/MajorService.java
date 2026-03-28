package com.bachld.backend.service;

import com.bachld.backend.dto.request.MajorCreateRequest;
import com.bachld.backend.dto.request.MajorUpdateRequest;
import com.bachld.backend.dto.response.DepartmentResponse;
import com.bachld.backend.dto.response.MajorResponse;
import com.bachld.backend.model.Major;
import com.bachld.backend.repository.DepartmentRepository;
import com.bachld.backend.repository.MajorRepository;
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
public class MajorService {

    MajorRepository majorRepository;

    DepartmentRepository departmentRepository;

    public Page<MajorResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return majorRepository.findByKeyword(pageable, keyword, status);
    }

    public MajorResponse getById(Integer id) {
        return majorRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    public void create(MajorCreateRequest request) {
        DepartmentResponse department = departmentRepository.findByIdAndStatus(request.getDepartmentId(), Status.ACTIVE.getValue());
        if (department == null) {
            throw new IllegalArgumentException("Không tìm thấy khoa có id: " + request.getDepartmentId());
        }

        Major major = new Major();

        major.setName(request.getName());
        major.setDepartmentId(request.getDepartmentId());
        major.setStatus(Status.ACTIVE.getValue());

        majorRepository.save(major);
    }

    public void update(MajorUpdateRequest request, int id) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyên ngành có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            major.setName(request.getName());
        }

        if (request.getDepartmentId() != null) {
            DepartmentResponse department = departmentRepository.findByIdAndStatus(request.getDepartmentId(), Status.ACTIVE.getValue());

            if (department == null) {
                throw new IllegalArgumentException("Không tìm thấy khoa có id: " + request.getDepartmentId());
            }

            major.setDepartmentId(request.getDepartmentId());
        }

        majorRepository.save(major);
    }
}
