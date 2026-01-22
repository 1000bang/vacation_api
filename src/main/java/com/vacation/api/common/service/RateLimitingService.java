package com.vacation.api.common.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 서비스 (Redis 기반, 메모리 Fallback)
 * IP 주소별로 요청 제한을 관리합니다.
 * Redis가 실패하면 메모리 기반으로 동작합니다.
 *
 * @author vacation-api
 * @version 2.0
 * @since 2026-01-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisHealthMonitor redisHealthMonitor;
    private final MeterRegistry meterRegistry;

    // 메모리 기반 Fallback (Redis 실패 시 사용)
    private final Map<String, Bucket> memoryBuckets = new ConcurrentHashMap<>();
    
    private Counter rateLimitExceededCounter;

    /**
     * 메트릭 초기화
     */
    @PostConstruct
    public void initMetrics() {
        rateLimitExceededCounter = Counter.builder("rate_limit.exceeded.total")
            .description("Total number of rate limit exceeded")
            .register(meterRegistry);
    }

    /**
     * IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Rate Limit 체크 (Redis 기반 - Sliding Window Log 알고리즘)
     * localhost IP는 제외 (테스트용)
     *
     * @param request HTTP 요청
     * @param capacity 시간당 허용 요청 수
     * @param windowSeconds 시간 윈도우 (초)
     * @return 허용 여부
     */
    public boolean tryConsume(HttpServletRequest request, int capacity, int windowSeconds) {
        String clientIp = getClientIp(request);
        
        // localhost IP는 Rate limit 제외 (테스트용)
        if (isLocalhost(clientIp)) {
            log.debug("Localhost IP는 Rate limit 제외: IP={}", clientIp);
            return true;
        }
        
        // Redis가 없거나 비정상이면 메모리 기반 Fallback 사용
        boolean useMemoryFallback = (redisTemplate == null || !redisHealthMonitor.isRedisHealthy());
        
        if (useMemoryFallback) {
            return tryConsumeWithMemory(clientIp, capacity, windowSeconds);
        }
        
        try {
            String key = "rate_limit:" + clientIp;
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - (windowSeconds * 1000L);
            
            // Redis Lua 스크립트로 원자적 연산 수행
            String luaScript = 
                "local key = KEYS[1]\n" +
                "local windowStart = tonumber(ARGV[1])\n" +
                "local capacity = tonumber(ARGV[2])\n" +
                "local currentTime = tonumber(ARGV[3])\n" +
                "local windowSeconds = tonumber(ARGV[4])\n" +
                "\n" +
                "-- 오래된 항목 제거\n" +
                "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)\n" +
                "\n" +
                "-- 현재 요청 수 확인\n" +
                "local count = redis.call('ZCARD', key)\n" +
                "\n" +
                "if count < capacity then\n" +
                "    -- 요청 추가\n" +
                "    redis.call('ZADD', key, currentTime, currentTime)\n" +
                "    redis.call('EXPIRE', key, windowSeconds)\n" +
                "    return 1\n" +
                "else\n" +
                "    return 0\n" +
                "end";
            
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(luaScript);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, 
                Collections.singletonList(key),
                String.valueOf(windowStart),
                String.valueOf(capacity),
                String.valueOf(currentTime),
                String.valueOf(windowSeconds)
            );
            
            boolean allowed = result != null && result == 1;
            
            if (!allowed) {
                log.warn("Rate limit 초과: IP={}, capacity={}/{}초", clientIp, capacity, windowSeconds);
                rateLimitExceededCounter.increment();
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("Rate limit 체크 중 오류 발생: IP={}, error={}", clientIp, e.getMessage(), e);
            // 오류 발생 시 허용 (서비스 중단 방지)
            return true;
        }
    }

    /**
     * localhost IP 여부 확인
     *
     * @param ip IP 주소
     * @return localhost 여부
     */
    private boolean isLocalhost(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // IPv4 localhost
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            return true;
        }
        
        // IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        
        return false;
    }

    /**
     * 메모리 기반 Rate Limiting (Redis Fallback)
     *
     * @param clientIp 클라이언트 IP
     * @param capacity 허용 요청 수
     * @param windowSeconds 시간 윈도우 (초)
     * @return 허용 여부
     */
    private boolean tryConsumeWithMemory(String clientIp, int capacity, int windowSeconds) {
        log.debug("메모리 기반 Rate Limiting 사용 (Redis Fallback): IP={}", clientIp);
        
        Bucket bucket = memoryBuckets.computeIfAbsent(clientIp, key -> {
            Refill refill = Refill.intervally(capacity, Duration.ofSeconds(windowSeconds));
            Bandwidth limit = Bandwidth.classic(capacity, refill);
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });

        boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            log.warn("Rate limit 초과 (메모리 기반): IP={}, capacity={}/{}초", clientIp, capacity, windowSeconds);
            rateLimitExceededCounter.increment();
        }
        return consumed;
    }

    /**
     * IP별 버킷 제거 (Redis + 메모리 모두 삭제)
     *
     * @param ip IP 주소
     */
    public void removeBucket(String ip) {
        // Redis에서 삭제
        if (redisTemplate != null) {
            try {
                String key = "rate_limit:" + ip;
                Boolean deleted = redisTemplate.delete(key);
                if (Boolean.TRUE.equals(deleted)) {
                    log.debug("Rate limit 버킷 제거 완료 (Redis): IP={}", ip);
                }
            } catch (Exception e) {
                log.error("Rate limit 버킷 제거 중 오류 발생 (Redis): IP={}, error={}", ip, e.getMessage(), e);
            }
        }
        
        // 메모리에서도 삭제
        memoryBuckets.remove(ip);
        log.debug("Rate limit 버킷 제거 완료 (메모리): IP={}", ip);
    }
}
