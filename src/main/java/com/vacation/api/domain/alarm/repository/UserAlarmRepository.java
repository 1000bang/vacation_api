package com.vacation.api.domain.alarm.repository;

import com.vacation.api.domain.alarm.entity.UserAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사용자 알람 리포지토리
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-12
 */
@Repository
public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    /**
     * 사용자의 읽지 않은 알람 목록 조회
     */
    List<UserAlarm> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 모든 알람 목록 조회
     */
    List<UserAlarm> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 알람 읽음 처리
     */
    @Modifying
    @Query("UPDATE UserAlarm u SET u.isRead = true WHERE u.seq = :seq")
    void markAsRead(@Param("seq") Long seq);

    /**
     * 사용자의 모든 알람 읽음 처리
     */
    @Modifying
    @Query("UPDATE UserAlarm u SET u.isRead = true WHERE u.userId = :userId AND u.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}
