package com.vacation.api.domain.rental.controller;

import com.vacation.api.annotation.RequiresAuth;
import com.vacation.api.domain.rental.entity.RentalApproval;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.request.RentalApprovalRequest;
import com.vacation.api.domain.rental.request.RentalSupportRequest;
import com.vacation.api.domain.rental.service.RentaltService;
import com.vacation.api.domain.sample.request.RentalSupportPropSampleRequest;
import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.enums.PaymentType;
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

/**
 * 월세 지원 정보 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalController {

    private final RentaltService rentaltService;
    private final UserService userService;
    private final TransactionIDCreator transactionIDCreator;

    /**
     * 월세 지원 정보 목록 조회
     *
     * @param request HTTP 요청
     * @return 월세 지원 정보 목록
     */
    @GetMapping
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getRentalSupportList(HttpServletRequest request) {
        log.info("월세 지원 정보 목록 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<RentalApproval> rentalApprovalList = rentaltService.getRentalSupportList(userId);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = rentalApprovalList.stream()
                    .map(approval -> {
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
                        
                        // userId로 사용자 이름 조회
                        User user = userService.getUserInfo(approval.getUserId());
                        map.put("applicant", user != null ? user.getName() : "");
                        
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", responseList, null));
        } catch (Exception e) {
            log.error("월세 지원 정보 목록 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 정보 목록 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 정보 조회
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 월세 지원 정보 (없으면 null)
     */
    @GetMapping("/{seq}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getRentalSupport(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 정보 조회 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalApproval rentalApproval = rentaltService.getRentalSupport(seq, userId);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", rentalApproval, null));
        } catch (Exception e) {
            log.error("월세 지원 정보 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 정보 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 정보 생성
     *
     * @param request HTTP 요청
     * @param rentalApprovalRequest 월세 지원 정보 요청 데이터
     * @return 생성된 월세 지원 정보
     */
    @PostMapping
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> createRentalSupport(
            HttpServletRequest request,
            @Valid @RequestBody RentalApprovalRequest rentalApprovalRequest) {
        log.info("월세 지원 정보 생성 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalApproval rentalApproval = rentaltService.createRentalSupport(userId, rentalApprovalRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", rentalApproval, null));
        } catch (Exception e) {
            log.error("월세 지원 정보 생성 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 정보 생성에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 정보 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param rentalApprovalRequest 월세 지원 정보 요청 데이터
     * @return 수정된 월세 지원 정보
     */
    @PutMapping("/{seq}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> updateRentalSupport(
            HttpServletRequest request,
            @PathVariable Long seq,
            @Valid @RequestBody RentalApprovalRequest rentalApprovalRequest) {
        log.info("월세 지원 정보 수정 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalApproval rentalApproval = rentaltService.updateRentalSupport(seq, userId, rentalApprovalRequest);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", rentalApproval, null));
        } catch (Exception e) {
            log.error("월세 지원 정보 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 정보 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 정보 삭제
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 삭제 결과
     */
    @DeleteMapping("/{seq}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> deleteRentalSupport(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 정보 삭제 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            rentaltService.deleteRentalSupport(seq, userId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message", "삭제되었습니다.");
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", resultData, null));
        } catch (Exception e) {
            log.error("월세 지원 정보 삭제 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 정보 삭제에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    // ========== 월세 지원 신청 (청구서) 관련 엔드포인트 ==========

    /**
     * 월세 지원 신청 목록 조회
     *
     * @param request HTTP 요청
     * @return 월세 지원 신청 목록
     */
    @GetMapping("/application")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getRentalSupportApplicationList(HttpServletRequest request) {
        log.info("월세 지원 신청 목록 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<RentalSupport> rentalSupportList = rentaltService.getRentalSupportApplicationList(userId);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = rentalSupportList.stream()
                    .map(rental -> {
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
                        
                        // userId로 사용자 이름 조회
                        User user = userService.getUserInfo(rental.getUserId());
                        map.put("applicant", user != null ? user.getName() : "");
                        
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", responseList, null));
        } catch (Exception e) {
            log.error("월세 지원 신청 목록 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 신청 목록 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 신청 조회
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 월세 지원 신청 정보 (없으면 null)
     */
    @GetMapping("/application/{seq}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> getRentalSupportApplication(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 신청 조회 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalSupport rentalSupport = rentaltService.getRentalSupportApplication(seq, userId);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", rentalSupport, null));
        } catch (Exception e) {
            log.error("월세 지원 신청 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 신청 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 신청 생성
     *
     * @param request HTTP 요청
     * @param rentalSupportRequest 월세 지원 신청 요청 데이터
     * @return 생성된 월세 지원 신청 정보
     */
    @PostMapping("/application")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> createRentalSupportApplication(
            HttpServletRequest request,
            @Valid @RequestBody RentalSupportRequest rentalSupportRequest) {
        log.info("월세 지원 신청 생성 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalSupport rentalSupport = rentaltService.createRentalSupportApplication(userId, rentalSupportRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", rentalSupport, null));
        } catch (Exception e) {
            log.error("월세 지원 신청 생성 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 신청 생성에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 신청 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param rentalSupportRequest 월세 지원 신청 요청 데이터
     * @return 수정된 월세 지원 신청 정보
     */
    @PutMapping("/application/{seq}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> updateRentalSupportApplication(
            HttpServletRequest request,
            @PathVariable Long seq,
            @Valid @RequestBody RentalSupportRequest rentalSupportRequest) {
        log.info("월세 지원 신청 수정 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalSupport rentalSupport = rentaltService.updateRentalSupportApplication(seq, userId, rentalSupportRequest);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", rentalSupport, null));
        } catch (Exception e) {
            log.error("월세 지원 신청 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 신청 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 신청 삭제
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 삭제 결과
     */
    @DeleteMapping("/application/{seq}")
    @RequiresAuth
    public ResponseEntity<ApiResponse<Object>> deleteRentalSupportApplication(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 신청 삭제 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            rentaltService.deleteRentalSupportApplication(seq, userId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message", "삭제되었습니다.");
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", resultData, null));
        } catch (Exception e) {
            log.error("월세 지원 신청 삭제 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "월세 지원 신청 삭제에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 월세 지원 신청서 다운로드 (청구서용)
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return XLSX 문서
     */
    @GetMapping("/application/{seq}/download")
    @RequiresAuth
    public ResponseEntity<byte[]> downloadRentalSupportApplication(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 신청서 다운로드 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 월세 지원 신청 조회
            RentalSupport rentalSupport = rentaltService.getRentalSupportApplication(seq, userId);
            if (rentalSupport == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(userId);
            String department = user.getDivision() + "/" + user.getTeam();
            
            // billingYyMonth를 month로 변환 (YYYYMM -> 1~12)
            int month = rentalSupport.getBillingYyMonth() % 100;
            
            // RentalSupportSampleRequest 생성
            RentalSupportSampleRequest sampleRequest = new RentalSupportSampleRequest();
            sampleRequest.setRequestDate(rentalSupport.getRequestDate());
            sampleRequest.setMonth(month);
            sampleRequest.setDepartment(department);
            sampleRequest.setApplicant(user.getName());
            sampleRequest.setContractStartDate(rentalSupport.getContractStartDate());
            sampleRequest.setContractEndDate(rentalSupport.getContractEndDate());
            sampleRequest.setContractMonthlyRent(rentalSupport.getContractMonthlyRent());
            sampleRequest.setPaymentType(rentalSupport.getPaymentType());
            sampleRequest.setBillingStartDate(rentalSupport.getBillingStartDate());
            sampleRequest.setBillingPeriodStartDate(rentalSupport.getBillingPeriodStartDate());
            sampleRequest.setBillingPeriodEndDate(rentalSupport.getBillingPeriodEndDate());
            sampleRequest.setPaymentDate(rentalSupport.getPaymentDate());
            sampleRequest.setPaymentAmount(rentalSupport.getPaymentAmount());
            sampleRequest.setBillingAmount(rentalSupport.getBillingAmount());
            
            // XLSX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] excelBytes = FileGenerateUtil.generateRentalSupportApplicationExcel(sampleRequest, null);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "월세지원신청서_" + user.getName() + "_" + dateStr + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
            headers.setContentLength(excelBytes.length);
            
            log.info("월세 지원 신청서 Excel 생성 완료. 크기: {} bytes", excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("월세 지원 신청서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월세 지원 품의서 다운로드
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return DOCX 문서
     */
    @GetMapping("/{seq}/download-proposal")
    @RequiresAuth
    public ResponseEntity<byte[]> downloadRentalProposal(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 품의서 다운로드 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 월세 지원 품의 정보 조회
            RentalApproval rentalApproval = rentaltService.getRentalSupport(seq, userId);
            if (rentalApproval == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(userId);
            String department = user.getDivision() + "/" + user.getTeam();
            
            // RentalSupportPropSampleRequest 생성
            RentalSupportPropSampleRequest sampleRequest = new RentalSupportPropSampleRequest();
            sampleRequest.setRequestDate(LocalDate.now()); // 품의 일자는 현재 날짜로 설정
            sampleRequest.setDepartment(department);
            sampleRequest.setApplicant(user.getName());
            sampleRequest.setCurrentAddress(rentalApproval.getPreviousAddress());
            sampleRequest.setRentalAddress(rentalApproval.getRentalAddress());
            sampleRequest.setContractStartDate(rentalApproval.getContractStartDate());
            sampleRequest.setContractEndDate(rentalApproval.getContractEndDate());
            sampleRequest.setContractMonthlyRent(rentalApproval.getContractMonthlyRent());
            sampleRequest.setBillingAmount(rentalApproval.getBillingAmount());
            sampleRequest.setBillingStartDate(rentalApproval.getBillingStartDate());
            sampleRequest.setReason(rentalApproval.getBillingReason());
            
            // DOCX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] docBytes = FileGenerateUtil.generateRentalSupportProposalDoc(sampleRequest, null);
            
            // 파일명 생성
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "월세지원품의서_" + user.getName() + "_" + dateStr + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
            headers.setContentLength(docBytes.length);
            
            log.info("월세 지원 품의서 DOCX 생성 완료. 크기: {} bytes", docBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(docBytes);
        } catch (Exception e) {
            log.error("월세 지원 품의서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

