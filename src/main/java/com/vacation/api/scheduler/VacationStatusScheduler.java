package com.vacation.api.scheduler;

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
import java.util.List;

/**
 * 연차 상태 업데이트 스케줄러
 * 매일 12시에 실행되어 종료일이 오늘인 휴가를 처리합니다.
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VacationStatusScheduler {

    private final VacationHistoryRepository vacationHistoryRepository;
    private final UserVacationInfoRepository userVacationInfoRepository;

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
                
                log.info("휴가 내역 처리 시작: seq={}, userId={}, period={}", 
                        vacation.getSeq(), userId, period);
                
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
                
                // RESERVED_VACATION_DAYS에서 제외
                Double currentReserved = vacationInfo.getReservedVacationDays();
                Double newReserved = Math.max(0.0, currentReserved - period);
                vacationInfo.setReservedVacationDays(newReserved);
                
                // USED_VACATION_DAYS에 추가
                Double currentUsed = vacationInfo.getUsedVacationDays();
                Double newUsed = currentUsed + period;
                vacationInfo.setUsedVacationDays(newUsed);
                
                // 저장
                userVacationInfoRepository.save(vacationInfo);
                
                // status를 'R'에서 'C'로 변경
                vacation.setStatus("C");
                vacationHistoryRepository.save(vacation);
                
                log.info("연차 상태 업데이트 완료: userId={}, period={}, reserved: {} -> {}, used: {} -> {}, status: R -> C", 
                        userId, period, currentReserved, newReserved, currentUsed, newUsed);
                
                successCount++;
            } catch (Exception e) {
                log.error("휴가 내역 처리 실패: seq={}, userId={}", 
                        vacation.getSeq(), vacation.getUserId(), e);
                failCount++;
            }
        }
        
        log.info("연차 상태 업데이트 스케줄러 완료: 성공={}, 실패={}", successCount, failCount);
    }
}

