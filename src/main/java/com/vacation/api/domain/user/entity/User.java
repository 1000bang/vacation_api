package com.vacation.api.domain.user.entity;

import com.vacation.api.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 Entity
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
@Entity
@Table(name = "tbl_users_bas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 사용자 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /**
     * 이메일
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * 이름
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 비밀번호 (암호화된 값)
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * 본부
     */
    @Column(name = "division", nullable = false)
    private String division;

    /**
     * 팀
     */
    @Column(name = "team", nullable = false)
    private String team;

    /**
     * 직급
     */
    @Column(name = "position", nullable = false)
    private String position;

    /**
     * 사용자 상태 (승인전, 승인, 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    /**
     * 생성일
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 로그인 일시
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 비밀번호 변경일
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * 비밀번호 변경 유무
     */
    @Column(name = "password_changed", nullable = false)
    @Builder.Default
    private Boolean passwordChanged = false;

    /**
     * 최초 로그인 유무
     */
    @Column(name = "first_login", nullable = false)
    @Builder.Default
    private Boolean firstLogin = true;

    /**
     * Refresh Token
     */
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    /**
     * 엔티티 저장 전 실행 (생성일 설정)
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

