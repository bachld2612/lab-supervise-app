package com.bachld.backend.service;

import com.bachld.backend.dto.request.ManageClassCreateRequest;
import com.bachld.backend.dto.request.ManageClassUpdateRequest;
import com.bachld.backend.dto.response.MajorResponse;
import com.bachld.backend.dto.response.ManageClassResponse;
import com.bachld.backend.dto.response.TeacherResponse;
import com.bachld.backend.model.ManageClass;
import com.bachld.backend.repository.MajorRepository;
import com.bachld.backend.repository.ManageClassRepository;
import com.bachld.backend.repository.TeacherRepository;
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
public class ManageClassService {

    ManageClassRepository manageClassRepository;

    MajorRepository majorRepository;

    TeacherRepository teacherRepository;

    public Page<ManageClassResponse> getList(Pageable pageable, String keyword, Integer status) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }

        return manageClassRepository.findByKeyword(pageable, keyword, status);
    }

    public ManageClassResponse getById(Integer id) {
        return manageClassRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
    }

    public void create(ManageClassCreateRequest request) {
        MajorResponse major = majorRepository.findByIdAndStatus(request.getMajorId(), Status.ACTIVE.getValue());
        if (major == null) {
            throw new IllegalArgumentException("Không tìm thấy chuyên ngành có id: " + request.getMajorId());
        }

        TeacherResponse teacher = teacherRepository.findTeacherByIdAndStatus(request.getTeacherId(), Status.ACTIVE.getValue());
        if (teacher == null) {
            throw new IllegalArgumentException("Không tìm thấy giảng viên có id: " + request.getTeacherId());
        }

        ManageClass manageClass = new ManageClass();

        manageClass.setName(request.getName());
        manageClass.setMaxStudent(request.getMaxStudent());
        manageClass.setTeacherId(request.getTeacherId());
        manageClass.setMajorId(request.getMajorId());
        manageClass.setStatus(Status.ACTIVE.getValue());

        manageClassRepository.save(manageClass);
    }

    public void update(ManageClassUpdateRequest request, int id) {
        ManageClass manageClass = manageClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp quản lý có id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            manageClass.setName(request.getName());
        }

        if (request.getMaxStudent() != null) {
            manageClass.setMaxStudent(request.getMaxStudent());
        }

        if (request.getTeacherId() != null) {
            TeacherResponse teacher = teacherRepository.findTeacherByIdAndStatus(request.getTeacherId(), Status.ACTIVE.getValue());

            if (teacher == null) {
                throw new IllegalArgumentException("Không tìm thấy giảng viên có id: " + request.getTeacherId());
            }

            manageClass.setTeacherId(request.getTeacherId());
        }

        if (request.getMajorId() != null) {
            MajorResponse major = majorRepository.findByIdAndStatus(request.getMajorId(), Status.ACTIVE.getValue());

            if (major == null) {
                throw new IllegalArgumentException("Không tìm thấy chuyên ngành có id: " + request.getMajorId());
            }

            manageClass.setMajorId(request.getMajorId());
        }

        manageClassRepository.save(manageClass);
    }

    public void deleteById(Integer id) {
        ManageClass manageClass = manageClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp quản lý có id: " + id));

        manageClass.setStatus(Status.INACTIVE.getValue());
        manageClassRepository.save(manageClass);
    }
}
