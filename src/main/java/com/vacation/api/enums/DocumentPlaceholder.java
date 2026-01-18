package com.vacation.api.enums;

/**
 * 문서 템플릿 플레이스홀더 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
public enum DocumentPlaceholder {
    // 공통
    DOCUMENT_NUMBER("{{DOCUMENT_NUMBER}}"),
    REQUEST_DATE("{{REQUEST_DATE}}"),
    DEPARTMENT("{{DEPARTMENT}}"),
    APPLICANT("{{APPLICANT}}"),

    // 연차 신청서 (DOCX)
    PERIOD("{{PERIOD}}"),
    REQDAYS("{{REQDAYS}}"),
    VACATION_TYPE("{{VACATION_TYPE}}"),
    REASON("{{REASON}}"),
    TOTAL_VACATION_DAYS("{{TOTAL_VACATION_DAYS}}"),
    PREVIOUS_REMAINING_DAYS("{{PREVIOUS_REMAINING_DAYS}}"),
    REQUESTED_VACATION_DAYS("{{REQUESTED_VACATION_DAYS}}"),
    USED_VACATION_DAYS("{{USED_VACATION_DAYS}}"),
    FINAL_REMAINING_DAYS("{{FINAL_REMAINING_DAYS}}"),
    CURRENT_YEAR("{{CURRENT_YEAR}}"),

    // 월세지원 청구서 (XLSX)
    CONTRACT_PERIOD("{{CONTRACT_PERIOD}}"),
    CONTRACT_YEARS("{{CONTRACT_YEARS}}"),
    CONTRACT_MONTHLY_RENT("{{CONTRACT_MONTHLY_RENT}}"),
    PAYMENT_TYPE("{{PAYMENT_TYPE}}"),
    BILLING_START_DATE("{{BILLING_START_DATE}}"),
    BILLING_PERIOD_START("{{BILLING_PERIOD_START}}"),
    BILLING_PERIOD_END("{{BILLING_PERIOD_END}}"),
    BILLING_DAYS("{{BILLING_DAYS}}"),
    BILLING_PERCENTAGE("{{BILLING_PERCENTAGE}}"),
    PAYMENT_DATE("{{PAYMENT_DATE}}"),
    PAYMENT_AMOUNT("{{PAYMENT_AMOUNT}}"),
    BILLING_AMOUNT("{{BILLING_AMOUNT}}"),
    MONTH("{{MONTH}}"),

    // 월세지원 품의서 (DOCX)
    CURRENT_ADDRESS("{{CURRENT_ADDRESS}}"),
    RENTAL_ADDRESS("{{RENTAL_ADDRESS}}"),
    CONTRACT_MONTH("{{CONTRACT_MONTH}}"),

    // 테이블 헤더
    USAGE_DETAIL("{{USAGE_DETAIL}}"),
    VENDOR("{{VENDOR}}"),
    PAYMENT_METHOD("{{PAYMENT_METHOD}}"),
    PROJECT("{{PROJECT}}"),
    AMOUNT("{{AMOUNT}}"),
    NOTE("{{NOTE}}"),

    // 업무관련 개인 비용 청구서
    DEPARTMENT_NAME("{{DEPARTMENT_NAME}}"),
    DATE("{{DATE}}"),
    TOTAL_AMOUNT("{{TOTAL_AMOUNT}}");

    private final String placeholder;

    DocumentPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * 플레이스홀더 문자열로 Enum 찾기
     */
    public static DocumentPlaceholder fromPlaceholder(String placeholder) {
        for (DocumentPlaceholder dp : values()) {
            if (dp.placeholder.equals(placeholder)) {
                return dp;
            }
        }
        return null;
    }
}

