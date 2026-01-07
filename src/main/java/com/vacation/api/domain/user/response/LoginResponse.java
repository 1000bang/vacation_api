package com.vacation.api.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 이메일
     */
    private String email;

    /**
     * 이름
     */
    private String name;

    /**
     * 본부
     */
    private String division;

    /**
     * 팀
     */
    private String team;

    /**
     * 직급
     */
    private String position;

    /**
     * 사용자 상태
     */
    private String status;
}

