package com.vacation.api.enums;

/**
 * 사용자 상태 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
public enum UserStatus {
    PENDING("승인전"),
    APPROVED("승인"),
    REJECTED("거부"),
    SUSPENDED("정지"),
    WITHDRAWN("탈퇴");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

