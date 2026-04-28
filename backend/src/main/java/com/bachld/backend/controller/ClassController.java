package com.bachld.backend.controller;

import com.bachld.backend.dto.request.ClassCreateRequest;
import com.bachld.backend.dto.request.ClassUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ClassService;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassController {

    ClassService classService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), classService.getList(pageable, keyword, status)));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), classService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> create(@RequestBody @Valid ClassCreateRequest request) {
        classService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> update(@RequestBody @Valid ClassUpdateRequest request, @PathVariable int id) {
        classService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> deleteById(@PathVariable int id) {
        classService.delete(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/v1/student")
    @AuthFilter(role = "STUDENT")
    public ResponseEntity<?> getListByStudentUserId() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), classService.getListByStudentUserId()));
    }

    @GetMapping("/v1/teacher")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getListByTeacherUserId() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), classService.getListByTeacherUserId()));
    }

    @GetMapping("/v1/{classId}/tracking")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getTrackingByClassId(
            @PathVariable int classId
    ) {
        LocalDate targetDate = LocalDate.now();
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), classService.getTrackingByClassId(classId, targetDate)));
    }

    @GetMapping("/v1/{classId}/student")
    @AuthFilter(role = "TEACHER,ADMIN")
    public ResponseEntity<?> getStudentsByClassId(
            @PathVariable int classId,
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), classService.getStudentsByClassId(classId, pageable, keyword)));
    }
}
