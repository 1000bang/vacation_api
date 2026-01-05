package com.vacation.api.domain.sample.controller;

import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.sample.request.VacationSampleRequest;
import com.vacation.api.domain.sample.service.SampleService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * 월세지원 청구와 관련된 요청을 처리하는 controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@RestController
@RequestMapping("/sample")
public class SampleController {
    Logger logger = LoggerFactory.getLogger(SampleController.class);

    private final SampleService sampleService;

    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    /**
     * 연차 신청 샘플 API - PDF 문서 반환
     *
     * @param request 연차 신청 요청 데이터
     * @return PDF 문서
     */
    @PostMapping("/vacation")
    public ResponseEntity<byte[]> sampleRequest(@Valid @RequestBody VacationSampleRequest request) {
        logger.info("연차 신청 샘플 요청 수신: {}", request);

        try {
            // PDF 생성
            byte[] pdfBytes = sampleService.generateVacationApplicationPdf(request);

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

    @PostMapping("/vacation_V2")
    public ResponseEntity<byte[]> sampleRequestV2(@Valid @RequestBody VacationSampleRequest request) {
        logger.info("연차 신청 샘플 요청 수신 (DOCX): {}", request);

        try {
            // DOCX 파일 생성
            byte[] docBytes = sampleService.generateVacationApplicationDoc(request);

            String dateStr = request.getRequestDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "vacation_application_" + dateStr + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
            headers.setContentLength(docBytes.length);

            logger.info("DOCX 생성 완료. 크기: {} bytes", docBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(docBytes);
        } catch (Exception e) {
            logger.error("DOCX 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월세지원 청구서 API - PDF 문서 반환
     *
     * @param request 월세지원 청구 요청 데이터
     * @return PDF 문서
     */
    @PostMapping("/rental")
    public ResponseEntity<byte[]> rentalSupportRequest(@Valid @RequestBody RentalSupportSampleRequest request) {
        logger.info("월세지원 청구 요청 수신: {}", request);
        
        try {
            // PDF 생성
            byte[] pdfBytes = sampleService.generateRentalSupportApplicationPdf(request);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "rental-support-application.pdf");
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
    /**
     * 월세지원 청구서 API - PDF 문서 반환
     *
     * @param request 월세지원 청구 요청 데이터
     * @return PDF 문서
     */
    @PostMapping("/rental_V2")
    public ResponseEntity<byte[]> rentalSupportRequestV2(@Valid @RequestBody RentalSupportSampleRequest request) {
        logger.info("월세지원 청구 요청 수신: {}", request);

        try {
            // Excel 생성
            byte[] excelBytes = sampleService.generateRentalSupportApplicationExcel(request);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "rental-support-application.xlsx");
            headers.setContentLength(excelBytes.length);

            logger.info("Excel 생성 완료. 크기: {} bytes", excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            logger.error("PDF 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

