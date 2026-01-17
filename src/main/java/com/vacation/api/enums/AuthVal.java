package com.vacation.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 권한 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-17
 */
@Getter
@RequiredArgsConstructor
public enum AuthVal {
    MASTER("ma", "MASTER", "관리자"),
    DIVISION_HEAD("bb", "DIVISION_HEAD", "본부장"),
    TEAM_LEADER("tj", "TEAM_LEADER", "팀장"),
    TEAM_MEMBER("tw", "TEAM_MEMBER", "팀원");

    private final String code;
    private final String name;
    private final String description;

    /**
     * 이름으로 AuthVal 찾기
     *
     * @param name 이름 (ma, bb, tj, tw)
     * @return AuthVal
     */
    public static AuthVal fromCode(String code) {
        if (code == null) {
            return TEAM_MEMBER; // null은 팀원으로 간주
        }
        for (AuthVal authVal : values()) {
            if (authVal.code.equals(code)) {
                return authVal;
            }
        }
        throw new IllegalArgumentException("Unknown auth val code: " + code);
    }
}
