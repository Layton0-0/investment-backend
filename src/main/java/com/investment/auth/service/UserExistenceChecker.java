package com.investment.auth.service;

import com.investment.domain.entity.User;
import com.investment.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * JWT 인증 시 DB에 사용자 존재 여부·조회를 담당하는 경량 서비스.
 * DB 초기화·삭제 후 오래된 JWT로 인증되는 것을 방지하고, 역할(role) 조회에 사용.
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

    /**
     * 사용자 ID로 User 엔티티를 조회한다. JWT 인증 시 역할(role) 반영용.
     *
     * @param userId 사용자 ID (UUID 문자열)
     * @return 사용자가 있으면 Optional에 담아 반환, 없으면 empty
     */
    public Optional<User> findUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }
}
