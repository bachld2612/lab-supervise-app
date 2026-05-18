package com.bachld.backend.controller;

import com.bachld.backend.dto.request.IncidentReportStudentCreateRequest;
import com.bachld.backend.dto.request.IncidentReportTeacherCreateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.IncidentReportService;
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
@RequestMapping("/api/incident-report")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IncidentReportController {

    IncidentReportService incidentReportService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN,IT_CENTER")
    public ResponseEntity<?> getListForItCenter(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer roomId
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                incidentReportService.getList(pageable, keyword, status, roomId)));
    }

    @GetMapping("/v1/teacher")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getListForTeacher(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer roomId
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                incidentReportService.getListForTeacher(pageable, keyword, status, roomId)));
    }

    @GetMapping("/v1/student")
    @AuthFilter(role = "STUDENT")
    public ResponseEntity<?> getListForStudent(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(),
                incidentReportService.getListForStudent(pageable, keyword, status)));
    }

    @PostMapping("/v1/student")
    @AuthFilter(role = "STUDENT")
    public ResponseEntity<?> createForStudent(@RequestBody @Valid IncidentReportStudentCreateRequest request) {
        incidentReportService.createForStudent(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/v1/teacher")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> createForTeacher(@RequestBody @Valid IncidentReportTeacherCreateRequest request) {
        incidentReportService.createForTeacher(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/student/{id}")
    @AuthFilter(role = "STUDENT")
    public ResponseEntity<?> updateForStudent(@PathVariable int id,
                                              @RequestBody @Valid IncidentReportStudentCreateRequest request) {
        incidentReportService.updateForStudent(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/teacher/{id}")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> updateForTeacher(@PathVariable int id,
                                              @RequestBody @Valid IncidentReportTeacherCreateRequest request) {
        incidentReportService.updateForTeacher(id, request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}/resolve")
    @AuthFilter(role = "IT_CENTER")
    public ResponseEntity<?> resolve(@PathVariable int id) {
        incidentReportService.resolve(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}/reject")
    @AuthFilter(role = "IT_CENTER")
    public ResponseEntity<?> reject(@PathVariable int id) {
        incidentReportService.reject(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}