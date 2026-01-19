package com.vacation.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서명용 폰트 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-19
 */
@Getter
@RequiredArgsConstructor
public enum SignatureFont {
    KANG_BUJANG("강부장님체", "nanum-kang-bujang.ttf", "Y"),
    KANG_INHAN("강인한위로", "nanum-kang-inhan.ttf", "Y"),
    NAMU_JEONGWON("나무정원", "nanum-namu-jeongwon.ttf", "Y"),
    DAE_GWANG("대광유리", "nanum-dae-gwang.ttf", "Y"),
    YEOL_IL("열일체", "nanum-yeol-il.ttf", "Y"),
    WILD("와일드", "nanum-wild.ttf", "Y"),
    HYEOK_I("혁이체", "nanum-hyeok-i.ttf", "Y");

    private final String displayName;  // 화면에 표시될 이름
    private final String fileName;     // 실제 파일명
    private final String useYn;        // 사용 여부 (Y: 사용, N: 미사용)

    /**
     * 파일명으로 SignatureFont 찾기
     *
     * @param fileName 파일명
     * @return SignatureFont, 없으면 null
     */
    public static SignatureFont findByFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        for (SignatureFont font : values()) {
            if (font.fileName.equals(fileName)) {
                return font;
            }
        }
        return null;
    }

    /**
     * 사용 가능한 폰트만 필터링
     *
     * @return 사용 가능한 폰트 목록
     */
    public static SignatureFont[] getAvailableFonts() {
        return java.util.Arrays.stream(values())
                .filter(font -> "Y".equals(font.useYn))
                .toArray(SignatureFont[]::new);
    }
}
