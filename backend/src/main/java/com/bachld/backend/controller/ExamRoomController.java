package com.bachld.backend.controller;

import com.bachld.backend.dto.request.ClassWifiSsidRequest;
import com.bachld.backend.dto.request.ExamRoomCreateRequest;
import com.bachld.backend.dto.request.ExamRoomUpdateRequest;
import com.bachld.backend.dto.request.StudentExamRoomRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ExamRoomService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/exam-room")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamRoomController {

    ExamRoomService examRoomService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN,TEACHER,IT_CENTER")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer semesterId,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.getList(pageable, keyword, semesterId, status)));
    }

    @GetMapping("/v1/student")
    @AuthFilter(role = "STUDENT")
    public ResponseEntity<?> getStudentExamRooms() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), examRoomService.getStudentExamRooms()));
    }

    @GetMapping("/v1/teacher")
    @AuthFilter(role = "TEACHER,IT_CENTER")
    public ResponseEntity<?> getTeacherExamRooms() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), examRoomService.getTeacherExamRooms()));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN,TEACHER")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), examRoomService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> create(@RequestBody @Valid ExamRoomCreateRequest request) {
        examRoomService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> update(@RequestBody @Valid ExamRoomUpdateRequest request, @PathVariable int id) {
        examRoomService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> delete(@PathVariable int id) {
        examRoomService.delete(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/{examRoomId}/student/import")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> importStudents(
            @PathVariable int examRoomId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.importStudents(examRoomId, file)));
    }

    @GetMapping("/v1/{examRoomId}/student")
    @AuthFilter(role = "ADMIN,TEACHER")
    public ResponseEntity<?> getStudentsByExamRoomId(
            @PathVariable int examRoomId,
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.getStudentsByExamRoomId(examRoomId, pageable, keyword)));
    }

    @GetMapping("/v1/{examRoomId}/student/available")
    @AuthFilter(role = "ADMIN,TEACHER")
    public ResponseEntity<?> getStudentsNotInExamRoom(
            @PathVariable int examRoomId,
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.getStudentsNotInExamRoom(examRoomId, pageable, keyword)));
    }

    @PostMapping("/v1/{examRoomId}/student")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> addStudentsToExamRoom(@PathVariable int examRoomId, @RequestBody StudentExamRoomRequest request) {
        examRoomService.addStudentsToExamRoom(examRoomId, request.getStudentIds());
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{examRoomId}/student")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> removeStudentsFromExamRoom(@PathVariable int examRoomId, @RequestBody StudentExamRoomRequest request) {
        examRoomService.removeStudentsFromExamRoom(examRoomId, request.getStudentIds());
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/v1/{examRoomId}/tracking")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getTracking(@PathVariable int examRoomId) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.getTrackingByExamRoomId(examRoomId)));
    }

    @GetMapping("/v1/{examRoomId}/connected-students")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getConnectedStudents(@PathVariable int examRoomId) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.getConnectedStudents(examRoomId)));
    }

    @GetMapping("/v1/{examRoomId}/study-status")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getStudyStatus(@PathVariable int examRoomId) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                examRoomService.getStudyStatus(examRoomId)));
    }

    @PutMapping("/v1/{examRoomId}/tracking-enabled")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> setTrackingEnabled(
            @PathVariable int examRoomId,
            @RequestParam boolean enabled
    ) {
        examRoomService.setTrackingEnabled(examRoomId, enabled);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}/wifi-ssid")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> updateWifiSsid(@PathVariable int id, @RequestBody ClassWifiSsidRequest request) {
        examRoomService.updateWifiSsid(id, request.getWifiSsid());
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/{id}/wifi-ssid/generate")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> generateWifiSsid(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), examRoomService.generateWifiSsid(id)));
    }

}
