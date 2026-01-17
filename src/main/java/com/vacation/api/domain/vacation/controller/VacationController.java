package com.vacation.api.domain.vacation.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.common.PagedResponse;
import com.vacation.api.domain.attachment.entity.Attachment;
import com.vacation.api.domain.attachment.service.FileService;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.domain.vacation.entity.UserVacationInfo;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.request.UpdateVacationInfoRequest;
import com.vacation.api.domain.vacation.request.VacationRequest;
import com.vacation.api.domain.vacation.response.UserVacationInfoResponse;
import com.vacation.api.domain.vacation.response.VacationHistoryResponse;
import com.vacation.api.domain.vacation.service.VacationService;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.exception.ApiException;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import com.vacation.api.util.ResponseMapper;
import com.vacation.api.util.ZipFileUtil;
import com.vacation.api.vo.VacationDocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final FileService fileService;
    private final ZipFileUtil zipFileUtil;

    public VacationController(VacationService vacationService, UserService userService,
                              ResponseMapper responseMapper, FileService fileService,
                              TransactionIDCreator transactionIDCreator,
                              ZipFileUtil zipFileUtil) {
        super(transactionIDCreator);
        this.vacationService = vacationService;
        this.userService = userService;
        this.responseMapper = responseMapper;
        this.fileService = fileService;
        this.zipFileUtil = zipFileUtil;
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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("연차 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 정보 조회에 실패했습니다.", e);
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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("연차 정보 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 정보 수정에 실패했습니다.", e);
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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("연차 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 정보 조회에 실패했습니다.", e);
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

            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("연차 정보 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 정보 수정에 실패했습니다.", e);
        }
    }

    /**
     * 연차 내역 목록 조회 (페이징)
     *
     * @param request HTTP 요청
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 5)
     * @return 연차 내역 목록
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getVacationHistoryList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("연차 내역 목록 조회 요청: page={}, size={}", page, size);


        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // totalCount 조회 (COUNT 쿼리)
            long totalCount = vacationService.getVacationHistoryCount(userId);
            
            // 페이징된 목록 조회
            List<VacationHistory> historyList = vacationService.getVacationHistoryList(userId, page, size);
            
            // 각 항목에 applicant 추가하여 Response VO로 변환
            List<VacationHistoryResponse> responseList = responseMapper.toVacationHistoryResponseList(
                    historyList, 
                    userService::getUserInfo
            );
            
            // totalCount 포함 응답 생성
            PagedResponse<VacationHistoryResponse> responseData = PagedResponse.<VacationHistoryResponse>builder()
                    .list(responseList)
                    .totalCount(totalCount)
                    .build();
            
            return successResponse(responseData);
        } catch (ApiException e) {
            return errorResponse("연차 내역 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 내역 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 캘린더용 휴가 목록 조회 (본부 전체, 현재 월 기준 전후 1개월)
     *
     * @param request HTTP 요청
     * @param year 조회할 연도 (선택, 기본값: 현재 연도)
     * @param month 조회할 월 (선택, 기본값: 현재 월)
     * @return 휴가 내역 목록
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<Object>> getCalendarVacationList(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        log.info("캘린더용 휴가 목록 조회 요청: year={}, month={}", year, month);


        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 파라미터가 없으면 현재 연도/월 사용
            if (year == null || month == null) {
                LocalDate now = LocalDate.now();
                year = year != null ? year : now.getYear();
                month = month != null ? month : now.getMonthValue();
            }
            
            List<VacationHistory> vacationList = vacationService.getCalendarVacationList(userId, year, month);
            
            // 각 항목에 applicant 추가하여 Response VO로 변환
            List<VacationHistoryResponse> responseList = responseMapper.toVacationHistoryResponseList(
                    vacationList, 
                    userService::getUserInfo
            );
            
            return successResponse(responseList);
        } catch (ApiException e) {
            return errorResponse("캘린더용 휴가 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("캘린더용 휴가 목록 조회에 실패했습니다.", e);
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


        try {
            Long userId = (Long) request.getAttribute("userId");
            VacationHistory vacationHistory = vacationService.getVacationHistory(seq, userId);
            
            if (vacationHistory == null) {
                return successResponse(null);
            }
            
            // 반려 상태인 경우 반려 사유 조회
            String rejectionReason = null;
            String approvalStatus = vacationHistory.getApprovalStatus();
            if ("RB".equals(approvalStatus) || "RC".equals(approvalStatus)) {
                rejectionReason = vacationService.getRejectionReason(seq);
            }
            
            // 첨부파일 정보 조회
            Attachment attachment = fileService.getAttachment(ApplicationType.VACATION.getCode(), seq);
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(vacationHistory.getUserId());
            String applicantName = user != null ? user.getName() : null;
            
            // Response VO로 변환
            VacationHistoryResponse response = responseMapper.toVacationHistoryResponse(
                    vacationHistory, applicantName, attachment, rejectionReason);
            
            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("연차 내역 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("연차 내역 조회에 실패했습니다.", e);
        }
    }

    /**
     * 휴가 신청
     *
     * @param request HTTP 요청
     * @param vacationRequest 휴가 신청 요청 데이터
     * @param file 첨부파일 (선택)
     * @return 생성된 연차 내역
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> createVacation(
            HttpServletRequest request,
            @RequestPart("vacationRequest") @Valid VacationRequest vacationRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("휴가 신청 요청");


        try {
            Long userId = (Long) request.getAttribute("userId");
            VacationHistory vacationHistory = vacationService.createVacation(userId, vacationRequest);
            
            // 파일이 있으면 업로드
            if (file != null && !file.isEmpty()) {
                try {
                    fileService.uploadFile(file, ApplicationType.VACATION.getCode(), vacationHistory.getSeq(), userId);
                } catch (Exception e) {
                    log.error("파일 업로드 실패: seq={}", vacationHistory.getSeq(), e);
                    // 파일 업로드 실패해도 신청은 성공으로 처리
                }
            }
            
            String transactionId = getOrCreateTransactionId();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(transactionId, "0", vacationHistory, null));
        } catch (ApiException e) {
            return errorResponse("휴가 신청에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("휴가 신청에 실패했습니다.", e);
        }
    }

    /**
     * 휴가 신청 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param vacationRequest 휴가 신청 요청 데이터
     * @param file 첨부파일 (선택)
     * @return 수정된 연차 내역
     */
    @PutMapping(value = "/{seq}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> updateVacation(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestPart("vacationRequest") @Valid VacationRequest vacationRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("휴가 신청 수정 요청: seq={}", seq);


        try {
            Long userId = (Long) request.getAttribute("userId");
            VacationHistory vacationHistory = vacationService.updateVacation(seq, userId, vacationRequest);
            
            // 파일이 있으면 업로드
            if (file != null && !file.isEmpty()) {
                try {
                    fileService.uploadFile(file, ApplicationType.VACATION.getCode(), vacationHistory.getSeq(), userId);
                } catch (Exception e) {
                    log.error("파일 업로드 실패: seq={}", vacationHistory.getSeq(), e);
                    // 파일 업로드 실패해도 수정은 성공으로 처리
                }
            }
            
            return successResponse(vacationHistory);
        } catch (ApiException e) {
            return errorResponse("휴가 신청 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("휴가 신청 수정에 실패했습니다.", e);
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


        try {
            Long userId = (Long) request.getAttribute("userId");
            vacationService.deleteVacation(seq, userId);
            return successResponse("삭제되었습니다.");
        } catch (ApiException e) {
            return errorResponse("휴가 신청 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("휴가 신청 삭제에 실패했습니다.", e);
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
            Long requesterId = (Long) request.getAttribute("userId");
            User requester = userService.getUserInfo(requesterId);
            
            // 휴가 내역 조회 (seq만으로 조회)
            VacationHistory vacationHistory = vacationService.getVacationHistoryById(seq);
            if (vacationHistory == null) {
                log.warn("존재하지 않는 휴가 신청: seq={}", seq);
                return ResponseEntity.notFound().build();
            }
            
            // 권한 체크: 본인 신청서이거나 결재 권한이 있는 경우만 다운로드 가능
            Long applicantId = vacationHistory.getUserId();
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
            
            // 신청자 정보 조회
            User applicant = userService.getUserInfo(applicantId);
            
            // 신청자 연차 정보 조회
            UserVacationInfo vacationInfo = vacationService.getUserVacationInfo(applicantId);
            
            // VO 생성
            VacationDocumentVO vo = vacationService.createVacationDocumentVO(
                    vacationHistory, applicant, vacationInfo);
            
            // DOCX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] docBytes = FileGenerateUtil.generateVacationApplicationDoc(vo, null);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String documentFileName = "휴가(결무)신청서_" + applicant.getName() + "_" + dateStr + ".docx";
            
            // 첨부파일 조회
            List<Attachment> attachments = 
                    fileService.getAttachments(ApplicationType.VACATION.getCode(), seq);
            
            // 첨부파일이 있으면 ZIP으로 묶기
            if (attachments != null && !attachments.isEmpty()) {
                byte[] zipBytes = zipFileUtil.createZipWithDocumentAndAttachments(
                        docBytes, documentFileName, attachments);
                
                String zipFileName = documentFileName.replace(".docx", ".zip");
                String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/zip"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
                headers.setContentLength(zipBytes.length);
                
                log.info("휴가 신청서 ZIP 생성 완료. 크기: {} bytes, 첨부파일: {}개", zipBytes.length, attachments.size());
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(zipBytes);
            } else {
                // 첨부파일이 없으면 기존처럼 문서만 반환
                String encodedFileName = URLEncoder.encode(documentFileName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
                headers.setContentLength(docBytes.length);
                
                log.info("휴가 신청서 DOCX 생성 완료. 크기: {} bytes", docBytes.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(docBytes);
            }
        } catch (Exception e) {
            log.error("휴가 신청서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 휴가 신청 첨부파일 다운로드
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 첨부파일
     */
    @GetMapping("/history/{seq}/attachment")
    public ResponseEntity<Resource> downloadVacationAttachment(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("휴가 신청 첨부파일 다운로드 요청: seq={}", seq);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            
            // 휴가 내역 조회 (권한 체크)
            VacationHistory vacationHistory = vacationService.getVacationHistory(seq, requesterId);
            if (vacationHistory == null) {
                log.warn("존재하지 않는 휴가 신청 또는 권한 없음: seq={}, requesterId={}", seq, requesterId);
                return ResponseEntity.notFound().build();
            }
            
            // 첨부파일 조회
            Attachment attachment = fileService.getAttachment(ApplicationType.VACATION.getCode(), seq);
            if (attachment == null) {
                log.warn("첨부파일이 없음: seq={}", seq);
                return ResponseEntity.notFound().build();
            }
            
            // 파일 다운로드
            Resource resource = fileService.downloadFile(attachment);
            
            // 파일명 인코딩
            String encodedFileName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            log.error("휴가 신청 첨부파일 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
