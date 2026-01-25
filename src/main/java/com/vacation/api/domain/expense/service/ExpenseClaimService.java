package com.vacation.api.domain.expense.service;

import com.vacation.api.domain.alarm.service.AlarmService;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.expense.repository.ExpenseSubRepository;
import com.vacation.api.domain.expense.request.ExpenseClaimRequest;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.util.ApprovalStatusResolver;
import com.vacation.api.vo.ExpenseClaimVO;
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
    private final AlarmService alarmService;
    private final UserRepository userRepository;
    private final ApprovalStatusResolver approvalStatusResolver;

    /**
     * 개인 비용 청구 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 개인 비용 청구 목록
     */
    public List<ExpenseClaim> getExpenseClaimList(Long userId, int page, int size) {
        log.info("개인 비용 청구 목록 조회: userId={}, page={}, size={}", userId, page, size);
        int offset = page * size;
        return expenseClaimRepository.findByUserIdOrderBySeqDescWithPaging(userId, offset, size);
    }
    
    /**
     * 개인 비용 청구 총 개수 조회
     *
     * @param userId 사용자 ID
     * @return 총 개수
     */
    public long getExpenseClaimCount(Long userId) {
        log.info("개인 비용 청구 총 개수 조회: userId={}", userId);
        return expenseClaimRepository.countByUserId(userId);
    }

    /**
     * 개인 비용 청구 조회
     *
     * @param seq 시퀀스
     * @param requesterId 요청자 사용자 ID
     * @return 개인 비용 청구 정보 (없으면 null)
     */
    public ExpenseClaim getExpenseClaim(Long seq, Long requesterId) {
        log.info("개인 비용 청구 조회: seq={}, requesterId={}", seq, requesterId);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        String authVal = requester.getAuthVal();
        
        if (AuthVal.MASTER.getCode().equals(authVal)) {
            // 관리자(ma)는 모든 개인 비용 청구 조회 가능
            return expenseClaimRepository.findById(seq)
                    .orElse(null);
        } else if (AuthVal.DIVISION_HEAD.getCode().equals(authVal)) {
            // 본부장(bb)은 자신의 본부만 모든 개인 비용 청구 조회 가능
            ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                    .orElse(null);
            if (expenseClaim != null) {
                User applicant = userRepository.findById(expenseClaim.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision())) {
                    return expenseClaim;
                }
            }
            return null;
        } else if (AuthVal.TEAM_LEADER.getCode().equals(authVal)) {
            // 팀장(tj)은 자신의 팀만 모든 개인 비용 청구 조회 가능
            ExpenseClaim expenseClaim = expenseClaimRepository.findById(seq)
                    .orElse(null);
            if (expenseClaim != null) {
                User applicant = userRepository.findById(expenseClaim.getUserId())
                        .orElse(null);
                if (applicant != null && requester.getDivision().equals(applicant.getDivision()) 
                    && requester.getTeam().equals(applicant.getTeam())) {
                    return expenseClaim;
                }
            }
            return null;
        } else {
            // 일반 사용자는 본인 신청 내역만 조회 가능
            return expenseClaimRepository.findBySeqAndUserId(seq, requesterId)
                    .orElse(null);
        }
    }

    /**
     * 개인 비용 청구 조회 (seq만으로 조회, 권한 체크 없음)
     *
     * @param seq 시퀀스
     * @return 개인 비용 청구 정보 (없으면 null)
     */
    public ExpenseClaim getExpenseClaimById(Long seq) {
        log.info("개인 비용 청구 조회: seq={}", seq);
        return expenseClaimRepository.findById(seq)
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

        // 사용자 정보 조회 (권한 확인용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        // 청구 년월 계산 (YYYYMM 형식)
        int billingYyMonth = com.vacation.api.util.BillingUtil.calculateBillingYyMonth(
                request.getRequestDate(), 
                request.getMonth()
        );
        
        // 같은 월에 개인비용 신청이 이미 존재하는지 확인
        if (expenseClaimRepository.existsByUserIdAndBillingYyMonth(userId, billingYyMonth)) {
            log.warn("해당 월에 개인비용 신청이 이미 존재함: userId={}, billingYyMonth={}", userId, billingYyMonth);
            throw new ApiException(ApiErrorCode.DUPLICATE_EXPENSE_MONTH);
        }
        
        // 권한에 따른 초기 approvalStatus 설정
        String initialApprovalStatus = approvalStatusResolver.resolveInitialApprovalStatus(user.getAuthVal());

        // 총 금액 계산
        Long totalAmount = request.getExpenseItems().stream()
                .mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L)
                .sum();

        // 부모 엔티티 생성
        ExpenseClaim expenseClaim = ExpenseClaim.builder()
                .userId(userId)
                .requestDate(request.getRequestDate())
                .billingYyMonth(billingYyMonth)
                .childCnt(request.getExpenseItems().size())
                .totalAmount(totalAmount)
                .approvalStatus(initialApprovalStatus) // 권한에 따라 초기 상태 설정 (tj: B, bb: C, 일반: A)
                .build();

        ExpenseClaim saved = expenseClaimRepository.save(expenseClaim);

        // 알람 생성: 팀장에게
        alarmService.createApplicationCreatedAlarm(userId, "EXPENSE", saved.getSeq());

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
        int billingYyMonth = com.vacation.api.util.BillingUtil.calculateBillingYyMonth(
                request.getRequestDate(), 
                request.getMonth()
        );

        // 부모 엔티티 수정
        expenseClaim.setRequestDate(request.getRequestDate());
        expenseClaim.setBillingYyMonth(billingYyMonth);
        expenseClaim.setChildCnt(request.getExpenseItems().size());
        expenseClaim.setTotalAmount(totalAmount);
        // 수정 시 무조건 AM 상태로 변경
        expenseClaim.setApprovalStatus(ApprovalStatus.MODIFIED.getName()); // 수정됨

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

    /**
     * 개인 비용 청구서 문서 생성용 VO 생성
     *
     * @param expenseClaim 개인 비용 청구 정보
     * @param expenseSubList 비용 항목 목록
     * @param user 사용자 정보
     * @return ExpenseClaimVO
     */
    public ExpenseClaimVO createExpenseClaimVO(
            ExpenseClaim expenseClaim,
            List<ExpenseSub> expenseSubList,
            com.vacation.api.domain.user.entity.User user) {
        log.info("개인 비용 청구서 문서 VO 생성: seq={}, userId={}", expenseClaim.getSeq(), expenseClaim.getUserId());
        
        String department = user.getDivision() + "/" + user.getTeam();
        int month = expenseClaim.getBillingYyMonth() % 100;
        
        // ExpenseItemVO 리스트 생성
        List<ExpenseClaimVO.ExpenseItemVO> expenseItemVOs = expenseSubList.stream()
                .map(sub -> ExpenseClaimVO.ExpenseItemVO.builder()
                        .date(sub.getDate())
                        .usageDetail(sub.getUsageDetail())
                        .vendor(sub.getVendor())
                        .paymentMethod(sub.getPaymentMethod())
                        .project(sub.getProject())
                        .amount(sub.getAmount())
                        .note(sub.getNote())
                        .build())
                .toList();
        
        return ExpenseClaimVO.builder()
                .requestDate(expenseClaim.getRequestDate())
                .month(month)
                .department(department)
                .applicant(user.getName())
                .expenseItems(expenseItemVOs)
                .build();
    }
}

