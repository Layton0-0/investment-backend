package com.investment.auth.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.LogMaskingUtil;
import com.investment.common.security.SecurityAuditService;
import com.investment.domain.entity.User;
import com.investment.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 계정 잠금 서비스
 * 
 * 인증 실패 시 계정을 일시적으로 잠금하여 무차별 대입 공격을 방지합니다.
 * - 5회 연속 실패 시 계정 잠금 (기본 15분)
 * - Redis 기반 분산 잠금 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final SecurityAuditService securityAuditService;
    
    @Value("${ACCOUNT_LOCK_ENABLED:true}")
    private boolean accountLockEnabled;
    
    @Value("${ACCOUNT_LOCK_MAX_ATTEMPTS:5}")
    private int maxAttempts;
    
    @Value("${ACCOUNT_LOCK_DURATION_MINUTES:15}")
    private int lockDurationMinutes;
    
    private static final String LOCK_KEY_PREFIX = "account_lock:";
    private static final String ATTEMPT_KEY_PREFIX = "auth_failure:";
    
    /**
     * 인증 실패 기록 및 계정 잠금 확인
     * 
     * @param username 사용자명
     * @return true: 계정이 잠금됨, false: 계정이 잠금되지 않음
     */
    public boolean recordFailureAndCheckLock(String username) {
        if (!accountLockEnabled) {
            return false;
        }
        
        String attemptKey = ATTEMPT_KEY_PREFIX + username;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        
        try {
            String currentAttempts = ops.get(attemptKey);
            int attempts = currentAttempts == null ? 0 : Integer.parseInt(currentAttempts);
            attempts++;
            
            // 실패 횟수 업데이트 (1시간 TTL)
            ops.set(attemptKey, String.valueOf(attempts), 1, TimeUnit.HOURS);
            
            // 최대 시도 횟수 초과 시 계정 잠금
            if (attempts >= maxAttempts) {
                lockAccount(username);
                return true;
            }
            
        } catch (Exception e) {
            log.error("계정 잠금 처리 오류: username={}", LogMaskingUtil.maskUsername(username), e);
            // 오류 발생 시 잠금하지 않음 (fail-open)
        }
        
        return false;
    }
    
    /**
     * 계정 잠금
     */
    private void lockAccount(String username) {
        String lockKey = LOCK_KEY_PREFIX + username;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        
        try {
            // 잠금 설정 (TTL: lockDurationMinutes)
            ops.set(lockKey, LocalDateTime.now().toString(), 
                    lockDurationMinutes, TimeUnit.MINUTES);
            
            // 사용자 엔티티에서도 잠금 정보 확인 가능하도록 (선택사항)
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                securityAuditService.logAccountLocked(
                        user.getId(), 
                        user.getUsername(), 
                        String.format("%d회 연속 인증 실패", maxAttempts)
                );
                log.warn("계정 잠금: username={}, duration={}분", 
                        LogMaskingUtil.maskUsername(username), lockDurationMinutes);
            }
            
        } catch (Exception e) {
            log.error("계정 잠금 설정 오류: username={}", LogMaskingUtil.maskUsername(username), e);
        }
    }
    
    /**
     * 계정 잠금 여부 확인
     * 
     * @param username 사용자명
     * @return true: 계정이 잠금됨, false: 계정이 잠금되지 않음
     */
    public boolean isAccountLocked(String username) {
        if (!accountLockEnabled) {
            return false;
        }
        
        String lockKey = LOCK_KEY_PREFIX + username;
        try {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            String lockTime = ops.get(lockKey);
            return lockTime != null;
        } catch (Exception e) {
            log.error("계정 잠금 확인 오류: username={}", LogMaskingUtil.maskUsername(username), e);
            return false; // 오류 발생 시 잠금하지 않은 것으로 간주 (fail-open)
        }
    }
    
    /**
     * 계정 잠금 해제 (인증 성공 시)
     */
    public void unlockAccount(String username) {
        if (!accountLockEnabled) {
            return;
        }
        
        String lockKey = LOCK_KEY_PREFIX + username;
        String attemptKey = ATTEMPT_KEY_PREFIX + username;
        
        try {
            redisTemplate.delete(lockKey);
            redisTemplate.delete(attemptKey);
            
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                securityAuditService.logAccountUnlocked(user.getId(), user.getUsername());
            }
            
        } catch (Exception e) {
            log.error("계정 잠금 해제 오류: username={}", LogMaskingUtil.maskUsername(username), e);
        }
    }
    
    /**
     * 계정 잠금 예외 발생
     */
    public void throwIfLocked(String username) {
        if (isAccountLocked(username)) {
            throw new DomainException(
                    ErrorCode.UNAUTHORIZED, 
                    String.format("계정이 잠금되었습니다. %d분 후 다시 시도해주세요.", lockDurationMinutes)
            );
        }
    }
}
