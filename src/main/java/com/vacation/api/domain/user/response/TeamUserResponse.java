package com.vacation.api.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팀별 사용자 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamUserResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 이름
     */
    private String name;

    /**
     * 직급
     */
    private String position;

    /**
     * 권한 (tj: 팀장, tw: 팀원)
     */
    private String authVal;

    /**
     * 권한 라벨
     */
    public String getAuthValLabel() {
        if ("tj".equals(authVal)) {
            return "팀장";
        } else if ("tw".equals(authVal)) {
            return "팀원";
        }
        return authVal;
    }
}
