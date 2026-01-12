package com.vacation.api.domain.vacation.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.vacation.api.domain.user.entity.QUser.user;
import static com.vacation.api.domain.vacation.entity.QVacationHistory.vacationHistory;

/**
 * VacationHistoryRepository 커스텀 구현체
 * QueryDSL을 사용한 복잡한 쿼리 구현
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-09
 */
@Repository
@RequiredArgsConstructor
public class VacationHistoryRepositoryImpl implements VacationHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<VacationHistory> findByUserIdsAndDateRange(List<Long> userIds, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .selectFrom(vacationHistory)
                .where(
                        vacationHistory.userId.in(userIds)
                                .and(
                                        // 휴가 기간이 조회 범위와 겹치는 경우
                                        // startDate <= endDate AND endDate >= startDate
                                        vacationHistory.startDate.loe(endDate)
                                                .and(vacationHistory.endDate.goe(startDate))
                                )
                )
                .orderBy(vacationHistory.startDate.asc())
                .fetch();
    }

    @Override
    public List<VacationHistory> findByDivisionAndDateRange(String division, List<String> authVals, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .selectFrom(vacationHistory)
                .innerJoin(user).on(vacationHistory.userId.eq(user.userId))
                .where(
                        user.division.eq(division)
                                .and(user.authVal.in(authVals))
                                .and(
                                        // 휴가 기간이 조회 범위와 겹치는 경우
                                        vacationHistory.startDate.loe(endDate)
                                                .and(vacationHistory.endDate.goe(startDate))
                                )
                )
                .orderBy(vacationHistory.startDate.asc())
                .fetch();
    }
}
