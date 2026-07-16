package com.bachld.backend.controller;

import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ClassService;
import com.bachld.backend.service.ExamRoomService;
import com.bachld.backend.service.ScreenshotCaptureService;
import com.bachld.backend.util.auth.AuthFilter;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/screenshots")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScreenshotCaptureController {

  static final String CLASS_NOT_ACTIVE = "Lop hoc hien khong hoat dong";
  static final String EXAM_NOT_ACTIVE = "Phong thi hien khong hoat dong";

  ScreenshotCaptureService screenshotCaptureService;

  ClassService classService;

  ExamRoomService examRoomService;

  @PostMapping("/v1/class/{classId}/students/{studentUserId}/request")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> requestClassScreenshot(
      @PathVariable Integer classId, @PathVariable Integer studentUserId) {
    if (classService.getStudyStatus(classId) != 1) {
      return ResponseEntity.unprocessableEntity()
          .body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), CLASS_NOT_ACTIVE));
    }
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(),
            screenshotCaptureService.requestClassScreenshot(classId, studentUserId)));
  }

  @PostMapping("/v1/exam-room/{examRoomId}/students/{studentUserId}/request")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> requestExamRoomScreenshot(
      @PathVariable Integer examRoomId, @PathVariable Integer studentUserId) {
    if (examRoomService.getStudyStatus(examRoomId) != 1) {
      return ResponseEntity.unprocessableEntity()
          .body(new BaseResponse<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), EXAM_NOT_ACTIVE));
    }
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(),
            screenshotCaptureService.requestExamRoomScreenshot(examRoomId, studentUserId)));
  }

  @PostMapping("/v1/{id}/image")
  @AuthFilter(role = "STUDENT")
  public ResponseEntity<?> uploadImage(
      @PathVariable Integer id, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), screenshotCaptureService.uploadScreenshot(id, file)));
  }

  @GetMapping("/v1/{id}")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> getMetadata(@PathVariable Integer id) {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), screenshotCaptureService.getMetadata(id)));
  }

  @GetMapping("/v1/history")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> getHistory(
      @RequestParam(defaultValue = "CLASS") String contextType,
      @RequestParam(required = false) Integer contextId,
      @RequestParam(required = false) Integer studentId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      Pageable pageable) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(),
            screenshotCaptureService.getHistory(
                contextType, contextId, studentId, date, pageable)));
  }

  @GetMapping("/v1/history/contexts")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> getContextOptions(
      @RequestParam(defaultValue = "CLASS") String contextType) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(), screenshotCaptureService.getContextOptions(contextType)));
  }

  @GetMapping("/v1/history/students")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<?> getStudentOptions(
      @RequestParam(defaultValue = "CLASS") String contextType, @RequestParam Integer contextId) {
    return ResponseEntity.ok(
        new BaseResponse<>(
            HttpStatus.OK.value(),
            screenshotCaptureService.getStudentOptions(contextType, contextId)));
  }

  @GetMapping("/v1/{id}/image")
  @AuthFilter(role = "ADMIN,TEACHER")
  public ResponseEntity<Resource> getImage(@PathVariable Integer id) {
    Resource image = screenshotCaptureService.getImage(id);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
        .body(image);
  }
}
