package com.vacation.api.domain.approval.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.domain.approval.service.ApprovalService;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.repository.RentalSupportRepository;
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
    private final UserRepository userRepository;

    public ApprovalController(ApprovalService approvalService, UserService userService,
                              VacationHistoryRepository vacationHistoryRepository,
                              ExpenseClaimRepository expenseClaimRepository,
                              RentalSupportRepository rentalSupportRepository,
                              UserRepository userRepository,
                              TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.approvalService = approvalService;
        this.userService = userService;
        this.vacationHistoryRepository = vacationHistoryRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.rentalSupportRepository = rentalSupportRepository;
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
            @RequestParam(required = false) String type) { // VACATION, EXPENSE, RENTAL
        log.info("승인 대기 목록 조회: type={}", type);

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

            Map<String, List<Map<String, Object>>> result = new HashMap<>();

            // 휴가 신청 목록
            if (type == null || "VACATION".equals(type)) {
                List<VacationHistory> vacationList;
                if (userIds == null) {
                    // 관리자: 전체 조회 후 승인 상태 필터링 (null도 포함)
                    vacationList = vacationHistoryRepository.findAll().stream()
                            .filter(vh -> {
                                String status = vh.getApprovalStatus();
                                // null이면 "A"로 간주 (초기 생성 상태)
                                if (status == null) {
                                    return approvalStatuses.contains("A");
                                }
                                return approvalStatuses.contains(status);
                            })
                            .toList();
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
                result.put("vacation", vacationResponseList);
            }

            // 개인 비용 청구 목록
            if (type == null || "EXPENSE".equals(type)) {
                List<ExpenseClaim> expenseList;
                if (userIds == null) {
                    // 관리자: 전체 조회 후 승인 상태 필터링 (null도 포함)
                    expenseList = expenseClaimRepository.findAll().stream()
                            .filter(ec -> {
                                String status = ec.getApprovalStatus();
                                // null이면 "A"로 간주 (초기 생성 상태)
                                if (status == null) {
                                    return approvalStatuses.contains("A");
                                }
                                return approvalStatuses.contains(status);
                            })
                            .toList();
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
                result.put("expense", expenseResponseList);
            }

            // 월세 지원 신청 목록
            if (type == null || "RENTAL".equals(type)) {
                List<RentalSupport> rentalList;
                if (userIds == null) {
                    // 관리자: 전체 조회 후 승인 상태 필터링 (null도 포함)
                    rentalList = rentalSupportRepository.findAll().stream()
                            .filter(rs -> {
                                String status = rs.getApprovalStatus();
                                // null이면 "A"로 간주 (초기 생성 상태)
                                if (status == null) {
                                    return approvalStatuses.contains("A");
                                }
                                return approvalStatuses.contains(status);
                            })
                            .toList();
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
                result.put("rental", rentalResponseList);
            }

            log.info("승인 대기 목록 조회 완료: vacation={}, expense={}, rental={}", 
                    result.get("vacation") != null ? ((List<?>) result.get("vacation")).size() : 0,
                    result.get("expense") != null ? ((List<?>) result.get("expense")).size() : 0,
                    result.get("rental") != null ? ((List<?>) result.get("rental")).size() : 0);

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
}
