package com.vacation.api.exception;

import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.response.data.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 모든 에러 응답을 ApiResponse 형식으로 통일 (resultCode, resultMsg에 errorCode/errorMessage 포함)
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TransactionIDCreator transactionIDCreator;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private String getOrCreateTransactionId() {
        String tid = MDC.get("transactionId");
        if (tid == null) {
            tid = transactionIDCreator.createTransactionId();
        }
        return tid;
    }

    private ResponseEntity<ApiResponse<Object>> apiError(HttpStatus status, String resultCode, Map<String, Object> errorData) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(getOrCreateTransactionId(), resultCode, errorData, null));
    }

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
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON 파싱 오류 발생", e);
        Map<String, Object> errorData = new HashMap<>();
        String message = e.getMessage();
        if (message != null && (message.contains("VacationType") || message.contains("not one of the values accepted for Enum"))) {
            errorData.put("errorCode", ApiErrorCode.INVALID_VACATION_TYPE.getCode());
            errorData.put("errorMessage", ApiErrorCode.INVALID_VACATION_TYPE.getDescription());
            return apiError(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_VACATION_TYPE.getCode(), errorData);
        }
        errorData.put("errorCode", ApiErrorCode.INVALID_REQUEST_FORMAT.getCode());
        errorData.put("errorMessage", ApiErrorCode.INVALID_REQUEST_FORMAT.getDescription());
        return apiError(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_REQUEST_FORMAT.getCode(), errorData);
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation 오류 발생", e);
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", ApiErrorCode.VALIDATION_FAILED.getCode());
        errorData.put("errorMessage", ApiErrorCode.VALIDATION_FAILED.getDescription());
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors().forEach(err -> errors.put(err.getObjectName(), err.getDefaultMessage()));
        errorData.put("errors", errors);
        return apiError(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED.getCode(), errorData);
    }

    /**
     * 파일 업로드 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("파일 업로드 크기 초과: {}", e.getMessage());
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", ApiErrorCode.INVALID_REQUEST_FORMAT.getCode());
        errorData.put("errorMessage", "파일 크기는 10MB를 초과할 수 없습니다.");
        return apiError(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_REQUEST_FORMAT.getCode(), errorData);
    }

    /**
     * ApiException 처리
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException e) {
        log.warn("API 예외 발생: {}", e.getMessage());
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", e.getApiErrorCode().getCode());
        errorData.put("errorMessage", e.getMessage() != null ? e.getMessage() : e.getApiErrorCode().getDescription());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getApiErrorCode() == ApiErrorCode.INVALID_LOGIN) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (e.getApiErrorCode() == ApiErrorCode.ACCESS_DENIED) {
            status = HttpStatus.FORBIDDEN;
        } else if (e.getApiErrorCode() == ApiErrorCode.USER_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        }
        return apiError(status, e.getApiErrorCode().getCode(), errorData);
    }

    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        if (isProduction()) {
            log.error("예외 발생: {}", e.getClass().getSimpleName());
        } else {
            log.error("예외 발생", e);
        }
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", ApiErrorCode.UNKNOWN_ERROR.getCode());
        errorData.put("errorMessage", ApiErrorCode.UNKNOWN_ERROR.getDescription());
        if (!isProduction()) {
            errorData.put("errorType", e.getClass().getSimpleName());
        }
        return apiError(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNKNOWN_ERROR.getCode(), errorData);
    }
}

