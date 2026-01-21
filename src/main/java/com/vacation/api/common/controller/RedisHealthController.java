package com.vacation.api.common.controller;

import com.vacation.api.common.service.RedisHealthMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis 상태 모니터링 Controller
 * 운영팀이 Redis 상태를 확인할 수 있는 엔드포인트 제공
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-19
 */
@Slf4j
@RestController
@RequestMapping("/admin/redis")
@RequiredArgsConstructor
public class RedisHealthController {

    private final RedisHealthMonitor redisHealthMonitor;

    /**
     * Redis 상태 조회
     *
     * @return Redis 상태 및 메트릭
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getRedisHealth() {
        RedisHealthMonitor.RedisMetrics metrics = redisHealthMonitor.getMetrics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("healthy", metrics.isHealthy());
        response.put("failureCount", metrics.getFailureCount());
        response.put("successCount", metrics.getSuccessCount());
        response.put("lastFailureTime", metrics.getLastFailureTime());
        response.put("lastSuccessTime", metrics.getLastSuccessTime());
        response.put("status", metrics.isHealthy() ? "UP" : "DOWN");
        
        return ResponseEntity.ok(response);
    }
}
