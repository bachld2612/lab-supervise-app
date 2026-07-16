package com.bachld.backend.controller;

import com.bachld.backend.dto.request.AllowedApplicationCreateRequest;
import com.bachld.backend.dto.request.AllowedApplicationUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.AllowedApplicationService;
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
@RequestMapping("/api/allowed-application")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AllowedApplicationController {

  AllowedApplicationService allowedApplicationService;

  @GetMapping("/v1")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> getList(
      @RequestParam Integer examRoomId,
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(),
            allowedApplicationService.getList(examRoomId, pageable, keyword)));
  }

  @PostMapping("/v1")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> create(@RequestBody @Valid AllowedApplicationCreateRequest request) {
    allowedApplicationService.create(request);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @PutMapping("/v1/{id}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> update(
      @RequestBody AllowedApplicationUpdateRequest request, @PathVariable int id) {
    allowedApplicationService.update(request, id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @DeleteMapping("/v1/{id}")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> delete(@PathVariable int id) {
    allowedApplicationService.delete(id);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }
}
