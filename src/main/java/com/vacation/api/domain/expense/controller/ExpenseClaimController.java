package com.vacation.api.domain.expense.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.request.ExpenseClaimRequest;
import com.vacation.api.domain.expense.service.ExpenseClaimService;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import com.vacation.api.util.ResponseMapper;
import com.vacation.api.exception.ApiException;
import com.vacation.api.vo.ExpenseClaimVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 개인 비용 청구 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-08
 */
@Slf4j
@RestController
@RequestMapping("/expense")
public class ExpenseClaimController extends BaseController {

    private final ExpenseClaimService expenseClaimService;
    private final UserService userService;
    private final ResponseMapper responseMapper;

    public ExpenseClaimController(ExpenseClaimService expenseClaimService, UserService userService,
                                 ResponseMapper responseMapper, TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.expenseClaimService = expenseClaimService;
        this.userService = userService;
        this.responseMapper = responseMapper;
    }

    /**
     * 개인 비용 청구 목록 조회 (페이징)
     *
     * @param request HTTP 요청
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 5)
     * @return 개인 비용 청구 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getExpenseClaimList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("개인 비용 청구 목록 조회 요청: page={}, size={}", page, size);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // totalCount 조회 (COUNT 쿼리)
            long totalCount = expenseClaimService.getExpenseClaimCount(userId);
            
            // 페이징된 목록 조회
            List<ExpenseClaim> expenseClaimList = expenseClaimService.getExpenseClaimList(userId, page, size);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = responseMapper.toExpenseClaimMapList(
                    expenseClaimList, 
                    userService::getUserInfo
            );
            
            // totalCount 포함 응답 생성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("list", responseList);
            responseData.put("totalCount", totalCount);
            
            return successResponse(responseData);
        } catch (ApiException e) {
            return errorResponse("개인 비용 청구 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("개인 비용 청구 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 개인 비용 청구 조회
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 개인 비용 청구 정보 (없으면 null)
     */
    @GetMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> getExpenseClaim(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("개인 비용 청구 조회 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaim(seq, userId);
            
            if (expenseClaim == null) {
                return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", null, null));
            }

            // 상세 항목 목록 조회
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            
            Map<String, Object> result = new HashMap<>();
            result.put("expenseClaim", expenseClaim);
            result.put("expenseSubList", expenseSubList);
            
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", result, null));
        } catch (Exception e) {
            log.error("개인 비용 청구 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "개인 비용 청구 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 개인 비용 청구 생성
     *
     * @param request HTTP 요청
     * @param expenseClaimRequest 개인 비용 청구 요청 데이터
     * @return 생성된 개인 비용 청구 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createExpenseClaim(
            HttpServletRequest request,
            @Valid @RequestBody ExpenseClaimRequest expenseClaimRequest) {
        log.info("개인 비용 청구 생성 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            ExpenseClaim expenseClaim = expenseClaimService.createExpenseClaim(userId, expenseClaimRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", expenseClaim, null));
        } catch (Exception e) {
            log.error("개인 비용 청구 생성 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "개인 비용 청구 생성에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 개인 비용 청구 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param expenseClaimRequest 개인 비용 청구 요청 데이터
     * @return 수정된 개인 비용 청구 정보
     */
    @PutMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> updateExpenseClaim(
            HttpServletRequest request,
            @PathVariable Long seq,
            @Valid @RequestBody ExpenseClaimRequest expenseClaimRequest) {
        log.info("개인 비용 청구 수정 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            ExpenseClaim expenseClaim = expenseClaimService.updateExpenseClaim(seq, userId, expenseClaimRequest);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", expenseClaim, null));
        } catch (Exception e) {
            log.error("개인 비용 청구 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "개인 비용 청구 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 개인 비용 청구 삭제
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 삭제 결과
     */
    @DeleteMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> deleteExpenseClaim(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("개인 비용 청구 삭제 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            expenseClaimService.deleteExpenseClaim(seq, userId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message", "삭제되었습니다.");
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", resultData, null));
        } catch (Exception e) {
            log.error("개인 비용 청구 삭제 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "개인 비용 청구 삭제에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 개인 비용 청구서 다운로드
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return XLSX 문서
     */
    @GetMapping("/{seq}/download")
    public ResponseEntity<byte[]> downloadExpenseClaim(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("개인 비용 청구서 다운로드 요청: seq={}", seq);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            User requester = userService.getUserInfo(requesterId);
            
            // 개인 비용 청구 조회 (seq만으로 조회)
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaimById(seq);
            if (expenseClaim == null) {
                log.warn("존재하지 않는 개인 비용 청구: seq={}", seq);
                return ResponseEntity.notFound().build();
            }
            
            // 권한 체크: 본인 신청서이거나 결재 권한이 있는 경우만 다운로드 가능
            Long applicantId = expenseClaim.getUserId();
            boolean canDownload = false;
            
            if (requesterId.equals(applicantId)) {
                // 본인 신청서
                canDownload = true;
            } else {
                // 결재 권한 체크
                String authVal = requester.getAuthVal();
                if ("ma".equals(authVal)) {
                    // 관리자는 모든 신청서 다운로드 가능
                    canDownload = true;
                } else if ("tj".equals(authVal) || "bb".equals(authVal)) {
                    // 팀장/본부장은 같은 본부의 신청서만 다운로드 가능
                    User applicant = userService.getUserInfo(applicantId);
                    if (applicant != null && requester.getDivision().equals(applicant.getDivision())) {
                        canDownload = true;
                    }
                }
            }
            
            if (!canDownload) {
                log.warn("다운로드 권한 없음: requesterId={}, applicantId={}, authVal={}", 
                        requesterId, applicantId, requester.getAuthVal());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 상세 항목 목록 조회
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            
            // 신청자 정보 조회
            User applicant = userService.getUserInfo(applicantId);
            
            // VO 생성
            ExpenseClaimVO vo = expenseClaimService.createExpenseClaimVO(
                    expenseClaim, expenseSubList, applicant);
            
            // XLSX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] excelBytes = FileGenerateUtil.generateExpenseClaimExcel(vo, null);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "개인비용신청서_" + applicant.getName() + "_" + dateStr + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
            headers.setContentLength(excelBytes.length);
            
            log.info("개인 비용 청구서 Excel 생성 완료. 크기: {} bytes", excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("개인 비용 청구서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

