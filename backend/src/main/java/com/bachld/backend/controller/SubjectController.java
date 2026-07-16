package com.bachld.backend.controller;

import com.bachld.backend.dto.request.SubjectCreateRequest;
import com.bachld.backend.dto.request.SubjectUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.SubjectService;
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
@RequestMapping("/api/subject")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectController {

  SubjectService subjectService;

  @GetMapping("/v1")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> getList(
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), subjectService.getList(pageable, keyword, status)));
  }

  @GetMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> getById(@PathVariable int id) {
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), subjectService.getById(id)));
  }

  @PostMapping("/v1")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> create(@RequestBody @Valid SubjectCreateRequest request) {
    subjectService.create(request);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @PutMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> update(
      @RequestBody @Valid SubjectUpdateRequest request, @PathVariable int id) {
    subjectService.update(request, id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @DeleteMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN")
  public ResponseEntity<?> deleteById(@PathVariable int id) {
    subjectService.deleteById(id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }
}
