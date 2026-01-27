package com.vacation.api.common.controller;

import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 모든 Controller의 공통 기능을 제공하는 Base Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
public abstract class BaseController {

    protected final TransactionIDCreator transactionIDCreator;

    protected BaseController(TransactionIDCreator transactionIDCreator) {
        this.transactionIDCreator = transactionIDCreator;
    }

    /**
     * TransactionId를 가져오거나 생성
     *
     * @return TransactionId
     */
    protected String getOrCreateTransactionId() {
        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }
        return transactionId;
    }

    /**
     * 성공 응답 생성 (200 OK)
     *
     * @param data 응답 데이터
     * @return ResponseEntity
     */
    protected <T> ResponseEntity<ApiResponse<T>> successResponse(T data) {
        String transactionId = getOrCreateTransactionId();
        return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", data, null));
    }

    /**
     * 생성 성공 응답 (201 Created)
     *
     * @param data 생성된 데이터
     * @return ResponseEntity
     */
    protected <T> ResponseEntity<ApiResponse<T>> createdResponse(T data) {
        String transactionId = getOrCreateTransactionId();
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(transactionId, "0", data, null));
    }

    /**
     * 에러 응답 생성 (일반 예외)
     *
     * @param errorMessage 에러 메시지
     * @param e 예외 객체
     * @return ResponseEntity
     */
    protected ResponseEntity<ApiResponse<Object>> errorResponse(String errorMessage, Exception e) {
        log.error(errorMessage, e);
        String transactionId = getOrCreateTransactionId();
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", "500");
        errorData.put("errorMessage", errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(transactionId, "500", errorData, null));
    }

    /**
     * 에러 응답 생성 (ApiException)
     *
     * @param errorMessage 에러 메시지
     * @param e ApiException 객체
     * @return ResponseEntity
     */
    protected ResponseEntity<ApiResponse<Object>> errorResponse(String errorMessage, ApiException e) {
        log.warn("{}: {}", errorMessage, e.getMessage());
        String transactionId = getOrCreateTransactionId();
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", e.getApiErrorCode().getCode());
        errorData.put("errorMessage", e.getMessage() != null ? e.getMessage() : errorMessage);
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getApiErrorCode() == ApiErrorCode.ACCESS_DENIED) {
            status = HttpStatus.FORBIDDEN;
        } else if (e.getApiErrorCode() == ApiErrorCode.USER_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else if (e.getApiErrorCode() == ApiErrorCode.INVALID_LOGIN) {
            status = HttpStatus.UNAUTHORIZED;
        }
        
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(transactionId, e.getApiErrorCode().getCode(), errorData, null));
    }

    /**
     * 에러 응답 생성 (에러 코드와 메시지 직접 지정)
     *
     * @param errorCode 에러 코드
     * @param errorMessage 에러 메시지
     * @return ResponseEntity
     */
    protected ResponseEntity<ApiResponse<Object>> errorResponse(String errorCode, String errorMessage) {
        log.error("에러 발생: {} - {}", errorCode, errorMessage);
        String transactionId = getOrCreateTransactionId();
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", errorCode);
        errorData.put("errorMessage", errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(transactionId, errorCode, errorData, null));
    }

    /**
     * 사용자 이름 조회 헬퍼 메서드
     * 
     * @param userId 사용자 ID
     * @param userService UserService 인스턴스
     * @return 사용자 이름 (없으면 null)
     */
    protected String getApplicantName(Long userId, UserService userService) {
        if (userId == null || userService == null) {
            return null;
        }
        try {
            User user = userService.getUserInfo(userId);
            return user != null ? user.getName() : null;
        } catch (Exception e) {
            log.warn("사용자 이름 조회 실패: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 공통 예외 처리 래퍼 (200 OK)
     *
     * @param errorMessage 에러 시 사용할 메시지
     * @param supplier 실행할 로직
     * @return ResponseEntity
     */
    @SuppressWarnings("unchecked")
    protected <T> ResponseEntity<ApiResponse<T>> executeWithErrorHandling(String errorMessage, Supplier<T> supplier) {
        try {
            return successResponse(supplier.get());
        } catch (ApiException e) {
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) errorResponse(errorMessage, e);
        } catch (Exception e) {
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) errorResponse(errorMessage, e);
        }
    }

    /**
     * 공통 예외 처리 래퍼 (201 Created)
     *
     * @param errorMessage 에러 시 사용할 메시지
     * @param supplier 실행할 로직
     * @return ResponseEntity
     */
    @SuppressWarnings("unchecked")
    protected <T> ResponseEntity<ApiResponse<T>> executeWithErrorHandlingCreated(String errorMessage, Supplier<T> supplier) {
        try {
            return createdResponse(supplier.get());
        } catch (ApiException e) {
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) errorResponse(errorMessage, e);
        } catch (Exception e) {
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) errorResponse(errorMessage, e);
        }
    }
}
