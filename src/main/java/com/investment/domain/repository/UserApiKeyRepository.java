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
     * 동일 증권사에 모의/실거래 키가 각각 있으면 그중 하나만 반환됨.
     */
    Optional<UserApiKey> findByUserIdAndBrokerType(String userId, BrokerType brokerType);

    /**
     * 사용자 ID, 증권사 타입, 서버 타입으로 API 키 조회
     * 
     * @param serverType "1": 모의투자, "0": 실거래
     */
    Optional<UserApiKey> findByUserIdAndBrokerTypeAndServerType(String userId, BrokerType brokerType,
            String serverType);

    /**
     * 사용자 ID로 API 키 존재 여부 확인
     */
    boolean existsByUserId(String userId);
}
