package com.investment.domain.repository;

import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 계좌 리포지토리
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

        /**
         * 사용자 ID로 모든 계좌 조회 (활성화된 계좌만)
         */
        List<UserAccount> findByUserIdAndIsActiveTrue(String userId);

        /**
         * 사용자 ID로 모든 계좌 조회 (활성화 여부 무관)
         */
        List<UserAccount> findByUserId(String userId);

        /**
         * 사용자 ID와 증권사 타입으로 계좌 목록 조회
         */
        List<UserAccount> findByUserIdAndBrokerType(String userId, BrokerType brokerType);

        /**
         * 사용자의 메인 계좌 조회
         */
        Optional<UserAccount> findByUserIdAndIsDefaultTrue(String userId);

        /**
         * 사용자 ID와 서버 타입으로 활성 계좌 목록 조회
         */
        List<UserAccount> findByUserIdAndServerTypeAndIsActiveTrue(String userId, String serverType);

        /**
         * 사용자 ID와 서버 타입으로 메인 계좌 조회
         */
        Optional<UserAccount> findByUserIdAndServerTypeAndIsDefaultTrue(String userId, String serverType);

        /**
         * 사용자 ID, 증권사 타입, 서버 타입으로 계좌 목록 조회
         */
        List<UserAccount> findByUserIdAndBrokerTypeAndServerType(String userId, BrokerType brokerType,
                        String serverType);

        /**
         * 사용자 ID와 계좌 ID로 계좌 조회
         */
        Optional<UserAccount> findByUserIdAndId(String userId, String accountId);

        /**
         * API 키 ID로 계좌 목록 조회
         */
        List<UserAccount> findByUserApiKeyId(String userApiKeyId);

        /**
         * 사용자 ID로 메인 계좌 존재 여부 확인
         */
        boolean existsByUserIdAndIsDefaultTrue(String userId);

        /**
         * 사용자 ID와 서버 타입으로 메인 계좌 존재 여부 확인
         */
        boolean existsByUserIdAndServerTypeAndIsDefaultTrue(String userId, String serverType);

        /**
         * 사용자의 모든 메인 계좌 해제 (메인 계좌 변경 시 사용)
         */
        @Modifying
        @Query("UPDATE UserAccount ua SET ua.isDefault = false WHERE ua.userId = :userId")
        void unsetAllDefaultAccounts(@Param("userId") String userId);

        /**
         * 사용자 ID와 서버 타입에 해당하는 메인 계좌 해제 (서버 타입별 메인 계좌 변경 시 사용)
         */
        @Modifying
        @Query("UPDATE UserAccount ua SET ua.isDefault = false WHERE ua.userId = :userId AND ua.serverType = :serverType")
        void unsetDefaultAccountsForUserAndServerType(@Param("userId") String userId,
                        @Param("serverType") String serverType);

        /**
         * 사용자 ID와 암호화된 계좌번호, 증권사 타입, 서버 타입으로 계좌 조회 (중복 확인용)
         */
        Optional<UserAccount> findByUserIdAndAccountNoEncryptedAndBrokerTypeAndServerType(
                        String userId, String accountNoEncrypted, BrokerType brokerType, String serverType);

        /**
         * 사용자 ID와 암호화된 계좌번호, 증권사 타입으로 계좌 조회 (중복 확인용, 하위 호환)
         */
        Optional<UserAccount> findByUserIdAndAccountNoEncryptedAndBrokerType(
                        String userId, String accountNoEncrypted, BrokerType brokerType);
}
