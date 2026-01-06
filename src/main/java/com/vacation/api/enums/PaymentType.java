package com.vacation.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static PaymentType fromString(String value) {
        if (value == null) {
            return null;
        }
        // Enum 이름으로 매칭 (PREPAID, POSTPAID)
        for (PaymentType type : PaymentType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        // 한글 값으로 매칭 (선불, 후불)
        for (PaymentType type : PaymentType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentType: " + value);
    }
}

