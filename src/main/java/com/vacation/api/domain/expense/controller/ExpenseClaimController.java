package com.vacation.api.domain.expense.controller;

import com.vacation.api.annotation.RequiresAuth;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.request.ExpenseClaimRequest;
import com.vacation.api.domain.expense.service.ExpenseClaimService;
import com.vacation.api.domain.sample.request.ExpenseClaimSampleRequest;
import com.vacation.api.domain.sample.request.ExpenseItem;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;

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
@RequiredArgsConstructor
public class ExpenseClaimController {

    private final ExpenseClaimService expenseClaimService;
    private final UserService userService;
    private final TransactionIDCreator transactionIDCreator;

    /**
     * 개인 비용 청구 목록 조회
     *
     * @param request HTTP 요청
     * @return 개인 비용 청구 목록
     */
    @GetMapping
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getExpenseClaimList(HttpServletRequest request) {
        log.info("개인 비용 청구 목록 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<ExpenseClaim> expenseClaimList = expenseClaimService.getExpenseClaimList(userId);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = expenseClaimList.stream()
                    .map(claim -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("seq", claim.getSeq());
                        map.put("userId", claim.getUserId());
                        map.put("requestDate", claim.getRequestDate());
                        map.put("billingYyMonth", claim.getBillingYyMonth());
                        map.put("childCnt", claim.getChildCnt());
                        map.put("totalAmount", claim.getTotalAmount());
                        map.put("createdAt", claim.getCreatedAt());
                        
                        // userId로 사용자 이름 조회
                        User user = userService.getUserInfo(claim.getUserId());
                        map.put("applicant", user != null ? user.getName() : "");
                        
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", responseList, null));
        } catch (Exception e) {
            log.error("개인 비용 청구 목록 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "개인 비용 청구 목록 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
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
    @RequiresAuth
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
    @RequiresAuth
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
    @RequiresAuth
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
    @RequiresAuth
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
    @RequiresAuth
    public ResponseEntity<byte[]> downloadExpenseClaim(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("개인 비용 청구서 다운로드 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 개인 비용 청구 조회
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaim(seq, userId);
            if (expenseClaim == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 상세 항목 목록 조회
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(userId);
            String department = user.getDivision() + "/" + user.getTeam();
            
            // billingYyMonth를 month로 변환 (YYYYMM -> 1~12)
            int month = expenseClaim.getBillingYyMonth() % 100;
            
            // ExpenseItem 리스트 생성
            List<ExpenseItem> expenseItems = expenseSubList.stream()
                    .map(sub -> {
                        ExpenseItem item = new ExpenseItem();
                        item.setDate(sub.getDate());
                        item.setUsageDetail(sub.getUsageDetail());
                        item.setVendor(sub.getVendor());
                        item.setPaymentMethod(sub.getPaymentMethod());
                        item.setProject(sub.getProject());
                        item.setAmount(sub.getAmount());
                        item.setNote(sub.getNote());
                        return item;
                    })
                    .collect(Collectors.toList());
            
            // ExpenseClaimSampleRequest 생성
            ExpenseClaimSampleRequest sampleRequest = new ExpenseClaimSampleRequest();
            sampleRequest.setRequestDate(expenseClaim.getRequestDate());
            sampleRequest.setMonth(month);
            sampleRequest.setDepartment(department);
            sampleRequest.setApplicant(user.getName());
            sampleRequest.setExpenseItems(expenseItems);
            
            // XLSX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] excelBytes = FileGenerateUtil.generateExpenseClaimExcel(sampleRequest, null);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "개인비용신청서_" + user.getName() + "_" + dateStr + ".xlsx";
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

