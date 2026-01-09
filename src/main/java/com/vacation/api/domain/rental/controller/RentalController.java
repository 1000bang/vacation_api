package com.vacation.api.domain.rental.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.domain.rental.entity.RentalApproval;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.request.RentalApprovalRequest;
import com.vacation.api.domain.rental.request.RentalSupportRequest;
import com.vacation.api.domain.rental.service.RentaltService;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import com.vacation.api.util.ResponseMapper;
import com.vacation.api.exception.ApiException;
import com.vacation.api.vo.RentalSupportApplicationVO;
import com.vacation.api.vo.RentalSupportProposalVO;
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
 * 월세 지원 정보 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/rental")
public class RentalController extends BaseController {

    private final RentaltService rentaltService;
    private final UserService userService;
    private final ResponseMapper responseMapper;

    public RentalController(RentaltService rentaltService, UserService userService, 
                           ResponseMapper responseMapper, TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.rentaltService = rentaltService;
        this.userService = userService;
        this.responseMapper = responseMapper;
    }

    /**
     * 월세 지원 정보 목록 조회
     *
     * @param request HTTP 요청
     * @return 월세 지원 정보 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getRentalSupportList(HttpServletRequest request) {
        log.info("월세 지원 정보 목록 조회 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<RentalApproval> rentalApprovalList = rentaltService.getRentalSupportList(userId);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = responseMapper.toRentalApprovalMapList(
                    rentalApprovalList, 
                    userService::getUserInfo
            );
            
            return successResponse(responseList);
        } catch (ApiException e) {
            return errorResponse("월세 지원 정보 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 정보 목록 조회에 실패했습니다.", e);
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
    public ResponseEntity<ApiResponse<Object>> getRentalSupport(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 정보 조회 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalApproval rentalApproval = rentaltService.getRentalSupport(seq, userId);
            return successResponse(rentalApproval);
        } catch (ApiException e) {
            return errorResponse("월세 지원 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 정보 조회에 실패했습니다.", e);
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
    public ResponseEntity<ApiResponse<Object>> createRentalSupport(
            HttpServletRequest request,
            @Valid @RequestBody RentalApprovalRequest rentalApprovalRequest) {
        log.info("월세 지원 정보 생성 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalApproval rentalApproval = rentaltService.createRentalSupport(userId, rentalApprovalRequest);
            String transactionId = getOrCreateTransactionId();
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", rentalApproval, null));
        } catch (ApiException e) {
            return errorResponse("월세 지원 정보 생성에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 정보 생성에 실패했습니다.", e);
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
    public ResponseEntity<ApiResponse<Object>> getRentalSupportApplicationList(HttpServletRequest request) {
        log.info("월세 지원 신청 목록 조회 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<RentalSupport> rentalSupportList = rentaltService.getRentalSupportApplicationList(userId);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = responseMapper.toRentalSupportMapList(
                    rentalSupportList, 
                    userService::getUserInfo
            );
            
            return successResponse(responseList);
        } catch (ApiException e) {
            return errorResponse("월세 지원 신청 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 신청 목록 조회에 실패했습니다.", e);
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
            
            // VO 생성
            RentalSupportApplicationVO vo = rentaltService.createRentalSupportApplicationVO(
                    rentalSupport, user);
            
            // XLSX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] excelBytes = FileGenerateUtil.generateRentalSupportApplicationExcel(vo, null);
            
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
            
            // VO 생성
            RentalSupportProposalVO vo = rentaltService.createRentalSupportProposalVO(
                    rentalApproval, user);
            
            // DOCX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] docBytes = FileGenerateUtil.generateRentalSupportProposalDoc(vo, null);
            
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

