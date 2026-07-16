package com.bachld.backend.controller;

import com.bachld.backend.config.RsaKeyManager;
import com.bachld.backend.dto.response.BaseResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/security")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityController {

  RsaKeyManager rsaKeyManager;

  @GetMapping("/v1/public-key")
  public ResponseEntity<?> getPublicKey() {
    return ResponseEntity.ok(
        new BaseResponse<>(HttpStatus.OK.value(), rsaKeyManager.getPublicKeyBase64()));
  }
}
