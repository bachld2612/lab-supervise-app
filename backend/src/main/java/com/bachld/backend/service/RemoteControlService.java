package com.bachld.backend.service;

import com.bachld.backend.dto.request.LockScreenExamRoomRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.dto.request.OpenWebsiteRequest;
import com.bachld.backend.dto.request.SendMessageRequest;
import com.bachld.backend.model.Student;
import com.bachld.backend.model.StudentClass;
import com.bachld.backend.model.StudentExamRoom;
import com.bachld.backend.repository.ClassRepository;
import com.bachld.backend.repository.ExamRoomRepository;
import com.bachld.backend.repository.StudentClassRepository;
import com.bachld.backend.repository.StudentExamRoomRepository;
import com.bachld.backend.repository.StudentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RemoteControlService {

    RemoteCommandService remoteCommandService;
    ClassRepository classRepository;
    ExamRoomRepository examRoomRepository;
    StudentClassRepository studentClassRepository;
    StudentExamRoomRepository studentExamRoomRepository;
    StudentRepository studentRepository;

    public void lockScreen(LockScreenRequest request) {
        classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay lop hoc co id: " + request.getClassId()));
        remoteCommandService.sendLockScreenCommand(request.getStudentUserId(), request.getActive());
    }

    public void openWebsiteForClass(Integer classId, OpenWebsiteRequest request) {
        List<StudentClass> studentClasses = studentClassRepository.findByClassId(classId);
        for (StudentClass studentClass : studentClasses) {
            Student student = studentRepository.findById(studentClass.getStudentId()).orElse(null);
            if (student != null) {
                remoteCommandService.sendOpenWebsiteCommand(student.getUserId(), request.getWebsiteUrl());
            }
        }
    }

    public void openWebsiteForStudent(Integer classId, Integer studentId, OpenWebsiteRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay sinh vien co id: " + studentId));
        remoteCommandService.sendOpenWebsiteCommand(student.getUserId(), request.getWebsiteUrl());
    }

    public void sendMessageForClass(Integer classId, SendMessageRequest request) {
        List<StudentClass> studentClasses = studentClassRepository.findByClassId(classId);
        for (StudentClass studentClass : studentClasses) {
            Student student = studentRepository.findById(studentClass.getStudentId()).orElse(null);
            if (student != null) {
                remoteCommandService.sendShowMessageCommand(student.getUserId(), request.getText());
            }
        }
    }

    public void sendMessageForStudent(Integer classId, Integer studentId, SendMessageRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay sinh vien co id: " + studentId));
        remoteCommandService.sendShowMessageCommand(student.getUserId(), request.getText());
    }

    public void lockScreenForExamRoom(LockScreenExamRoomRequest request) {
        examRoomRepository.findById(request.getExamRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phong thi co id: " + request.getExamRoomId()));
        remoteCommandService.sendLockScreenCommand(request.getStudentUserId(), request.getActive());
    }

    public void openWebsiteForExamRoom(Integer examRoomId, OpenWebsiteRequest request) {
        List<StudentExamRoom> enrolled = studentExamRoomRepository.findByExamRoomId(examRoomId);
        for (StudentExamRoom studentExamRoom : enrolled) {
            Student student = studentRepository.findById(studentExamRoom.getStudentId()).orElse(null);
            if (student != null) {
                remoteCommandService.sendOpenWebsiteCommand(student.getUserId(), request.getWebsiteUrl());
            }
        }
    }

    public void openWebsiteForExamRoomStudent(Integer examRoomId, Integer studentId, OpenWebsiteRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay sinh vien co id: " + studentId));
        remoteCommandService.sendOpenWebsiteCommand(student.getUserId(), request.getWebsiteUrl());
    }

    public void sendMessageForExamRoom(Integer examRoomId, SendMessageRequest request) {
        List<StudentExamRoom> enrolled = studentExamRoomRepository.findByExamRoomId(examRoomId);
        for (StudentExamRoom studentExamRoom : enrolled) {
            Student student = studentRepository.findById(studentExamRoom.getStudentId()).orElse(null);
            if (student != null) {
                remoteCommandService.sendShowMessageCommand(student.getUserId(), request.getText());
            }
        }
    }

    public void sendMessageForExamRoomStudent(Integer examRoomId, Integer studentId, SendMessageRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay sinh vien co id: " + studentId));
        remoteCommandService.sendShowMessageCommand(student.getUserId(), request.getText());
    }
}
