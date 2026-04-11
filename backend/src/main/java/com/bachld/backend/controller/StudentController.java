package com.bachld.backend.controller;

import com.bachld.backend.dto.request.StudentCreateRequest;
import com.bachld.backend.dto.request.StudentUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.StudentService;
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

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentController {

    StudentService studentService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer manageClassId
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), studentService.getList(pageable, keyword, status, manageClassId)));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), studentService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> create(@RequestBody @Valid StudentCreateRequest request) {
        studentService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody @Valid StudentUpdateRequest request) {
        studentService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> delete(@PathVariable int id) {
        studentService.delete(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
