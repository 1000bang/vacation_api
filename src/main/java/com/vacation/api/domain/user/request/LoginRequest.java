package com.vacation.api.domain.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 로그인 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
public class LoginRequest {

    /**
     * 이메일
     */
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}

