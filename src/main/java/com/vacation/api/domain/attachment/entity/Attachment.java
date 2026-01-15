package com.vacation.api.domain.attachment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 첨부파일 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-15
 */
@Entity
@Table(name = "tbl_attachment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    /**
     * 시퀀스 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    /**
     * 신청 타입 (VACATION, RENTAL, RENTAL_APPROVAL, EXPENSE)
     */
    @Column(name = "application_type", nullable = false, length = 20)
    private String applicationType;

    /**
     * 신청 시퀀스 (vacation_seq, rental_support_seq 등)
     */
    @Column(name = "application_seq", nullable = false)
    private Long applicationSeq;

    /**
     * 파일명
     */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /**
     * 파일 경로
     */
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    /**
     * 파일 크기 (bytes)
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 파일 순서 (개인비용용, 여러개일 때 순서)
     */
    @Column(name = "file_order")
    private Integer fileOrder;

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
