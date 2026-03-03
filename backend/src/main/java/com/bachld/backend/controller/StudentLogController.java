package com.bachld.backend.controller;

import com.bachld.backend.dto.request.StudentLogCreateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.model.StudentLog;
import com.bachld.backend.service.StudentLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/student-log")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentLogController {

    StudentLogService studentLogService;

    @PostMapping("v1/list")
    public ResponseEntity<?> create(@RequestBody List<StudentLogCreateRequest> request){
        studentLogService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("v1/student/{id}")
    public ResponseEntity<?> getListByStudentId(@PageableDefault Pageable pageable,
                                                @PathVariable Integer id,
                                                @RequestParam(required = false) String keyword){
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), studentLogService.getListByStudentId(pageable, id, keyword)));
    }

}
