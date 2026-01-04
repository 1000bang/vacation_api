package com.vacation.api.domain.vacation.service;

import com.vacation.api.domain.vacation.request.VacationSampleRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.lowagie.text.pdf.BaseFont;

import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

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
            String documentNumber = generateDocumentNumber(request.getRequestDate());

            // 기간 문자열 생성
            String period = formatPeriod(request.getStartDate(), request.getEndDate());

            // 최종 잔여 연차일수 계산
            Double finalRemainingDays = calculateFinalRemainingDays(
                    request.getRemainingVacationDays(),
                    request.getRequestedVacationDays()
            );

            // 현재 연도 가져오기
            int currentYear = LocalDate.now().getYear();
            
            // Thymeleaf 컨텍스트 설정
            Context context = new Context();
            context.setVariable("documentNumber", documentNumber);
            context.setVariable("applicationDate", formatDate(request.getRequestDate()));
            context.setVariable("department", request.getDepartment());
            context.setVariable("applicant", request.getApplicant());
            context.setVariable("period", period);
            context.setVariable("vacationType", request.getVacationType().getValue());
            context.setVariable("reason", request.getReason());
            context.setVariable("totalVacationDays", request.getTotalVacationDays());
            context.setVariable("previousRemainingDays", request.getRemainingVacationDays());
            context.setVariable("requestedVacationDays", request.getRequestedVacationDays());
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
            
            // 한글 폰트 설정 (프로젝트 내 폰트 사용)
            try {
                // 프로젝트 resources/fonts 폴더에서 폰트 로드 (우선순위 순서)
                String[] fontResourcePaths = {
                    "fonts/NanumGothic.ttf",      // 나눔고딕 (가장 안정적)
                    "fonts/malgun.ttf",           // 맑은 고딕
                    "fonts/AppleGothic.ttf",      // AppleGothic (OS/2 테이블 문제 가능)
                    "fonts/BareunBatangPro1.ttf"  // 바른바탕
                };
                
                boolean fontLoaded = false;
                for (String fontResourcePath : fontResourcePaths) {
                    try {
                        ClassPathResource fontResource = new ClassPathResource(fontResourcePath);
                        if (fontResource.exists()) {
                            InputStream fontInputStream = fontResource.getInputStream();
                            
                            // 임시 파일로 복사 (ITextRenderer는 파일 경로를 요구)
                            java.io.File tempFontFile = java.io.File.createTempFile("font_", ".ttf");
                            tempFontFile.deleteOnExit();
                            
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFontFile)) {
                                fontInputStream.transferTo(fos);
                            }
                            
                            // ITextRenderer에 폰트 등록
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
                                // 임시 파일 정리
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
     * 문서 번호 생성
     */
    private String generateDocumentNumber(LocalDate requestDate) {
        try {
            String dateStr = requestDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return "KP-" + dateStr + "-01";
        } catch (Exception e) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return "KP-" + dateStr + "-01";
        }
    }

    /**
     * 날짜 포맷팅 (yyyy-MM-dd -> yyyy년 MM월 dd일)
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
    }

    /**
     * 기간 포맷팅 (시작일 ~ 종료일)
     */
    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "";
        }
        return formatDate(startDate) + " ~ " + formatDate(endDate);
    }

    
    /**
     * 최종 잔여 연차일수 계산 (0.5 단위 지원)
     */
    private Double calculateFinalRemainingDays(Double previousRemainingDays, Double requestedDays) {
        if (previousRemainingDays == null || requestedDays == null) {
            return null;
        }
        return previousRemainingDays - requestedDays;
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

