package com.vacation.api.domain.expense.repository;

import com.vacation.api.domain.expense.entity.ExpenseSub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 개인 비용 청구 상세 항목 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Repository
public interface ExpenseSubRepository extends JpaRepository<ExpenseSub, Long> {

    /**
     * 부모 시퀀스로 상세 항목 목록 조회 (자식 번호 순)
     *
     * @param parentSeq 부모 시퀀스
     * @return 상세 항목 목록
     */
    List<ExpenseSub> findByParentSeqOrderByChildNoAsc(Long parentSeq);

    /**
     * 부모 시퀀스로 상세 항목 삭제
     *
     * @param parentSeq 부모 시퀀스
     */
    void deleteByParentSeq(Long parentSeq);
}

