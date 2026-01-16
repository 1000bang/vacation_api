package com.vacation.api.domain.expense.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vacation.api.domain.attachment.response.AttachmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 개인 비용 상세 항목 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS) // null 값도 포함하여 직렬화
public class ExpenseSubResponse {
    private Long seq;
    private Long expenseClaimSeq; // parentSeq 매핑
    private Long parentSeq; // 호환성을 위해 추가
    private Integer childNo;
    private LocalDate date;
    private String usageDetail; // itemName과 동일한 값
    private String itemName; // usageDetail 매핑 (호환성)
    private String vendor;
    private String paymentMethod;
    private String project;
    private Long amount;
    private String note; // description과 동일한 값
    private String description; // note 매핑 (호환성)
    private LocalDateTime createdAt;
    private AttachmentResponse attachment;
}
