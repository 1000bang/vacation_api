package com.vacation.api.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:vacation-api-secret-key-change-this-in-production-environment-minimum-256-bits}")
    private String secret;

    @Value("${jwt.expiration:3600000}") // Access Token: 기본 1시간 (밀리초)
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}") // Refresh Token: 기본 7일 (밀리초)
    private Long refreshExpiration;

    /**
     * JWT Secret Key 생성
     *
     * @return SecretKey
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     *
     * @param userId 사용자 ID
     * @param email 이메일
     * @return Access Token
     */
    public String generateAccessToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * @param userId 사용자 ID
     * @param email 이메일
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * JWT 토큰 생성 (하위 호환성을 위한 메서드)
     *
     * @param userId 사용자 ID
     * @param email 이메일
     * @return Access Token
     */
    @Deprecated
    public String generateToken(Long userId, String email) {
        return generateAccessToken(userId, email);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * JWT 토큰에서 이메일 추출
     *
     * @param token JWT 토큰
     * @return 이메일
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * JWT 토큰에서 Claims 추출
     *
     * @param token JWT 토큰
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT 토큰 유효성 검증
     *
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 타입 확인 (access 또는 refresh)
     *
     * @param token JWT 토큰
     * @return 토큰 타입
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * Refresh Token인지 확인
     *
     * @param token JWT 토큰
     * @return Refresh Token 여부
     */
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }
}

