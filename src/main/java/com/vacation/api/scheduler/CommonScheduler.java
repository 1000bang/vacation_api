package com.vacation.api.scheduler;

import com.vacation.api.domain.alarm.repository.UserAlarmRepository;
import com.vacation.api.domain.vacation.entity.UserVacationInfo;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.repository.UserVacationInfoRepository;
import com.vacation.api.domain.vacation.repository.VacationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 공통 스케줄러
 * - 연차 상태 업데이트
 * - 7일 경과된 읽은 알람 삭제
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonScheduler {

    private final VacationHistoryRepository vacationHistoryRepository;
    private final UserVacationInfoRepository userVacationInfoRepository;
    private final UserAlarmRepository userAlarmRepository;

    /**
     * 매일 12시에 실행되는 스케줄러
     * 종료일이 오늘인 휴가 내역 중 status가 'R'인 항목을 찾아서:
     * - status를 'C'로 변경
     * - RESERVED_VACATION_DAYS에서 제외
     * - USED_VACATION_DAYS에 추가
     */
    @Scheduled(cron = "0 0 12 * * ?") // 매일 12시에 실행
    @Transactional
    public void updateVacationStatus() {
        log.info("연차 상태 업데이트 스케줄러 시작");
        
        LocalDate today = LocalDate.now();
        log.info("오늘 날짜: {}", today);
        
        // 종료일이 오늘이고 status가 'R'인 휴가 내역 조회
        List<VacationHistory> completedVacations = vacationHistoryRepository.findByEndDate(today)
                .stream()
                .filter(v -> "R".equals(v.getStatus()))
                .toList();
        
        if (completedVacations.isEmpty()) {
            log.info("종료일이 오늘이고 status가 'R'인 휴가 내역이 없습니다.");
            return;
        }
        
        log.info("종료일이 오늘이고 status가 'R'인 휴가 내역 {}건 발견", completedVacations.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (VacationHistory vacation : completedVacations) {
            try {
                Long userId = vacation.getUserId();
                Double period = vacation.getPeriod();
                
                Double usedVacationDays = vacation.getUsedVacationDays() != null ? vacation.getUsedVacationDays() : 0.0;
                
                log.info("휴가 내역 처리 시작: seq={}, userId={}, period={}, usedVacationDays={}", 
                        vacation.getSeq(), userId, period, usedVacationDays);
                
                // 사용자별 연차 정보 조회
                UserVacationInfo vacationInfo = userVacationInfoRepository.findByUserId(userId)
                        .orElseGet(() -> {
                            log.warn("연차 정보가 없어 새로 생성: userId={}", userId);
                            // 연차 정보가 없으면 새로 생성
                            UserVacationInfo newInfo = UserVacationInfo.builder()
                                    .userId(userId)
                                    .annualVacationDays(0.0)
                                    .usedVacationDays(0.0)
                                    .reservedVacationDays(0.0)
                                    .build();
                            return userVacationInfoRepository.save(newInfo);
                        });
                
                // 연차 차감이 필요한 경우에만 UserVacationInfo 업데이트
                if (usedVacationDays > 0) {
                    // RESERVED_VACATION_DAYS에서 제외
                    Double currentReserved = vacationInfo.getReservedVacationDays();
                    Double newReserved = Math.max(0.0, currentReserved - usedVacationDays);
                    vacationInfo.setReservedVacationDays(newReserved);
                    
                    // USED_VACATION_DAYS에 추가
                    Double currentUsed = vacationInfo.getUsedVacationDays();
                    Double newUsed = currentUsed + usedVacationDays;
                    vacationInfo.setUsedVacationDays(newUsed);
                    
                    // 저장
                    userVacationInfoRepository.save(vacationInfo);
                    
                    log.info("연차 상태 업데이트 완료: userId={}, usedVacationDays={}, reserved: {} -> {}, used: {} -> {}", 
                            userId, usedVacationDays, currentReserved, newReserved, currentUsed, newUsed);
                } else {
                    log.info("연차 차감이 없는 휴가 내역: seq={}, userId={}, usedVacationDays={}", 
                            vacation.getSeq(), userId, usedVacationDays);
                }
                
                // status를 'R'에서 'C'로 변경
                vacation.setStatus("C");
                vacationHistoryRepository.save(vacation);
                
                
                successCount++;
            } catch (Exception e) {
                log.error("휴가 내역 처리 실패: seq={}, userId={}", 
                        vacation.getSeq(), vacation.getUserId(), e);
                failCount++;
            }
        }
        
        log.info("연차 상태 업데이트 스케줄러 완료: 성공={}, 실패={}", successCount, failCount);
    }

    /**
     * 매일 새벽 2시에 실행되는 스케줄러
     * 7일 경과된 읽은 알람을 삭제합니다.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시에 실행
    @Transactional
    public void deleteOldReadAlarms() {
        log.info("7일 경과된 읽은 알람 삭제 스케줄러 시작");
        
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        log.info("삭제 기준일: {} (7일 이전)", sevenDaysAgo);
        
        try {
            // 7일 경과된 읽은 알람 삭제
            int deletedCount = userAlarmRepository.deleteByIsReadTrueAndCreatedAtBefore(sevenDaysAgo);
            
            log.info("7일 경과된 읽은 알람 삭제 완료: 삭제된 알람 수={}", deletedCount);
        } catch (Exception e) {
            log.error("7일 경과된 읽은 알람 삭제 실패", e);
        }
    }
}
