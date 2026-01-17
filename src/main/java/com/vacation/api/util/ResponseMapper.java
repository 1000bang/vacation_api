package com.vacation.api.util;

import com.vacation.api.domain.attachment.entity.Attachment;
import com.vacation.api.domain.attachment.response.AttachmentResponse;
import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.response.ExpenseClaimResponse;
import com.vacation.api.domain.expense.response.ExpenseSubResponse;
import com.vacation.api.domain.rental.entity.RentalProposal;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.response.RentalProposalResponse;
import com.vacation.api.domain.rental.response.RentalSupportResponse;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.response.VacationHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entity를 Response VO로 변환하는 Mapper 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Slf4j
@Component
public class ResponseMapper {

    /**
     * Attachment를 AttachmentResponse로 변환
     *
     * @param attachment Attachment 엔티티
     * @return AttachmentResponse
     */
    public AttachmentResponse toAttachmentResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return AttachmentResponse.builder()
                .seq(attachment.getSeq())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .build();
    }

    /**
     * RentalProposal을 RentalProposalResponse로 변환하고 applicant 추가
     *
     * @param proposal RentalProposal 엔티티
     * @param applicantName 신청자 이름
     * @param attachment 첨부파일 (선택)
     * @return RentalProposalResponse
     */
    public RentalProposalResponse toRentalProposalResponse(RentalProposal proposal, String applicantName, Attachment attachment) {
        if (proposal == null) {
            return null;
        }
        return RentalProposalResponse.builder()
                .seq(proposal.getSeq())
                .userId(proposal.getUserId())
                .applicant(applicantName != null ? applicantName : "")
                .previousAddress(proposal.getPreviousAddress())
                .rentalAddress(proposal.getRentalAddress())
                .contractStartDate(proposal.getContractStartDate())
                .contractEndDate(proposal.getContractEndDate())
                .contractMonthlyRent(proposal.getContractMonthlyRent())
                .billingAmount(proposal.getBillingAmount())
                .billingStartDate(proposal.getBillingStartDate())
                .billingReason(proposal.getBillingReason())
                .approvalStatus(proposal.getApprovalStatus())
                .createdAt(proposal.getCreatedAt())
                .attachment(toAttachmentResponse(attachment))
                .build();
    }

    /**
     * RentalSupport를 RentalSupportResponse로 변환하고 applicant 추가
     *
     * @param rental RentalSupport 엔티티
     * @param applicantName 신청자 이름
     * @param attachment 첨부파일 (선택)
     * @return RentalSupportResponse
     */
    public RentalSupportResponse toRentalSupportResponse(RentalSupport rental, String applicantName, Attachment attachment) {
        if (rental == null) {
            return null;
        }
        return RentalSupportResponse.builder()
                .seq(rental.getSeq())
                .userId(rental.getUserId())
                .applicant(applicantName != null ? applicantName : "")
                .requestDate(rental.getRequestDate())
                .billingYyMonth(rental.getBillingYyMonth())
                .contractStartDate(rental.getContractStartDate())
                .contractEndDate(rental.getContractEndDate())
                .contractMonthlyRent(rental.getContractMonthlyRent())
                .paymentType(rental.getPaymentType())
                .billingStartDate(rental.getBillingStartDate())
                .billingPeriodStartDate(rental.getBillingPeriodStartDate())
                .billingPeriodEndDate(rental.getBillingPeriodEndDate())
                .paymentDate(rental.getPaymentDate())
                .paymentAmount(rental.getPaymentAmount())
                .billingAmount(rental.getBillingAmount())
                .approvalStatus(rental.getApprovalStatus())
                .createdAt(rental.getCreatedAt())
                .attachment(toAttachmentResponse(attachment))
                .build();
    }

    /**
     * ExpenseClaim을 ExpenseClaimResponse로 변환하고 applicant 추가
     *
     * @param claim ExpenseClaim 엔티티
     * @param applicantName 신청자 이름
     * @param expenseSubList 상세 항목 목록
     * @return ExpenseClaimResponse
     */
    public ExpenseClaimResponse toExpenseClaimResponse(ExpenseClaim claim, String applicantName, List<ExpenseSubResponse> expenseSubList) {
        if (claim == null) {
            return null;
        }
        return ExpenseClaimResponse.builder()
                .seq(claim.getSeq())
                .userId(claim.getUserId())
                .applicant(applicantName != null ? applicantName : "")
                .requestDate(claim.getRequestDate())
                .billingYyMonth(claim.getBillingYyMonth())
                .childCnt(claim.getChildCnt())
                .totalAmount(claim.getTotalAmount())
                .approvalStatus(claim.getApprovalStatus())
                .createdAt(claim.getCreatedAt())
                .expenseSubList(expenseSubList)
                .build();
    }

    /**
     * ExpenseSub를 ExpenseSubResponse로 변환
     *
     * @param expenseSub ExpenseSub 엔티티
     * @param attachment 첨부파일 (선택)
     * @return ExpenseSubResponse
     */
    public ExpenseSubResponse toExpenseSubResponse(ExpenseSub expenseSub, Attachment attachment) {
        if (expenseSub == null) {
            return null;
        }
        ExpenseSubResponse response = ExpenseSubResponse.builder()
                .seq(expenseSub.getSeq())
                .expenseClaimSeq(expenseSub.getParentSeq())
                .parentSeq(expenseSub.getParentSeq())
                .childNo(expenseSub.getChildNo())
                .date(expenseSub.getDate())
                .usageDetail(expenseSub.getUsageDetail())
                .itemName(expenseSub.getUsageDetail()) // 호환성을 위해 동일한 값
                .vendor(expenseSub.getVendor())
                .paymentMethod(expenseSub.getPaymentMethod())
                .project(expenseSub.getProject())
                .amount(expenseSub.getAmount())
                .note(expenseSub.getNote())
                .description(expenseSub.getNote()) // 호환성을 위해 동일한 값
                .createdAt(expenseSub.getCreatedAt())
                .attachment(toAttachmentResponse(attachment))
                .build();
        
        // 디버깅용 로그 (개발 환경에서만)
        log.debug("ExpenseSubResponse 매핑: seq={}, date={}, usageDetail={}, vendor={}, paymentMethod={}, project={}, note={}", 
                response.getSeq(), response.getDate(), response.getUsageDetail(), 
                response.getVendor(), response.getPaymentMethod(), response.getProject(), response.getNote());
        
        return response;
    }

    /**
     * VacationHistory를 VacationHistoryResponse로 변환하고 applicant 추가
     *
     * @param history VacationHistory 엔티티
     * @param applicantName 신청자 이름
     * @param attachment 첨부파일 (선택)
     * @param rejectionReason 반려 사유 (선택)
     * @return VacationHistoryResponse
     */
    public VacationHistoryResponse toVacationHistoryResponse(VacationHistory history, String applicantName, Attachment attachment, String rejectionReason) {
        if (history == null) {
            return null;
        }
        return VacationHistoryResponse.builder()
                .seq(history.getSeq())
                .userId(history.getUserId())
                .applicant(applicantName != null ? applicantName : "")
                .startDate(history.getStartDate())
                .endDate(history.getEndDate())
                .period(history.getPeriod())
                .type(history.getType())
                .reason(history.getReason())
                .requestDate(history.getRequestDate())
                .annualVacationDays(history.getAnnualVacationDays())
                .previousRemainingDays(history.getPreviousRemainingDays())
                .usedVacationDays(history.getUsedVacationDays())
                .remainingVacationDays(history.getRemainingVacationDays())
                .status(history.getStatus())
                .approvalStatus(history.getApprovalStatus())
                .createdAt(history.getCreatedAt())
                .attachment(toAttachmentResponse(attachment))
                .rejectionReason(rejectionReason)
                .build();
    }

    /**
     * 공통 리스트 변환 헬퍼 메서드
     * applicantName 추출 로직을 재사용
     * 
     * @param entities 엔티티 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @param converter 엔티티를 Response로 변환하는 함수
     * @return Response 리스트
     */
    private <E, R> List<R> convertListWithApplicant(
            List<E> entities,
            Function<Long, User> userProvider,
            Function<E, Long> userIdExtractor,
            java.util.function.BiFunction<E, String, R> converter) {
        return entities.stream()
                .map(entity -> {
                    Long userId = userIdExtractor.apply(entity);
                    User user = userProvider.apply(userId);
                    String applicantName = user != null ? user.getName() : null;
                    return converter.apply(entity, applicantName);
                })
                .collect(Collectors.toList());
    }

    /**
     * RentalProposal 리스트를 RentalProposalResponse 리스트로 변환 (applicant 포함)
     *
     * @param proposals RentalProposal 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return RentalProposalResponse 리스트
     */
    public List<RentalProposalResponse> toRentalProposalResponseList(
            List<RentalProposal> proposals,
            Function<Long, User> userProvider) {
        return convertListWithApplicant(
                proposals,
                userProvider,
                RentalProposal::getUserId,
                (proposal, applicantName) -> toRentalProposalResponse(proposal, applicantName, null)
        );
    }

    /**
     * RentalSupport 리스트를 RentalSupportResponse 리스트로 변환 (applicant 포함)
     *
     * @param rentals RentalSupport 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return RentalSupportResponse 리스트
     */
    public List<RentalSupportResponse> toRentalSupportResponseList(
            List<RentalSupport> rentals,
            Function<Long, User> userProvider) {
        return convertListWithApplicant(
                rentals,
                userProvider,
                RentalSupport::getUserId,
                (rental, applicantName) -> toRentalSupportResponse(rental, applicantName, null)
        );
    }

    /**
     * ExpenseClaim 리스트를 ExpenseClaimResponse 리스트로 변환 (applicant 포함)
     *
     * @param claims ExpenseClaim 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return ExpenseClaimResponse 리스트
     */
    public List<ExpenseClaimResponse> toExpenseClaimResponseList(
            List<ExpenseClaim> claims,
            Function<Long, User> userProvider) {
        return convertListWithApplicant(
                claims,
                userProvider,
                ExpenseClaim::getUserId,
                (claim, applicantName) -> toExpenseClaimResponse(claim, applicantName, null)
        );
    }

    /**
     * VacationHistory 리스트를 VacationHistoryResponse 리스트로 변환 (applicant 포함)
     *
     * @param histories VacationHistory 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return VacationHistoryResponse 리스트
     */
    public List<VacationHistoryResponse> toVacationHistoryResponseList(
            List<VacationHistory> histories,
            Function<Long, User> userProvider) {
        return convertListWithApplicant(
                histories,
                userProvider,
                VacationHistory::getUserId,
                (history, applicantName) -> toVacationHistoryResponse(history, applicantName, null, null)
        );
    }

    /**
     * 승인 상태 코드를 실제 값으로 변환 (Response용)
     * DB에 저장된 코드(AS_01) 또는 기존 값(A)을 실제 값(A)으로 변환
     *
     * @param approvalStatus 코드 또는 기존 값
     * @return 실제 값 (A, AM, B, RB, C, RC)
     */
    public String convertApprovalStatusForResponse(String approvalStatus) {
        if (approvalStatus == null) {
            return ApprovalStatus.INITIAL.getName();
        }
        try {
            ApprovalStatus status = ApprovalStatus.fromCodeOrName(approvalStatus);
            return status.getName();
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 승인 상태: {}, 원본 값 반환", approvalStatus);
            return approvalStatus; // 변환 실패 시 원본 반환
        }
    }

    /**
     * 권한 값을 실제 값으로 변환 (Response용)
     * DB에 저장된 값(ma, bb, tj, tw)을 그대로 반환
     *
     * @param authVal 권한 값
     * @return 실제 값 (ma, bb, tj, tw)
     */
    public String convertAuthValForResponse(String authVal) {
        if (authVal == null) {
            return AuthVal.TEAM_MEMBER.getCode();
        }
        try {
            AuthVal auth = AuthVal.fromCode(authVal);
            return auth.getCode(); // DB에 저장된 값(code)을 그대로 반환
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 권한 값: {}, 원본 값 반환", authVal);
            return authVal; // 변환 실패 시 원본 반환
        }
    }
}
