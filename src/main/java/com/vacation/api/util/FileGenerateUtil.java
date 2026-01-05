package com.vacation.api.util;

import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.sample.request.VacationSampleRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static com.vacation.api.util.CommonUtil.*;

/**
 * 파일 생성 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
@Slf4j
public class FileGenerateUtil {

    /**
     * 연차 신청서 Doc 생성
     *
     * @param request 연차 신청 요청 데이터
     * @return DOCX 바이트 배열
     */
    public static byte[] generateVacationApplicationDoc(VacationSampleRequest request) {
        try {
            // 템플릿 파일 로드 (resources/templates/vacation-application.docx)
            // .docx 형식만 지원합니다. .doc 파일은 .docx로 변환해주세요.
            ClassPathResource templateResource = new ClassPathResource("templates/vacation-application.docx");
            
            if (!templateResource.exists()) {
                throw new RuntimeException("템플릿 파일을 찾을 수 없습니다: templates/vacation-application.docx. " +
                        "resources/templates/ 폴더에 .docx 형식의 템플릿 파일을 추가해주세요.");
            }
            
            InputStream templateInputStream = templateResource.getInputStream();
            XWPFDocument document = new XWPFDocument(templateInputStream);

            // 문서 번호 생성
            String documentNumber = generateDocumentNumber(request.getRequestDate());
            
            // 기간 문자열 생성
            String period = formatPeriod(request.getStartDate(), request.getEndDate());
            
            // 최종 잔여 연차일수 계산
            Double finalRemainingDays = calculateFinalRemainingDays(
                    request.getRemainingVacationDays(),
                    request.getRequestedVacationDays()
            );
            
            // 현재 연도
            int currentYear = LocalDate.now().getYear();
            
            // 데이터 매핑
            Map<String, String> values = new HashMap<>();
            values.put("{{DOCUMENT_NUMBER}}", documentNumber);
            values.put("{{REQUEST_DATE}}", formatDate(request.getRequestDate()));
            values.put("{{DEPARTMENT}}", request.getDepartment());
            values.put("{{APPLICANT}}", request.getApplicant());
            values.put("{{PERIOD}}", period);
            values.put("{{REQDAYS}}", formatVacationDays(request.getRequestedVacationDays()));
            values.put("{{VACATION_TYPE}}", request.getVacationType().getValue());
            values.put("{{REASON}}", request.getReason() != null ? request.getReason() : "");
            values.put("{{TOTAL_VACATION_DAYS}}", formatVacationDays(request.getTotalVacationDays()));
            values.put("{{PREVIOUS_REMAINING_DAYS}}", formatVacationDays(request.getRemainingVacationDays()));
            values.put("{{REQUESTED_VACATION_DAYS}}", formatVacationDays(request.getRequestedVacationDays()));
            values.put("{{FINAL_REMAINING_DAYS}}", formatVacationDays(finalRemainingDays));
            values.put("{{CURRENT_YEAR}}", String.valueOf(currentYear));

            // 문단(Paragraph) 치환
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replaceTextInParagraph(paragraph, values);
            }

            // 테이블 안 텍스트도 치환
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replaceTextInParagraph(paragraph, values);
                        }
                    }
                }
            }

            // ByteArrayOutputStream에 문서 쓰기
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);

            document.close();
            templateInputStream.close();
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("DOC 생성 중 오류 발생", e);
            throw new RuntimeException("DOC 생성 실패", e);
        }
    }

    /**
     * 월세지원 청구서 Excel 생성
     *
     * @param request 월세지원 청구 요청 데이터
     * @return XLSX 바이트 배열
     */
    public static byte[] generateRentalSupportApplicationExcel(RentalSupportSampleRequest request) {
        try {
            // 템플릿 파일 로드 (resources/templates/rental-support-application.xlsx)
            ClassPathResource templateResource = new ClassPathResource("templates/rental-support-application.xlsx");
            
            if (!templateResource.exists()) {
                throw new RuntimeException("템플릿 파일을 찾을 수 없습니다: templates/rental-support-application.xlsx. " +
                        "resources/templates/ 폴더에 .xlsx 형식의 템플릿 파일을 추가해주세요.");
            }
            
            InputStream templateInputStream = templateResource.getInputStream();
            Workbook workbook = new XSSFWorkbook(templateInputStream);

            // 문서 번호 생성
            String documentNumber = generateDocumentNumber(request.getRequestDate());
            
            // 계약 기간 계산
            long contractYears = ChronoUnit.YEARS.between(
                    request.getContractStartDate(), 
                    request.getContractEndDate()
            );
            
            // 계약 기간 문자열 생성
            String contractPeriod = formatPeriod(request.getContractStartDate(), request.getContractEndDate());
            
            // 청구 기간 일수 계산
            long billingDays = ChronoUnit.DAYS.between(
                    request.getBillingPeriodStartDate(),
                    request.getBillingPeriodEndDate()
            ) + 1; // 시작일 포함

            // Excel 수식 형식: "(" & DATEDIF(C13, F13, "D")+1 & "/" & DATEDIF(C13, F13, "D")+1 & " 일)"
            String billingDaysFormatted = String.format("(%d/%d일)", billingDays, billingDays);

            // 청구 기간 비율 계산 (항상 100%)
            double billingPercentage = 100.0;
            
            // 현재 월 가져오기
            int currentMonth = request.getBillingPeriodStartDate().getMonthValue();
            
            // 데이터 매핑
            Map<String, String> values = new HashMap<>();
            values.put("{{DOCUMENT_NUMBER}}", documentNumber);
            values.put("{{REQUEST_DATE}}", formatDate(request.getRequestDate()));
            values.put("{{DEPARTMENT}}", request.getDepartment());
            values.put("{{APPLICANT}}", request.getApplicant());
            values.put("{{CONTRACT_PERIOD}}", contractPeriod);
            values.put("{{CONTRACT_YEARS}}", String.valueOf(contractYears));
            values.put("{{CONTRACT_MONTHLY_RENT}}", formatNumber(request.getContractMonthlyRent()));
            values.put("{{PAYMENT_TYPE}}", request.getPaymentType().getValue());
            values.put("{{BILLING_START_DATE}}", formatDate(request.getBillingStartDate()));
            values.put("{{BILLING_PERIOD_START}}", formatDateShort(request.getBillingPeriodStartDate()));
            values.put("{{BILLING_PERIOD_END}}", formatDateShort(request.getBillingPeriodEndDate()));
            values.put("{{BILLING_DAYS}}", billingDaysFormatted);
            values.put("{{BILLING_PERCENTAGE}}", String.format("%.2f", billingPercentage)+"%");
            values.put("{{PAYMENT_DATE}}", formatDateShort(request.getPaymentDate()));
            values.put("{{PAYMENT_AMOUNT}}", formatNumber(request.getPaymentAmount()));
            values.put("{{BILLING_AMOUNT}}", formatNumber(request.getBillingAmount()));
            values.put("{{MONTH}}", "( "+String.valueOf(currentMonth)+" 월)");

            // 모든 시트를 순회하며 텍스트 치환
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                replaceTextInSheet(sheet, values);
            }

            // ByteArrayOutputStream에 Excel 쓰기
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            workbook.close();
            templateInputStream.close();
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Excel 생성 중 오류 발생", e);
            throw new RuntimeException("Excel 생성 실패", e);
        }
    }

    /**
     * Excel 시트의 모든 셀에서 텍스트 치환
     */
    private static void replaceTextInSheet(Sheet sheet, Map<String, String> values) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                replaceTextInCellV2(cell, values);
            }
        }
    }

    /**
     * Excel 셀의 텍스트 치환
     */
    private static void replaceTextInCell(Cell cell, Map<String, String> values) {
        if (cell == null) {
            return;
        }

        CellType cellType = cell.getCellType();
        
        // 문자열 셀인 경우
        if (cellType == CellType.STRING) {
            String cellValue = cell.getStringCellValue();
            if (cellValue == null || cellValue.isEmpty()) {
                return;
            }

            String replacedValue = cellValue;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (replacedValue.contains(entry.getKey())) {
                    replacedValue = replacedValue.replace(entry.getKey(), entry.getValue());
                }
            }

            // 값이 변경된 경우에만 셀에 다시 쓰기
            if (!replacedValue.equals(cellValue)) {
                cell.setCellValue(replacedValue);
            }
        }
        // 수식 셀인 경우 (수식의 문자열 부분만 치환)
        else if (cellType == CellType.FORMULA) {
            String formula = cell.getCellFormula();
            if (formula != null && !formula.isEmpty()) {
                String replacedFormula = formula;
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    if (replacedFormula.contains(entry.getKey())) {
                        replacedFormula = replacedFormula.replace(entry.getKey(), entry.getValue());
                    }
                }
                
                if (!replacedFormula.equals(formula)) {
                    cell.setCellFormula(replacedFormula);
                }
            }
        }
    }

    /**
     * month 에 red 컬러 적용
     * @param cell
     * @param values
     */
    private static void replaceTextInCellV2(Cell cell, Map<String, String> values) {
        if (cell == null || cell.getCellType() != CellType.STRING) return;

        String cellValue = cell.getStringCellValue();
        String resultValue = cellValue;

        // 1. 전체 치환 수행
        for (Map.Entry<String, String> entry : values.entrySet()) {
            resultValue = resultValue.replace(entry.getKey(), entry.getValue());
        }

        if (resultValue.equals(cellValue)) return;

        XSSFRichTextString richText = new XSSFRichTextString(resultValue);

        // 2. {{MONTH}}가 포함된 경우만 빨간색 적용
        if (cellValue.contains("{{MONTH}}")) {
            String monthVal = values.get("{{MONTH}}");

            Font redFont = cell.getSheet().getWorkbook().createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            redFont.setFontName("맑은 고딕");
            redFont.setFontHeightInPoints((short) 28);         // 폰트 크기 27
            redFont.setUnderline(Font.U_SINGLE);              // 한 줄 밑줄 적용
            redFont.setBold(true);

            int startIdx = resultValue.indexOf(monthVal);
            while (startIdx != -1) {
                richText.applyFont(startIdx, startIdx + monthVal.length(), redFont);
                startIdx = resultValue.indexOf(monthVal, startIdx + monthVal.length());
            }
        }

        cell.setCellValue(richText);
    }

    private static void replaceTextInParagraph(XWPFParagraph paragraph, Map<String, String> values) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text == null) continue;

            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (text.contains(entry.getKey())) {
                    text = text.replace(entry.getKey(), entry.getValue());
                }
            }
            run.setText(text, 0);
        }
    }
}

