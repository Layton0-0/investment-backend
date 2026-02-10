package com.investment.auth.service;

import com.investment.auth.dto.CreateAdminUserRequestDto;
import com.investment.auth.dto.CreateAdminUserResponseDto;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.common.validation.PasswordValidator;
import com.investment.domain.entity.User;
import com.investment.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자(Admin) 생성 서비스. ADMIN 역할만 호출 가능한 API에서 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateAdminUserResponseDto createAdminUser(CreateAdminUserRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DomainException(ErrorCode.DUPLICATE_USERNAME, "이미 사용 중인 사용자 ID입니다");
        }
        PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validatePassword(request.getPassword());
        if (!passwordValidation.isValid()) {
            throw new DomainException(ErrorCode.INVALID_PASSWORD, passwordValidation.getMessage());
        }
        String role = request.getRole().trim();
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        user = userRepository.save(user);
        log.info("관리자 계정 생성: userId={}, username={}, role={}",
                LogMaskingUtil.maskUserId(user.getId()),
                LogMaskingUtil.maskUsername(user.getUsername()),
                role);
        if (log.isDebugEnabled()) {
            log.debug("  [DEBUG] userId(actual)={}, username(actual)={}", user.getId(), user.getUsername());
        }
        return CreateAdminUserResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
