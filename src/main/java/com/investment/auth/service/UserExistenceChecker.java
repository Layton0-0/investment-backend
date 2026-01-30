package com.investment.auth.service;

import com.investment.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * JWT 인증 시 DB에 사용자 존재 여부를 검사하는 경량 서비스.
 * DB 초기화·삭제 후 오래된 JWT로 인증되는 것을 방지하기 위해 사용.
 */
@Service
@RequiredArgsConstructor
public class UserExistenceChecker {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 DB에 해당 사용자가 존재하는지 확인한다.
     *
     * @param userId 사용자 ID (UUID 문자열)
     * @return 존재하면 true, 없으면 false
     */
    public boolean exists(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return userRepository.existsById(userId);
    }
}
