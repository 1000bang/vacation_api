package com.vacation.api.domain.alarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 알람 엔티티
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-12
 */
@Entity
@Table(name = "tbl_users_alarm")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "alarm_type", length = 30, nullable = false)
    private String alarmType; // AS_07, AS_03, AS_05, AS_08 (ApprovalStatus enum의 code)

    @Column(name = "application_type", length = 20, nullable = false)
    private String applicationType; // VACATION, EXPENSE, RENTAL, RENTAL_PROPOSAL (ApplicationType enum의 code)

    @Column(name = "application_seq", nullable = false)
    private Long applicationSeq;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "redirect_url", length = 200)
    private String redirectUrl; // RU_001, RU_002 (RedirectUrl enum의 code)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
