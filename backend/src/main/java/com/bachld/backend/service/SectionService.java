package com.bachld.backend.service;

import com.bachld.backend.dto.request.SectionCreateRequest;
import com.bachld.backend.dto.request.SectionUpdateRequest;
import com.bachld.backend.dto.response.DepartmentResponse;
import com.bachld.backend.dto.response.SectionResponse;
import com.bachld.backend.model.Section;
import com.bachld.backend.repository.DepartmentRepository;
import com.bachld.backend.repository.SectionRepository;
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
public class SectionService {

    SectionRepository sectionRepository;

    DepartmentRepository departmentRepository;

    public Page<SectionResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return sectionRepository.findByKeyword(pageable, keyword, status);
    }

    public SectionResponse getById(Integer id) {
        return sectionRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    public void create(SectionCreateRequest request) {
        DepartmentResponse department = departmentRepository.findByIdAndStatus(request.getDepartmentId(), Status.ACTIVE.getValue());
        if (department == null) {
            throw new IllegalArgumentException("Không tìm thấy khoa có id: " + request.getDepartmentId());
        }

        Section section = new Section();

        section.setName(request.getName());
        section.setDepartmentId(request.getDepartmentId());
        section.setStatus(Status.ACTIVE.getValue());

        sectionRepository.save(section);
    }

    public void update(SectionUpdateRequest request, int id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ môn có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            section.setName(request.getName());
        }

        if (request.getDepartmentId() != null) {
            DepartmentResponse department = departmentRepository.findByIdAndStatus(request.getDepartmentId(), Status.ACTIVE.getValue());

            if (department == null) {
                throw new IllegalArgumentException("Không tìm thấy khoa có id: " + request.getDepartmentId());
            }

            section.setDepartmentId(request.getDepartmentId());
        }

        sectionRepository.save(section);
    }

    public void deleteById(Integer id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ môn có id: " + id));

        section.setStatus(Status.INACTIVE.getValue());
        sectionRepository.save(section);
    }
}
