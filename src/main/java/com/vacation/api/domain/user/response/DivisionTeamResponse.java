package com.vacation.api.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 본부별 팀 목록 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DivisionTeamResponse {

    /**
     * 본부
     */
    private String division;

    /**
     * 팀 목록 (정렬됨)
     */
    private List<String> teams;
}
