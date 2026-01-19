package com.vacation.api.util;

import com.vacation.api.enums.SignaturePlaceholder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 서명 이미지 생성 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-16
 */
@Slf4j
@Component
public class SignatureImageUtil {

    private static final float DEFAULT_FONT_SIZE = 60.0f; // 기본 폰트 크기

    /**
     * TTF 폰트 파일 로드
     *
     * @param fontFileName 폰트 파일명 (예: "나눔손글씨 강부장님체.ttf")
     * @return 로드된 Font 객체
     * @throws IOException 폰트 파일 로드 실패 시
     * @throws FontFormatException 폰트 형식 오류 시
     */
    public Font loadFont(String fontFileName) throws IOException, FontFormatException {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/" + fontFileName);
            if (!fontResource.exists()) {
                throw new IOException("폰트 파일을 찾을 수 없습니다: " + fontFileName);
            }

            try (InputStream fontInputStream = fontResource.getInputStream()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontInputStream);
                log.debug("폰트 로드 완료: {}", fontFileName);
                return font;
            }
        } catch (Exception e) {
            log.error("폰트 로드 실패: {}", fontFileName, e);
            throw e;
        }
    }

    /**
     * 사용자 이름을 폰트로 렌더링하여 PNG 이미지 생성
     * 백그라운드 없는 투명 PNG 이미지를 생성합니다.
     *
     * @param userName 사용자 이름
     * @param fontFileName 폰트 파일명 (예: "나눔손글씨 강부장님체.ttf")
     * @param signatureSize 서명 크기 (SIG1 또는 SIG2)
     * @return PNG 이미지 바이트 배열
     * @throws IOException 이미지 생성 실패 시
     */
    public byte[] generateSignatureImage(String userName, String fontFileName, SignaturePlaceholder.SignatureSize signatureSize) throws IOException {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이름이 비어있습니다.");
        }

        try {
            // 폰트 로드
            Font baseFont = loadFont(fontFileName);
            
            // 서명 크기에 따라 폰트 크기 조정
            float fontSize = signatureSize == SignaturePlaceholder.SignatureSize.SIG1 
                ? DEFAULT_FONT_SIZE 
                : DEFAULT_FONT_SIZE * 0.6f; // SIG2는 작게
            
            Font font = baseFont.deriveFont(fontSize);

            // 텍스트 크기 측정을 위한 임시 Graphics 객체 생성
            BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tempG2d = tempImage.createGraphics();
            tempG2d.setFont(font);
            FontMetrics fontMetrics = tempG2d.getFontMetrics();
            
            // 텍스트 크기 계산
            int textWidth = fontMetrics.stringWidth(userName);
            int textHeight = fontMetrics.getHeight();
            int textAscent = fontMetrics.getAscent();
            
            tempG2d.dispose();

            // 이미지 크기 결정 (텍스트 크기 + 최소 여백)
            int padding = 5; // 여백을 최소화 (20 -> 5)
            int imageWidth = textWidth + padding * 2; // 최소 크기 제한 제거
            int imageHeight = textHeight + padding * 2; // 최소 크기 제한 제거

            // 투명 배경 이미지 생성
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            try {
                // 렌더링 품질 설정
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // 투명 배경 (이미 TYPE_INT_ARGB로 생성했으므로 투명함)
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, imageWidth, imageHeight);
                g2d.setComposite(AlphaComposite.SrcOver);

                // 폰트 및 색상 설정
                g2d.setFont(font);
                g2d.setColor(Color.BLACK);

                // 텍스트 그리기 (중앙 정렬)
                int x = (imageWidth - textWidth) / 2;
                int y = padding + textAscent; // 상단 여백 최소화

                g2d.drawString(userName, x, y);

            } finally {
                g2d.dispose();
            }

            // PNG로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            log.debug("서명 이미지 생성 완료: userName={}, font={}, size={}x{}, bytes={}", 
                    userName, fontFileName, imageWidth, imageHeight, imageBytes.length);

            return imageBytes;

        } catch (FontFormatException e) {
            log.error("폰트 형식 오류: {}", fontFileName, e);
            throw new IOException("폰트 형식 오류: " + fontFileName, e);
        } catch (Exception e) {
            log.error("서명 이미지 생성 실패: userName={}, font={}", userName, fontFileName, e);
            throw new IOException("서명 이미지 생성 실패", e);
        }
    }

    /**
     * 날짜를 폰트로 렌더링하여 PNG 이미지 생성
     * SIG2 크기로 날짜 이미지를 생성합니다.
     *
     * @param date 날짜 문자열 (예: "2025.01.16")
     * @param fontFileName 폰트 파일명
     * @return PNG 이미지 바이트 배열
     * @throws IOException 이미지 생성 실패 시
     */
    public byte[] generateDateImage(String date, String fontFileName) throws IOException {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("날짜가 비어있습니다.");
        }

        try {
            // 폰트 로드
            Font baseFont = loadFont(fontFileName);
            
            // 날짜는 작은 크기로 표시
            float fontSize = DEFAULT_FONT_SIZE * 0.4f;
            Font font = baseFont.deriveFont(fontSize);

            // 텍스트 크기 측정
            BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tempG2d = tempImage.createGraphics();
            tempG2d.setFont(font);
            FontMetrics fontMetrics = tempG2d.getFontMetrics();
            
            int textWidth = fontMetrics.stringWidth(date);
            int textHeight = fontMetrics.getHeight();
            int textAscent = fontMetrics.getAscent();
            
            tempG2d.dispose();

            // 이미지 크기 결정 (날짜는 작은 여백)
            int padding = 3; // 날짜 이미지 여백 최소화 (10 -> 3)
            int imageWidth = textWidth + padding * 2;
            int imageHeight = textHeight + padding * 2;

            // 투명 배경 이미지 생성
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            try {
                // 렌더링 품질 설정
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // 투명 배경
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, imageWidth, imageHeight);
                g2d.setComposite(AlphaComposite.SrcOver);

                // 폰트 및 색상 설정
                g2d.setFont(font);
                g2d.setColor(Color.BLACK);

                // 텍스트 그리기
                int x = padding;
                int y = padding + textAscent;
                g2d.drawString(date, x, y);

            } finally {
                g2d.dispose();
            }

            // PNG로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            log.debug("날짜 이미지 생성 완료: date={}, font={}, size={}x{}, bytes={}", 
                    date, fontFileName, imageWidth, imageHeight, imageBytes.length);

            return imageBytes;

        } catch (FontFormatException e) {
            log.error("폰트 형식 오류: {}", fontFileName, e);
            throw new IOException("폰트 형식 오류: " + fontFileName, e);
        } catch (Exception e) {
            log.error("날짜 이미지 생성 실패: date={}, font={}", date, fontFileName, e);
            throw new IOException("날짜 이미지 생성 실패", e);
        }
    }
}
