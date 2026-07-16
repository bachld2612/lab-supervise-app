package com.bachld.backend.controller;

import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ClassService;
import com.bachld.backend.service.FileShareService;
import com.bachld.backend.util.auth.AuthFilter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileShareController {

  FileShareService fileShareService;

  ClassService classService;

  @PostMapping("/api/class/{id}/send-file")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> sendFileToClass(
      @PathVariable Integer id, @RequestParam("file") MultipartFile file) {
    if (classService.getStudyStatus(id) != 1)
      return ResponseEntity.unprocessableEntity()
          .body(
              new BaseResponse<>(
                  HttpStatus.UNPROCESSABLE_ENTITY.value(), "Lớp học hiện không hoạt động"));
    fileShareService.sendFileToClass(id, file);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @PostMapping("/api/student/{id}/send-file")
  @AuthFilter(role = "TEACHER")
  public ResponseEntity<?> sendFileToStudent(
      @PathVariable Integer id, @RequestParam("file") MultipartFile file) {
    fileShareService.sendFileToStudent(id, file);
    return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
  }

  @GetMapping("/api/file-share/v1/{token}/download")
  @AuthFilter(role = "STUDENT")
  public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
    return fileShareService.downloadSharedFile(token);
  }
}
