package com.vacation.api.util;

import com.vacation.api.domain.attachment.entity.Attachment;
import com.vacation.api.domain.attachment.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP 파일 생성 유틸리티
 * 
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Slf4j
@Component
public class ZipFileUtil {

    private final FileService fileService;

    public ZipFileUtil(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 문서와 첨부파일을 ZIP으로 묶기
     * 
     * @param documentBytes 문서 바이트 배열
     * @param documentFileName 문서 파일명
     * @param attachments 첨부파일 목록
     * @return ZIP 파일 바이트 배열
     * @throws IOException ZIP 생성 실패 시
     */
    public byte[] createZipWithDocumentAndAttachments(
            byte[] documentBytes,
            String documentFileName,
            List<Attachment> attachments) throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 문서 추가
            ZipEntry documentEntry = new ZipEntry(documentFileName);
            documentEntry.setSize(documentBytes.length);
            zos.putNextEntry(documentEntry);
            zos.write(documentBytes);
            zos.closeEntry();
            
            // 첨부파일 추가 (루트에 직접 추가)
            if (attachments != null && !attachments.isEmpty()) {
                for (Attachment attachment : attachments) {
                    try {
                        Resource resource = fileService.downloadFile(attachment);
                        if (resource.exists() && resource.isReadable()) {
                            // 첨부파일을 루트에 직접 추가
                            String attachmentFileName = attachment.getFileName();
                            ZipEntry attachmentEntry = new ZipEntry(attachmentFileName);
                            
                            // 파일 크기 설정
                            long fileSize = attachment.getFileSize() != null ? attachment.getFileSize() : 0;
                            attachmentEntry.setSize(fileSize);
                            
                            zos.putNextEntry(attachmentEntry);
                            
                            // 파일 내용 복사
                            try (InputStream is = resource.getInputStream()) {
                                byte[] buffer = new byte[8192];
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    zos.write(buffer, 0, len);
                                }
                            }
                            
                            zos.closeEntry();
                            log.debug("첨부파일 추가 완료: {}", attachment.getFileName());
                        } else {
                            log.warn("첨부파일을 읽을 수 없음: {}", attachment.getFilePath());
                        }
                    } catch (Exception e) {
                        log.warn("첨부파일 추가 실패: {}", attachment.getFileName(), e);
                        // 첨부파일 추가 실패해도 계속 진행
                    }
                }
            }
        }
        
        byte[] zipBytes = baos.toByteArray();
        log.info("ZIP 파일 생성 완료. 크기: {} bytes, 문서: {}, 첨부파일: {}개", 
                zipBytes.length, documentFileName, 
                attachments != null ? attachments.size() : 0);
        
        return zipBytes;
    }

    /**
     * 문서와 개인비용 항목별 첨부파일을 ZIP으로 묶기
     * 개인비용의 경우 각 항목별로 첨부파일이 있을 수 있음
     * 
     * @param documentBytes 문서 바이트 배열
     * @param documentFileName 문서 파일명
     * @param expenseSubAttachments 개인비용 항목별 첨부파일 목록 (Map<childNo, List<Attachment>>)
     * @return ZIP 파일 바이트 배열
     * @throws IOException ZIP 생성 실패 시
     */
    public byte[] createZipWithDocumentAndExpenseAttachments(
            byte[] documentBytes,
            String documentFileName,
            java.util.Map<Integer, List<Attachment>> expenseSubAttachments) throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 문서 추가
            ZipEntry documentEntry = new ZipEntry(documentFileName);
            documentEntry.setSize(documentBytes.length);
            zos.putNextEntry(documentEntry);
            zos.write(documentBytes);
            zos.closeEntry();
            
            // 첨부파일 추가 (루트에 직접 추가, 파일명 중복 방지를 위해 항목번호 포함)
            if (expenseSubAttachments != null && !expenseSubAttachments.isEmpty()) {
                for (java.util.Map.Entry<Integer, List<Attachment>> entry : expenseSubAttachments.entrySet()) {
                    Integer childNo = entry.getKey();
                    List<Attachment> attachments = entry.getValue();
                    
                    if (attachments != null && !attachments.isEmpty()) {
                        for (int i = 0; i < attachments.size(); i++) {
                            Attachment attachment = attachments.get(i);
                            try {
                                Resource resource = fileService.downloadFile(attachment);
                                if (resource.exists() && resource.isReadable()) {
                                    // 첨부파일을 루트에 직접 추가 (파일명 중복 방지를 위해 항목번호 포함)
                                    String originalFileName = attachment.getFileName();
                                    String attachmentFileName;
                                    
                                    // 파일명에 확장자가 있는지 확인
                                    int lastDotIndex = originalFileName.lastIndexOf('.');
                                    if (lastDotIndex > 0) {
                                        String nameWithoutExt = originalFileName.substring(0, lastDotIndex);
                                        String extension = originalFileName.substring(lastDotIndex);
                                        // 같은 항목에 여러 파일이 있을 경우를 대비해 인덱스 추가
                                        if (attachments.size() > 1) {
                                            attachmentFileName = String.format("%d_%s_%d%s", childNo, nameWithoutExt, i + 1, extension);
                                        } else {
                                            attachmentFileName = String.format("%d_%s%s", childNo, nameWithoutExt, extension);
                                        }
                                    } else {
                                        // 같은 항목에 여러 파일이 있을 경우를 대비해 인덱스 추가
                                        if (attachments.size() > 1) {
                                            attachmentFileName = String.format("%d_%s_%d", childNo, originalFileName, i + 1);
                                        } else {
                                            attachmentFileName = String.format("%d_%s", childNo, originalFileName);
                                        }
                                    }
                                    
                                    ZipEntry attachmentEntry = new ZipEntry(attachmentFileName);
                                    
                                    // 파일 크기 설정
                                    long fileSize = attachment.getFileSize() != null ? attachment.getFileSize() : 0;
                                    attachmentEntry.setSize(fileSize);
                                    
                                    zos.putNextEntry(attachmentEntry);
                                    
                                    // 파일 내용 복사
                                    try (InputStream is = resource.getInputStream()) {
                                        byte[] buffer = new byte[8192];
                                        int len;
                                        while ((len = is.read(buffer)) > 0) {
                                            zos.write(buffer, 0, len);
                                        }
                                    }
                                    
                                    zos.closeEntry();
                                    log.debug("첨부파일 추가 완료: 항목{}, {}", childNo, attachmentFileName);
                                } else {
                                    log.warn("첨부파일을 읽을 수 없음: {}", attachment.getFilePath());
                                }
                            } catch (Exception e) {
                                log.warn("첨부파일 추가 실패: 항목{}, {}", childNo, attachment.getFileName(), e);
                                // 첨부파일 추가 실패해도 계속 진행
                            }
                        }
                    }
                }
            }
        }
        
        int totalAttachments = expenseSubAttachments != null 
                ? expenseSubAttachments.values().stream().mapToInt(List::size).sum() 
                : 0;
        
        byte[] zipBytes = baos.toByteArray();
        log.info("ZIP 파일 생성 완료. 크기: {} bytes, 문서: {}, 첨부파일: {}개", 
                zipBytes.length, documentFileName, totalAttachments);
        
        return zipBytes;
    }
}
