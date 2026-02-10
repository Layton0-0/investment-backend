package com.investment.api.controller;

import com.investment.auth.dto.CreateAdminUserRequestDto;
import com.investment.auth.dto.CreateAdminUserResponseDto;
import com.investment.auth.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * 관리자(Admin) 생성 API. ADMIN 역할만 호출 가능.
 */
@Tag(name = "관리자", description = "관리자 계정 생성 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 계정 생성", description = "Admin 역할의 사용자를 생성합니다. ADMIN만 호출 가능.")
    public ResponseEntity<CreateAdminUserResponseDto> createAdminUser(
            @Valid @RequestBody CreateAdminUserRequestDto request) {
        CreateAdminUserResponseDto response = adminUserService.createAdminUser(request);
        return ResponseEntity.status(CREATED).body(response);
    }
}
