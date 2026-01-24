package com.vacation.api.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 팀 관리 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamManagementResponse {

    /**
     * 팀 관리 시퀀스
     */
    private Long seq;

    /**
     * 본부
     */
    private String division;

    /**
     * 팀 (null이면 본부만)
     */
    private String team;

    /**
     * 생성일
     */
    private LocalDateTime createdAt;

    /**
     * 사용자 수 (해당 팀에 속한 사용자 수)
     */
    private Long userCount;
}
