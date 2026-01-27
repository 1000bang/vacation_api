package com.vacation.api.domain.user.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.entity.UserSignature;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.repository.UserSignatureRepository;
import com.vacation.api.enums.SignatureFont;
import com.vacation.api.enums.SignaturePlaceholder;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.util.SignatureFileUtil;
import com.vacation.api.util.SignatureImageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 서명 관련 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    private static final long MAX_SIGNATURE_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final UserRepository userRepository;
    private final UserSignatureRepository userSignatureRepository;
    private final SignatureFileUtil signatureFileUtil;
    private final SignatureImageUtil signatureImageUtil;

    /**
     * 서명 생성/업로드
     *
     * @param userId 사용자 ID
     * @param signatureImage 서명 이미지 파일 (PNG, 선택)
     * @param fontType 폰트 타입 (선택, 폰트 방식인 경우)
     * @param userName 사용자 이름 (선택, 폰트 방식인 경우)
     * @return 응답 데이터
     * @throws ApiException 검증 실패 시
     * @throws IOException 파일 처리 실패 시
     */
    @Transactional
    public Map<String, Object> uploadSignature(Long userId, MultipartFile signatureImage, 
                                                 String fontType, String userName) throws IOException {
        log.info("서명 생성/업로드: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        byte[] imageBytes;
        String fontNameToSave;

        // 방법 1: 직접 업로드한 이미지 파일 사용
        if (signatureImage != null && !signatureImage.isEmpty()) {
            validateSignatureImage(signatureImage);
            imageBytes = signatureImage.getBytes();
            fontNameToSave = "none";
        }
        // 방법 2: 폰트로 서명 생성
        else if (fontType != null && !fontType.trim().isEmpty() && userName != null && !userName.trim().isEmpty()) {
            String nameToUse = userName.trim().isEmpty() ? user.getName() : userName.trim();
            imageBytes = signatureImageUtil.generateSignatureImage(
                    nameToUse,
                    fontType,
                    SignaturePlaceholder.SignatureSize.SIG1
            );
            fontNameToSave = fontType;
        } else {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, 
                    "서명 이미지 파일 또는 폰트 타입과 사용자 이름을 제공해주세요.");
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
                    existing.setFileName(fileName);
                    existing.setFontName(fontNameToSave);
                    log.info("기존 서명 정보 업데이트: userId={}, fontName={}", userId, fontNameToSave);
                    return existing;
                })
                .orElseGet(() -> {
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
        return responseData;
    }

    /**
     * 서명 이미지 파일 검증
     *
     * @param signatureImage 서명 이미지 파일
     * @throws ApiException 검증 실패 시
     */
    private void validateSignatureImage(MultipartFile signatureImage) {
        // 파일 형식 검증 (PNG만 허용)
        String originalFilename = signatureImage.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".png")) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "PNG 파일만 업로드 가능합니다.");
        }

        // 파일 크기 제한 (5MB)
        if (signatureImage.getSize() > MAX_SIGNATURE_FILE_SIZE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "파일 크기는 5MB를 초과할 수 없습니다.");
        }
    }

    /**
     * 서명 미리보기 생성 (저장하지 않음)
     *
     * @param fontType 폰트 파일명
     * @param userName 사용자 이름
     * @return 서명 이미지 바이트 배열
     * @throws ApiException 폰트 검증 실패 시
     * @throws IOException 이미지 생성 실패 시
     */
    public byte[] previewSignature(String fontType, String userName) throws IOException {
        log.info("서명 미리보기 생성: fontType={}, userName={}", fontType, userName);

        // Enum에서 폰트 찾기
        SignatureFont font = SignatureFont.findByFileName(fontType);
        if (font == null || !"Y".equals(font.getUseYn())) {
            log.warn("사용할 수 없는 폰트: {}", fontType);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "사용할 수 없는 폰트입니다.");
        }

        // 폰트 파일 존재 여부 확인
        ClassPathResource fontResource = new ClassPathResource("fonts/" + font.getFileName());
        try (InputStream testStream = fontResource.getInputStream()) {
            // InputStream을 열 수 있으면 파일이 존재함
        } catch (IOException e) {
            log.warn("폰트 파일을 찾을 수 없음: {}", font.getFileName(), e);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "해당 폰트를 찾을 수 없습니다.");
        }

        // 서명 이미지 생성 (저장하지 않음)
        return signatureImageUtil.generateSignatureImage(
                userName.trim(),
                font.getFileName(),
                SignaturePlaceholder.SignatureSize.SIG1
        );
    }

    /**
     * 서명 조회
     *
     * @param userId 사용자 ID
     * @return 서명 정보 (signatureUrl, hasSignature)
     */
    public Map<String, Object> getSignature(Long userId) {
        log.info("서명 조회: userId={}", userId);

        Map<String, Object> responseData = new HashMap<>();

        if (signatureFileUtil.signatureFileExists(userId)) {
            String signatureUrl = signatureFileUtil.getSignatureDownloadUrl(userId);
            responseData.put("signatureUrl", signatureUrl);
            responseData.put("hasSignature", true);
        } else {
            responseData.put("hasSignature", false);
        }

        return responseData;
    }

    /**
     * 서명 삭제
     *
     * @param userId 사용자 ID
     * @return 응답 데이터
     */
    @Transactional
    public Map<String, Object> deleteSignature(Long userId) {
        log.info("서명 삭제: userId={}", userId);

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

        return responseData;
    }

    /**
     * 사용 가능한 폰트 목록 조회
     *
     * @return 폰트 목록
     */
    public List<Map<String, String>> getAvailableFonts() {
        log.info("사용 가능한 폰트 목록 조회");

        List<Map<String, String>> availableFonts = new ArrayList<>();

        SignatureFont[] fonts = SignatureFont.getAvailableFonts();
        for (SignatureFont font : fonts) {
            Map<String, String> fontMap = new HashMap<>();
            fontMap.put("name", font.getDisplayName());
            fontMap.put("file", font.getFileName());
            availableFonts.add(fontMap);
        }

        log.info("사용 가능한 폰트 개수: {}", availableFonts.size());
        return availableFonts;
    }

    /**
     * 서명 파일 다운로드 (권한 체크 포함)
     *
     * @param requesterId 요청자 사용자 ID
     * @param fileName 파일명 (예: "123_signature.png")
     * @return 서명 파일 Resource
     * @throws ApiException 권한 없음 또는 파일 없음 시
     * @throws IOException 파일 로드 실패 시
     */
    public Resource downloadSignatureFile(Long requesterId, String fileName) throws IOException {
        log.info("서명 파일 다운로드: requesterId={}, fileName={}", requesterId, fileName);

        // 파일명에서 userId 추출
        if (!fileName.endsWith("_signature.png")) {
            log.warn("잘못된 파일명 형식: {}", fileName);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "잘못된 파일명 형식입니다.");
        }

        String userIdStr = fileName.replace("_signature.png", "");
        Long fileUserId;
        try {
            fileUserId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("잘못된 파일명 형식: {}", fileName);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "잘못된 파일명 형식입니다.");
        }

        // 권한 체크: 자신의 서명만 다운로드 가능
        if (!requesterId.equals(fileUserId)) {
            log.warn("서명 파일 다운로드 권한 없음: requesterId={}, fileUserId={}", requesterId, fileUserId);
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "자신의 서명만 다운로드할 수 있습니다.");
        }

        // 서명 파일 존재 여부 확인
        if (!signatureFileUtil.signatureFileExists(fileUserId)) {
            log.warn("서명 파일이 없음: userId={}", fileUserId);
            throw new ApiException(ApiErrorCode.USER_NOT_FOUND, "서명 파일이 존재하지 않습니다.");
        }

        // 서명 파일 로드
        return signatureFileUtil.loadSignatureFileAsResource(fileUserId);
    }
}
