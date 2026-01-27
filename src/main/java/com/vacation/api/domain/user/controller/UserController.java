package com.vacation.api.domain.user.controller;

import com.vacation.api.annotation.RateLimit;
import com.vacation.api.common.controller.BaseController;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.domain.user.request.RefreshTokenRequest;
import com.vacation.api.domain.user.request.UpdateUserRequest;
import com.vacation.api.domain.user.response.DivisionTeamResponse;
import com.vacation.api.domain.user.response.JoinResponse;
import com.vacation.api.domain.user.response.LoginResponse;
import com.vacation.api.domain.user.response.RefreshTokenResponse;
import com.vacation.api.domain.user.response.UserInfoResponse;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.domain.user.service.SignatureService;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final SignatureService signatureService;

    public UserController(UserService userService, SignatureService signatureService,
                         TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.userService = userService;
        this.signatureService = signatureService;
    }

    /**
     * 회원가입 API
     * Rate Limiting: IP당 1시간에 3회 제한
     *
     * @param joinRequest 회원가입 요청 데이터
     * @return ApiResponse
     */
    @PostMapping("/join")
    @RateLimit(capacity = 10, windowSeconds = 3600)
    public ResponseEntity<ApiResponse<Object>> joinMember(@Valid @RequestBody JoinRequest joinRequest) {
        log.info("회원가입 요청 수신: email={}", joinRequest.getEmail());

        try {
            User user = userService.join(joinRequest);
            JoinResponse res = JoinResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .status(user.getStatus() != null ? user.getStatus().name() : null)
                    .build();
            log.info("회원가입 성공: userId={}, email={}", user.getUserId(), user.getEmail());
            return createdResponse(res);

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
    @RateLimit(capacity = 10, windowSeconds = 3600)
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("로그인 요청 수신: email={}", loginRequest.getEmail());

        try {
            // 로그인 처리 (Access Token과 Refresh Token 생성)
            String[] tokens = userService.login(loginRequest);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            // 사용자 정보 조회
            User user = userService.findByEmail(loginRequest.getEmail())
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
     * 로그아웃 API
     *
     * @param request HTTP 요청
     * @return ApiResponse
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        log.info("로그아웃 요청 수신");

        try {
            Long userId = (Long) request.getAttribute("userId");
            userService.logout(userId);

            log.info("로그아웃 성공: userId={}", userId);
            return successResponse("로그아웃되었습니다.");

        } catch (Exception e) {
            log.error("로그아웃 실패", e);
            return errorResponse("로그아웃에 실패했습니다.", e);
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

    /**
     * 서명 생성/업로드 API
     *
     * @param request HTTP 요청
     * @param signatureImage 서명 이미지 파일 (PNG)
     * @param fontType 폰트 타입 (선택, 폰트 방식인 경우)
     * @param userName 사용자 이름 (선택, 폰트 방식인 경우)
     * @return 성공/실패 메시지
     */
    @PostMapping(value = "/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> uploadSignature(
            HttpServletRequest request,
            @RequestPart(value = "signatureImage", required = false) MultipartFile signatureImage,
            @RequestPart(value = "fontType", required = false) String fontType,
            @RequestPart(value = "userName", required = false) String userName) {
        log.info("서명 생성/업로드 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            Map<String, Object> responseData = signatureService.uploadSignature(userId, signatureImage, fontType, userName);
            return successResponse(responseData);
        } catch (ApiException e) {
            return errorResponse("서명 저장에 실패했습니다.", e);
        } catch (IOException e) {
            log.error("서명 저장 실패", e);
            return errorResponse("500", "서명 저장에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("서명 저장 실패", e);
            return errorResponse("서명 저장에 실패했습니다.", e);
        }
    }

    /**
     * 서명 미리보기 API (저장하지 않음)
     *
     * @param fontType 폰트 파일명
     * @param userName 사용자 이름
     * @return 서명 이미지 (PNG)
     */
    @GetMapping("/signature/preview")
    public ResponseEntity<byte[]> previewSignature(
            @RequestParam String fontType,
            @RequestParam String userName) {
        log.info("서명 미리보기 요청: fontType={}, userName={}", fontType, userName);

        try {
            byte[] imageBytes = signatureService.previewSignature(fontType, userName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);
        } catch (ApiException e) {
            log.warn("서명 미리보기 생성 실패: fontType={}, userName={}, error={}", fontType, userName, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("서명 미리보기 생성 실패: fontType={}, userName={}", fontType, userName, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("서명 미리보기 생성 실패: fontType={}, userName={}", fontType, userName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 서명 조회 API
     *
     * @param request HTTP 요청
     * @return 서명 파일 정보
     */
    @GetMapping("/signature")
    public ResponseEntity<ApiResponse<Object>> getSignature(HttpServletRequest request) {
        log.info("서명 조회 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            Map<String, Object> responseData = signatureService.getSignature(userId);
            return successResponse(responseData);
        } catch (ApiException e) {
            return errorResponse("서명 조회에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("서명 조회 실패", e);
            return errorResponse("서명 조회에 실패했습니다.", e);
        }
    }

    /**
     * 서명 삭제 API
     *
     * @param request HTTP 요청
     * @return 성공/실패 메시지
     */
    @DeleteMapping("/signature")
    public ResponseEntity<ApiResponse<Object>> deleteSignature(HttpServletRequest request) {
        log.info("서명 삭제 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            Map<String, Object> responseData = signatureService.deleteSignature(userId);
            return successResponse(responseData);
        } catch (ApiException e) {
            return errorResponse("서명 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("서명 삭제 실패", e);
            return errorResponse("서명 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 사용 가능한 폰트 목록 조회 API
     * Enum에 정의된 폰트 중 사용 가능한 것만 반환
     *
     * @return 폰트 목록
     */
    @GetMapping("/signature/fonts")
    public ResponseEntity<ApiResponse<Object>> getAvailableFonts() {
        log.info("사용 가능한 폰트 목록 조회 요청");

        try {
            List<Map<String, String>> availableFonts = signatureService.getAvailableFonts();
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("fonts", availableFonts);
            return successResponse(responseData);
        } catch (Exception e) {
            log.error("폰트 목록 조회 실패", e);
            return errorResponse("폰트 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 서명 파일 다운로드 엔드포인트
     *
     * @param request HTTP 요청
     * @param fileName 파일명 (예: "123_signature.png")
     * @return 서명 파일
     */
    @GetMapping("/download/signature/{fileName}")
    public ResponseEntity<Resource> downloadSignatureFile(
            HttpServletRequest request,
            @PathVariable String fileName) {
        log.info("서명 파일 다운로드 요청: fileName={}", fileName);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            Resource resource = signatureService.downloadSignatureFile(requesterId, fileName);

            // 파일명 인코딩
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (ApiException e) {
            if (e.getApiErrorCode() == ApiErrorCode.ACCESS_DENIED) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("서명 파일 다운로드 실패: fileName={}", fileName, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("서명 파일 다운로드 실패: fileName={}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 본부별 팀 목록 조회 API
     * 회원가입, 내 정보 수정, 팀 관리 화면에서 사용
     *
     * @return ApiResponse (본부별 팀 목록)
     */
    @GetMapping("/team/list")
    public ResponseEntity<ApiResponse<Object>> getDivisionTeamList() {
        log.info("본부별 팀 목록 조회 요청");

        try {
            List<DivisionTeamResponse> divisionTeamList = userService.getDivisionTeamList();
            return successResponse(divisionTeamList);
        } catch (Exception e) {
            log.error("본부별 팀 목록 조회 실패", e);
            return errorResponse("본부별 팀 목록 조회에 실패했습니다.", e);
        }
    }
}
