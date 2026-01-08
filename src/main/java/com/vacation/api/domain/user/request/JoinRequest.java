package com.vacation.api.domain.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 회원가입 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
@Data
public class JoinRequest {

    /**
     * 이메일
     */
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    /**
     * 이름
     */
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    /**
     * 본부
     */
    @NotBlank(message = "본부는 필수입니다.")
    private String division;

    /**
     * 팀
     */
    @NotBlank(message = "팀은 필수입니다.")
    private String team;

    /**
     * 직급
     */
    @NotBlank(message = "직급은 필수입니다.")
    private String position;

    /**
     * 입사일
     */
    @NotNull(message = "입사일은 필수입니다.")
    private LocalDate joinDate;
}

