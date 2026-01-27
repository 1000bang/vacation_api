package com.vacation.api.domain.user.controller;

import com.vacation.api.common.controller.BaseController;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.domain.user.request.TeamManagementRequest;
import com.vacation.api.domain.user.response.TeamManagementResponse;
import com.vacation.api.domain.user.service.TeamManagementService;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 팀 관리 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-23
 */
@Slf4j
@RestController
@RequestMapping("/team")
public class TeamManagementController extends BaseController {

    private final TeamManagementService teamManagementService;

    public TeamManagementController(TeamManagementService teamManagementService,
                                   TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.teamManagementService = teamManagementService;
    }

    /**
     * 전체 팀 관리 목록 조회
     *
     * @param request HTTP 요청
     * @return 팀 관리 목록
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Object>> getAllTeamManagement(HttpServletRequest request) {
        log.info("전체 팀 관리 목록 조회 요청");
        return executeWithErrorHandling("팀 관리 목록 조회에 실패했습니다.", teamManagementService::getAllTeamManagement);
    }

    /**
     * 본부별 팀 관리 목록 조회
     *
     * @param request HTTP 요청
     * @param division 본부
     * @return 팀 관리 목록
     */
    @GetMapping("/list/{division}")
    public ResponseEntity<ApiResponse<Object>> getTeamManagementByDivision(
            HttpServletRequest request,
            @PathVariable String division) {
        log.info("본부별 팀 관리 목록 조회 요청: division={}", division);
        return executeWithErrorHandling("팀 관리 목록 조회에 실패했습니다.", () -> teamManagementService.getTeamManagementByDivision(division));
    }

    /**
     * 팀 관리 생성 (본부 또는 팀 추가)
     *
     * @param request HTTP 요청
     * @param teamRequest 팀 관리 요청
     * @return 생성된 팀 관리 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createTeamManagement(
            HttpServletRequest request,
            @Valid @RequestBody TeamManagementRequest teamRequest) {
        log.info("팀 관리 생성 요청: division={}, team={}", teamRequest.getDivision(), teamRequest.getTeam());

        try {
            TeamManagementResponse response = teamManagementService.createTeamManagement(teamRequest);
            return createdResponse(response);
        } catch (ApiException e) {
            return errorResponse("팀 관리 생성에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("팀 관리 생성 실패", e);
            return errorResponse("팀 관리 생성에 실패했습니다.", e);
        }
    }

    /**
     * 팀 관리 수정
     *
     * @param request HTTP 요청
     * @param seq 팀 관리 시퀀스
     * @param teamRequest 팀 관리 요청
     * @return 수정된 팀 관리 정보
     */
    @PutMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> updateTeamManagement(
            HttpServletRequest request,
            @PathVariable Long seq,
            @Valid @RequestBody TeamManagementRequest teamRequest) {
        log.info("팀 관리 수정 요청: seq={}, division={}, team={}", seq, teamRequest.getDivision(), teamRequest.getTeam());

        try {
            TeamManagementResponse response = teamManagementService.updateTeamManagement(seq, teamRequest);
            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("팀 관리 수정에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("팀 관리 수정 실패", e);
            return errorResponse("팀 관리 수정에 실패했습니다.", e);
        }
    }

    /**
     * 팀 관리 삭제
     *
     * @param request HTTP 요청
     * @param seq 팀 관리 시퀀스
     * @return 삭제 결과
     */
    @DeleteMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> deleteTeamManagement(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("팀 관리 삭제 요청: seq={}", seq);

        try {
            teamManagementService.deleteTeamManagement(seq);
            return successResponse("팀 관리가 삭제되었습니다.");
        } catch (ApiException e) {
            return errorResponse("팀 관리 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("팀 관리 삭제 실패", e);
            return errorResponse("팀 관리 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 팀별 사용자 목록 조회
     *
     * @param request HTTP 요청
     * @param teamSeq 팀 관리 시퀀스
     * @return 사용자 목록
     */
    @GetMapping("/{teamSeq}/users")
    public ResponseEntity<ApiResponse<Object>> getUsersByTeamSeq(
            HttpServletRequest request,
            @PathVariable Long teamSeq) {
        log.info("팀별 사용자 목록 조회 요청: teamSeq={}", teamSeq);
        return executeWithErrorHandling("팀별 사용자 목록 조회에 실패했습니다.", () -> teamManagementService.getUsersByTeamSeq(teamSeq));
    }

    /**
     * 본부별 사용자 목록 조회 (본부장)
     *
     * @param request HTTP 요청
     * @param division 본부
     * @return 사용자 목록
     */
    @GetMapping("/division/{division}/users")
    public ResponseEntity<ApiResponse<Object>> getUsersByDivision(
            HttpServletRequest request,
            @PathVariable String division) {
        log.info("본부별 사용자 목록 조회 요청: division={}", division);
        return executeWithErrorHandling("본부별 사용자 목록 조회에 실패했습니다.", () -> teamManagementService.getUsersByDivision(division));
    }
}
