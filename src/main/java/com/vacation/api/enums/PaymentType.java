package com.vacation.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 선불/후불 구분 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
public enum PaymentType {
    PREPAID("선불"),
    POSTPAID("후불");

    private final String value;

    PaymentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

