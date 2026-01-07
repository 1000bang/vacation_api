package com.vacation.api.domain.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
public class RefreshTokenRequest {

    /**
     * Refresh Token
     */
    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}

