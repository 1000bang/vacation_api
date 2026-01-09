package com.vacation.api.domain.user.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.domain.user.request.RefreshTokenRequest;
import com.vacation.api.domain.user.request.UpdateUserRequest;
import com.vacation.api.domain.user.response.LoginResponse;
import com.vacation.api.domain.user.response.RefreshTokenResponse;
import com.vacation.api.domain.user.response.UserInfoResponse;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 관련 Controller
 * 인증(회원가입, 로그인, 토큰 갱신) 및 사용자 정보 관리
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository, TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * 회원가입 API
     * Rate Limiting: IP당 1시간에 3회 제한
     *
     * @param joinRequest 회원가입 요청 데이터
     * @return ApiResponse
     */
    @PostMapping("/join")
    @com.vacation.api.annotation.RateLimit(capacity = 3, windowSeconds = 3600)
    public ResponseEntity<ApiResponse<Object>> joinMember(@Valid @RequestBody JoinRequest joinRequest) {
        log.info("회원가입 요청 수신: email={}", joinRequest.getEmail());

        try {
            // 회원가입 처리
            User user = userService.join(joinRequest);

            // 응답 데이터 생성
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("userId", user.getUserId());
            resultData.put("email", user.getEmail());
            resultData.put("name", user.getName());
            resultData.put("status", user.getStatus().getValue());

            log.info("회원가입 성공: userId={}, email={}", user.getUserId(), user.getEmail());
            String transactionId = getOrCreateTransactionId();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", resultData, null));

        } catch (ApiException e) {
            return errorResponse("회원가입에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("회원가입에 실패했습니다.", e);
        }
    }

    /**
     * 로그인 API
     * Rate Limiting: IP당 1시간에 5회 제한
     *
     * @param loginRequest 로그인 요청 데이터
     * @return ApiResponse (JWT 토큰 포함)
     */
    @PostMapping("/login")
    @com.vacation.api.annotation.RateLimit(capacity = 5, windowSeconds = 3600)
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("로그인 요청 수신: email={}", loginRequest.getEmail());

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

            log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());
            return successResponse(loginResponse);

        } catch (ApiException e) {
            return errorResponse("로그인에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("로그인에 실패했습니다.", e);
        }
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     *
     * @param refreshTokenRequest Refresh Token 요청 데이터
     * @return ApiResponse (새로운 Access Token 포함)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("Access Token 갱신 요청 수신");

        try {
            // Refresh Token으로 새로운 Access Token 발급
            String newAccessToken = userService.refreshAccessToken(refreshTokenRequest.getRefreshToken());

            // 응답 데이터 생성
            RefreshTokenResponse refreshTokenResponse = RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .build();

            log.info("Access Token 갱신 성공");
            return successResponse(refreshTokenResponse);

        } catch (ApiException e) {
            return errorResponse("Access Token 갱신에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("Access Token 갱신에 실패했습니다.", e);
        }
    }

    /**
     * 사용자 정보 조회
     *
     * @param request HTTP 요청
     * @return 사용자 정보
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Object>> getUserInfo(HttpServletRequest request) {
        log.info("사용자 정보 조회 요청");

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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("사용자 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("사용자 정보 조회에 실패했습니다.", e);
        }
    }

    /**
     * 사용자 정보 리스트 조회
     *
     * @param request HTTP 요청
     * @return 전체 사용자 정보
     */
    @GetMapping("/info/list")
    public ResponseEntity<ApiResponse<Object>> getUserInfoList(HttpServletRequest request) {
        log.info("사용자 정보 리스트 조회 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<User> user = userService.getUserInfoList(userId);

            return successResponse(user);
        } catch (ApiException e) {
            return errorResponse("사용자 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("사용자 정보 조회에 실패했습니다.", e);
        }
    }

    /**
     * 사용자 정보 수정
     *
     * @param request HTTP 요청
     * @param updateRequest 수정 요청 데이터
     * @return 수정된 사용자 정보
     */
    @PutMapping("/info")
    public ResponseEntity<ApiResponse<Object>> updateUserInfo(
            HttpServletRequest request,
            @Valid @RequestBody UpdateUserRequest updateRequest) {
        log.info("사용자 정보 수정 요청");

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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("사용자 정보 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("사용자 정보 수정에 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자 정보 조회 (관리자용)
     *
     * @param request HTTP 요청
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/info/{userId}")
    public ResponseEntity<ApiResponse<Object>> getUserInfoByUserId(
            HttpServletRequest request,
            @PathVariable Long userId) {
        log.info("특정 사용자 정보 조회 요청: userId={}", userId);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            
            // 권한 체크
            userService.checkUserAccessPermission(requesterId, userId);
            
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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("사용자 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("사용자 정보 조회에 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자 정보 수정 (관리자용)
     *
     * @param request HTTP 요청
     * @param userId 사용자 ID
     * @param updateRequest 수정 요청 데이터
     * @return 수정된 사용자 정보
     */
    @PutMapping("/info/{userId}")
    public ResponseEntity<ApiResponse<Object>> updateUserInfoByUserId(
            HttpServletRequest request,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest updateRequest) {
        log.info("특정 사용자 정보 수정 요청: userId={}", userId);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            
            // 권한 체크
            userService.checkUserAccessPermission(requesterId, userId);
            
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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("사용자 정보 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("사용자 정보 수정에 실패했습니다.", e);
        }
    }
}
