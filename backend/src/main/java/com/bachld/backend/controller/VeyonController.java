package com.bachld.backend.controller;

import com.bachld.backend.dto.request.ImportVeyonKeyRequest;
import com.bachld.backend.dto.request.LockScreenExamRoomRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.dto.request.OpenWebsiteRequest;
import com.bachld.backend.dto.request.SendMessageRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ClassService;
import com.bachld.backend.service.ExamRoomService;
import com.bachld.backend.service.VeyonService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/veyon")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VeyonController {

    VeyonService veyonService;
    ClassService classService;
    ExamRoomService examRoomService;

    private static final String CLASS_NOT_ACTIVE = "Lớp học hiện không hoạt động";
    private static final String EXAM_NOT_ACTIVE  = "Phòng thi hiện không hoạt động";

    @GetMapping("/v1/teacher/keys/public-key")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), veyonService.getPublicKey()));
    }

    @PostMapping("/v1/teacher/keys/import")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> importKey(@RequestBody @Valid ImportVeyonKeyRequest request) {
        veyonService.importKey(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/class/lock-screen")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> lockScreen(@RequestBody @Valid LockScreenRequest request) {
        if (classService.getStudyStatus(request.getClassId()) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        veyonService.lockScreen(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/v1/class/screenshot")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getScreenshot(@RequestParam Integer classId, @RequestParam Integer studentUserId) {
        if (classService.getStudyStatus(classId) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), veyonService.getScreenshot(classId, studentUserId)));
    }

    @PostMapping("/v1/class/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForClass(@PathVariable Integer id, @RequestBody @Valid OpenWebsiteRequest request) {
        if (classService.getStudyStatus(id) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        veyonService.openWebsiteForClass(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/class/{classId}/student/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForStudent(@PathVariable Integer id, @PathVariable Integer classId, @RequestBody @Valid OpenWebsiteRequest request) {
        if (classService.getStudyStatus(classId) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        veyonService.openWebsiteForStudent(classId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/class/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForClass(@PathVariable Integer id, @RequestBody @Valid SendMessageRequest request) {
        if (classService.getStudyStatus(id) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        veyonService.sendMessageForClass(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/class/{classId}/student/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForStudent(@PathVariable Integer id, @PathVariable Integer classId, @RequestBody @Valid SendMessageRequest request) {
        if (classService.getStudyStatus(classId) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
        veyonService.sendMessageForStudent(classId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    // ===== EXAM ROOM VEYON ENDPOINTS =====

    @PostMapping("/v1/exam-room/lock-screen")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> lockScreenForExamRoom(@RequestBody @Valid LockScreenExamRoomRequest request) {
        if (examRoomService.getStudyStatus(request.getExamRoomId()) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        veyonService.lockScreenForExamRoom(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/v1/exam-room/screenshot")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getScreenshotForExamRoom(@RequestParam Integer examRoomId, @RequestParam Integer studentUserId) {
        if (examRoomService.getStudyStatus(examRoomId) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), veyonService.getScreenshotForExamRoom(examRoomId, studentUserId)));
    }

    @PostMapping("/v1/exam-room/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForExamRoom(@PathVariable Integer id, @RequestBody @Valid OpenWebsiteRequest request) {
        if (examRoomService.getStudyStatus(id) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        veyonService.openWebsiteForExamRoom(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/exam-room/{examRoomId}/student/{id}/open-website")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> openWebsiteForExamRoomStudent(@PathVariable Integer id, @PathVariable Integer examRoomId, @RequestBody @Valid OpenWebsiteRequest request) {
        if (examRoomService.getStudyStatus(examRoomId) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        veyonService.openWebsiteForExamRoomStudent(examRoomId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/exam-room/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForExamRoom(@PathVariable Integer id, @RequestBody @Valid SendMessageRequest request) {
        if (examRoomService.getStudyStatus(id) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        veyonService.sendMessageForExamRoom(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/exam-room/{examRoomId}/student/{id}/text-message")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> sendMessageForExamRoomStudent(@PathVariable Integer id, @PathVariable Integer examRoomId, @RequestBody @Valid SendMessageRequest request) {
        if (examRoomService.getStudyStatus(examRoomId) != 1)
            return ResponseEntity.unprocessableEntity().body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
        veyonService.sendMessageForExamRoomStudent(examRoomId, id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}