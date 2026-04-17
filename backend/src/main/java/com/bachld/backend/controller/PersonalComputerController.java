package com.bachld.backend.controller;

import com.bachld.backend.dto.request.PersonalComputerUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.dto.response.PersonalComputerResponse;
import com.bachld.backend.service.PersonalComputerService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personal-computer")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalComputerController {

    PersonalComputerService personalComputerService;

    @PostMapping("/v1/update")
    @AuthFilter(role = "TEACHER,STUDENT")
    public ResponseEntity<?> update(@RequestBody @Valid PersonalComputerUpdateRequest request) {
        personalComputerService.update(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/v1/me")
    @AuthFilter(role = "TEACHER,STUDENT")
    public ResponseEntity<?> getByUserId() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), personalComputerService.getByUserId()));
    }
}
