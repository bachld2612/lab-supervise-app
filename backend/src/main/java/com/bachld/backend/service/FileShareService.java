package com.bachld.backend.service;

import com.bachld.backend.dto.response.FileSharePayload;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.StudentClass;
import com.bachld.backend.repository.StudentClassRepository;
import com.bachld.backend.repository.StudentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileShareService {

    StudentClassRepository studentClassRepository;
    StudentRepository studentRepository;
    SimpMessagingTemplate messagingTemplate;

    public void sendFileToClass(Integer classId, MultipartFile file) {
        List<StudentClass> studentClasses = studentClassRepository.findByClassId(classId);

        String base64Content;
        try {
            base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file: " + e.getMessage());
        }

        FileSharePayload payload = FileSharePayload.builder()
                .fileName(file.getOriginalFilename())
                .fileContentBase64(base64Content)
                .fileSize(file.getSize())
                .build();

        for (StudentClass sc : studentClasses) {
            Student student = studentRepository.findById(sc.getStudentId()).orElse(null);
            if (student == null) continue;
            messagingTemplate.convertAndSend("/topic/user/" + student.getUserId() + "/file", payload);
        }
    }

    public void sendFileToStudent(Integer studentId, MultipartFile file) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có id: " + studentId));

        String base64Content;
        try {
            base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file: " + e.getMessage());
        }

        FileSharePayload payload = FileSharePayload.builder()
                .fileName(file.getOriginalFilename())
                .fileContentBase64(base64Content)
                .fileSize(file.getSize())
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + student.getUserId() + "/file", payload);
    }
}