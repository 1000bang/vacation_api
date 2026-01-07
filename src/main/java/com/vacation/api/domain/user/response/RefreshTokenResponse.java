package com.vacation.api.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    /**
     * 새로운 Access Token
     */
    private String accessToken;
}

