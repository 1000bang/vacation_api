package com.vacation.api.domain.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 사용자 정보 수정 요청 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    /**
     * 본부
     */
    @NotBlank(message = "본부는 필수입니다")
    private String division;

    /**
     * 팀 (본부장인 경우 null 가능)
     */
    private String team;

    /**
     * 직급
     */
    @NotBlank(message = "직급은 필수입니다")
    private String position;

    /**
     * 입사일 (선택)
     */
    private LocalDate joinDate;

    /**
     * 사용자 상태 (선택)
     */
    private String status;

    /**
     * 권한 값 (선택)
     */
    private String authVal;

    /**
     * 최초 로그인 유무 (선택)
     */
    private Boolean firstLogin;
}

