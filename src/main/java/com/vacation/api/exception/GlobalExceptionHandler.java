package com.vacation.api.exception;

import lombok.extern.slf4j.Slf4j;
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
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("예외 발생", e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("errorCode", ApiErrorCode.UNKNOWN_ERROR.getCode());
        errorResponse.put("errorMessage", ApiErrorCode.UNKNOWN_ERROR.getDescription());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

