package com.vacation.api.domain.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팀 관리 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamManagementRequest {

    /**
     * 본부 (필수)
     */
    @NotBlank(message = "본부는 필수입니다")
    private String division;

    /**
     * 팀 (선택, null이면 본부만)
     */
    private String team;
}
