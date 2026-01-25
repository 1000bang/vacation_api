package com.vacation.api.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원가입 응답 DTO
 * Entity 노출 최소화 (password, refreshToken 등 제외)
 *
 * @author vacation-api
 * @since 2026-01-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinResponse {
    private Long userId;
    private String email;
    private String name;
    private String status;
}
