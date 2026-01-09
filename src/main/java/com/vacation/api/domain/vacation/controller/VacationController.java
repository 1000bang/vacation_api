package com.vacation.api.domain.vacation.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.domain.vacation.entity.UserVacationInfo;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.request.UpdateVacationInfoRequest;
import com.vacation.api.domain.vacation.request.VacationRequest;
import com.vacation.api.domain.vacation.response.UserVacationInfoResponse;
import com.vacation.api.domain.vacation.service.VacationService;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import com.vacation.api.util.ResponseMapper;
import com.vacation.api.vo.VacationDocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
import org.slf4j.MDC;

/**
 * 연차 Controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/vacation")
public class VacationController extends BaseController {

    private final VacationService vacationService;
    private final UserService userService;
    private final ResponseMapper responseMapper;

    public VacationController(VacationService vacationService, UserService userService,
                              ResponseMapper responseMapper, TransactionIDCreator transactionIDCreator) {
        super(transactionIDCreator);
        this.vacationService = vacationService;
        this.userService = userService;
        this.responseMapper = responseMapper;
    }

    /**
     * 사용자별 연차 정보 조회
     *
     * @param request HTTP 요청
     * @return 연차 정보
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Object>> getUserVacationInfo(HttpServletRequest request) {
        log.info("사용자별 연차 정보 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            User userInfo = userService.getUserInfo(userId);
            UserVacationInfo vacationInfo = vacationService.getUserVacationInfo(userId);
            
            Double remainingDays = vacationInfo.getAnnualVacationDays() 
                    - vacationInfo.getUsedVacationDays() 
                    - vacationInfo.getReservedVacationDays();
            
            UserVacationInfoResponse response = UserVacationInfoResponse.builder()
                    .seq(vacationInfo.getSeq())
                    .userId(vacationInfo.getUserId())
                    .annualVacationDays(vacationInfo.getAnnualVacationDays())
                    .usedVacationDays(vacationInfo.getUsedVacationDays())
                    .reservedVacationDays(vacationInfo.getReservedVacationDays())
                    .remainingVacationDays(remainingDays)
                    .isFirstLogin(userInfo.getFirstLogin())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("사용자별 연차 정보 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "연차 정보 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 사용자별 연차 정보 수정
     *
     * @param request HTTP 요청
     * @param updateRequest 연차 정보 수정 요청 데이터
     * @return 수정된 연차 정보
     */
    @PutMapping("/info")
    public ResponseEntity<ApiResponse<Object>> updateUserVacationInfo(
            HttpServletRequest request,
            @Valid @RequestBody UpdateVacationInfoRequest updateRequest) {
        log.info("사용자별 연차 정보 수정 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            // 요청자와 userId가 같으면 isFirstLogin을 false로 변경 (VacationService에서 처리)
            UserVacationInfo vacationInfo = vacationService.updateUserVacationInfo(userId, updateRequest);
            
            Double remainingDays = vacationInfo.getAnnualVacationDays() 
                    - vacationInfo.getUsedVacationDays() 
                    - vacationInfo.getReservedVacationDays();
            
            // 업데이트된 사용자 정보 다시 조회 (firstLogin이 false로 변경되었을 수 있음)
            User updatedUserInfo = userService.getUserInfo(userId);
            
            UserVacationInfoResponse response = UserVacationInfoResponse.builder()
                    .seq(vacationInfo.getSeq())
                    .userId(vacationInfo.getUserId())
                    .annualVacationDays(vacationInfo.getAnnualVacationDays())
                    .usedVacationDays(vacationInfo.getUsedVacationDays())
                    .reservedVacationDays(vacationInfo.getReservedVacationDays())
                    .remainingVacationDays(remainingDays)
                    .isFirstLogin(updatedUserInfo.getFirstLogin())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("사용자별 연차 정보 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "연차 정보 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 특정 사용자별 연차 정보 조회 (관리자용)
     *
     * @param userId 사용자 ID
     * @return 연차 정보
     */
    @GetMapping("/info/{userId}")
    public ResponseEntity<ApiResponse<Object>> getUserVacationInfoByUserId(@PathVariable Long userId) {
        log.info("특정 사용자별 연차 정보 조회 요청: userId={}", userId);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // TODO: 권한 체크 (관리자만 조회 가능)
            User userInfo = userService.getUserInfo(userId);
            UserVacationInfo vacationInfo = vacationService.getUserVacationInfo(userId);
            
            Double remainingDays = vacationInfo.getAnnualVacationDays() 
                    - vacationInfo.getUsedVacationDays() 
                    - vacationInfo.getReservedVacationDays();
            
            UserVacationInfoResponse response = UserVacationInfoResponse.builder()
                    .seq(vacationInfo.getSeq())
                    .userId(vacationInfo.getUserId())
                    .annualVacationDays(vacationInfo.getAnnualVacationDays())
                    .usedVacationDays(vacationInfo.getUsedVacationDays())
                    .reservedVacationDays(vacationInfo.getReservedVacationDays())
                    .remainingVacationDays(remainingDays)
                    .isFirstLogin(userInfo.getFirstLogin())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("특정 사용자별 연차 정보 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "연차 정보 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 특정 사용자별 연차 정보 수정 (관리자용)
     *
     * @param userId 사용자 ID
     * @param updateRequest 수정 요청 데이터
     * @return 수정된 연차 정보
     */
    @PutMapping("/info/{userId}")
    public ResponseEntity<ApiResponse<Object>> updateUserVacationInfoByUserId(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateVacationInfoRequest updateRequest) {
        log.info("특정 사용자별 연차 정보 수정 요청: userId={}", userId);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            // TODO: 권한 체크 (관리자만 수정 가능)
            // 관리자가 수정하는 경우이므로 firstLogin을 변경하지 않음
            UserVacationInfo vacationInfo = vacationService.updateUserVacationInfo(userId, updateRequest);
            
            Double remainingDays = vacationInfo.getAnnualVacationDays() 
                    - vacationInfo.getUsedVacationDays() 
                    - vacationInfo.getReservedVacationDays();
            
            User userInfo = userService.getUserInfo(userId);
            
            UserVacationInfoResponse response = UserVacationInfoResponse.builder()
                    .seq(vacationInfo.getSeq())
                    .userId(vacationInfo.getUserId())
                    .annualVacationDays(vacationInfo.getAnnualVacationDays())
                    .usedVacationDays(vacationInfo.getUsedVacationDays())
                    .reservedVacationDays(vacationInfo.getReservedVacationDays())
                    .remainingVacationDays(remainingDays)
                    .isFirstLogin(userInfo.getFirstLogin())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", response, null));
        } catch (Exception e) {
            log.error("특정 사용자별 연차 정보 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "연차 정보 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 연차 내역 목록 조회
     *
     * @param request HTTP 요청
     * @return 연차 내역 목록
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getVacationHistoryList(HttpServletRequest request) {
        log.info("연차 내역 목록 조회 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            List<VacationHistory> historyList = vacationService.getVacationHistoryList(userId);
            
            // 각 항목에 applicant 추가
            List<Map<String, Object>> responseList = responseMapper.toVacationHistoryMapList(
                    historyList, 
                    userService::getUserInfo
            );
            
            return successResponse(responseList);
        } catch (ApiException e) {
            return errorResponse("연차 내역 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 내역 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 연차 내역 조회
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 연차 내역 (없으면 null)
     */
    @GetMapping("/history/{seq}")
    public ResponseEntity<ApiResponse<Object>> getVacationHistory(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("연차 내역 조회 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            VacationHistory vacationHistory = vacationService.getVacationHistory(seq, userId);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", vacationHistory, null));
        } catch (Exception e) {
            log.error("연차 내역 조회 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", "연차 내역 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 휴가 신청
     *
     * @param request HTTP 요청
     * @param vacationRequest 휴가 신청 요청 데이터
     * @return 생성된 연차 내역
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createVacation(
            HttpServletRequest request,
            @Valid @RequestBody VacationRequest vacationRequest) {
        log.info("휴가 신청 요청");

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            VacationHistory vacationHistory = vacationService.createVacation(userId, vacationRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", vacationHistory, null));
        } catch (Exception e) {
            log.error("휴가 신청 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", e.getMessage() != null ? e.getMessage() : "휴가 신청에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 휴가 신청 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param vacationRequest 휴가 신청 요청 데이터
     * @return 수정된 연차 내역
     */
    @PutMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> updateVacation(
            HttpServletRequest request,
            @PathVariable Long seq,
            @Valid @RequestBody VacationRequest vacationRequest) {
        log.info("휴가 신청 수정 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            VacationHistory vacationHistory = vacationService.updateVacation(seq, userId, vacationRequest);
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", vacationHistory, null));
        } catch (Exception e) {
            log.error("휴가 신청 수정 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", e.getMessage() != null ? e.getMessage() : "휴가 신청 수정에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 휴가 신청 삭제
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 삭제 결과
     */
    @DeleteMapping("/{seq}")
    public ResponseEntity<ApiResponse<Object>> deleteVacation(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("휴가 신청 삭제 요청: seq={}", seq);

        String transactionId = MDC.get("transactionId");
        if (transactionId == null) {
            transactionId = transactionIDCreator.createTransactionId();
        }

        try {
            Long userId = (Long) request.getAttribute("userId");
            vacationService.deleteVacation(seq, userId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message", "삭제되었습니다.");
            return ResponseEntity.ok(new ApiResponse<>(transactionId, "0", resultData, null));
        } catch (ApiException e) {
            log.error("휴가 신청 삭제 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", e.getApiErrorCode().getCode());
            errorData.put("errorMessage", e.getMessage() != null ? e.getMessage() : e.getApiErrorCode().getDescription());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(transactionId, e.getApiErrorCode().getCode(), errorData, null));
        } catch (Exception e) {
            log.error("휴가 신청 삭제 실패", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorCode", "500");
            errorData.put("errorMessage", e.getMessage() != null ? e.getMessage() : "휴가 신청 삭제에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(transactionId, "500", errorData, null));
        }
    }

    /**
     * 휴가 신청서 다운로드
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return DOCX 문서
     */
    @GetMapping("/history/{seq}/download")
    public ResponseEntity<byte[]> downloadVacationDocument(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("휴가 신청서 다운로드 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 휴가 내역 조회
            VacationHistory vacationHistory = vacationService.getVacationHistory(seq, userId);
            if (vacationHistory == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(userId);
            
            // 연차 정보 조회
            UserVacationInfo vacationInfo = vacationService.getUserVacationInfo(userId);
            
            // VO 생성
            VacationDocumentVO vo = vacationService.createVacationDocumentVO(
                    vacationHistory, user, vacationInfo);
            
            // DOCX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] docBytes = FileGenerateUtil.generateVacationApplicationDoc(vo, null);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = "휴가(결무)신청서_" + user.getName() + "_" + dateStr + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
            headers.setContentLength(docBytes.length);
            
            log.info("휴가 신청서 DOCX 생성 완료. 크기: {} bytes", docBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(docBytes);
        } catch (Exception e) {
            log.error("휴가 신청서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
