package com.vacation.api.util;

import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.rental.entity.RentalApproval;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entity를 Map으로 변환하는 Mapper 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@Component
public class ResponseMapper {

    /**
     * RentalApproval을 Map으로 변환하고 applicant 추가
     *
     * @param approval RentalApproval 엔티티
     * @param applicantName 신청자 이름
     * @return Map
     */
    public Map<String, Object> toRentalApprovalMap(RentalApproval approval, String applicantName) {
        Map<String, Object> map = new HashMap<>();
        map.put("seq", approval.getSeq());
        map.put("userId", approval.getUserId());
        map.put("previousAddress", approval.getPreviousAddress());
        map.put("rentalAddress", approval.getRentalAddress());
        map.put("contractStartDate", approval.getContractStartDate());
        map.put("contractEndDate", approval.getContractEndDate());
        map.put("contractMonthlyRent", approval.getContractMonthlyRent());
        map.put("billingAmount", approval.getBillingAmount());
        map.put("billingStartDate", approval.getBillingStartDate());
        map.put("billingReason", approval.getBillingReason());
        map.put("createdAt", approval.getCreatedAt());
        map.put("applicant", applicantName != null ? applicantName : "");
        return map;
    }

    /**
     * RentalSupport를 Map으로 변환하고 applicant 추가
     *
     * @param rental RentalSupport 엔티티
     * @param applicantName 신청자 이름
     * @return Map
     */
    public Map<String, Object> toRentalSupportMap(RentalSupport rental, String applicantName) {
        Map<String, Object> map = new HashMap<>();
        map.put("seq", rental.getSeq());
        map.put("userId", rental.getUserId());
        map.put("requestDate", rental.getRequestDate());
        map.put("billingYyMonth", rental.getBillingYyMonth());
        map.put("contractStartDate", rental.getContractStartDate());
        map.put("contractEndDate", rental.getContractEndDate());
        map.put("contractMonthlyRent", rental.getContractMonthlyRent());
        map.put("paymentType", rental.getPaymentType());
        map.put("billingStartDate", rental.getBillingStartDate());
        map.put("billingPeriodStartDate", rental.getBillingPeriodStartDate());
        map.put("billingPeriodEndDate", rental.getBillingPeriodEndDate());
        map.put("paymentDate", rental.getPaymentDate());
        map.put("paymentAmount", rental.getPaymentAmount());
        map.put("billingAmount", rental.getBillingAmount());
        map.put("createdAt", rental.getCreatedAt());
        map.put("applicant", applicantName != null ? applicantName : "");
        return map;
    }

    /**
     * ExpenseClaim을 Map으로 변환하고 applicant 추가
     *
     * @param claim ExpenseClaim 엔티티
     * @param applicantName 신청자 이름
     * @return Map
     */
    public Map<String, Object> toExpenseClaimMap(ExpenseClaim claim, String applicantName) {
        Map<String, Object> map = new HashMap<>();
        map.put("seq", claim.getSeq());
        map.put("userId", claim.getUserId());
        map.put("requestDate", claim.getRequestDate());
        map.put("billingYyMonth", claim.getBillingYyMonth());
        map.put("childCnt", claim.getChildCnt());
        map.put("totalAmount", claim.getTotalAmount());
        map.put("createdAt", claim.getCreatedAt());
        map.put("applicant", applicantName != null ? applicantName : "");
        return map;
    }

    /**
     * VacationHistory를 Map으로 변환하고 applicant 추가
     *
     * @param history VacationHistory 엔티티
     * @param applicantName 신청자 이름
     * @return Map
     */
    public Map<String, Object> toVacationHistoryMap(VacationHistory history, String applicantName) {
        Map<String, Object> map = new HashMap<>();
        map.put("seq", history.getSeq());
        map.put("userId", history.getUserId());
        map.put("startDate", history.getStartDate());
        map.put("endDate", history.getEndDate());
        map.put("period", history.getPeriod());
        map.put("type", history.getType());
        map.put("reason", history.getReason());
        map.put("requestDate", history.getRequestDate());
        map.put("annualVacationDays", history.getAnnualVacationDays());
        map.put("previousRemainingDays", history.getPreviousRemainingDays());
        map.put("usedVacationDays", history.getUsedVacationDays());
        map.put("remainingVacationDays", history.getRemainingVacationDays());
        map.put("status", history.getStatus());
        map.put("createdAt", history.getCreatedAt());
        map.put("applicant", applicantName != null ? applicantName : "");
        return map;
    }

    /**
     * RentalApproval 리스트를 Map 리스트로 변환 (applicant 포함)
     *
     * @param approvals RentalApproval 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return Map 리스트
     */
    public List<Map<String, Object>> toRentalApprovalMapList(
            List<RentalApproval> approvals,
            Function<Long, User> userProvider) {
        return approvals.stream()
                .map(approval -> {
                    User user = userProvider.apply(approval.getUserId());
                    String applicantName = user != null ? user.getName() : null;
                    return toRentalApprovalMap(approval, applicantName);
                })
                .collect(Collectors.toList());
    }

    /**
     * RentalSupport 리스트를 Map 리스트로 변환 (applicant 포함)
     *
     * @param rentals RentalSupport 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return Map 리스트
     */
    public List<Map<String, Object>> toRentalSupportMapList(
            List<RentalSupport> rentals,
            Function<Long, User> userProvider) {
        return rentals.stream()
                .map(rental -> {
                    User user = userProvider.apply(rental.getUserId());
                    String applicantName = user != null ? user.getName() : null;
                    return toRentalSupportMap(rental, applicantName);
                })
                .collect(Collectors.toList());
    }

    /**
     * ExpenseClaim 리스트를 Map 리스트로 변환 (applicant 포함)
     *
     * @param claims ExpenseClaim 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return Map 리스트
     */
    public List<Map<String, Object>> toExpenseClaimMapList(
            List<ExpenseClaim> claims,
            Function<Long, User> userProvider) {
        return claims.stream()
                .map(claim -> {
                    User user = userProvider.apply(claim.getUserId());
                    String applicantName = user != null ? user.getName() : null;
                    return toExpenseClaimMap(claim, applicantName);
                })
                .collect(Collectors.toList());
    }

    /**
     * VacationHistory 리스트를 Map 리스트로 변환 (applicant 포함)
     *
     * @param histories VacationHistory 리스트
     * @param userProvider userId로 User를 조회하는 함수
     * @return Map 리스트
     */
    public List<Map<String, Object>> toVacationHistoryMapList(
            List<VacationHistory> histories,
            Function<Long, User> userProvider) {
        return histories.stream()
                .map(history -> {
                    User user = userProvider.apply(history.getUserId());
                    String applicantName = user != null ? user.getName() : null;
                    return toVacationHistoryMap(history, applicantName);
                })
                .collect(Collectors.toList());
    }
}
