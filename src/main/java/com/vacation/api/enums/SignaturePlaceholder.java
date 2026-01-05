package com.vacation.api.enums;

/**
 * 서명 이미지 플레이스홀더 Enum
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
public enum SignaturePlaceholder {
    // 담당
    DAM_SIG1("{{DAM_SIG1}}", SignatureType.DAM, SignatureSize.SIG1),
    DAM_SIG2("{{DAM_SIG2}}", SignatureType.DAM, SignatureSize.SIG2),

    // 팀장
    TIM_SIG1("{{TIM_SIG1}}", SignatureType.TIM, SignatureSize.SIG1),
    TIM_SIG2("{{TIM_SIG2}}", SignatureType.TIM, SignatureSize.SIG2),

    // 부장
    BU_SIG1("{{BU_SIG1}}", SignatureType.BU, SignatureSize.SIG1),
    BU_SIG2("{{BU_SIG2}}", SignatureType.BU, SignatureSize.SIG2),

    // 대표이사
    DEA_SIG1("{{DEA_SIG1}}", SignatureType.DEA, SignatureSize.SIG1),
    DEA_SIG2("{{DEA_SIG2}}", SignatureType.DEA, SignatureSize.SIG2);

    private final String placeholder;
    private final SignatureType type;
    private final SignatureSize size;

    SignaturePlaceholder(String placeholder, SignatureType type, SignatureSize size) {
        this.placeholder = placeholder;
        this.type = type;
        this.size = size;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public SignatureType getType() {
        return type;
    }

    public SignatureSize getSize() {
        return size;
    }

    /**
     * SIG1인지 여부
     */
    public boolean isSig1() {
        return size == SignatureSize.SIG1;
    }

    /**
     * 플레이스홀더 문자열로 Enum 찾기
     */
    public static SignaturePlaceholder fromPlaceholder(String placeholder) {
        for (SignaturePlaceholder sp : values()) {
            if (sp.placeholder.equals(placeholder)) {
                return sp;
            }
        }
        return null;
    }

    /**
     * 모든 서명 플레이스홀더 배열 반환
     */
    public static SignaturePlaceholder[] getAll() {
        return values();
    }

    /**
     * 서명 타입 Enum
     */
    public enum SignatureType {
        DAM,  // 담당
        TIM,  // 팀장
        BU,   // 부장
        DEA   // 대표이사
    }

    /**
     * 서명 크기 Enum
     */
    public enum SignatureSize {
        SIG1(720000, 540000, 0.10, 0.80),  // 첫 번째 서명 (큰 크기)
        SIG2(720000, 360000, 0.10, 0.20);  // 두 번째 서명 (작은 크기)

        // DOCX 크기 (EMU 단위)
        private final int docxWidthEMU;
        private final int docxHeightEMU;

        // XLSX 크기 (인치 단위)
        private final double xlsxWidthInches;
        private final double xlsxHeightInches;

        SignatureSize(int docxWidthEMU, int docxHeightEMU, double xlsxWidthInches, double xlsxHeightInches) {
            this.docxWidthEMU = docxWidthEMU;
            this.docxHeightEMU = docxHeightEMU;
            this.xlsxWidthInches = xlsxWidthInches;
            this.xlsxHeightInches = xlsxHeightInches;
        }

        /**
         * DOCX 이미지 너비 (EMU)
         */
        public int getDocxWidthEMU() {
            return docxWidthEMU;
        }

        /**
         * DOCX 이미지 높이 (EMU)
         */
        public int getDocxHeightEMU() {
            return docxHeightEMU;
        }

        /**
         * XLSX 이미지 너비 (인치)
         */
        public double getXlsxWidthInches() {
            return xlsxWidthInches;
        }

        /**
         * XLSX 이미지 높이 (인치)
         */
        public double getXlsxHeightInches() {
            return xlsxHeightInches;
        }
    }
}

