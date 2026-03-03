package com.bachld.backend.util;

import com.bachld.backend.dto.response.StudentResponse;
import com.bachld.backend.model.Student;
import com.bachld.backend.repository.StudentRepository;
import com.bachld.backend.util.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Util {

    @Autowired
    private StudentRepository studentRepository;

    public void validateStudent(int id) {
        Optional<Student> student = studentRepository.findStudentByIdAndStatus(id, Status.ACTIVE.getValue());
        if (student.isEmpty()) {
            throw new IllegalArgumentException("Sinh viên có id: " + id + " không tồn tại hoặc đã bị xoá");
        }
    }
}
