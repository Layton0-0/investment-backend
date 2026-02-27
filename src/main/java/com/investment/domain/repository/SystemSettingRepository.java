package com.investment.domain.repository;

import com.investment.domain.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 서버 전역 설정 리포지토리.
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

    List<SystemSetting> findAllByKeyIn(Iterable<String> keys);
}
