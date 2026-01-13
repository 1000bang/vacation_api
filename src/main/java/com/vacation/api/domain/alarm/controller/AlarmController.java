package com.vacation.api.domain.alarm.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.alarm.entity.UserAlarm;
import com.vacation.api.domain.alarm.service.AlarmService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알람 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-12
 */
@Slf4j
@RestController
@RequestMapping("/alarm")
public class AlarmController extends BaseController {

    private final AlarmService alarmService;

    public AlarmController(TransactionIDCreator transactionIDCreator, AlarmService alarmService) {
        super(transactionIDCreator);
        this.alarmService = alarmService;
    }

    /**
     * 읽지 않은 알람 목록 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadAlarms(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<UserAlarm> alarms = alarmService.getUnreadAlarms(userId);
            return successResponse(alarms);
        } catch (Exception e) {
            log.error("알람 조회 실패", e);
            return errorResponse("알람 조회에 실패했습니다.", e);
        }
    }

    /**
     * 모든 알람 목록 조회
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllAlarms(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<UserAlarm> alarms = alarmService.getAllAlarms(userId);
            return successResponse(alarms);
        } catch (Exception e) {
            log.error("알람 조회 실패", e);
            return errorResponse("알람 조회에 실패했습니다.", e);
        }
    }

    /**
     * 알람 읽음 처리
     */
    @PutMapping("/{seq}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long seq, HttpServletRequest request) {
        try {
            alarmService.markAsRead(seq);
            return successResponse("알람을 읽음 처리했습니다.");
        } catch (Exception e) {
            log.error("알람 읽음 처리 실패", e);
            return errorResponse("알람 읽음 처리에 실패했습니다.", e);
        }
    }

    /**
     * 모든 알람 읽음 처리
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            alarmService.markAllAsRead(userId);
            return successResponse("모든 알람을 읽음 처리했습니다.");
        } catch (Exception e) {
            log.error("알람 읽음 처리 실패", e);
            return errorResponse("알람 읽음 처리에 실패했습니다.", e);
        }
    }
}
