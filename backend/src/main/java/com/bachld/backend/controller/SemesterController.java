package com.bachld.backend.controller;

import com.bachld.backend.dto.request.SemesterCreateRequest;
import com.bachld.backend.dto.request.SemesterUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.SemesterService;
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
@RequestMapping("/api/semester")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SemesterController {

    SemesterService semesterService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), semesterService.getList(pageable, keyword, status)));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), semesterService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> create(@RequestBody @Valid SemesterCreateRequest request) {
        semesterService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> update(@RequestBody @Valid SemesterUpdateRequest request, @PathVariable int id) {
        semesterService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
