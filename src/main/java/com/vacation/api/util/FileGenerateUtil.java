package com.vacation.api.util;

import com.vacation.api.domain.sample.request.ExpenseClaimSampleRequest;
import com.vacation.api.domain.sample.request.ExpenseItem;
import com.vacation.api.domain.sample.request.RentalSupportPropSampleRequest;
import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.sample.request.VacationSampleRequest;
import com.vacation.api.enums.DocumentPlaceholder;
import com.vacation.api.enums.SignaturePlaceholder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
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
     * 서명 기능 활성화 여부
     * false: 서명 기능 비활성화 (현재 설정)
     * true: 서명 기능 활성화
     */
    private static final boolean isSig = false;

    /**
     * 연차 신청서 Doc 생성
     *
     * @param request 연차 신청 요청 데이터
     * @return DOCX 바이트 배열
     */
    public static byte[] generateVacationApplicationDoc(VacationSampleRequest request) {
        return generateVacationApplicationDoc(request, null);
    }

    /**
     * 연차 신청서 Doc 생성 (서명 이미지 맵 포함)
     *
     * @param request 연차 신청 요청 데이터
     * @param signatureImageMap 서명 이미지 맵 (플레이스홀더 -> 이미지 바이트 배열, null이면 빈 문자열로 치환)
     * @return DOCX 바이트 배열
     */
    public static byte[] generateVacationApplicationDoc(VacationSampleRequest request, Map<String, byte[]> signatureImageMap) {
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
            values.put(DocumentPlaceholder.DOCUMENT_NUMBER.getPlaceholder(), documentNumber);
            values.put(DocumentPlaceholder.REQUEST_DATE.getPlaceholder(), formatDate(request.getRequestDate()));
            values.put(DocumentPlaceholder.DEPARTMENT.getPlaceholder(), request.getDepartment());
            values.put(DocumentPlaceholder.APPLICANT.getPlaceholder(), request.getApplicant());
            values.put(DocumentPlaceholder.PERIOD.getPlaceholder(), period);
            values.put(DocumentPlaceholder.REQDAYS.getPlaceholder(), formatVacationDays(request.getRequestedVacationDays()));
            values.put(DocumentPlaceholder.VACATION_TYPE.getPlaceholder(), request.getVacationType().getValue());
            values.put(DocumentPlaceholder.REASON.getPlaceholder(), request.getReason() != null ? request.getReason() : "");
            values.put(DocumentPlaceholder.TOTAL_VACATION_DAYS.getPlaceholder(), formatVacationDays(request.getTotalVacationDays()));
            values.put(DocumentPlaceholder.PREVIOUS_REMAINING_DAYS.getPlaceholder(), formatVacationDays(request.getRemainingVacationDays()));
            values.put(DocumentPlaceholder.REQUESTED_VACATION_DAYS.getPlaceholder(), formatVacationDays(request.getRequestedVacationDays()));
            values.put(DocumentPlaceholder.FINAL_REMAINING_DAYS.getPlaceholder(), formatVacationDays(finalRemainingDays));
            values.put(DocumentPlaceholder.CURRENT_YEAR.getPlaceholder(), String.valueOf(currentYear));

            // 서명 플레이스홀더 처리 (isSig가 false이면 빈 문자열로 치환)
            if (!isSig) {
                for (SignaturePlaceholder sigPlaceholder : SignaturePlaceholder.values()) {
                    values.put(sigPlaceholder.getPlaceholder(), "");
                }
            }

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

            // 서명 이미지 치환 (8개 플레이스홀더)
            if (isSig) {
                replaceSignatureImagesInDocument(document, signatureImageMap);
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
        return generateRentalSupportApplicationExcel(request, null);
    }

    /**
     * 월세지원 품의서 Doc 생성
     *
     * @param request 월세지원 품의서 요청 데이터
     * @return DOCX 바이트 배열
     */
    public static byte[] generateRentalSupportProposalDoc(RentalSupportPropSampleRequest request) {
        return generateRentalSupportProposalDoc(request, null);
    }

    /**
     * 월세지원 품의서 Doc 생성 (서명 이미지 맵 포함)
     *
     * @param request 월세지원 품의서 요청 데이터
     * @param signatureImageMap 서명 이미지 맵 (플레이스홀더 -> 이미지 바이트 배열, null이면 빈 문자열로 치환)
     * @return DOCX 바이트 배열
     */
    public static byte[] generateRentalSupportProposalDoc(RentalSupportPropSampleRequest request, Map<String, byte[]> signatureImageMap) {
        try {
            // 템플릿 파일 로드 (resources/templates/rental-support-proposal.docx)
            ClassPathResource templateResource = new ClassPathResource("templates/rental-support-proposal.docx");
            
            if (!templateResource.exists()) {
                throw new RuntimeException("템플릿 파일을 찾을 수 없습니다: templates/rental-support-proposal.docx. " +
                        "resources/templates/ 폴더에 .docx 형식의 템플릿 파일을 추가해주세요.");
            }
            
            InputStream templateInputStream = templateResource.getInputStream();
            XWPFDocument document = new XWPFDocument(templateInputStream);

            // 문서 번호 생성
            String documentNumber = generateDocumentNumber(request.getRequestDate());
            
            // 계약 기간 문자열 생성
            String contractPeriod = formatPeriod(request.getContractStartDate(), request.getContractEndDate());
            
            // 계약 개월 수 계산
            long contractMonths = ChronoUnit.MONTHS.between(
                    request.getContractStartDate(), 
                    request.getContractEndDate()
            );

            // 데이터 매핑
            Map<String, String> values = new HashMap<>();
            values.put(DocumentPlaceholder.DOCUMENT_NUMBER.getPlaceholder(), documentNumber);
            values.put(DocumentPlaceholder.REQUEST_DATE.getPlaceholder(), formatDate(request.getRequestDate()));
            values.put(DocumentPlaceholder.DEPARTMENT.getPlaceholder(), request.getDepartment());
            values.put(DocumentPlaceholder.APPLICANT.getPlaceholder(), request.getApplicant());
            values.put(DocumentPlaceholder.CURRENT_ADDRESS.getPlaceholder(), request.getCurrentAddress() != null ? request.getCurrentAddress() : "");
            values.put(DocumentPlaceholder.RENTAL_ADDRESS.getPlaceholder(), request.getRentalAddress() != null ? request.getRentalAddress() : "");
            values.put(DocumentPlaceholder.CONTRACT_PERIOD.getPlaceholder(), contractPeriod);
            values.put(DocumentPlaceholder.CONTRACT_MONTH.getPlaceholder(), String.valueOf(contractMonths));
            values.put(DocumentPlaceholder.PAYMENT_AMOUNT.getPlaceholder(), formatNumber(request.getContractMonthlyRent()));
            values.put(DocumentPlaceholder.BILLING_AMOUNT.getPlaceholder(), formatNumber(request.getBillingAmount()));
            values.put(DocumentPlaceholder.BILLING_START_DATE.getPlaceholder(), formatDate(request.getBillingStartDate()));
            values.put(DocumentPlaceholder.REASON.getPlaceholder(), request.getReason() != null ? request.getReason() : "");

            // 서명 플레이스홀더 처리 (isSig가 false이면 빈 문자열로 치환)
            if (!isSig) {
                for (SignaturePlaceholder sigPlaceholder : SignaturePlaceholder.values()) {
                    values.put(sigPlaceholder.getPlaceholder(), "");
                }
            }

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

            // 서명 이미지 치환 (8개 플레이스홀더)
            if (isSig) {
                replaceSignatureImagesInDocument(document, signatureImageMap);
            }

            // ByteArrayOutputStream에 문서 쓰기
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);

            document.close();
            templateInputStream.close();
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("품의서 DOC 생성 중 오류 발생", e);
            throw new RuntimeException("품의서 DOC 생성 실패", e);
        }
    }

    /**
     * 월세지원 청구서 Excel 생성 (서명 이미지 맵 포함)
     *
     * @param request 월세지원 청구 요청 데이터
     * @param signatureImageMap 서명 이미지 맵 (플레이스홀더 -> 이미지 바이트 배열, null이면 빈 문자열로 치환)
     * @return XLSX 바이트 배열
     */
    public static byte[] generateRentalSupportApplicationExcel(RentalSupportSampleRequest request, Map<String, byte[]> signatureImageMap) {
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
            
            // 계약 기간 문자열 생성 (시작일 ~ 종료일)
            String contractPeriod = formatPeriod(request.getContractStartDate(), request.getContractEndDate());
            
            // 계약 기간 상세 계산 ("1년", "2년", "2년 6개월" 형식)
            String contractPeriodDetail = calculateContractPeriod(
                    request.getContractStartDate(),
                    request.getContractEndDate()
            );
            
            // 청구 기간 일수 계산
            long billingDays = ChronoUnit.DAYS.between(
                    request.getBillingPeriodStartDate(),
                    request.getBillingPeriodEndDate()
            ) + 1; // 시작일 포함

            // Excel 수식 형식: "(" & DATEDIF(C13, F13, "D")+1 & "/" & DATEDIF(C13, F13, "D")+1 & " 일)"
            String billingDaysFormatted = String.format("(%d/%d일)", billingDays, billingDays);

            // 청구 기간 비율 계산 (항상 100%)
            double billingPercentage = 100.0;
            
            // 데이터 매핑
            Map<String, String> values = new HashMap<>();
            values.put(DocumentPlaceholder.DOCUMENT_NUMBER.getPlaceholder(), documentNumber);
            values.put(DocumentPlaceholder.REQUEST_DATE.getPlaceholder(), formatDate(request.getRequestDate()));
            values.put(DocumentPlaceholder.DEPARTMENT.getPlaceholder(), request.getDepartment());
            values.put(DocumentPlaceholder.APPLICANT.getPlaceholder(), request.getApplicant());
            values.put(DocumentPlaceholder.CONTRACT_PERIOD.getPlaceholder(), contractPeriod);
            values.put(DocumentPlaceholder.CONTRACT_YEARS.getPlaceholder(), contractPeriodDetail);
            values.put(DocumentPlaceholder.CONTRACT_MONTHLY_RENT.getPlaceholder(), formatNumber(request.getContractMonthlyRent()));
            values.put(DocumentPlaceholder.PAYMENT_TYPE.getPlaceholder(), request.getPaymentType().getValue());
            values.put(DocumentPlaceholder.BILLING_START_DATE.getPlaceholder(), formatDate(request.getBillingStartDate()));
            values.put(DocumentPlaceholder.BILLING_PERIOD_START.getPlaceholder(), formatDateShort(request.getBillingPeriodStartDate()));
            values.put(DocumentPlaceholder.BILLING_PERIOD_END.getPlaceholder(), formatDateShort(request.getBillingPeriodEndDate()));
            values.put(DocumentPlaceholder.BILLING_DAYS.getPlaceholder(), billingDaysFormatted);
            values.put(DocumentPlaceholder.BILLING_PERCENTAGE.getPlaceholder(), String.format("%.2f", billingPercentage)+"%");
            values.put(DocumentPlaceholder.PAYMENT_DATE.getPlaceholder(), formatDateShort(request.getPaymentDate()));
            values.put(DocumentPlaceholder.PAYMENT_AMOUNT.getPlaceholder(), formatNumber(request.getPaymentAmount()));
            values.put(DocumentPlaceholder.BILLING_AMOUNT.getPlaceholder(), formatNumber(request.getBillingAmount()));
            values.put(DocumentPlaceholder.MONTH.getPlaceholder(), "( "+String.valueOf(request.getMonth())+" 월)");

            // 서명 플레이스홀더 처리 (isSig가 false이면 빈 문자열로 치환)
            if (!isSig) {
                for (SignaturePlaceholder sigPlaceholder : SignaturePlaceholder.values()) {
                    values.put(sigPlaceholder.getPlaceholder(), "");
                }
            }

            // 모든 시트를 순회하며 텍스트 치환 및 이미지 치환
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                replaceTextInSheet(sheet, values);
            }

            // 서명 이미지 치환 (8개 플레이스홀더)
            if (isSig) {
                replaceSignatureImagesInWorkbook(workbook, signatureImageMap);
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
     * 업무관련 개인 비용 청구서 Excel 생성
     *
     * @param request 업무관련 개인 비용 청구서 요청 데이터
     * @return XLSX 바이트 배열
     */
    public static byte[] generateExpenseClaimExcel(ExpenseClaimSampleRequest request) {
        return generateExpenseClaimExcel(request, null);
    }

    /**
     * 업무관련 개인 비용 청구서 Excel 생성 (서명 이미지 맵 포함)
     *
     * @param request 업무관련 개인 비용 청구서 요청 데이터
     * @param signatureImageMap 서명 이미지 맵 (플레이스홀더 -> 이미지 바이트 배열, null이면 빈 문자열로 치환)
     * @return XLSX 바이트 배열
     */
    public static byte[] generateExpenseClaimExcel(ExpenseClaimSampleRequest request, Map<String, byte[]> signatureImageMap) {
        try {
            // 템플릿 파일 로드 (resources/templates/expense-claim.xlsx)
            ClassPathResource templateResource = new ClassPathResource("templates/expense-claim.xlsx");
            
            if (!templateResource.exists()) {
                throw new RuntimeException("템플릿 파일을 찾을 수 없습니다: templates/expense-claim.xlsx. " +
                        "resources/templates/ 폴더에 .xlsx 형식의 템플릿 파일을 추가해주세요.");
            }
            
            InputStream templateInputStream = templateResource.getInputStream();
            Workbook workbook = new XSSFWorkbook(templateInputStream);

            // 문서 번호 생성
            String documentNumber = generateDocumentNumber(request.getRequestDate());

            // DEPARTMENT_NAME 생성: department를 "/" 기준으로 자르고, 뒤에 있는 부분과 "/" 그리고 applicant를 붙임
            String departmentName = buildDepartmentName(request.getDepartment(), request.getApplicant());

            // 총액 계산 (모든 expenseItems의 amount 합계)
            long totalAmount = 0;
            if (request.getExpenseItems() != null) {
                for (ExpenseItem item : request.getExpenseItems()) {
                    if (item.getAmount() != null) {
                        totalAmount += item.getAmount();
                    }
                }
            }


            // 데이터 매핑
            Map<String, String> values = new HashMap<>();
            values.put(DocumentPlaceholder.DOCUMENT_NUMBER.getPlaceholder(), documentNumber);
            values.put(DocumentPlaceholder.REQUEST_DATE.getPlaceholder(), formatDate(request.getRequestDate()));
            values.put(DocumentPlaceholder.DEPARTMENT_NAME.getPlaceholder(), departmentName);
            values.put(DocumentPlaceholder.MONTH.getPlaceholder(), "(  "+String.valueOf(request.getMonth())+"월  )");
            values.put(DocumentPlaceholder.TOTAL_AMOUNT.getPlaceholder(), formatNumber(totalAmount));

            // 서명 플레이스홀더 처리 (isSig가 false이면 빈 문자열로 치환)
            if (!isSig) {
                for (SignaturePlaceholder sigPlaceholder : SignaturePlaceholder.values()) {
                    values.put(sigPlaceholder.getPlaceholder(), "");
                }
            }

            // 모든 시트를 순회하며 텍스트 치환
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                replaceTextInSheet(sheet, values);
                
                // 비용 항목 데이터 행 치환
                replaceExpenseItemsInSheet(sheet, request.getExpenseItems());
            }

            // 서명 이미지 치환 (8개 플레이스홀더)
            if (isSig) {
                replaceSignatureImagesInWorkbook(workbook, signatureImageMap);
            }

            // ByteArrayOutputStream에 Excel 쓰기
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            workbook.close();
            templateInputStream.close();
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("비용 청구서 Excel 생성 중 오류 발생", e);
            throw new RuntimeException("비용 청구서 Excel 생성 실패", e);
        }
    }

    /**
     * Excel 시트에서 비용 항목 데이터 행 치환
     * 템플릿에 {{DATE}}, {{USAGE_DETAIL}}, {{VENDOR}}, {{PAYMENT_METHOD}}, {{PROJECT}}, {{AMOUNT}}, {{NOTE}} 플레이스홀더가 있는 행을 찾아서
     * expenseItems 리스트의 데이터로 치환합니다.
     */
    private static void replaceExpenseItemsInSheet(Sheet sheet, List<ExpenseItem> expenseItems) {
        if (expenseItems == null || expenseItems.isEmpty()) {
            return;
        }

        // 플레이스홀더가 있는 행 찾기
        int templateRowIndex = -1;
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    if (cellValue != null && cellValue.contains("{{DATE}}")) {
                        templateRowIndex = row.getRowNum();
                        break;
                    }
                }
            }
            if (templateRowIndex != -1) {
                break;
            }
        }

        if (templateRowIndex == -1) {
            log.warn("비용 항목 템플릿 행을 찾을 수 없습니다. {{DATE}} 플레이스홀더가 있는 행이 필요합니다.");
            return;
        }

        // 템플릿 행 복사하여 각 비용 항목에 대해 행 생성
        Row templateRow = sheet.getRow(templateRowIndex);
        if (templateRow == null) {
            return;
        }

        for (int i = 0; i < expenseItems.size(); i++) {
            ExpenseItem item = expenseItems.get(i);
            
            // 첫 번째 항목은 템플릿 행을 직접 사용, 나머지는 새 행 생성
            Row targetRow;
            if (i == 0) {
                targetRow = templateRow;
            } else {
                targetRow = sheet.createRow(templateRowIndex + i);
            }
            
            // 템플릿 행의 셀 구조 복사 (새 행인 경우)
            if (i > 0) {
                for (int cellIndex = 0; cellIndex < templateRow.getLastCellNum(); cellIndex++) {
                    Cell templateCell = templateRow.getCell(cellIndex);
                    if (templateCell == null) {
                        continue;
                    }
                    Cell newCell = targetRow.createCell(cellIndex);
                    CellStyle cellStyle = templateCell.getCellStyle();
                    newCell.setCellStyle(cellStyle);
                }
            }
            
            // 셀 값 치환
            for (int cellIndex = 0; cellIndex < templateRow.getLastCellNum(); cellIndex++) {
                Cell templateCell = templateRow.getCell(cellIndex);
                if (templateCell == null) {
                    continue;
                }

                Cell targetCell = targetRow.getCell(cellIndex);
                if (targetCell == null) {
                    continue;
                }

                // 템플릿 셀의 값 가져오기
                String cellValue = "";
                if (templateCell.getCellType() == CellType.STRING) {
                    cellValue = templateCell.getStringCellValue();
                }

                // 플레이스홀더 치환
                String replacedValue = cellValue;
                if (cellValue != null) {
                    replacedValue = cellValue
                            .replace(DocumentPlaceholder.DATE.getPlaceholder(), item.getDate() != null ? formatDateShort(item.getDate()) : "")
                            .replace(DocumentPlaceholder.USAGE_DETAIL.getPlaceholder(), item.getUsageDetail() != null ? item.getUsageDetail() : "")
                            .replace(DocumentPlaceholder.VENDOR.getPlaceholder(), item.getVendor() != null ? item.getVendor() : "")
                            .replace(DocumentPlaceholder.PAYMENT_METHOD.getPlaceholder(), item.getPaymentMethod() != null ? item.getPaymentMethod() : "")
                            .replace(DocumentPlaceholder.PROJECT.getPlaceholder(), item.getProject() != null ? item.getProject() : "")
                            .replace(DocumentPlaceholder.AMOUNT.getPlaceholder(), item.getAmount() != null ? formatNumber(item.getAmount()) : "")
                            .replace(DocumentPlaceholder.NOTE.getPlaceholder(), item.getNote() != null ? item.getNote() : "");
                }

                targetCell.setCellValue(replacedValue);
            }
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

    /**
     * 서명 이미지 로드
     */
    private static byte[] loadSignatureImage() {
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

    /**
     * DOCX 문서에서 서명 이미지 치환
     */
    private static void replaceSignatureImagesInDocument(XWPFDocument document, Map<String, byte[]> signatureImageMap) {
        if (signatureImageMap == null) {
            signatureImageMap = new HashMap<>();
        }

        // 모든 서명 플레이스홀더 목록
        SignaturePlaceholder[] signaturePlaceholders = SignaturePlaceholder.getAll();

        for (SignaturePlaceholder sigPlaceholder : signaturePlaceholders) {
            String placeholder = sigPlaceholder.getPlaceholder();
            byte[] imageBytes = signatureImageMap.get(placeholder);
            
            // 문단에서 치환
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (imageBytes != null) {
                    replaceTextWithImageInParagraph(paragraph, placeholder, imageBytes, sigPlaceholder);
                } else {
                    replaceTextInParagraph(paragraph, Map.of(placeholder, ""));
                }
            }

            // 테이블에서 치환
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            if (imageBytes != null) {
                                replaceTextWithImageInParagraph(paragraph, placeholder, imageBytes, sigPlaceholder);
                            } else {
                                replaceTextInParagraph(paragraph, Map.of(placeholder, ""));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * XLSX 워크북에서 서명 이미지 치환
     */
    private static void replaceSignatureImagesInWorkbook(Workbook workbook, Map<String, byte[]> signatureImageMap) {
        if (signatureImageMap == null) {
            signatureImageMap = new HashMap<>();
        }

        // 모든 서명 플레이스홀더 목록
        SignaturePlaceholder[] signaturePlaceholders = SignaturePlaceholder.getAll();

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            
            for (SignaturePlaceholder sigPlaceholder : signaturePlaceholders) {
                String placeholder = sigPlaceholder.getPlaceholder();
                byte[] imageBytes = signatureImageMap.get(placeholder);
                
                if (imageBytes != null) {
                    replaceTextWithImageInSheet(sheet, placeholder, imageBytes, sigPlaceholder);
                } else {
                    // 빈 문자열로 치환
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            if (cell != null && cell.getCellType() == CellType.STRING) {
                                String cellValue = cell.getStringCellValue();
                                if (cellValue != null && cellValue.contains(placeholder)) {
                                    cell.setCellValue(cellValue.replace(placeholder, ""));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * DOCX 문단에서 텍스트를 이미지로 치환
     * 
     * @param paragraph 문단
     * @param placeholder 플레이스홀더
     * @param imageBytes 이미지 바이트 배열
     * @param sigPlaceholder 서명 플레이스홀더 Enum
     */
    private static void replaceTextWithImageInParagraph(XWPFParagraph paragraph, String placeholder, byte[] imageBytes, SignaturePlaceholder sigPlaceholder) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text == null || !text.contains(placeholder)) {
                continue;
            }

            // 플레이스홀더 제거
            String newText = text.replace(placeholder, "");
            run.setText(newText, 0);

            // 이미지 추가
            try (ByteArrayInputStream imageStream = new ByteArrayInputStream(imageBytes)) {
                // SignatureSize Enum에서 크기 정보 가져오기
                SignaturePlaceholder.SignatureSize sigSize = sigPlaceholder.getSize();
                
                int widthEMU = sigSize.getDocxWidthEMU();
                int heightEMU = sigSize.getDocxHeightEMU();
                
                run.addPicture(imageStream, XWPFDocument.PICTURE_TYPE_PNG, "signature", widthEMU, heightEMU);
            } catch (Exception e) {
                log.error("DOCX에 이미지 추가 중 오류 발생", e);
            }
        }
    }

    /**
     * XLSX 시트에서 텍스트를 이미지로 치환
     * 
     * @param sheet 시트
     * @param placeholder 플레이스홀더
     * @param imageBytes 이미지 바이트 배열
     * @param sigPlaceholder 서명 플레이스홀더 Enum
     */
    private static void replaceTextWithImageInSheet(Sheet sheet, String placeholder, byte[] imageBytes, SignaturePlaceholder sigPlaceholder) {
        if (!(sheet instanceof XSSFSheet)) {
            return;
        }

        XSSFSheet xssfSheet = (XSSFSheet) sheet;
        XSSFWorkbook workbook = xssfSheet.getWorkbook();

        // SignatureSize Enum에서 크기 정보 가져오기
        SignaturePlaceholder.SignatureSize sigSize = sigPlaceholder.getSize();
        
        double widthInches = sigSize.getXlsxWidthInches();
        double heightInches = sigSize.getXlsxHeightInches();

        // 이미지를 workbook에 한 번만 추가
        int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);

        // Drawing 객체 가져오기 또는 생성
        XSSFDrawing drawing = xssfSheet.createDrawingPatriarch();
        if (drawing == null) {
            drawing = xssfSheet.getDrawingPatriarch();
        }

        // 플레이스홀더가 있는 첫 번째 셀에만 이미지 삽입
        boolean imageInserted = false;
        for (Row row : xssfSheet) {
            for (Cell cell : row) {
                if (cell == null || cell.getCellType() != CellType.STRING) {
                    continue;
                }

                String cellValue = cell.getStringCellValue();
                if (cellValue == null || !cellValue.contains(placeholder)) {
                    continue;
                }

                // 텍스트 제거
                cell.setCellValue(cellValue.replace(placeholder, ""));

                // 이미지는 첫 번째 매칭된 셀에만 삽입
                if (!imageInserted) {
                    try {
                        // 이미지 위치 설정 (셀에 맞춤)
                        // dx1, dy1: 시작 위치 (0, 0)
                        // dx2, dy2: 종료 위치 (인치를 EMU로 변환: 1인치 = 914400 EMU)
                        XSSFClientAnchor anchor = new XSSFClientAnchor(
                            0, 0,
                            (int)(widthInches * 914400), (int)(heightInches * 914400),
                            (short) cell.getColumnIndex(),
                            cell.getRowIndex(),
                            (short) (cell.getColumnIndex() + 1),
                            cell.getRowIndex() + 1
                        );

                        // 이미지 삽입
                        drawing.createPicture(anchor, pictureIdx);
                        imageInserted = true;
                    } catch (Exception e) {
                        log.error("XLSX에 이미지 추가 중 오류 발생", e);
                    }
                }
            }
        }
    }

    /**
     * DEPARTMENT_NAME 생성
     * department를 "/" 기준으로 자르고, 뒤에 있는 부분과 "/" 그리고 applicant를 붙임
     * 예: "서비스본부/서비스개발2팀" + "천병재" → "서비스개발2팀/천병재"
     */
    private static String buildDepartmentName(String department, String applicant) {
        if (department == null || department.isEmpty()) {
            return applicant != null ? applicant : "";
        }
        
        if (applicant == null || applicant.isEmpty()) {
            return department;
        }

        // "/" 기준으로 분리
        String[] parts = department.split("/");
        if (parts.length > 1) {
            // "/"가 있으면 마지막 부분 사용
            return parts[parts.length - 1] + "/" + applicant;
        } else {
            // "/"가 없으면 전체 department 사용
            return department + "/" + applicant;
        }
    }
}

