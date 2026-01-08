package com.vacation.api.enums;

/**
 * 휴가 구분 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
public enum VacationType {
    YEONCHA("연차"),
    GYEONGJO("경조"),
    CHULSAN("출산"),
    GYEOLGEUN("결근"),
    BYEONGGA("병가"),
    GITA("기타"),
    YEBIGUN("예비군"),
    AM_HALF("오전 반차"),
    PM_HALF("오후 반차");

    private final String value;

    VacationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

