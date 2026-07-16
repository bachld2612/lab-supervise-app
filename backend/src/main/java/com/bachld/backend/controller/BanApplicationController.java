package com.bachld.backend.controller;

import com.bachld.backend.dto.request.BanApplicationCreateRequest;
import com.bachld.backend.dto.request.BanApplicationUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.BanApplicationService;
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
@RequestMapping("/api/ban-application")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BanApplicationController {

  BanApplicationService banApplicationService;

  @GetMapping("/v1")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> getList(
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), banApplicationService.getList(pageable, keyword, status)));
  }

  @GetMapping("/v1/{id}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> getById(@PathVariable int id) {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), banApplicationService.getById(id)));
  }

  @PostMapping("/v1")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> create(@RequestBody @Valid BanApplicationCreateRequest request) {
    banApplicationService.create(request);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @PutMapping("/v1/{id}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> update(
      @RequestBody BanApplicationUpdateRequest request, @PathVariable int id) {
    banApplicationService.update(request, id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @DeleteMapping("/v1/{id}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> delete(@PathVariable int id) {
    banApplicationService.delete(id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }
}
