package com.vacation.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 리다이렉트 URL Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-17
 */
@Getter
@RequiredArgsConstructor
public enum RedirectUrl {
    MY_APPLICATIONS("RU_001", "/my-applications", "내 신청 목록"),
    APPROVAL_LIST("RU_002", "/approval-list", "결재 목록");

    private final String code;
    private final String url;
    private final String description;

    /**
     * 코드로 RedirectUrl 찾기
     *
     * @param code 코드
     * @return RedirectUrl
     */
    public static RedirectUrl fromCode(String code) {
        for (RedirectUrl redirectUrl : values()) {
            if (redirectUrl.code.equals(code)) {
                return redirectUrl;
            }
        }
        throw new IllegalArgumentException("Unknown redirect url code: " + code);
    }

    /**
     * URL로 RedirectUrl 찾기
     *
     * @param url URL
     * @return RedirectUrl
     */
    public static RedirectUrl fromUrl(String url) {
        for (RedirectUrl redirectUrl : values()) {
            if (redirectUrl.url.equals(url)) {
                return redirectUrl;
            }
        }
        throw new IllegalArgumentException("Unknown redirect url: " + url);
    }
}
