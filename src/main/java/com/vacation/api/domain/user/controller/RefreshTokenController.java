package com.vacation.api.domain.user.controller;

import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.request.RefreshTokenRequest;
import com.vacation.api.domain.user.response.RefreshTokenResponse;
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
 * Refresh Token Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final UserService userService;
    private final TransactionIDCreator transactionIDCreator;

    /**
     * Refresh Token으로 새로운 Access Token 발급
     *
     * @param refreshTokenRequest Refresh Token 요청 데이터
     * @return ApiResponse (새로운 Access Token 포함)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("Access Token 갱신 요청 수신");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // Refresh Token으로 새로운 Access Token 발급
            String newAccessToken = userService.refreshAccessToken(refreshTokenRequest.getRefreshToken());

            // 응답 데이터 생성
            RefreshTokenResponse refreshTokenResponse = RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .build();

            ApiResponse<Object> response = new ApiResponse<>(
                    transactionId,
                    "0",
                    refreshTokenResponse,
                    null
            );

            log.info("Access Token 갱신 성공");
            return ResponseEntity.ok().body(response);

        } catch (ApiException e) {
            log.warn("Access Token 갱신 실패: {}", e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", e.getApiErrorCode().getCode());
            errorData.put("errorMessage", e.getMessage());

            ApiResponse<Object> response = new ApiResponse<>(
                    transactionId,
                    e.getApiErrorCode().getCode(),
                    errorData,
                    null
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Access Token 갱신 중 예상치 못한 오류 발생", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "9999");
            errorData.put("errorMessage", "알 수 없는 오류가 발생했습니다.");

            ApiResponse<Object> response = new ApiResponse<>(
                    transactionId,
                    "9999",
                    errorData,
                    null
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

