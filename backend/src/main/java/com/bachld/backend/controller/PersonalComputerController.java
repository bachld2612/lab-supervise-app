package com.bachld.backend.controller;

import com.bachld.backend.dto.request.PersonalComputerUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.PersonalComputerService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personal-computer")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalComputerController {

    PersonalComputerService personalComputerService;

    @PostMapping("/v1/update")
    @AuthFilter(role = "IT_CENTER,TEACHER")
    public ResponseEntity<?> update(@RequestBody @Valid PersonalComputerUpdateRequest request) {
        personalComputerService.update(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/v1/me")
    @AuthFilter(role = "TEACHER,STUDENT")
    public ResponseEntity<?> getByUserId() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), personalComputerService.getByUserId()));
    }

    @GetMapping("/v1/by-class/{classId}")
    @AuthFilter(role = "TEACHER,IT_CENTER")
    public ResponseEntity<?> getStudentsByClassId(@PathVariable Integer classId) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), personalComputerService.getStudentsByClassId(classId)));
    }

    @GetMapping("/v1/by-exam-room/{examRoomId}")
    @AuthFilter(role = "TEACHER,IT_CENTER")
    public ResponseEntity<?> getStudentsByExamRoomId(@PathVariable Integer examRoomId) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), personalComputerService.getStudentsByExamRoomId(examRoomId)));
    }

    @PutMapping("/v1/student/{userId}")
    @AuthFilter(role = "TEACHER,IT_CENTER")
    public ResponseEntity<?> updateStudentPc(@PathVariable Integer userId, @RequestBody @Valid PersonalComputerUpdateRequest request) {
        personalComputerService.updateStudentPcByUserId(userId, request.getIpAddress());
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
