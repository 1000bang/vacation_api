package com.vacation.api.domain.sample.service;

import com.lowagie.text.pdf.BaseFont;
import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.sample.request.VacationSampleRequest;
import com.vacation.api.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static com.vacation.api.util.CommonUtil.*;

/**
 * PDF 생성 서비스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 연차 신청서 PDF 생성
     *
     * @param request 연차 신청 요청 데이터
     * @return PDF 바이트 배열
     */
    public byte[] generateVacationApplicationPdf(VacationSampleRequest request) {
        try {
            // 문서 번호 생성 (예: KP-20251226-01)
            String documentNumber = CommonUtil.generateDocumentNumber(request.getRequestDate());

            // 기간 문자열 생성
            String period = CommonUtil.formatPeriod(request.getStartDate(), request.getEndDate());

            // 최종 잔여 연차일수 계산
            Double finalRemainingDays = CommonUtil.calculateFinalRemainingDays(
                    request.getRemainingVacationDays(),
                    request.getRequestedVacationDays()
            );

            // 현재 연도 가져오기
            int currentYear = LocalDate.now().getYear();
            
            // Thymeleaf 컨텍스트 설정
            Context context = new Context();
            context.setVariable("documentNumber", documentNumber);
            context.setVariable("requestDate", CommonUtil.formatDate(request.getRequestDate()));
            context.setVariable("department", request.getDepartment());
            context.setVariable("applicant", request.getApplicant());
            context.setVariable("period", period);
            context.setVariable("vacationType", request.getVacationType().getValue());
            context.setVariable("reason", request.getReason());
            context.setVariable("totalVacationDays", request.getTotalVacationDays());
            context.setVariable("previousRemainingDays", request.getRemainingVacationDays());
            context.setVariable("requestedVacationDays", request.getRequestedVacationDays());
            context.setVariable("reqDays", CommonUtil.formatVacationDays(request.getRequestedVacationDays()));
            context.setVariable("finalRemainingDays", finalRemainingDays);
            context.setVariable("currentYear", currentYear);
            // 회사 로고 이미지를 Base64로 인코딩
            String companyLogoBase64 = loadCompanyLogoAsBase64();
            context.setVariable("companyLogoBase64", companyLogoBase64);

            // HTML 템플릿 렌더링
            String html = templateEngine.process("vacation-application", context);

            // PDF 생성
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            
            // 한글 폰트 설정
            setupKoreanFonts(renderer);
            
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("PDF 생성 중 오류 발생", e);
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }


    /**
     * 월세지원 청구서 PDF 생성
     *
     * @param request 월세지원 청구 요청 데이터
     * @return PDF 바이트 배열
     */
    public byte[] generateRentalSupportApplicationPdf(RentalSupportSampleRequest request) {
        try {
            // 문서 번호 생성
            String documentNumber = CommonUtil.generateDocumentNumber(request.getRequestDate());

            // 계약 기간 계산
            long contractYears = ChronoUnit.YEARS.between(
                    request.getContractStartDate(), 
                    request.getContractEndDate()
            );
            
            // 계약 기간 문자열 생성
            String contractPeriod = CommonUtil.formatPeriod(request.getContractStartDate(), request.getContractEndDate());
            
            // 청구 기간 일수 계산
            long billingDays = ChronoUnit.DAYS.between(
                    request.getBillingPeriodStartDate(),
                    request.getBillingPeriodEndDate()
            ) + 1; // 시작일 포함
            String billingDaysFormatted = String.format("(%d/%d일)", billingDays, billingDays);
            // 청구 기간 비율 계산 (항상 100%)
            double billingPercentage = 100.0;
            
            // 현재 월 가져오기
            int currentMonth = request.getRequestDate().getMonthValue();
            
            // Thymeleaf 컨텍스트 설정
            Context context = new Context();
            context.setVariable("documentNumber", documentNumber);
            context.setVariable("requestDate", CommonUtil.formatDate(request.getRequestDate()));
            context.setVariable("department", request.getDepartment());
            context.setVariable("applicant", request.getApplicant());
            context.setVariable("contractPeriod", contractPeriod);
            context.setVariable("contractYears", contractYears);
            context.setVariable("contractMonthlyRent", request.getContractMonthlyRent());
            context.setVariable("paymentType", request.getPaymentType().getValue());
            context.setVariable("billingStartDate", CommonUtil.formatDate(request.getBillingStartDate()));
            context.setVariable("billingPeriodStart", CommonUtil.formatDateShort(request.getBillingPeriodStartDate()));
            context.setVariable("billingPeriodEnd", CommonUtil.formatDateShort(request.getBillingPeriodEndDate()));
            context.setVariable("billingDays", billingDaysFormatted);
            context.setVariable("billingPercentage", String.format("%.2f", billingPercentage));
            context.setVariable("paymentDate", formatDateShort(request.getPaymentDate()));
            context.setVariable("paymentAmount", request.getPaymentAmount());
            context.setVariable("billingAmount", request.getBillingAmount());
            context.setVariable("month", currentMonth);
            
            // 회사 로고 이미지를 Base64로 인코딩
            String companyLogoBase64 = loadCompanyLogoAsBase64();
            context.setVariable("companyLogoBase64", companyLogoBase64);

            // HTML 템플릿 렌더링
            String html = templateEngine.process("rental-support-application", context);

            // PDF 생성
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            
            // 한글 폰트 설정 (기존과 동일)
            setupKoreanFonts(renderer);
            
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("월세지원 청구서 PDF 생성 중 오류 발생", e);
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }


    /**
     * 한글 폰트 설정 (공통 메서드)
     */
    private void setupKoreanFonts(ITextRenderer renderer) {
        try {
            String[] fontResourcePaths = {
                "fonts/NanumGothic.ttf",
                "fonts/malgun.ttf",
                "fonts/AppleGothic.ttf",
                "fonts/BareunBatangPro1.ttf"
            };
            
            boolean fontLoaded = false;
            for (String fontResourcePath : fontResourcePaths) {
                try {
                    ClassPathResource fontResource = new ClassPathResource(fontResourcePath);
                    if (fontResource.exists()) {
                        InputStream fontInputStream = fontResource.getInputStream();
                        
                        java.io.File tempFontFile = java.io.File.createTempFile("font_", ".ttf");
                        tempFontFile.deleteOnExit();
                        
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFontFile)) {
                            fontInputStream.transferTo(fos);
                        }
                        
                        try {
                            renderer.getFontResolver().addFont(
                                tempFontFile.getAbsolutePath(),
                                BaseFont.IDENTITY_H,
                                BaseFont.EMBEDDED
                            );
                            
                            log.info("프로젝트 내 한글 폰트 등록 완료: {}", fontResourcePath);
                            fontLoaded = true;
                            break;
                        } catch (Exception fontException) {
                            log.warn("폰트 등록 실패: {} - {}", fontResourcePath, fontException.getMessage());
                            tempFontFile.delete();
                            continue;
                        }
                    }
                } catch (Exception e) {
                    log.warn("폰트 리소스 로드 실패: {} - {}", fontResourcePath, e.getMessage());
                    continue;
                }
            }
            
            if (!fontLoaded) {
                log.warn("프로젝트 내 한글 폰트를 찾을 수 없습니다. resources/fonts/ 폴더에 폰트 파일을 추가해주세요.");
            }
        } catch (Exception e) {
            log.error("폰트 설정 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 회사 로고 이미지를 Base64로 인코딩하여 반환
     */
    private String loadCompanyLogoAsBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("logo.png");
            InputStream inputStream = resource.getInputStream();
            byte[] imageBytes = inputStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            log.warn("회사 로고 이미지를 로드할 수 없습니다. 기본 이미지를 사용합니다.", e);
            return "https://via.placeholder.com/150x60?text=Company+Logo";
        }
    }
}

