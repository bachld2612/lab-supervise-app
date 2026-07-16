package com.bachld.backend.controller;

import com.bachld.backend.config.ConnectedStudentRegistry;
import com.bachld.backend.dto.request.ClassCreateRequest;
import com.bachld.backend.dto.request.ClassUpdateRequest;
import com.bachld.backend.dto.request.ClassWifiSsidRequest;
import com.bachld.backend.dto.request.StudentClassRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ClassService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassController {

  ClassService classService;

  ConnectedStudentRegistry connectedStudentRegistry;

  @GetMapping("/v1/{classId}/connected-students")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> getConnectedStudents(@PathVariable int classId) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), connectedStudentRegistry.getConnectedStudents(classId)));
  }

  @GetMapping("/v1")
  @AuthFilter(role = "ADMIN,IT_CENTER")
  public ResponseEntity<?> getList(
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status) {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), classService.getList(pageable, keyword, status)));
  }

  @GetMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN,TEACHER")
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
  public ResponseEntity<?> update(
      @RequestBody @Valid ClassUpdateRequest request, @PathVariable int id) {
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
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), classService.getListByStudentUserId()));
  }

  @GetMapping("/v1/teacher")
  @AuthFilter(role = "TEACHER,IT_CENTER")
  public ResponseEntity<?> getListByTeacherUserId() {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), classService.getListByTeacherUserId()));
  }

  @GetMapping("/v1/{classId}/study-status")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> getStudyStatus(@PathVariable int classId) {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), classService.getStudyStatus(classId)));
  }

  @GetMapping("/v1/{classId}/tracking")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> getTrackingByClassId(@PathVariable int classId) {
    LocalDate targetDate = LocalDate.now();
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), classService.getTrackingByClassId(classId, targetDate)));
  }

  @PutMapping("/v1/{classId}/tracking-enabled")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> setTrackingEnabled(
      @PathVariable int classId, @RequestParam boolean enabled) {
    classService.setTrackingEnabled(classId, enabled);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @GetMapping("/v1/{classId}/student")
  @AuthFilter(role = "TEACHER,ADMIN")
  public ResponseEntity<?> getStudentsByClassId(
      @PathVariable int classId,
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), classService.getStudentsByClassId(classId, pageable, keyword)));
  }

  @GetMapping("/v1/{classId}/student/available")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> getStudentsNotInClass(
      @PathVariable int classId,
      @PageableDefault Pageable pageable,
      @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), classService.getStudentsNotInClass(classId, pageable, keyword)));
  }

  @PostMapping("/v1/{classId}/student")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> addStudentsToClass(
      @PathVariable int classId, @RequestBody StudentClassRequest request) {
    classService.addStudentsToClass(classId, request.getStudentIds());
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @DeleteMapping("/v1/{classId}/student")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> removeStudentsFromClass(
      @PathVariable int classId, @RequestBody StudentClassRequest request) {
    classService.removeStudentsFromClass(classId, request.getStudentIds());
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @GetMapping("v1/template/download")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<InputStreamResource> downloadClassStudentImportTemplate()
      throws IOException {
    return classService.downloadClassStudentImportTemplate();
  }

  @PutMapping("/v1/{id}/wifi-ssid")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> updateWifiSsid(
      @PathVariable int id, @RequestBody ClassWifiSsidRequest request) {
    classService.updateWifiSsid(id, request.getWifiSsid());
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @PostMapping("/v1/{id}/wifi-ssid/generate")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> generateWifiSsid(@PathVariable int id) {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), classService.generateWifiSsid(id)));
  }

  @PostMapping("/v1/{classId}/student/import")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> importStudentsToClass(
      @PathVariable int classId, @RequestParam("file") MultipartFile file) throws IOException {
    classService.importStudentsToClass(classId, file);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }
}
