package com.vacation.api.domain.attachment.service;

import com.vacation.api.domain.attachment.entity.Attachment;
import com.vacation.api.domain.attachment.repository.AttachmentRepository;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 파일 서비스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final AttachmentRepository attachmentRepository;

    @Value("${file.upload.path.prod:/upload}")
    private String prodUploadPath;

    @Value("${file.upload.path.dev:/Users/1000bang/Downloads/upload}")
    private String devUploadPath;

    @Value("${file.upload.path.local:C:/Downloads/upload}")
    private String localUploadPath;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 파일 업로드 경로 가져오기
     *
     * @return 업로드 경로
     */
    private String getUploadBasePath() {
        if ("prod".equals(activeProfile)) {
            return prodUploadPath;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return localUploadPath;
        } else {
            return devUploadPath;
        }
    }

    /**
     * 파일 저장 경로 생성
     *
     * @param applicationType 신청 타입
     * @param userId 사용자 ID
     * @param applicationSeq 신청 시퀀스
     * @param originalFileName 원본 파일명
     * @return 파일 저장 경로
     */
    private Path generateFilePath(String applicationType, Long userId, Long applicationSeq, String originalFileName) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 파일 확장자 추출
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
        }
        
        // 파일명: 사용자id_문서seq.{ext}
        String fileName = userId + "_" + applicationSeq + extension;
        
        // 전체 경로: /upload/yyyy/mm/dd/{applicationType}/사용자id_문서seq.{ext}
        String fullPath = getUploadBasePath() + "/" + datePath + "/" + applicationType.toLowerCase() + "/" + fileName;
        
        return Paths.get(fullPath);
    }

    /**
     * 파일 확장자 검증
     *
     * @param fileName 파일명
     * @param allowedExtensions 허용된 확장자 목록
     * @return 검증 통과 여부
     */
    private boolean isValidExtension(String fileName, String[] allowedExtensions) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        String lowerFileName = fileName.toLowerCase();
        for (String ext : allowedExtensions) {
            if (lowerFileName.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 파일 업로드 (단일 파일 - 휴가, 월세지원, 월세품의용)
     *
     * @param file 업로드할 파일
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @param userId 사용자 ID
     * @return 저장된 첨부파일 정보
     */
    @Transactional
    public Attachment uploadFile(MultipartFile file, String applicationType, Long applicationSeq, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "파일이 없습니다.");
        }

        // 파일 크기 검증 (10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 파일 확장자 검증
        String[] allowedExtensions = {"png", "jpg", "jpeg", "pdf"};
        if (!isValidExtension(file.getOriginalFilename(), allowedExtensions)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "PNG, JPG, PDF 파일만 업로드 가능합니다.");
        }

        try {
            // 파일 저장 경로 생성
            Path filePath = generateFilePath(applicationType, userId, applicationSeq, file.getOriginalFilename());
            
            // 디렉토리 생성
            Files.createDirectories(filePath.getParent());

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 기존 첨부파일 삭제 (단일 파일이므로)
            attachmentRepository.deleteByApplicationTypeAndApplicationSeq(applicationType, applicationSeq);

            // 첨부파일 정보 저장
            Attachment attachment = Attachment.builder()
                    .applicationType(applicationType)
                    .applicationSeq(applicationSeq)
                    .fileName(file.getOriginalFilename())
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .fileOrder(1) // 단일 파일이므로 1
                    .build();

            Attachment saved = attachmentRepository.save(attachment);
            log.info("파일 업로드 완료: seq={}, applicationType={}, applicationSeq={}, fileName={}", 
                    saved.getSeq(), applicationType, applicationSeq, file.getOriginalFilename());

            return saved;
        } catch (IOException e) {
            log.error("파일 업로드 실패: applicationType={}, applicationSeq={}", applicationType, applicationSeq, e);
            throw new ApiException(ApiErrorCode.UNKNOWN_ERROR, "파일 업로드에 실패했습니다.");
        }
    }

    /**
     * 첨부파일 조회
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @return 첨부파일 목록
     */
    public List<Attachment> getAttachments(String applicationType, Long applicationSeq) {
        return attachmentRepository.findByApplicationTypeAndApplicationSeqOrderByFileOrderAsc(applicationType, applicationSeq);
    }

    /**
     * 첨부파일 조회 (단일 파일용)
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @return 첨부파일
     */
    public Attachment getAttachment(String applicationType, Long applicationSeq) {
        return attachmentRepository.findFirstByApplicationTypeAndApplicationSeqOrderByFileOrderAsc(applicationType, applicationSeq)
                .orElse(null);
    }

    /**
     * 파일 다운로드
     *
     * @param attachment 첨부파일 정보
     * @return 파일 리소스
     */
    public Resource downloadFile(Attachment attachment) {
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException(ApiErrorCode.UNKNOWN_ERROR, "파일을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            log.error("파일 다운로드 실패: seq={}, filePath={}", attachment.getSeq(), attachment.getFilePath(), e);
            throw new ApiException(ApiErrorCode.UNKNOWN_ERROR, "파일 다운로드에 실패했습니다.");
        }
    }

    /**
     * 첨부파일 삭제
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     */
    @Transactional
    public void deleteAttachments(String applicationType, Long applicationSeq) {
        List<Attachment> attachments = attachmentRepository.findByApplicationTypeAndApplicationSeqOrderByFileOrderAsc(applicationType, applicationSeq);
        
        for (Attachment attachment : attachments) {
            try {
                // 파일 삭제
                Path filePath = Paths.get(attachment.getFilePath());
                Files.deleteIfExists(filePath);
                log.info("파일 삭제 완료: filePath={}", attachment.getFilePath());
            } catch (IOException e) {
                log.warn("파일 삭제 실패: filePath={}", attachment.getFilePath(), e);
            }
        }
        
        // DB에서 삭제
        attachmentRepository.deleteByApplicationTypeAndApplicationSeq(applicationType, applicationSeq);
        log.info("첨부파일 삭제 완료: applicationType={}, applicationSeq={}", applicationType, applicationSeq);
    }
}
