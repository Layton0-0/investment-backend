package com.investment.domain.repository;

import com.investment.domain.entity.TradingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradingSettingRepository extends JpaRepository<TradingSetting, String> {

    Optional<TradingSetting> findByAccountNo(String accountNo);

    /**
     * 사용자별 거래설정 목록 (리스크 리포트 등에서 사용자 계좌 범위 조회용).
     */
    List<TradingSetting> findByUserIdOrderByAccountNo(String userId);

    /**
     * 거래설정이 있는 계좌번호 목록 (자동투자 실행 대상).
     */
    @Query("SELECT DISTINCT t.accountNo FROM TradingSetting t ORDER BY t.accountNo")
    List<String> findDistinctAccountNos();

    /**
     * 자동투자 ON인 거래설정 목록 (모의·실 구분 없이 실행 대상).
     */
    List<TradingSetting> findAllByAutoTradingEnabledTrue();

    /**
     * 자동투자 ON + 로보 어드바이저 ON인 거래설정 목록 (로보 리밸런싱 스케줄러 대상).
     */
    List<TradingSetting> findAllByAutoTradingEnabledTrueAndRoboAdvisorEnabledTrue();
}
