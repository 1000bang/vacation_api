package com.vacation.api.domain.approval.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.domain.approval.service.ApprovalService;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.repository.RentalSupportRepository;
import com.vacation.api.domain.rental.entity.RentalApproval;
import com.vacation.api.domain.rental.repository.RentalApprovalRepository;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.repository.VacationHistoryRepository;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 승인/반려 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/approval")
public class ApprovalController extends BaseController {

    private final ApprovalService approvalService;
    private final UserService userService;
    private final VacationHistoryRepository vacationHistoryRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final RentalSupportRepository rentalSupportRepository;
    private final RentalApprovalRepository rentalApprovalRepository;
    private final UserRepository userRepository;

    public ApprovalController(ApprovalService approvalService, UserService userService,
                              VacationHistoryRepository vacationHistoryRepository,
                              ExpenseClaimRepository expenseClaimRepository,
                              RentalSupportRepository rentalSupportRepository,
                              RentalApprovalRepository rentalApprovalRepository,
                              UserRepository userRepository,
                              TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.approvalService = approvalService;
        this.userService = userService;
        this.vacationHistoryRepository = vacationHistoryRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.rentalSupportRepository = rentalSupportRepository;
        this.rentalApprovalRepository = rentalApprovalRepository;
        this.userRepository = userRepository;
    }

    /**
     * 권한별 승인 대기 목록 조회
     * - 팀장: 해당 팀원의 A, AM
     * - 본부장: 해당 본부의 B
     * - 관리자: 전체 본부의 전체
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Object>> getPendingApprovals(
            HttpServletRequest request,
            @RequestParam(required = false) String type, // VACATION, EXPENSE, RENTAL, RENTAL_APPROVAL
            @RequestParam(required = false) String listType, // vacation, expense, rental, rentalApproval
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("승인 대기 목록 조회: type={}, listType={}, page={}, size={}", type, listType, page, size);

        try {
            Long userId = (Long) request.getAttribute("userId");
            User requester = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

            String authVal = requester.getAuthVal();
            List<Long> userIds;
            final List<String> approvalStatuses;

            // 권한별 필터링 조건 설정
            if ("ma".equals(authVal)) {
                // 관리자: 전체 본부의 모든 상태 (null 포함: A, AM, B, RB, RC, C)
                userIds = null; // null이면 전체 조회
                approvalStatuses = List.of("A", "AM", "B", "RB", "RC", "C");
            } else if ("bb".equals(authVal)) {
                // 본부장: 해당 본부의 B, C 상태 (승인 대기 및 승인 완료)
                List<User> teamMembers = userService.getUserInfoList(userId);
                userIds = teamMembers.stream()
                        .map(User::getUserId)
                        .toList();
                approvalStatuses = List.of("B", "C");
            } else if ("tj".equals(authVal)) {
                // 팀장: 해당 팀원의 A, AM 상태 (null도 포함: 초기 생성 상태)
                List<User> teamMembers = userService.getUserInfoList(userId);
                userIds = teamMembers.stream()
                        .map(User::getUserId)
                        .toList();
                approvalStatuses = List.of("A", "AM");
            } else {
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "승인 권한이 없습니다.");
            }

            Map<String, Object> result = new HashMap<>();

            // 휴가 신청 목록
            if (type == null || "VACATION".equals(type) || (listType != null && "vacation".equals(listType))) {
                List<VacationHistory> vacationList;
                if (userIds == null) {
                    // 관리자: 쿼리로 승인 상태 필터링 (null도 포함)
                    List<VacationHistory> filteredList = vacationHistoryRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<VacationHistory> nullStatusList = vacationHistoryRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(VacationHistory::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<VacationHistory> additionalList = nullStatusList.stream()
                                .filter(vh -> !existingSeqs.contains(vh.getSeq()))
                                .toList();
                        vacationList = new java.util.ArrayList<>(filteredList);
                        vacationList.addAll(additionalList);
                        vacationList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        vacationList = filteredList;
                    }
                } else {
                    // 팀장/본부장: userIds와 approvalStatuses로 필터링
                    List<VacationHistory> filteredList = vacationHistoryRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                            userIds, approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<VacationHistory> nullStatusList = vacationHistoryRepository.findByUserIdInOrderByStartDateAsc(userIds).stream()
                                .filter(vh -> vh.getApprovalStatus() == null)
                                .toList();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(VacationHistory::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<VacationHistory> additionalList = nullStatusList.stream()
                                .filter(vh -> !existingSeqs.contains(vh.getSeq()))
                                .toList();
                        vacationList = new java.util.ArrayList<>(filteredList);
                        vacationList.addAll(additionalList);
                        vacationList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        vacationList = filteredList;
                    }
                }
                List<Map<String, Object>> vacationResponseList = vacationList.stream()
                        .map(vh -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("applicationType", "VACATION");
                            map.put("seq", vh.getSeq());
                            map.put("userId", vh.getUserId());
                            map.put("applicant", userRepository.findById(vh.getUserId())
                                    .map(User::getName)
                                    .orElse(""));
                            map.put("startDate", vh.getStartDate());
                            map.put("endDate", vh.getEndDate());
                            map.put("period", vh.getPeriod());
                            map.put("type", vh.getType());
                            map.put("reason", vh.getReason());
                            map.put("approvalStatus", vh.getApprovalStatus());
                            map.put("createdAt", vh.getCreatedAt());
                            return map;
                        })
                        .toList();
                
                // 페이징 처리
                long vacationTotalCount = vacationResponseList.size();
                int vacationStart = page * size;
                int vacationEnd = Math.min(vacationStart + size, vacationResponseList.size());
                List<Map<String, Object>> vacationPagedList = vacationStart < vacationResponseList.size() 
                    ? vacationResponseList.subList(vacationStart, vacationEnd) 
                    : List.of();
                
                Map<String, Object> vacationData = new HashMap<>();
                vacationData.put("list", vacationPagedList);
                vacationData.put("totalCount", vacationTotalCount);
                result.put("vacation", vacationData);
            }

            // 개인 비용 청구 목록
            if (type == null || "EXPENSE".equals(type) || (listType != null && "expense".equals(listType))) {
                List<ExpenseClaim> expenseList;
                if (userIds == null) {
                    // 관리자: 쿼리로 승인 상태 필터링 (null도 포함)
                    List<ExpenseClaim> filteredList = expenseClaimRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<ExpenseClaim> nullStatusList = expenseClaimRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(ExpenseClaim::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<ExpenseClaim> additionalList = nullStatusList.stream()
                                .filter(ec -> !existingSeqs.contains(ec.getSeq()))
                                .toList();
                        expenseList = new java.util.ArrayList<>(filteredList);
                        expenseList.addAll(additionalList);
                        expenseList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        expenseList = filteredList;
                    }
                } else {
                    // 팀장/본부장: userIds와 approvalStatuses로 필터링
                    List<ExpenseClaim> filteredList = expenseClaimRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                            userIds, approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<ExpenseClaim> nullStatusList = expenseClaimRepository.findByUserIdOrderBySeqDesc(userIds.get(0)).stream()
                                .filter(ec -> {
                                    if (ec.getApprovalStatus() == null) {
                                        return userIds.contains(ec.getUserId());
                                    }
                                    return false;
                                })
                                .toList();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(ExpenseClaim::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<ExpenseClaim> additionalList = nullStatusList.stream()
                                .filter(ec -> !existingSeqs.contains(ec.getSeq()))
                                .toList();
                        expenseList = new java.util.ArrayList<>(filteredList);
                        expenseList.addAll(additionalList);
                        expenseList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        expenseList = filteredList;
                    }
                }
                List<Map<String, Object>> expenseResponseList = expenseList.stream()
                        .map(ec -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("applicationType", "EXPENSE");
                            map.put("seq", ec.getSeq());
                            map.put("userId", ec.getUserId());
                            map.put("applicant", userRepository.findById(ec.getUserId())
                                    .map(User::getName)
                                    .orElse(""));
                            map.put("requestDate", ec.getRequestDate());
                            map.put("billingYyMonth", ec.getBillingYyMonth());
                            map.put("childCnt", ec.getChildCnt());
                            map.put("totalAmount", ec.getTotalAmount());
                            map.put("approvalStatus", ec.getApprovalStatus());
                            map.put("createdAt", ec.getCreatedAt());
                            return map;
                        })
                        .toList();
                
                // 페이징 처리
                long expenseTotalCount = expenseResponseList.size();
                int expenseStart = page * size;
                int expenseEnd = Math.min(expenseStart + size, expenseResponseList.size());
                List<Map<String, Object>> expensePagedList = expenseStart < expenseResponseList.size() 
                    ? expenseResponseList.subList(expenseStart, expenseEnd) 
                    : List.of();
                
                Map<String, Object> expenseData = new HashMap<>();
                expenseData.put("list", expensePagedList);
                expenseData.put("totalCount", expenseTotalCount);
                result.put("expense", expenseData);
            }

            // 월세 지원 신청 목록
            if (type == null || "RENTAL".equals(type) || (listType != null && "rental".equals(listType))) {
                List<RentalSupport> rentalList;
                if (userIds == null) {
                    // 관리자: 쿼리로 승인 상태 필터링 (null도 포함)
                    List<RentalSupport> filteredList = rentalSupportRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<RentalSupport> nullStatusList = rentalSupportRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(RentalSupport::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<RentalSupport> additionalList = nullStatusList.stream()
                                .filter(rs -> !existingSeqs.contains(rs.getSeq()))
                                .toList();
                        rentalList = new java.util.ArrayList<>(filteredList);
                        rentalList.addAll(additionalList);
                        rentalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        rentalList = filteredList;
                    }
                } else {
                    // 팀장/본부장: userIds와 approvalStatuses로 필터링
                    List<RentalSupport> filteredList = rentalSupportRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                            userIds, approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<RentalSupport> nullStatusList = rentalSupportRepository.findByUserIdOrderBySeqDesc(userIds.get(0)).stream()
                                .filter(rs -> {
                                    if (rs.getApprovalStatus() == null) {
                                        return userIds.contains(rs.getUserId());
                                    }
                                    return false;
                                })
                                .toList();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(RentalSupport::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<RentalSupport> additionalList = nullStatusList.stream()
                                .filter(rs -> !existingSeqs.contains(rs.getSeq()))
                                .toList();
                        rentalList = new java.util.ArrayList<>(filteredList);
                        rentalList.addAll(additionalList);
                        rentalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        rentalList = filteredList;
                    }
                }
                List<Map<String, Object>> rentalResponseList = rentalList.stream()
                        .map(rs -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("applicationType", "RENTAL");
                            map.put("seq", rs.getSeq());
                            map.put("userId", rs.getUserId());
                            map.put("applicant", userRepository.findById(rs.getUserId())
                                    .map(User::getName)
                                    .orElse(""));
                            map.put("requestDate", rs.getRequestDate());
                            map.put("billingYyMonth", rs.getBillingYyMonth());
                            map.put("contractMonthlyRent", rs.getContractMonthlyRent());
                            map.put("billingAmount", rs.getBillingAmount());
                            map.put("paymentDate", rs.getPaymentDate());
                            map.put("approvalStatus", rs.getApprovalStatus());
                            map.put("createdAt", rs.getCreatedAt());
                            return map;
                        })
                        .toList();
                
                // 페이징 처리
                long rentalTotalCount = rentalResponseList.size();
                int rentalStart = page * size;
                int rentalEnd = Math.min(rentalStart + size, rentalResponseList.size());
                List<Map<String, Object>> rentalPagedList = rentalStart < rentalResponseList.size() 
                    ? rentalResponseList.subList(rentalStart, rentalEnd) 
                    : List.of();
                
                Map<String, Object> rentalData = new HashMap<>();
                rentalData.put("list", rentalPagedList);
                rentalData.put("totalCount", rentalTotalCount);
                result.put("rental", rentalData);
            }

            // 월세 지원 품의서 목록
            if (type == null || "RENTAL_APPROVAL".equals(type) || (listType != null && "rentalApproval".equals(listType))) {
                List<RentalApproval> rentalApprovalList;
                if (userIds == null) {
                    // 관리자: 쿼리로 승인 상태 필터링 (null도 포함)
                    List<RentalApproval> filteredList = rentalApprovalRepository.findByApprovalStatusInOrderByCreatedAtDesc(approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<RentalApproval> nullStatusList = rentalApprovalRepository.findByApprovalStatusIsNullOrderByCreatedAtDesc();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(RentalApproval::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<RentalApproval> additionalList = nullStatusList.stream()
                                .filter(ra -> !existingSeqs.contains(ra.getSeq()))
                                .toList();
                        rentalApprovalList = new java.util.ArrayList<>(filteredList);
                        rentalApprovalList.addAll(additionalList);
                        rentalApprovalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        rentalApprovalList = filteredList;
                    }
                } else {
                    // 팀장/본부장: userIds와 approvalStatuses로 필터링
                    List<RentalApproval> filteredList = rentalApprovalRepository.findByUserIdInAndApprovalStatusInOrderByCreatedAtDesc(
                            userIds, approvalStatuses);
                    // null 상태도 포함 (초기 생성 상태는 "A"로 간주)
                    if (approvalStatuses.contains("A")) {
                        List<RentalApproval> nullStatusList = rentalApprovalRepository.findAll().stream()
                                .filter(ra -> {
                                    if (ra.getApprovalStatus() == null) {
                                        return userIds.contains(ra.getUserId());
                                    }
                                    return false;
                                })
                                .toList();
                        // 중복 제거를 위해 seq 기준으로 합치기
                        java.util.Set<Long> existingSeqs = filteredList.stream()
                                .map(RentalApproval::getSeq)
                                .collect(java.util.stream.Collectors.toSet());
                        List<RentalApproval> additionalList = nullStatusList.stream()
                                .filter(ra -> !existingSeqs.contains(ra.getSeq()))
                                .toList();
                        rentalApprovalList = new java.util.ArrayList<>(filteredList);
                        rentalApprovalList.addAll(additionalList);
                        rentalApprovalList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        rentalApprovalList = filteredList;
                    }
                }
                List<Map<String, Object>> rentalApprovalResponseList = rentalApprovalList.stream()
                        .map(ra -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("applicationType", "RENTAL_APPROVAL");
                            map.put("seq", ra.getSeq());
                            map.put("userId", ra.getUserId());
                            map.put("applicant", userRepository.findById(ra.getUserId())
                                    .map(User::getName)
                                    .orElse(""));
                            map.put("rentalAddress", ra.getRentalAddress());
                            map.put("contractStartDate", ra.getContractStartDate());
                            map.put("contractEndDate", ra.getContractEndDate());
                            map.put("contractMonthlyRent", ra.getContractMonthlyRent());
                            map.put("billingAmount", ra.getBillingAmount());
                            map.put("billingStartDate", ra.getBillingStartDate());
                            map.put("approvalStatus", ra.getApprovalStatus());
                            map.put("createdAt", ra.getCreatedAt());
                            return map;
                        })
                        .toList();
                
                // 페이징 처리
                long rentalApprovalTotalCount = rentalApprovalResponseList.size();
                int rentalApprovalStart = page * size;
                int rentalApprovalEnd = Math.min(rentalApprovalStart + size, rentalApprovalResponseList.size());
                List<Map<String, Object>> rentalApprovalPagedList = rentalApprovalStart < rentalApprovalResponseList.size() 
                    ? rentalApprovalResponseList.subList(rentalApprovalStart, rentalApprovalEnd) 
                    : List.of();
                
                Map<String, Object> rentalApprovalData = new HashMap<>();
                rentalApprovalData.put("list", rentalApprovalPagedList);
                rentalApprovalData.put("totalCount", rentalApprovalTotalCount);
                result.put("rentalApproval", rentalApprovalData);
            }

            log.info("승인 대기 목록 조회 완료: vacation={}, expense={}, rental={}, rentalApproval={}", 
                    result.get("vacation") != null ? ((Map<?, ?>) result.get("vacation")).get("list") != null ? ((List<?>) ((Map<?, ?>) result.get("vacation")).get("list")).size() : 0 : 0,
                    result.get("expense") != null ? ((Map<?, ?>) result.get("expense")).get("list") != null ? ((List<?>) ((Map<?, ?>) result.get("expense")).get("list")).size() : 0 : 0,
                    result.get("rental") != null ? ((Map<?, ?>) result.get("rental")).get("list") != null ? ((List<?>) ((Map<?, ?>) result.get("rental")).get("list")).size() : 0 : 0,
                    result.get("rentalApproval") != null ? ((Map<?, ?>) result.get("rentalApproval")).get("list") != null ? ((List<?>) ((Map<?, ?>) result.get("rentalApproval")).get("list")).size() : 0 : 0);

            return successResponse(result);
        } catch (ApiException e) {
            return errorResponse("승인 대기 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 대기 목록 조회 실패", e);
            return errorResponse("승인 대기 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 휴가 신청 승인 (팀장)
     */
    @PostMapping("/vacation/{seq}/approve/team-leader")
    public ResponseEntity<ApiResponse<Object>> approveVacationByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("휴가 신청 팀장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveVacationByTeamLeader(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 휴가 신청 반려 (팀장)
     */
    @PostMapping("/vacation/{seq}/reject/team-leader")
    public ResponseEntity<ApiResponse<Object>> rejectVacationByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("휴가 신청 팀장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectVacationByTeamLeader(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 휴가 신청 승인 (본부장)
     */
    @PostMapping("/vacation/{seq}/approve/division-head")
    public ResponseEntity<ApiResponse<Object>> approveVacationByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("휴가 신청 본부장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveVacationByDivisionHead(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 휴가 신청 반려 (본부장)
     */
    @PostMapping("/vacation/{seq}/reject/division-head")
    public ResponseEntity<ApiResponse<Object>> rejectVacationByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("휴가 신청 본부장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectVacationByDivisionHead(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 개인 비용 청구 승인 (팀장)
     */
    @PostMapping("/expense/{seq}/approve/team-leader")
    public ResponseEntity<ApiResponse<Object>> approveExpenseClaimByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("개인 비용 청구 팀장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveExpenseClaimByTeamLeader(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 개인 비용 청구 반려 (팀장)
     */
    @PostMapping("/expense/{seq}/reject/team-leader")
    public ResponseEntity<ApiResponse<Object>> rejectExpenseClaimByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("개인 비용 청구 팀장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectExpenseClaimByTeamLeader(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 개인 비용 청구 승인 (본부장)
     */
    @PostMapping("/expense/{seq}/approve/division-head")
    public ResponseEntity<ApiResponse<Object>> approveExpenseClaimByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("개인 비용 청구 본부장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveExpenseClaimByDivisionHead(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 개인 비용 청구 반려 (본부장)
     */
    @PostMapping("/expense/{seq}/reject/division-head")
    public ResponseEntity<ApiResponse<Object>> rejectExpenseClaimByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("개인 비용 청구 본부장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectExpenseClaimByDivisionHead(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 신청 승인 (팀장)
     */
    @PostMapping("/rental/{seq}/approve/team-leader")
    public ResponseEntity<ApiResponse<Object>> approveRentalSupportByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 신청 팀장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveRentalSupportByTeamLeader(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 신청 반려 (팀장)
     */
    @PostMapping("/rental/{seq}/reject/team-leader")
    public ResponseEntity<ApiResponse<Object>> rejectRentalSupportByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("월세 지원 신청 팀장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectRentalSupportByTeamLeader(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 신청 승인 (본부장)
     */
    @PostMapping("/rental/{seq}/approve/division-head")
    public ResponseEntity<ApiResponse<Object>> approveRentalSupportByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 신청 본부장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveRentalSupportByDivisionHead(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 신청 반려 (본부장)
     */
    @PostMapping("/rental/{seq}/reject/division-head")
    public ResponseEntity<ApiResponse<Object>> rejectRentalSupportByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("월세 지원 신청 본부장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectRentalSupportByDivisionHead(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 품의서 승인 (팀장)
     */
    @PostMapping("/rental-approval/{seq}/approve/team-leader")
    public ResponseEntity<ApiResponse<Object>> approveRentalApprovalByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 품의서 팀장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveRentalApprovalByTeamLeader(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 품의서 반려 (팀장)
     */
    @PostMapping("/rental-approval/{seq}/reject/team-leader")
    public ResponseEntity<ApiResponse<Object>> rejectRentalApprovalByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("월세 지원 품의서 팀장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectRentalApprovalByTeamLeader(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 품의서 승인 (본부장)
     */
    @PostMapping("/rental-approval/{seq}/approve/division-head")
    public ResponseEntity<ApiResponse<Object>> approveRentalApprovalByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 품의서 본부장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveRentalApprovalByDivisionHead(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 품의서 반려 (본부장)
     */
    @PostMapping("/rental-approval/{seq}/reject/division-head")
    public ResponseEntity<ApiResponse<Object>> rejectRentalApprovalByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("월세 지원 품의서 본부장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectRentalApprovalByDivisionHead(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }
}
