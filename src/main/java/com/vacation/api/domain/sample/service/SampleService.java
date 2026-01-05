package com.vacation.api.domain.sample.service;

import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.sample.request.VacationSampleRequest;
import com.vacation.api.util.FileGenerateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 샘플 서비스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
@Slf4j
@Service
public class SampleService {

    private final PdfGenerationService pdfGenerationService;

    public SampleService(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * 연차 신청서 PDF 생성
     *
     * @param request 연차 신청 요청 데이터
     * @return PDF 바이트 배열
     */
    public byte[] generateVacationApplicationPdf(VacationSampleRequest request) {
        log.info("연차 신청서 PDF 생성 요청: {}", request);
        return pdfGenerationService.generateVacationApplicationPdf(request);
    }

    /**
     * 연차 신청서 DOCX 생성
     *
     * @param request 연차 신청 요청 데이터
     * @return DOCX 바이트 배열
     */
    public byte[] generateVacationApplicationDoc(VacationSampleRequest request) {
        log.info("연차 신청서 DOCX 생성 요청: {}", request);
        return FileGenerateUtil.generateVacationApplicationDoc(request);
    }

    /**
     * 월세지원 청구서 PDF 생성
     *
     * @param request 월세지원 청구 요청 데이터
     * @return PDF 바이트 배열
     */
    public byte[] generateRentalSupportApplicationPdf(RentalSupportSampleRequest request) {
        log.info("월세지원 청구서 PDF 생성 요청: {}", request);
        return pdfGenerationService.generateRentalSupportApplicationPdf(request);
    }

    /**
     * 월세지원 청구서 Excel 생성
     *
     * @param request 월세지원 청구 요청 데이터
     * @return XLSX 바이트 배열
     */
    public byte[] generateRentalSupportApplicationExcel(RentalSupportSampleRequest request) {
        log.info("월세지원 청구서 Excel 생성 요청: {}", request);
        return FileGenerateUtil.generateRentalSupportApplicationExcel(request);
    }
}

