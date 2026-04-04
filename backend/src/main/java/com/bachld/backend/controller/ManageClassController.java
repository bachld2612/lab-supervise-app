package com.bachld.backend.controller;

import com.bachld.backend.dto.request.ManageClassCreateRequest;
import com.bachld.backend.dto.request.ManageClassUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.ManageClassService;
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
@RequestMapping("/api/manage-class")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManageClassController {

    ManageClassService manageClassService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), manageClassService.getList(pageable, keyword, status)));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), manageClassService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> create(@RequestBody @Valid ManageClassCreateRequest request) {
        manageClassService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> update(@RequestBody @Valid ManageClassUpdateRequest request, @PathVariable int id) {
        manageClassService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> delete(@PathVariable int id) {
        manageClassService.deleteById(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
