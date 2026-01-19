package com.vacation.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * 서명 파일 저장 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-16
 */
@Slf4j
@Component
public class SignatureFileUtil {

    @Value("${file.signature.path.prod:/signature}")
    private String prodSignaturePath;

    @Value("${file.signature.path.dev:/Users/1000bang/Downloads/signature}")
    private String devSignaturePath;

    @Value("${file.signature.path.local:C:/Downloads/signature}")
    private String localSignaturePath;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 서명 파일 저장 기본 경로 가져오기
     *
     * @return 서명 파일 저장 경로
     */
    public String getSignatureBasePath() {
        if ("prod".equals(activeProfile)) {
            return prodSignaturePath;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return localSignaturePath;
        } else {
            return devSignaturePath;
        }
    }

    /**
     * 서명 파일 저장 디렉토리 생성
     * 디렉토리가 없으면 자동 생성하고, 적절한 파일 권한을 설정합니다.
     *
     * @return 생성된 디렉토리 경로
     * @throws IOException 디렉토리 생성 실패 시
     */
    public Path ensureSignatureDirectoryExists() throws IOException {
        String basePath = getSignatureBasePath();
        Path signatureDir = Paths.get(basePath);

        // 디렉토리가 없으면 생성
        if (!Files.exists(signatureDir)) {
            Files.createDirectories(signatureDir);
            log.info("서명 파일 저장 디렉토리 생성 완료: {}", signatureDir.toAbsolutePath());
        }

        // 파일 권한 설정 (Unix/Linux/Mac 시스템인 경우)
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
                Files.setPosixFilePermissions(signatureDir, permissions);
                log.debug("서명 파일 저장 디렉토리 권한 설정 완료: {}", signatureDir);
            } catch (UnsupportedOperationException e) {
                // Windows 시스템에서는 PosixFilePermissions를 지원하지 않으므로 무시
                log.debug("파일 권한 설정을 지원하지 않는 시스템입니다: {}", System.getProperty("os.name"));
            } catch (IOException e) {
                log.warn("서명 파일 저장 디렉토리 권한 설정 실패: {}", signatureDir, e);
                // 권한 설정 실패해도 디렉토리는 생성되었으므로 계속 진행
            }
        }

        return signatureDir;
    }

    /**
     * 서명 파일 전체 경로 생성
     *
     * @param userId 사용자 ID
     * @return 서명 파일 전체 경로
     */
    public Path getSignatureFilePath(Long userId) {
        String basePath = getSignatureBasePath();
        String fileName = userId + "_signature.png";
        return Paths.get(basePath, fileName);
    }

    /**
     * 서명 파일 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 파일 존재 여부
     */
    public boolean signatureFileExists(Long userId) {
        Path filePath = getSignatureFilePath(userId);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    /**
     * 서명 파일 저장
     * 디렉토리가 없으면 자동 생성합니다.
     *
     * @param userId 사용자 ID
     * @param imageBytes 서명 이미지 바이트 배열
     * @throws IOException 파일 저장 실패 시
     */
    public void saveSignatureFile(Long userId, byte[] imageBytes) throws IOException {
        // 디렉토리 생성
        ensureSignatureDirectoryExists();

        // 파일 경로 생성
        Path filePath = getSignatureFilePath(userId);

        // 파일 저장
        Files.write(filePath, imageBytes);
        log.info("서명 파일 저장 완료: userId={}, filePath={}, size={} bytes", 
                userId, filePath, imageBytes.length);
    }

    /**
     * 서명 파일 저장 (InputStream 사용)
     * 디렉토리가 없으면 자동 생성합니다.
     *
     * @param userId 사용자 ID
     * @param inputStream 서명 이미지 입력 스트림
     * @throws IOException 파일 저장 실패 시
     */
    public void saveSignatureFile(Long userId, java.io.InputStream inputStream) throws IOException {
        // 디렉토리 생성
        ensureSignatureDirectoryExists();

        // 파일 경로 생성
        Path filePath = getSignatureFilePath(userId);

        // 파일 저장
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("서명 파일 저장 완료: userId={}, filePath={}", userId, filePath);
    }

    /**
     * 서명 파일 로드
     *
     * @param userId 사용자 ID
     * @return 서명 이미지 바이트 배열
     * @throws IOException 파일 로드 실패 시
     */
    public byte[] loadSignatureFile(Long userId) throws IOException {
        Path filePath = getSignatureFilePath(userId);

        if (!Files.exists(filePath)) {
            throw new IOException("서명 파일을 찾을 수 없습니다: " + filePath);
        }

        byte[] imageBytes = Files.readAllBytes(filePath);
        log.debug("서명 파일 로드 완료: userId={}, filePath={}, size={} bytes", 
                userId, filePath, imageBytes.length);

        return imageBytes;
    }

    /**
     * 서명 파일을 Resource로 로드
     *
     * @param userId 사용자 ID
     * @return 서명 파일 Resource
     * @throws IOException 파일 로드 실패 시
     */
    public Resource loadSignatureFileAsResource(Long userId) throws IOException {
        Path filePath = getSignatureFilePath(userId);

        if (!Files.exists(filePath)) {
            throw new IOException("서명 파일을 찾을 수 없습니다: " + filePath);
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            log.debug("서명 파일 Resource 로드 완료: userId={}, filePath={}", userId, filePath);
            return resource;
        } else {
            throw new IOException("서명 파일을 읽을 수 없습니다: " + filePath);
        }
    }

    /**
     * 서명 파일 삭제
     *
     * @param userId 사용자 ID
     * @return 삭제 성공 여부
     */
    public boolean deleteSignatureFile(Long userId) {
        Path filePath = getSignatureFilePath(userId);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("서명 파일 삭제 완료: userId={}, filePath={}", userId, filePath);
            } else {
                log.debug("서명 파일이 존재하지 않음: userId={}, filePath={}", userId, filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("서명 파일 삭제 실패: userId={}, filePath={}", userId, filePath, e);
            return false;
        }
    }

    /**
     * 서명 파일 다운로드 URL 경로 생성
     * HTTP 엔드포인트 경로를 반환합니다.
     * 주의: 파일 시스템 경로가 아닌 HTTP 엔드포인트 경로를 반환합니다.
     * baseURL이 이미 /api를 포함하므로 /api를 제외한 경로를 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 서명 파일 다운로드 URL 경로 (예: "/user/download/signature/1_signature.png")
     */
    public String getSignatureDownloadUrl(Long userId) {
        String fileName = userId + "_signature.png";
        // HTTP 엔드포인트 경로는 환경과 무관하게 항상 동일해야 함
        // 파일 시스템 경로(getSignatureBasePath)와는 다름
        return "/user/download/signature/" + fileName;
    }
}
