package com.vacation.api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 서비스
 * IP 주소별로 요청 제한을 관리합니다.
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@Service
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

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
     * Rate Limit 체크
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
        
        Bucket bucket = buckets.computeIfAbsent(clientIp, key -> {
            Refill refill = Refill.intervally(capacity, Duration.ofSeconds(windowSeconds));
            Bandwidth limit = Bandwidth.classic(capacity, refill);
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });

        boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            log.warn("Rate limit 초과: IP={}, capacity={}/{}초", clientIp, capacity, windowSeconds);
        }
        return consumed;
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
     * IP별 버킷 제거 (메모리 관리용)
     */
    public void removeBucket(String ip) {
        buckets.remove(ip);
    }
}
