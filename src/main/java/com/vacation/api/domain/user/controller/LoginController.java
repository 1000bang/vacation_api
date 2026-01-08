package com.vacation.api.domain.user.controller;

import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.domain.user.response.LoginResponse;
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
 * 로그인 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TransactionIDCreator transactionIDCreator;

    /**
     * 로그인 API
     *
     * @param loginRequest 로그인 요청 데이터
     * @return ApiResponse (JWT 토큰 포함)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("로그인 요청 수신: email={}", loginRequest.getEmail());

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // 로그인 처리 (Access Token과 Refresh Token 생성)
            String[] tokens = userService.login(loginRequest);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            // 사용자 정보 조회
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ApiException(com.vacation.api.exception.ApiErrorCode.INVALID_LOGIN));

            // 응답 데이터 생성
            LoginResponse loginResponse = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .division(user.getDivision())
                    .team(user.getTeam())
                    .position(user.getPosition())
                    .status(user.getStatus().name())
                    .firstLogin(user.getFirstLogin())
                    .authVal(user.getAuthVal())
                    .build();

            ApiResponse<Object> response = new ApiResponse<>(
                    transactionId,
                    "0",
                    loginResponse,
                    null
            );

            log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());
            return ResponseEntity.ok().body(response);

        } catch (ApiException e) {
            log.warn("로그인 실패: {}", e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", e.getApiErrorCode().getCode());
            errorData.put("errorMessage", e.getMessage());

            ApiResponse<Object> response = new ApiResponse<>(
                    transactionId,
                    e.getApiErrorCode().getCode(),
                    errorData,
                    null
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("로그인 중 예상치 못한 오류 발생", e);
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
