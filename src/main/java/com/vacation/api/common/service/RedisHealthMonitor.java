package com.vacation.api.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 상태 모니터링 서비스
 * Redis 연결 상태를 주기적으로 체크하고 메트릭을 수집합니다.
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisHealthMonitor {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private final AtomicBoolean isRedisHealthy = new AtomicBoolean(true);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private LocalDateTime lastFailureTime;
    private LocalDateTime lastSuccessTime;

    /**
     * Redis 상태 체크 (1분마다 실행)
     */
    @Scheduled(fixedRate = 60000) // 1분
    public void checkRedisHealth() {
        if (redisTemplate == null) {
            if (isProduction()) {
                log.error("프로덕션 환경에서 Redis가 비활성화되어 있습니다!");
                isRedisHealthy.set(false);
            }
            return;
        }

        try {
            // PING 명령으로 Redis 연결 상태 확인
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            if ("PONG".equals(result)) {
                isRedisHealthy.set(true);
                successCount.incrementAndGet();
                lastSuccessTime = LocalDateTime.now();
                
                if (failureCount.get() > 0) {
                    log.info("Redis 연결 복구됨: failureCount={}, lastFailureTime={}", 
                            failureCount.get(), lastFailureTime);
                    failureCount.set(0);
                }
            } else {
                handleRedisFailure("Redis PING 응답이 예상과 다름: " + result);
            }
        } catch (Exception e) {
            handleRedisFailure("Redis 연결 실패: " + e.getMessage());
        }
    }

    /**
     * Redis 장애 처리
     */
    private void handleRedisFailure(String message) {
        isRedisHealthy.set(false);
        failureCount.incrementAndGet();
        lastFailureTime = LocalDateTime.now();
        
        if (isProduction()) {
            log.error("프로덕션 환경에서 Redis 장애 발생: {}", message);
            // 프로덕션에서는 알림 시스템 연동 가능
        } else {
            log.warn("Redis 장애 발생: {}", message);
        }
    }

    /**
     * Redis 상태 조회
     */
    public boolean isRedisHealthy() {
        if (redisTemplate == null) {
            return false;
        }
        return isRedisHealthy.get();
    }

    /**
     * Redis 메트릭 조회
     */
    public RedisMetrics getMetrics() {
        return RedisMetrics.builder()
                .healthy(isRedisHealthy.get())
                .failureCount(failureCount.get())
                .successCount(successCount.get())
                .lastFailureTime(lastFailureTime)
                .lastSuccessTime(lastSuccessTime)
                .build();
    }

    /**
     * 프로덕션 환경 여부
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    /**
     * Redis 메트릭 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class RedisMetrics {
        private boolean healthy;
        private long failureCount;
        private long successCount;
        private LocalDateTime lastFailureTime;
        private LocalDateTime lastSuccessTime;
    }
}
