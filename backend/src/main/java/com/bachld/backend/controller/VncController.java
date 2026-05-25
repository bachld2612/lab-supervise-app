package com.bachld.backend.controller;

import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.model.PersonalComputer;
import com.bachld.backend.repository.PersonalComputerRepository;
import com.bachld.backend.service.VncSessionService;
import com.bachld.backend.util.auth.AuthFilter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vnc")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VncController {

    VncSessionService vncSessionService;
    PersonalComputerRepository personalComputerRepository;

    @PostMapping("/v1/session/{classId}/{studentUserId}")
    @AuthFilter(role = "TEACHER")
    public ResponseEntity<?> createSession(
            @PathVariable int classId,
            @PathVariable int studentUserId) {

        PersonalComputer pc = personalComputerRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên chưa đăng ký máy tính"));

        String token = vncSessionService.createSession(pc.getIpAddress());

        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), Map.of(
                "token", token
        )));
    }
}
