package com.bachld.backend.controller;

import com.bachld.backend.dto.request.ImportVeyonKeyRequest;
import com.bachld.backend.dto.request.LockScreenRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.VeyonService;
import com.bachld.backend.util.auth.AuthFilter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VeyonController {

    VeyonService veyonService;

    @GetMapping("/api/v1/teacher/keys/public-key")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), veyonService.getPublicKey()));
    }

    @PostMapping("/api/v1/teacher/keys/import")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> importKey(@RequestBody @Valid ImportVeyonKeyRequest request) {
        veyonService.importKey(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PostMapping("/api/class/lock-screen")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> lockScreen(@RequestBody @Valid LockScreenRequest request) {
        veyonService.lockScreen(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @GetMapping("/api/class/screenshot")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> getScreenshot(@RequestParam Integer classId) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), veyonService.getScreenshot(classId)));
    }
}