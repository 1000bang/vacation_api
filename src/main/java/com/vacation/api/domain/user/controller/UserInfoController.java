package com.vacation.api.domain.user.controller;

import com.vacation.api.annotation.RequiresAuth;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.request.UpdateUserRequest;
import com.vacation.api.domain.user.response.UserInfoResponse;
import com.vacation.api.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 정보 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/user/info")
@RequiredArgsConstructor
public class UserInfoController {

    private final UserService userService;
    private final TransactionIDCreator transactionIDCreator;

    /**
     * 사용자 정보 조회
     *
     * @param request HTTP 요청
     * @return 사용자 정보
     */
    @GetMapping
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getUserInfo(HttpServletRequest request) {
        log.info("사용자 정보 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            var user = userService.getUserInfo(userId);
            UserInfoResponse response = UserInfoResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .division(user.getDivision())
                    .team(user.getTeam())
                    .position(user.getPosition())
                    .status(user.getStatus().name())
                    .firstLogin(user.getFirstLogin())
                    .joinDate(user.getJoinDate())
                    .authVal(user.getAuthVal())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "사용자 정보 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }


    /**
     * 사용자 정보 리스트 조회
     *
     * @param request HTTP 요청
     * @return 전체 사용자 정보
     */
    @GetMapping("/list")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getUserInfoList(HttpServletRequest request) {
        log.info("사용자 정보 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<User> user = userService.getUserInfoList(userId);

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", user, null));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "사용자 정보 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 사용자 정보 수정
     *
     * @param request HTTP 요청
     * @param updateRequest 수정 요청 데이터
     * @return 수정된 사용자 정보
     */
    @PutMapping
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> updateUserInfo(
            HttpServletRequest request,
            @Valid @RequestBody UpdateUserRequest updateRequest) {
        log.info("사용자 정보 수정 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            // TODO: 권한 체크 (특정 권한 이상인 자만 수정 가능)

            var user = userService.updateUserInfo(userId, updateRequest);
            UserInfoResponse response = UserInfoResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .division(user.getDivision())
                    .team(user.getTeam())
                    .position(user.getPosition())
                    .status(user.getStatus().name())
                    .firstLogin(user.getFirstLogin())
                    .joinDate(user.getJoinDate())
                    .authVal(user.getAuthVal())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("사용자 정보 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "사용자 정보 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 특정 사용자 정보 조회 (관리자용)
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/{userId}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getUserInfoByUserId(@PathVariable Long userId) {
        log.info("특정 사용자 정보 조회 요청: userId={}", userId);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // TODO: 권한 체크 (관리자만 조회 가능)
            var user = userService.getUserInfo(userId);
            UserInfoResponse response = UserInfoResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .division(user.getDivision())
                    .team(user.getTeam())
                    .position(user.getPosition())
                    .status(user.getStatus().name())
                    .firstLogin(user.getFirstLogin())
                    .joinDate(user.getJoinDate())
                    .authVal(user.getAuthVal())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "사용자 정보 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 특정 사용자 정보 수정 (관리자용)
     *
     * @param userId 사용자 ID
     * @param updateRequest 수정 요청 데이터
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{userId}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> updateUserInfoByUserId(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest updateRequest) {
        log.info("특정 사용자 정보 수정 요청: userId={}", userId);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // TODO: 권한 체크 (관리자만 수정 가능)
            var user = userService.updateUserInfo(userId, updateRequest);
            UserInfoResponse response = UserInfoResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .division(user.getDivision())
                    .team(user.getTeam())
                    .position(user.getPosition())
                    .status(user.getStatus().name())
                    .firstLogin(user.getFirstLogin())
                    .joinDate(user.getJoinDate())
                    .authVal(user.getAuthVal())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("사용자 정보 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "사용자 정보 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

}

