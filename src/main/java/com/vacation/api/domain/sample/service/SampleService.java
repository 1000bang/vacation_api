package com.vacation.api.domain.sample.service;

import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.sample.request.VacationSampleRequest;
import com.vacation.api.enums.SignaturePlaceholder;
import com.vacation.api.util.FileGenerateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
        
        // Sample에서는 DAM_SIG1, DAM_SIG2만 이미지로 채우고 나머지는 빈 문자열
        Map<String, byte[]> signatureImageMap = createSampleSignatureImageMap();
        
        return FileGenerateUtil.generateVacationApplicationDoc(request, signatureImageMap);
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
        
        // Sample에서는 DAM_SIG1, DAM_SIG2만 이미지로 채우고 나머지는 빈 문자열
        Map<String, byte[]> signatureImageMap = createSampleSignatureImageMap();
        
        return FileGenerateUtil.generateRentalSupportApplicationExcel(request, signatureImageMap);
    }

    /**
     * Sample용 서명 이미지 맵 생성
     * DAM_SIG1, DAM_SIG2만 천병재.png 이미지로 채우고, 나머지는 null (빈 문자열로 치환됨)
     */
    private Map<String, byte[]> createSampleSignatureImageMap() {
        Map<String, byte[]> signatureImageMap = new HashMap<>();
        
        // 서명 이미지 로드 (천병재.png)
        byte[] signatureImage = loadSignatureImage();
        
        if (signatureImage != null) {
            // DAM_SIG1, DAM_SIG2만 이미지로 채움
            signatureImageMap.put(SignaturePlaceholder.DAM_SIG1.getPlaceholder(), signatureImage);
            signatureImageMap.put(SignaturePlaceholder.DAM_SIG2.getPlaceholder(), signatureImage);
        }
        
        // 나머지 플레이스홀더들 (TIM_SIG1, TIM_SIG2, BU_SIG1, BU_SIG2, DEA_SIG1, DEA_SIG2)는
        // 맵에 없으므로 null로 처리되어 빈 문자열로 치환됨
        
        return signatureImageMap;
    }

    /**
     * 서명 이미지 로드
     */
    private byte[] loadSignatureImage() {
        try {
            ClassPathResource imageResource = new ClassPathResource("templates/천병재.png");
            if (!imageResource.exists()) {
                log.warn("서명 이미지를 찾을 수 없습니다: templates/천병재.png");
                return null;
            }
            try (InputStream imageStream = imageResource.getInputStream()) {
                return imageStream.readAllBytes();
            }
        } catch (Exception e) {
            log.error("서명 이미지 로드 중 오류 발생", e);
            return null;
        }
    }
}

