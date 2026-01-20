package com.vacation.api.domain.user.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.entity.UserSignature;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.repository.UserSignatureRepository;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.domain.user.request.RefreshTokenRequest;
import com.vacation.api.domain.user.request.UpdateUserRequest;
import com.vacation.api.domain.user.response.LoginResponse;
import com.vacation.api.domain.user.response.RefreshTokenResponse;
import com.vacation.api.domain.user.response.UserInfoResponse;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.enums.SignaturePlaceholder;
import com.vacation.api.enums.SignatureFont;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.util.SignatureFileUtil;
import com.vacation.api.util.SignatureImageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private final UserSignatureRepository userSignatureRepository;
    private final SignatureFileUtil signatureFileUtil;
    private final SignatureImageUtil signatureImageUtil;

    public UserController(UserService userService, UserRepository userRepository,
                         UserSignatureRepository userSignatureRepository,
                         SignatureFileUtil signatureFileUtil, SignatureImageUtil signatureImageUtil,
                         TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.userService = userService;
        this.userRepository = userRepository;
        this.userSignatureRepository = userSignatureRepository;
        this.signatureFileUtil = signatureFileUtil;
        this.signatureImageUtil = signatureImageUtil;
    }

    /**
     * 회원가입 API
     * Rate Limiting: IP당 1시간에 3회 제한
     *
     * @param joinRequest 회원가입 요청 데이터
     * @return ApiResponse
     */
    @PostMapping("/join")
    @com.vacation.api.annotation.RateLimit(capacity = 10, windowSeconds = 3600)
    public ResponseEntity<ApiResponse<Object>> joinMember(@Valid @RequestBody JoinRequest joinRequest) {
        log.info("회원가입 요청 수신: email={}", joinRequest.getEmail());

        try {
            // 회원가입 처리
            User user = userService.join(joinRequest);

            log.info("회원가입 성공: userId={}, email={}", user.getUserId(), user.getEmail());
            String transactionId = getOrCreateTransactionId();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", user, null));

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
    @com.vacation.api.annotation.RateLimit(capacity = 10, windowSeconds = 3600)
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
    @Transactional
    public ResponseEntity<ApiResponse<Object>> uploadSignature(
            HttpServletRequest request,
            @RequestPart(value = "signatureImage", required = false) MultipartFile signatureImage,
            @RequestPart(value = "fontType", required = false) String fontType,
            @RequestPart(value = "userName", required = false) String userName) {
        log.info("서명 생성/업로드 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.getUserInfo(userId);

            byte[] imageBytes;
            String fontNameToSave; // DB에 저장할 폰트명

            // 방법 1: 직접 업로드한 이미지 파일 사용 (캔버스 그리기)
            if (signatureImage != null && !signatureImage.isEmpty()) {
                // 파일 형식 검증 (PNG만 허용)
                String originalFilename = signatureImage.getOriginalFilename();
                if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".png")) {
                    return errorResponse("400", "PNG 파일만 업로드 가능합니다.");
                }

                // 파일 크기 제한 (5MB)
                long maxSize = 5 * 1024 * 1024; // 5MB
                if (signatureImage.getSize() > maxSize) {
                    return errorResponse("400", "파일 크기는 5MB를 초과할 수 없습니다.");
                }

                imageBytes = signatureImage.getBytes();
                fontNameToSave = "none"; // 캔버스 그리기는 "none"
            }
            // 방법 2: 폰트로 서명 생성
            else if (fontType != null && !fontType.trim().isEmpty() && userName != null && !userName.trim().isEmpty()) {
                // 사용자 이름이 제공되지 않으면 DB에서 가져오기
                String nameToUse = userName.trim().isEmpty() ? user.getName() : userName;
                
                // 서명 이미지 생성
                imageBytes = signatureImageUtil.generateSignatureImage(
                    nameToUse,
                    fontType,
                    SignaturePlaceholder.SignatureSize.SIG1
                );
                fontNameToSave = fontType; // 폰트명 저장
            } else {
                return errorResponse("400", "서명 이미지 파일 또는 폰트 타입과 사용자 이름을 제공해주세요.");
            }

            // 기존 서명 파일이 있으면 삭제
            if (signatureFileUtil.signatureFileExists(userId)) {
                signatureFileUtil.deleteSignatureFile(userId);
                log.info("기존 서명 파일 삭제: userId={}", userId);
            }

            // 서명 파일 저장
            String fileName = userId + "_signature.png";
            signatureFileUtil.saveSignatureFile(userId, imageBytes);

            // DB에 서명 정보 저장 (기존 레코드가 있으면 update, 없으면 insert)
            UserSignature userSignature = userSignatureRepository.findByUserSeq(userId)
                    .map(existing -> {
                        // 기존 레코드 업데이트
                        existing.setFileName(fileName);
                        existing.setFontName(fontNameToSave);
                        log.info("기존 서명 정보 업데이트: userId={}, fontName={}", userId, fontNameToSave);
                        return existing;
                    })
                    .orElseGet(() -> {
                        // 신규 레코드 생성
                        log.info("신규 서명 정보 생성: userId={}, fontName={}", userId, fontNameToSave);
                        return UserSignature.builder()
                                .userSeq(userId)
                                .fileName(fileName)
                                .fontName(fontNameToSave)
                                .build();
                    });
            
            userSignatureRepository.save(userSignature);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "서명이 저장되었습니다.");

            log.info("서명 저장 완료: userId={}, fontName={}", userId, fontNameToSave);
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
            // Enum에서 폰트 찾기
            SignatureFont font = SignatureFont.findByFileName(fontType);
            if (font == null || !"Y".equals(font.getUseYn())) {
                log.warn("사용할 수 없는 폰트: {}", fontType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("사용할 수 없는 폰트입니다.".getBytes(StandardCharsets.UTF_8));
            }

            // 폰트 파일 존재 여부 확인 (JAR 내부에서도 작동하도록 getInputStream() 시도)
            ClassPathResource fontResource = new ClassPathResource("fonts/" + font.getFileName());
            try (InputStream testStream = fontResource.getInputStream()) {
                // InputStream을 열 수 있으면 파일이 존재함
            } catch (IOException e) {
                log.warn("폰트 파일을 찾을 수 없음: {}", font.getFileName(), e);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("해당 폰트를 찾을 수 없습니다.".getBytes(StandardCharsets.UTF_8));
            }

            // 서명 이미지 생성 (저장하지 않음)
            byte[] imageBytes = signatureImageUtil.generateSignatureImage(
                userName.trim(),
                font.getFileName(),
                SignaturePlaceholder.SignatureSize.SIG1
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

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

            Map<String, Object> responseData = new HashMap<>();

            if (signatureFileUtil.signatureFileExists(userId)) {
                // 서명 파일 URL 생성 (다운로드 경로)
                // SignatureFileUtil을 사용하여 URL 경로 생성
                String signatureUrl = signatureFileUtil.getSignatureDownloadUrl(userId);
                
                responseData.put("signatureUrl", signatureUrl);
                responseData.put("hasSignature", true);
            } else {
                responseData.put("hasSignature", false);
            }

            return successResponse(responseData);

        } catch (ApiException e) {
            return errorResponse("서명 조회에 실패했습니다.", e);
        } catch (Exception e) {
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

            // 파일 삭제
            boolean deleted = signatureFileUtil.deleteSignatureFile(userId);
            
            // DB 레코드 삭제
            userSignatureRepository.deleteByUserSeq(userId);

            Map<String, Object> responseData = new HashMap<>();
            if (deleted) {
                responseData.put("message", "서명이 삭제되었습니다.");
            } else {
                responseData.put("message", "서명이 존재하지 않습니다.");
            }

            return successResponse(responseData);

        } catch (ApiException e) {
            return errorResponse("서명 삭제에 실패했습니다.", e);
        } catch (Exception e) {
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
            List<Map<String, String>> availableFonts = new ArrayList<>();
            
            // Enum에서 사용 가능한 폰트만 가져오기 (useYn이 'Y'인 것만)
            SignatureFont[] fonts = SignatureFont.getAvailableFonts();
            
            for (SignatureFont font : fonts) {
                Map<String, String> fontMap = new HashMap<>();
                fontMap.put("name", font.getDisplayName());
                fontMap.put("file", font.getFileName());
                availableFonts.add(fontMap);
            }
            
            log.info("사용 가능한 폰트 개수: {}", availableFonts.size());

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

            // 파일명에서 userId 추출
            if (!fileName.endsWith("_signature.png")) {
                log.warn("잘못된 파일명 형식: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            String userIdStr = fileName.replace("_signature.png", "");
            Long fileUserId;
            try {
                fileUserId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("잘못된 파일명 형식: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            // 권한 체크: 자신의 서명만 다운로드 가능
            if (!requesterId.equals(fileUserId)) {
                log.warn("서명 파일 다운로드 권한 없음: requesterId={}, fileUserId={}", requesterId, fileUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 서명 파일 존재 여부 확인
            if (!signatureFileUtil.signatureFileExists(fileUserId)) {
                log.warn("서명 파일이 없음: userId={}", fileUserId);
                return ResponseEntity.notFound().build();
            }

            // 서명 파일 로드
            Resource resource = signatureFileUtil.loadSignatureFileAsResource(fileUserId);

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

        } catch (IOException e) {
            log.error("서명 파일 다운로드 실패: fileName={}", fileName, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("서명 파일 다운로드 실패: fileName={}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
