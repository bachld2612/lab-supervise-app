package com.bachld.backend.controller;

import com.bachld.backend.dto.request.TeacherCreateRequest;
import com.bachld.backend.dto.request.TeacherUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.TeacherService;
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
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherController {

  TeacherService teacherService;

  @GetMapping("/v1")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> getList(
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), teacherService.getList(pageable, keyword, status)));
  }

  @GetMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> getById(@PathVariable int id) {
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), teacherService.getById(id)));
  }

  @PostMapping("/v1")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> create(@RequestBody @Valid TeacherCreateRequest request) {
    teacherService.create(request);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @PutMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> update(
      @PathVariable int id, @RequestBody @Valid TeacherUpdateRequest request) {
    teacherService.update(request, id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @DeleteMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> deleteById(@PathVariable int id) {
    teacherService.deleteById(id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }
}
