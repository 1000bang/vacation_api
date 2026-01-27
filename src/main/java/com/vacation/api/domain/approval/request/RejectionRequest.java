package com.vacation.api.domain.approval.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 반려 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectionRequest {

    /**
     * 반려 사유
     */
    @NotBlank(message = "반려 사유를 입력해주세요.")
    private String rejectionReason;
}
