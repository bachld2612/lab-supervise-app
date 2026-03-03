package com.bachld.backend.service;

import com.bachld.backend.dto.request.StudentCreateRequest;
import com.bachld.backend.dto.response.StudentResponse;
import com.bachld.backend.model.Student;
import com.bachld.backend.repository.StudentRepository;
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
public class StudentService {

    StudentRepository studentRepository;

    public void create(StudentCreateRequest request) {
        Student student = new Student();
        student.setFullName(request.getFullName());
        student.setPassword(request.getPassword());
        student.setHostname(request.getHostname());
        student.setUsername(request.getUsername());
        student.setStatus(Status.ACTIVE.getValue());
        studentRepository.save(student);
    }

    public StudentResponse findById(int id) {
        return studentRepository.findByIdAndStatus(id, Status.ACTIVE.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên có id: " + id + " không tồn tại hoặc đã bị xoá"));
    }

    public Page<StudentResponse> getList(Pageable pageable, String keyword) {
        if (keyword != null) {
            keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        else {
            keyword = "%%";
        }
        return studentRepository.findAllByKeyword(pageable, keyword, Status.ACTIVE.getValue());
    }

}
