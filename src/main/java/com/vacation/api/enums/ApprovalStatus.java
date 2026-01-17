package com.vacation.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 승인 상태 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-17
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalStatus {
    // 승인 상태
    INITIAL("AS_01", "A", "요청"),
    MODIFIED("AS_02", "AM", "수정후 재요청"),
    TEAM_LEADER_APPROVED("AS_03", "B", "팀장 승인"),
    TEAM_LEADER_REJECTED("AS_04", "RB", "팀장 반려"),
    DIVISION_HEAD_APPROVED("AS_05", "C", "본부장 승인"),
    DIVISION_HEAD_REJECTED("AS_06", "RC", "본부장 반려");

    private final String code;
    private final String name;
    private final String description;

    /**
     * 코드로 ApprovalStatus 찾기
     *
     * @param code 코드 (AS_01, AS_02, ...)
     * @return ApprovalStatus
     */
    public static ApprovalStatus fromCode(String code) {
        for (ApprovalStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown approval status code: " + code);
    }

    /**
     * 이름으로 ApprovalStatus 찾기 (기존 값과의 호환성)
     *
     * @param name 이름 (A, AM, B, RB, C, RC)
     * @return ApprovalStatus
     */
    public static ApprovalStatus fromName(String name) {
        if (name == null) {
            return INITIAL; // null은 초기 상태로 간주
        }
        for (ApprovalStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown approval status name: " + name);
    }

    /**
     * 코드 또는 이름으로 ApprovalStatus 찾기 (마이그레이션 지원)
     *
     * @param value 코드 또는 이름
     * @return ApprovalStatus
     */
    public static ApprovalStatus fromCodeOrName(String value) {
        if (value == null) {
            return INITIAL;
        }
        // 먼저 코드로 시도
        try {
            return fromCode(value);
        } catch (IllegalArgumentException e) {
            // 코드가 아니면 이름으로 시도
            return fromName(value);
        }
    }
}
