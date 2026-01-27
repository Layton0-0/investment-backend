package com.investment.domain.repository;

import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 API 키 리포지토리
 */
@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, String> {
    
    /**
     * 사용자 ID로 모든 API 키 조회
     */
    List<UserApiKey> findByUserId(String userId);
    
    /**
     * 사용자 ID와 증권사 타입으로 API 키 조회
     */
    Optional<UserApiKey> findByUserIdAndBrokerType(String userId, BrokerType brokerType);
    
    /**
     * 사용자 ID로 API 키 존재 여부 확인
     */
    boolean existsByUserId(String userId);
}
