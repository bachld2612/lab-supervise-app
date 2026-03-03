package com.bachld.backend.controller;

import com.bachld.backend.dto.request.StudentCreateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.StudentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentController {

    StudentService studentService;

    @PostMapping("v1")
    public ResponseEntity<?> create(@RequestBody StudentCreateRequest request) {
        studentService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("v1/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), studentService.findById(id)));
    }

    @GetMapping("/v1")
    public ResponseEntity<?> getList(@PageableDefault Pageable pageable,
                                     @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), studentService.getList(pageable, keyword)));
    }

}
