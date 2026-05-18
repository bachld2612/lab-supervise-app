package com.bachld.backend.controller;

import com.bachld.backend.dto.request.RoomCreateRequest;
import com.bachld.backend.dto.request.RoomUpdateRequest;
import com.bachld.backend.dto.response.BaseResponse;
import com.bachld.backend.service.RoomService;
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
@RequestMapping("/api/room")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;

    @GetMapping("/v1")
    @AuthFilter(role = "ADMIN,IT_CENTER,TEACHER")
    public ResponseEntity<?> getList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), roomService.getList(pageable, keyword, status)));
    }

    @GetMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN,IT_CENTER")
    public ResponseEntity<?> getById(@PathVariable int id) {
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), roomService.getById(id)));
    }

    @PostMapping("/v1")
    @AuthFilter(role = "ADMIN,IT_CENTER")
    public ResponseEntity<?> create(@RequestBody @Valid RoomCreateRequest request) {
        roomService.create(request);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @PutMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN,IT_CENTER")
    public ResponseEntity<?> update(@RequestBody @Valid RoomUpdateRequest request, @PathVariable int id) {
        roomService.update(request, id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }

    @DeleteMapping("/v1/{id}")
    @AuthFilter(role = "ADMIN,IT_CENTER")
    public ResponseEntity<?> delete(@PathVariable int id) {
        roomService.deleteById(id);
        return ResponseEntity.ok(new BaseResponse<>(HttpStatus.OK.value(), null));
    }
}