package com.vacation.api.domain.vacation.controller;

import com.vacation.api.domain.vacation.request.VacationSampleRequest;
import com.vacation.api.domain.vacation.service.PdfGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 연차 신청 및 관리와 관련된 요청을 처리하는 controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@RestController
public class VacationController {
    Logger logger = LoggerFactory.getLogger(VacationController.class);

    private final PdfGenerationService pdfGenerationService;

    public VacationController(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * 연차 신청 샘플 API - PDF 문서 반환
     *
     * @param request 연차 신청 요청 데이터
     * @return PDF 문서
     */
    @PostMapping("/sample/request")
    public ResponseEntity<byte[]> sampleRequest(@Valid @RequestBody VacationSampleRequest request) {
        logger.info("연차 신청 샘플 요청 수신: {}", request);
        
        try {
            // PDF 생성
            byte[] pdfBytes = pdfGenerationService.generateVacationApplicationPdf(request);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "vacation-application.pdf");
            headers.setContentLength(pdfBytes.length);
            
            logger.info("PDF 생성 완료. 크기: {} bytes", pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            logger.error("PDF 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

