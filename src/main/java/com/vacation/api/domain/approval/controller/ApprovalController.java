package com.vacation.api.domain.approval.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.domain.approval.response.PendingApprovalResponse;
import com.vacation.api.domain.approval.service.ApprovalService;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public ApprovalController(ApprovalService approvalService,
                              TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.approvalService = approvalService;
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
            @RequestParam(required = false) String type, // VACATION, EXPENSE, RENTAL, RENTAL_PROPOSAL
            @RequestParam(required = false) String listType, // vacation, expense, rental, rental_proposal
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("승인 대기 목록 조회: type={}, listType={}, page={}, size={}", type, listType, page, size);

        try {
            Long userId = (Long) request.getAttribute("userId");
            PendingApprovalResponse response = approvalService.getPendingApprovals(userId, type, listType, page, size);
            return successResponse(response);
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
     * 월세 품의서 승인 (팀장)
     */
    @PostMapping("/rental-proposal/{seq}/approve/team-leader")
    public ResponseEntity<ApiResponse<Object>> approveRentalProposalByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 품의서 팀장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveRentalProposalByTeamLeader(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 월세 품의서 반려 (팀장)
     */
    @PostMapping("/rental-proposal/{seq}/reject/team-leader")
    public ResponseEntity<ApiResponse<Object>> rejectRentalProposalByTeamLeader(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("월세 품의서 팀장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectRentalProposalByTeamLeader(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }

    /**
     * 월세 품의서 승인 (본부장)
     */
    @PostMapping("/rental-proposal/{seq}/approve/division-head")
    public ResponseEntity<ApiResponse<Object>> approveRentalProposalByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 품의서 본부장 승인 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            approvalService.approveRentalProposalByDivisionHead(seq, approverId);
            return successResponse("승인되었습니다.");
        } catch (ApiException e) {
            return errorResponse("승인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("승인 실패", e);
            return errorResponse("승인에 실패했습니다.", e);
        }
    }

    /**
     * 월세 품의서 반려 (본부장)
     */
    @PostMapping("/rental-proposal/{seq}/reject/division-head")
    public ResponseEntity<ApiResponse<Object>> rejectRentalProposalByDivisionHead(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestBody Map<String, String> requestBody) {
        log.info("월세 품의서 본부장 반려 요청: seq={}", seq);

        try {
            Long approverId = (Long) request.getAttribute("userId");
            String rejectionReason = requestBody.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "반려 사유를 입력해주세요.");
            }
            approvalService.rejectRentalProposalByDivisionHead(seq, approverId, rejectionReason);
            return successResponse("반려되었습니다.");
        } catch (ApiException e) {
            return errorResponse("반려에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("반려 실패", e);
            return errorResponse("반려에 실패했습니다.", e);
        }
    }
}
