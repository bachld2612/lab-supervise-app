package com.bachld.backend.controller;

import com.bachld.backend.dto.request.LockScreenExamRoomRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.dto.request.OpenWebsiteRequest;
import com.bachld.backend.dto.request.SendMessageRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ClassService;
import com.bachld.backend.service.ExamRoomService;
import com.bachld.backend.service.RemoteControlService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/remote-control/v1")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RemoteControlController {

    static final String CLASS_NOT_ACTIVE = "Lop hoc hien khong hoat dong";
    static final String EXAM_NOT_ACTIVE = "Phong thi hien khong hoat dong";

    RemoteControlService remoteControlService;

    ClassService classService;

    ExamRoomService examRoomService;

    @PostMapping("/class/lock-screen")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> lockScreen(@RequestBody @Valid LockScreenRequest request) {
        if (classService.getStudyStatus(request.getClassId()) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        }
        remoteControlService.lockScreen(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/class/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForClass(@PathVariable Integer id, @RequestBody @Valid OpenWebsiteRequest request) {
        if (classService.getStudyStatus(id) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        }
        remoteControlService.openWebsiteForClass(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/class/{classId}/student/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForStudent(
            @PathVariable Integer classId,
            @PathVariable Integer id,
            @RequestBody @Valid OpenWebsiteRequest request
    ) {
        if (classService.getStudyStatus(classId) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        }
        remoteControlService.openWebsiteForStudent(classId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/class/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForClass(@PathVariable Integer id, @RequestBody @Valid SendMessageRequest request) {
        if (classService.getStudyStatus(id) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        }
        remoteControlService.sendMessageForClass(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/class/{classId}/student/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForStudent(
            @PathVariable Integer classId,
            @PathVariable Integer id,
            @RequestBody @Valid SendMessageRequest request
    ) {
        if (classService.getStudyStatus(classId) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        }
        remoteControlService.sendMessageForStudent(classId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/exam-room/lock-screen")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> lockScreenForExamRoom(@RequestBody @Valid LockScreenExamRoomRequest request) {
        if (examRoomService.getStudyStatus(request.getExamRoomId()) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        }
        remoteControlService.lockScreenForExamRoom(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/exam-room/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForExamRoom(@PathVariable Integer id, @RequestBody @Valid OpenWebsiteRequest request) {
        if (examRoomService.getStudyStatus(id) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        }
        remoteControlService.openWebsiteForExamRoom(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/exam-room/{examRoomId}/student/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForExamRoomStudent(
            @PathVariable Integer examRoomId,
            @PathVariable Integer id,
            @RequestBody @Valid OpenWebsiteRequest request
    ) {
        if (examRoomService.getStudyStatus(examRoomId) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        }
        remoteControlService.openWebsiteForExamRoomStudent(examRoomId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/exam-room/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForExamRoom(@PathVariable Integer id, @RequestBody @Valid SendMessageRequest request) {
        if (examRoomService.getStudyStatus(id) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        }
        remoteControlService.sendMessageForExamRoom(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/exam-room/{examRoomId}/student/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForExamRoomStudent(
            @PathVariable Integer examRoomId,
            @PathVariable Integer id,
            @RequestBody @Valid SendMessageRequest request
    ) {
        if (examRoomService.getStudyStatus(examRoomId) != 1) {
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        }
        remoteControlService.sendMessageForExamRoomStudent(examRoomId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
