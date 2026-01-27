package com.investment.domain.repository;

import com.investment.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 리포지토리
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);
}
