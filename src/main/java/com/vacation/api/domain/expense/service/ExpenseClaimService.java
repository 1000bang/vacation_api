package com.vacation.api.domain.expense.service;

import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.expense.repository.ExpenseSubRepository;
import com.vacation.api.domain.expense.request.ExpenseClaimRequest;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 개인 비용 청구 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseClaimService {

    private final ExpenseClaimRepository expenseClaimRepository;
    private final ExpenseSubRepository expenseSubRepository;

    /**
     * 개인 비용 청구 목록 조회
     *
     * @param userId 사용자 ID
     * @return 개인 비용 청구 목록
     */
    public List<ExpenseClaim> getExpenseClaimList(Long userId) {
        log.info("개인 비용 청구 목록 조회: userId={}", userId);
        return expenseClaimRepository.findByUserIdOrderBySeqDesc(userId);
    }

    /**
     * 개인 비용 청구 조회
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @return 개인 비용 청구 정보 (없으면 null)
     */
    public ExpenseClaim getExpenseClaim(Long seq, Long userId) {
        log.info("개인 비용 청구 조회: seq={}, userId={}", seq, userId);
        return expenseClaimRepository.findBySeqAndUserId(seq, userId)
                .orElse(null);
    }

    /**
     * 개인 비용 청구 상세 항목 목록 조회
     *
     * @param parentSeq 부모 시퀀스
     * @return 상세 항목 목록
     */
    public List<ExpenseSub> getExpenseSubList(Long parentSeq) {
        log.info("개인 비용 청구 상세 항목 목록 조회: parentSeq={}", parentSeq);
        return expenseSubRepository.findByParentSeqOrderByChildNoAsc(parentSeq);
    }

    /**
     * 개인 비용 청구 생성
     *
     * @param userId 사용자 ID
     * @param request 개인 비용 청구 요청 데이터
     * @return 생성된 개인 비용 청구 정보
     */
    @Transactional
    public ExpenseClaim createExpenseClaim(Long userId, ExpenseClaimRequest request) {
        log.info("개인 비용 청구 생성: userId={}", userId);

        // 총 금액 계산
        Long totalAmount = request.getExpenseItems().stream()
                .mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L)
                .sum();

        // 청구 년월 계산 (YYYYMM 형식)
        // requestDate가 1월이고 month가 12이면 전년도 12월로 설정
        int requestYear = request.getRequestDate().getYear();
        int requestMonth = request.getRequestDate().getMonthValue();
        int month = request.getMonth();
        int year = requestYear;
        // requestDate가 1월이고 month가 12이면 전년도 사용
        if (requestMonth == 1 && month == 12) {
            year = requestYear - 1;
        }
        int billingYyMonth = year * 100 + month;

        // 부모 엔티티 생성
        ExpenseClaim expenseClaim = ExpenseClaim.builder()
                .userId(userId)
                .requestDate(request.getRequestDate())
                .billingYyMonth(billingYyMonth)
                .childCnt(request.getExpenseItems().size())
                .totalAmount(totalAmount)
                .build();

        ExpenseClaim saved = expenseClaimRepository.save(expenseClaim);

        // 자식 엔티티 생성
        List<ExpenseSub> expenseSubs = IntStream.range(0, request.getExpenseItems().size())
                .mapToObj(i -> {
                    var item = request.getExpenseItems().get(i);
                    return ExpenseSub.builder()
                            .parentSeq(saved.getSeq())
                            .childNo(i + 1)
                            .date(item.getDate())
                            .usageDetail(item.getUsageDetail())
                            .vendor(item.getVendor())
                            .paymentMethod(item.getPaymentMethod())
                            .project(item.getProject())
                            .amount(item.getAmount())
                            .note(item.getNote())
                            .build();
                })
                .toList();

        expenseSubRepository.saveAll(expenseSubs);

        log.info("개인 비용 청구 생성 완료: seq={}, userId={}, totalAmount={}", saved.getSeq(), userId, totalAmount);
        return saved;
    }

    /**
     * 개인 비용 청구 수정
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     * @param request 개인 비용 청구 요청 데이터
     * @return 수정된 개인 비용 청구 정보
     */
    @Transactional
    public ExpenseClaim updateExpenseClaim(Long seq, Long userId, ExpenseClaimRequest request) {
        log.info("개인 비용 청구 수정: seq={}, userId={}", seq, userId);

        ExpenseClaim expenseClaim = expenseClaimRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });

        // 총 금액 계산
        Long totalAmount = request.getExpenseItems().stream()
                .mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L)
                .sum();

        // 청구 년월 계산 (YYYYMM 형식)
        // requestDate가 1월이고 month가 12이면 전년도 12월로 설정
        int requestYear = request.getRequestDate().getYear();
        int requestMonth = request.getRequestDate().getMonthValue();
        int month = request.getMonth();
        int year = requestYear;
        // requestDate가 1월이고 month가 12이면 전년도 사용
        if (requestMonth == 1 && month == 12) {
            year = requestYear - 1;
        }
        int billingYyMonth = year * 100 + month;

        // 부모 엔티티 수정
        expenseClaim.setRequestDate(request.getRequestDate());
        expenseClaim.setBillingYyMonth(billingYyMonth);
        expenseClaim.setChildCnt(request.getExpenseItems().size());
        expenseClaim.setTotalAmount(totalAmount);

        // 기존 자식 항목 삭제
        expenseSubRepository.deleteByParentSeq(seq);

        // 새로운 자식 항목 생성
        List<ExpenseSub> expenseSubs = IntStream.range(0, request.getExpenseItems().size())
                .mapToObj(i -> {
                    var item = request.getExpenseItems().get(i);
                    return ExpenseSub.builder()
                            .parentSeq(seq)
                            .childNo(i + 1)
                            .date(item.getDate())
                            .usageDetail(item.getUsageDetail())
                            .vendor(item.getVendor())
                            .paymentMethod(item.getPaymentMethod())
                            .project(item.getProject())
                            .amount(item.getAmount())
                            .note(item.getNote())
                            .build();
                })
                .toList();

        expenseSubRepository.saveAll(expenseSubs);
        ExpenseClaim updated = expenseClaimRepository.save(expenseClaim);

        log.info("개인 비용 청구 수정 완료: seq={}, userId={}, totalAmount={}", seq, userId, totalAmount);
        return updated;
    }

    /**
     * 개인 비용 청구 삭제
     *
     * @param seq 시퀀스
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteExpenseClaim(Long seq, Long userId) {
        log.info("개인 비용 청구 삭제: seq={}, userId={}", seq, userId);

        ExpenseClaim expenseClaim = expenseClaimRepository.findBySeqAndUserId(seq, userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 개인 비용 청구: seq={}, userId={}", seq, userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });

        // 자식 항목 삭제
        expenseSubRepository.deleteByParentSeq(seq);

        // 부모 항목 삭제
        expenseClaimRepository.delete(expenseClaim);

        log.info("개인 비용 청구 삭제 완료: seq={}, userId={}", seq, userId);
    }
}

