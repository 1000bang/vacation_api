package com.vacation.api.enums;

import lombok.Getter;

/**
 * 신청 타입 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Getter
public enum ApplicationType {
    VACATION("VACATION", "vacation", "휴가"),
    EXPENSE("EXPENSE", "expense", "개인비용"),
    RENTAL("RENTAL", "rental", "월세지원"),
    RENTAL_PROPOSAL("RENTAL_PROPOSAL", "rental_proposal", "월세품의");

    private final String code;
    private final String lowerCase;
    private final String description;

    ApplicationType(String code, String lowerCase, String description) {
        this.code = code;
        this.lowerCase = lowerCase;
        this.description = description;
    }

    /**
     * 코드로 ApplicationType 찾기
     *
     * @param code 코드
     * @return ApplicationType
     */
    public static ApplicationType fromCode(String code) {
        for (ApplicationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown application type code: " + code);
    }

    /**
     * 소문자 코드로 ApplicationType 찾기
     *
     * @param lowerCase 소문자 코드
     * @return ApplicationType
     */
    public static ApplicationType fromLowerCase(String lowerCase) {
        for (ApplicationType type : values()) {
            if (type.lowerCase.equals(lowerCase)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown application type lower case: " + lowerCase);
    }
}
