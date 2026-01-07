package com.vacation.api.domain.user.controller;

import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관련 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TransactionIDCreator transactionIDCreator;

    /**
     * 회원가입 API
     *
     * @param joinRequest 회원가입 요청 데이터
     * @return ApiResponse
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinMember(@Valid @RequestBody JoinRequest joinRequest) {
        log.info("회원가입 요청 수신: email={}", joinRequest.getEmail());

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // 회원가입 처리
            User user = userService.join(joinRequest);

            // 응답 데이터 생성
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("userId", user.getUserId());
            resultData.put("email", user.getEmail());
            resultData.put("name", user.getName());
            resultData.put("status", user.getStatus().getValue());

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    transactionId,
                    "0",
                    resultData,
                    null
            );

            log.info("회원가입 성공: userId={}, email={}", user.getUserId(), user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ApiException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", e.getApiErrorCode().getCode());
            errorData.put("errorMessage", e.getMessage());

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    transactionId,
                    e.getApiErrorCode().getCode(),
                    errorData,
                    null
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("회원가입 중 예상치 못한 오류 발생", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "9999");
            errorData.put("errorMessage", "알 수 없는 오류가 발생했습니다.");

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    transactionId,
                    "9999",
                    errorData,
                    null
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
