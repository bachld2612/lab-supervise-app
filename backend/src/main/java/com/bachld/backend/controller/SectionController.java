package com.bachld.backend.controller;

import com.bachld.backend.dto.request.SectionCreateRequest;
import com.bachld.backend.dto.request.SectionUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.SectionService;
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
@RequestMapping("/api/section")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SectionController {

    SectionService sectionService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), sectionService.getList(pageable, keyword, status)));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), sectionService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> create(@RequestBody @Valid SectionCreateRequest request) {
        sectionService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> update(@RequestBody @Valid SectionUpdateRequest request, @PathVariable int id) {
        sectionService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN")
    public ResponseEntity<?> delete(@PathVariable int id) {
        sectionService.deleteById(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}
