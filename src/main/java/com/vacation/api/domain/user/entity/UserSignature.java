package com.vacation.api.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 서명 정보 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-19
 */
@Entity
@Table(name = "tbl_users_signature",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_seq"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignature {

    /**
     * 시퀀스 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    /**
     * 사용자 시퀀스 (FK)
     */
    @Column(name = "user_seq", nullable = false)
    private Long userSeq;

    /**
     * 서명 파일명 (예: 1_signature.png)
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 폰트명 (폰트 선택 시 폰트명, 캔버스 그리기 시 "none")
     */
    @Column(name = "font_name", nullable = false, length = 100)
    private String fontName;

    /**
     * 생성일
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

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
