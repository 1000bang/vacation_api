package com.vacation.api.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * 서비스 상태를 확인하는 엔드포인트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    /**
     * Health Check 엔드포인트
     * 
     * @return 서비스 상태 정보
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        
        // 데이터베이스 연결 확인
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5); // 5초 타임아웃
            health.put("database", isValid ? "UP" : "DOWN");
            
            if (!isValid) {
                health.put("status", "DOWN");
                return ResponseEntity.status(503).body(health);
            }
        } catch (Exception e) {
            log.error("Health check 실패: 데이터베이스 연결 오류", e);
            health.put("database", "DOWN");
            health.put("status", "DOWN");
            health.put("error", "데이터베이스 연결 실패");
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * 간단한 Health Check (데이터베이스 체크 없음)
     * 
     * @return 서비스 상태
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
