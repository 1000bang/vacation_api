package com.vacation.api.common.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스 (Redis 기반, DB Fallback)
 * Redis가 실패하면 DB에 저장/조회하는 복구 전략 적용
 *
 * @author vacation-api
 * @version 2.0
 * @since 2026-01-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final RedisHealthMonitor redisHealthMonitor;
    private final MeterRegistry meterRegistry;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 밀리초

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    private Timer refreshTokenOperationTimer;

    /**
     * 메트릭 초기화
     */
    @PostConstruct
    public void initMetrics() {
        refreshTokenOperationTimer = Timer.builder("refresh_token.operation.duration")
            .description("Refresh token operation duration")
            .register(meterRegistry);
    }
    
    /**
     * Refresh Token 작업 카운터 생성 (태그 포함)
     */
    private Counter getRefreshTokenCounter(String operation) {
        return Counter.builder("refresh_token.operation.total")
            .description("Total refresh token operations")
            .tag("operation", operation)
            .register(meterRegistry);
    }

    /**
     * Refresh Token 저장 (Redis 우선, 실패 시 DB Fallback)
     *
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     */
    @Transactional
    public void saveRefreshToken(Long userId, String refreshToken) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            boolean redisSuccess = false;
            
            // 1. Redis에 저장 시도
            if (redisTemplate != null && redisHealthMonitor.isRedisHealthy()) {
                try {
                    String key = REFRESH_TOKEN_PREFIX + userId;
                    long ttlSeconds = refreshExpiration / 1000; // 밀리초를 초로 변환
                    
                    redisTemplate.opsForValue().set(key, refreshToken, ttlSeconds, TimeUnit.SECONDS);
                    redisSuccess = true;
                    log.debug("Refresh Token 저장 완료 (Redis): userId={}, TTL={}초", userId, ttlSeconds);
                } catch (Exception e) {
                    log.warn("Refresh Token 저장 실패 (Redis): userId={}, error={}, DB Fallback 시도", userId, e.getMessage());
                }
            }
            
            // 2. Redis 실패 시 DB에 저장 (Fallback)
            if (!redisSuccess) {
                try {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: userId=" + userId));
                    user.setRefreshToken(refreshToken);
                    userRepository.save(user);
                    log.info("Refresh Token 저장 완료 (DB Fallback): userId={}", userId);
                } catch (Exception e) {
                    log.error("Refresh Token 저장 실패 (DB Fallback): userId={}, error={}", userId, e.getMessage(), e);
                    if (isProduction()) {
                        throw new RuntimeException("프로덕션 환경에서 Refresh Token 저장 실패", e);
                    }
                }
            }
            
            getRefreshTokenCounter("save").increment();
        } finally {
            sample.stop(refreshTokenOperationTimer);
        }
    }

    /**
     * Refresh Token 조회 (Redis 우선, 실패 시 DB Fallback)
     *
     * @param userId 사용자 ID
     * @return Refresh Token (없으면 null)
     */
    public String getRefreshToken(Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // 1. Redis에서 조회 시도
            if (redisTemplate != null && redisHealthMonitor.isRedisHealthy()) {
                try {
                    String key = REFRESH_TOKEN_PREFIX + userId;
                    String refreshToken = redisTemplate.opsForValue().get(key);
                    
                    if (refreshToken != null) {
                        log.debug("Refresh Token 조회 성공 (Redis): userId={}", userId);
                        getRefreshTokenCounter("get").increment();
                        return refreshToken;
                    }
                } catch (Exception e) {
                    log.warn("Refresh Token 조회 실패 (Redis): userId={}, error={}, DB Fallback 시도", userId, e.getMessage());
                }
            }
            
            // 2. Redis 실패 또는 없으면 DB에서 조회 (Fallback)
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getRefreshToken() != null) {
                    log.info("Refresh Token 조회 성공 (DB Fallback): userId={}", userId);
                    getRefreshTokenCounter("get").increment();
                    return user.getRefreshToken();
                }
            } catch (Exception e) {
                log.error("Refresh Token 조회 실패 (DB Fallback): userId={}, error={}", userId, e.getMessage(), e);
            }
            
            log.debug("Refresh Token 조회 실패: userId={} (토큰이 존재하지 않음)", userId);
            return null;
        } finally {
            sample.stop(refreshTokenOperationTimer);
        }
    }

    /**
     * Refresh Token 검증
     *
     * @param userId 사용자 ID
     * @param refreshToken 검증할 Refresh Token
     * @return 일치 여부
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        
        if (storedToken == null) {
            log.warn("저장된 Refresh Token이 없음: userId={}", userId);
            return false;
        }
        
        boolean isValid = storedToken.equals(refreshToken);
        if (!isValid) {
            log.warn("Refresh Token 불일치: userId={}", userId);
        }
        
        return isValid;
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용, Redis + DB 모두 삭제)
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteRefreshToken(Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // 1. Redis에서 삭제
            if (redisTemplate != null) {
                try {
                    String key = REFRESH_TOKEN_PREFIX + userId;
                    Boolean deleted = redisTemplate.delete(key);
                    if (Boolean.TRUE.equals(deleted)) {
                        log.debug("Refresh Token 삭제 완료 (Redis): userId={}", userId);
                    }
                } catch (Exception e) {
                    log.warn("Refresh Token 삭제 실패 (Redis): userId={}, error={}", userId, e.getMessage());
                }
            }
            
            // 2. DB에서도 삭제 (항상 실행)
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getRefreshToken() != null) {
                    user.setRefreshToken(null);
                    userRepository.save(user);
                    log.info("Refresh Token 삭제 완료 (DB): userId={}", userId);
                }
            } catch (Exception e) {
                log.error("Refresh Token 삭제 실패 (DB): userId={}, error={}", userId, e.getMessage(), e);
            }
            
            getRefreshTokenCounter("delete").increment();
        } finally {
            sample.stop(refreshTokenOperationTimer);
        }
    }

    /**
     * Refresh Token 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    public boolean existsRefreshToken(Long userId) {
        if (redisTemplate == null) {
            log.warn("Redis가 비활성화되어 있습니다. Refresh Token 존재 여부를 확인할 수 없습니다: userId={}", userId);
            return false;
        }
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Refresh Token 존재 여부 확인 실패 (Redis 연결 오류): userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 프로덕션 환경 여부
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }
}
