package com.vacation.api.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 팀 관리 Entity
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-22
 */
@Entity
@Table(name = "tbl_team_management")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "users") // 순환 참조 방지
public class TeamManagement {

    /**
     * 팀 관리 시퀀스 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    /**
     * 본부
     */
    @Column(name = "division", nullable = false)
    private String division;

    /**
     * 팀 (null이면 본부만, 값이 있으면 해당 팀)
     */
    @Column(name = "team")
    private String team;

    /**
     * 생성일
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 해당 팀에 속한 사용자 목록
     */
    @OneToMany(mappedBy = "teamManagement", fetch = FetchType.LAZY)
    @JsonIgnore // JSON 직렬화 시 순환 참조 방지
    private List<User> users;

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
