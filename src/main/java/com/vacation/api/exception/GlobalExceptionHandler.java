package com.vacation.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 프로덕션 환경 여부 확인
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    /**
     * JSON 파싱 예외 처리 (Enum 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON 파싱 오류 발생", e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        
        // VacationType enum 파싱 에러인지 확인 : 901
        String message = e.getMessage();
        if (message != null && (message.contains("VacationType") || message.contains("not one of the values accepted for Enum"))) {
            errorResponse.put("errorCode", ApiErrorCode.INVALID_VACATION_TYPE.getCode());
            errorResponse.put("errorMessage", ApiErrorCode.INVALID_VACATION_TYPE.getDescription());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        // 기타 JSON 파싱 에러 : 900
        errorResponse.put("errorCode", ApiErrorCode.INVALID_REQUEST_FORMAT.getCode());
        errorResponse.put("errorMessage", ApiErrorCode.INVALID_REQUEST_FORMAT.getDescription());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation 오류 발생", e);
        // Validation 예외 : 902
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("errorCode", ApiErrorCode.VALIDATION_FAILED.getCode());
        errorResponse.put("errorMessage", ApiErrorCode.VALIDATION_FAILED.getDescription());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        e.getBindingResult().getGlobalErrors().forEach(error -> {
            errors.put(error.getObjectName(), error.getDefaultMessage());
        });
        errorResponse.put("errors", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * ApiException 처리
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
        log.warn("API 예외 발생: {}", e.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("errorCode", e.getApiErrorCode().getCode());
        errorResponse.put("errorMessage", e.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        // 인증 관련 에러는 401로 처리
        if (e.getApiErrorCode() == ApiErrorCode.INVALID_LOGIN) {
            status = HttpStatus.UNAUTHORIZED;
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 일반 예외 처리
     * 프로덕션 환경에서는 상세 에러 정보를 숨깁니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        // 프로덕션이 아닌 경우에만 전체 스택 트레이스 로깅
        if (isProduction()) {
            log.error("예외 발생: {}", e.getClass().getSimpleName());
            // 프로덕션에서는 민감한 정보를 로그에 남기지 않음
        } else {
            log.error("예외 발생", e);
        }
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("errorCode", ApiErrorCode.UNKNOWN_ERROR.getCode());
        
        // 프로덕션 환경에서는 일반적인 메시지만 반환
        if (isProduction()) {
            errorResponse.put("errorMessage", ApiErrorCode.UNKNOWN_ERROR.getDescription());
        } else {
            // 개발 환경에서는 상세 정보 포함 (선택사항)
            errorResponse.put("errorMessage", ApiErrorCode.UNKNOWN_ERROR.getDescription());
            errorResponse.put("errorType", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

