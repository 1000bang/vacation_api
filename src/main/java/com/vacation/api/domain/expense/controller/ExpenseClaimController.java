package com.vacation.api.domain.expense.controller;

import com.vacation.api.common.controller.BaseController;
import com.vacation.api.response.data.PagedResponse;
import com.vacation.api.domain.attachment.entity.Attachment;
import com.vacation.api.domain.attachment.service.FileService;
import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.request.ExpenseClaimRequest;
import com.vacation.api.domain.expense.response.ExpenseClaimResponse;
import com.vacation.api.domain.expense.response.ExpenseSubResponse;
import com.vacation.api.domain.expense.service.ExpenseClaimService;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import com.vacation.api.util.ResponseMapper;
import com.vacation.api.util.ZipFileUtil;
import com.vacation.api.util.CommonUtil;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.exception.ApiException;
import com.vacation.api.vo.ExpenseClaimVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
public class ExpenseClaimController extends BaseController {

    private final ExpenseClaimService expenseClaimService;
    private final UserService userService;
    private final ResponseMapper responseMapper;
    private final FileService fileService;
    private final ZipFileUtil zipFileUtil;
    private final FileGenerateUtil fileGenerateUtil;
    private final UserRepository userRepository;

    public ExpenseClaimController(ExpenseClaimService expenseClaimService, UserService userService,
                                 ResponseMapper responseMapper, TransactionIDCreator transactionIDCreator,
                                 FileService fileService, ZipFileUtil zipFileUtil,
                                 FileGenerateUtil fileGenerateUtil,
                                 UserRepository userRepository) {
        super(transactionIDCreator);
        this.expenseClaimService = expenseClaimService;
        this.userService = userService;
        this.responseMapper = responseMapper;
        this.fileService = fileService;
        this.zipFileUtil = zipFileUtil;
        this.fileGenerateUtil = fileGenerateUtil;
        this.userRepository = userRepository;
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
            
            // 각 항목에 applicant 추가하여 Response VO로 변환
            List<ExpenseClaimResponse> responseList = responseMapper.toExpenseClaimResponseList(
                    expenseClaimList, 
                    userService::getUserInfo
            );
            
            // totalCount 포함 응답 생성
            PagedResponse<ExpenseClaimResponse> responseData = PagedResponse.<ExpenseClaimResponse>builder()
                    .list(responseList)
                    .totalCount(totalCount)
                    .build();
            
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaim(seq, userId);
            
            if (expenseClaim == null) {
                return successResponse(null);
            }

            // 상세 항목 목록 조회
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            
            // 각 항목에 첨부파일 정보 추가하여 Response VO로 변환
            List<ExpenseSubResponse> expenseSubListWithAttachments = expenseSubList.stream()
                    .map(sub -> {
                        // 첨부파일 정보 조회
                        List<Attachment> attachments = fileService.getExpenseItemAttachments(sub.getSeq());
                        Attachment attachment = (attachments != null && !attachments.isEmpty()) 
                                ? attachments.get(0) : null;
                        
                        return responseMapper.toExpenseSubResponse(sub, attachment);
                    })
                    .collect(Collectors.toList());
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(expenseClaim.getUserId());
            String applicantName = user != null ? user.getName() : null;
            
            // Response VO로 변환
            ExpenseClaimResponse response = responseMapper.toExpenseClaimResponse(
                    expenseClaim, applicantName, expenseSubListWithAttachments);
            
            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("개인 비용 청구 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("개인 비용 청구 조회에 실패했습니다.", e);
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            ExpenseClaim expenseClaim = expenseClaimService.createExpenseClaim(userId, expenseClaimRequest);
            User applicant = userService.getUserInfo(userId);
            ExpenseClaimResponse resp = responseMapper.toExpenseClaimResponse(
                    expenseClaim, applicant != null ? applicant.getName() : null, List.of());
            return createdResponse(resp);
        } catch (ApiException e) {
            return errorResponse("개인 비용 청구 생성에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("개인 비용 청구 생성에 실패했습니다.", e);
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            ExpenseClaim expenseClaim = expenseClaimService.updateExpenseClaim(seq, userId, expenseClaimRequest);
            return successResponse(expenseClaim);
        } catch (ApiException e) {
            return errorResponse("개인 비용 청구 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("개인 비용 청구 수정에 실패했습니다.", e);
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            expenseClaimService.deleteExpenseClaim(seq, userId);
            return successResponse("삭제되었습니다.");
        } catch (ApiException e) {
            return errorResponse("개인 비용 청구 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("개인 비용 청구 삭제에 실패했습니다.", e);
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
                if (AuthVal.MASTER.getCode().equals(authVal)) {
                    // 관리자는 모든 신청서 다운로드 가능
                    canDownload = true;
                } else if (AuthVal.TEAM_LEADER.getCode().equals(authVal) || AuthVal.DIVISION_HEAD.getCode().equals(authVal)) {
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
            
            // 서명 이미지 맵 생성 (승인 상태 포함)
            String approvalStatus = expenseClaim.getApprovalStatus();
            if (approvalStatus == null) {
                approvalStatus = ApprovalStatus.INITIAL.getName();
            }
            Map<String, byte[]> signatureImageMap = createSignatureImageMapForExpense(
                    expenseClaim, applicant, approvalStatus, vo.getRequestDate());
            
            // XLSX 생성 (서명 이미지 맵 전달)
            byte[] excelBytes = FileGenerateUtil.generateExpenseClaimExcel(vo, signatureImageMap);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String documentFileName = "개인비용신청서_" + applicant.getName() + "_" + dateStr + ".xlsx";
            
            // 개인비용 항목별 첨부파일 조회 (childNo를 키로 사용)
            java.util.Map<Integer, List<Attachment>> expenseSubAttachments = 
                    new java.util.HashMap<>();
            boolean hasAttachments = false;
            
            for (ExpenseSub expenseSub : expenseSubList) {
                List<Attachment> attachments = 
                        fileService.getExpenseItemAttachments(expenseSub.getSeq());
                if (attachments != null && !attachments.isEmpty()) {
                    expenseSubAttachments.put(expenseSub.getChildNo(), attachments);
                    hasAttachments = true;
                }
            }
            
            // 첨부파일이 있으면 ZIP으로 묶기
            if (hasAttachments) {
                byte[] zipBytes = zipFileUtil.createZipWithDocumentAndExpenseAttachments(
                        excelBytes, documentFileName, expenseSubAttachments);
                
                String zipFileName = documentFileName.replace(".xlsx", ".zip");
                String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/zip"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
                headers.setContentLength(zipBytes.length);
                
                int totalAttachments = expenseSubAttachments.values().stream().mapToInt(List::size).sum();
                log.info("개인 비용 청구서 ZIP 생성 완료. 크기: {} bytes, 첨부파일: {}개", zipBytes.length, totalAttachments);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(zipBytes);
            } else {
                // 첨부파일이 없으면 기존처럼 문서만 반환
                String encodedFileName = URLEncoder.encode(documentFileName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
                headers.setContentLength(excelBytes.length);
                
                log.info("개인 비용 청구서 Excel 생성 완료. 크기: {} bytes", excelBytes.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(excelBytes);
            }
        } catch (Exception e) {
            log.error("개인 비용 청구서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 개인비용 항목별 첨부파일 업로드
     *
     * @param request HTTP 요청
     * @param seq 개인비용 신청 시퀀스
     * @param expenseSubSeq 개인비용 항목 시퀀스
     * @param file 업로드할 파일
     * @return 업로드된 첨부파일 정보
     */
    @PostMapping(value = "/{seq}/item/{expenseSubSeq}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> uploadExpenseItemFile(
            HttpServletRequest request,
            @PathVariable Long seq,
            @PathVariable Long expenseSubSeq,
            @RequestPart("file") MultipartFile file) {
        log.info("개인비용 항목별 첨부파일 업로드 요청: seq={}, expenseSubSeq={}", seq, expenseSubSeq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 개인비용 신청 조회 및 권한 체크
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaim(seq, userId);
            if (expenseClaim == null) {
                log.warn("존재하지 않는 개인비용 신청 또는 권한 없음: seq={}, userId={}", seq, userId);
                return errorResponse("404", "개인비용 신청을 찾을 수 없습니다.");
            }
            
            // 항목이 해당 신청에 속하는지 확인
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            boolean isValidSub = expenseSubList.stream()
                    .anyMatch(sub -> sub.getSeq().equals(expenseSubSeq));
            
            if (!isValidSub) {
                log.warn("유효하지 않은 개인비용 항목: seq={}, expenseSubSeq={}", seq, expenseSubSeq);
                return errorResponse("400", "유효하지 않은 개인비용 항목입니다.");
            }
            
            // 파일 업로드
            Attachment attachment = fileService.uploadExpenseItemFile(file, ApplicationType.EXPENSE.getCode(), seq, expenseSubSeq, userId);
            return createdResponse(attachment);
        } catch (ApiException e) {
            return errorResponse("첨부파일 업로드에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("첨부파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * 개인비용 항목별 첨부파일 다운로드
     *
     * @param request HTTP 요청
     * @param seq 개인비용 신청 시퀀스
     * @param expenseSubSeq 개인비용 항목 시퀀스
     * @return 첨부파일
     */
    @GetMapping("/{seq}/item/{expenseSubSeq}/file")
    public ResponseEntity<Resource> downloadExpenseItemFile(
            HttpServletRequest request,
            @PathVariable Long seq,
            @PathVariable Long expenseSubSeq) {
        log.info("개인비용 항목별 첨부파일 다운로드 요청: seq={}, expenseSubSeq={}", seq, expenseSubSeq);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            
            // 개인비용 신청 조회 및 권한 체크
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaim(seq, requesterId);
            if (expenseClaim == null) {
                log.warn("존재하지 않는 개인비용 신청 또는 권한 없음: seq={}, requesterId={}", seq, requesterId);
                return ResponseEntity.notFound().build();
            }
            
            // 항목이 해당 신청에 속하는지 확인
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            boolean isValidSub = expenseSubList.stream()
                    .anyMatch(sub -> sub.getSeq().equals(expenseSubSeq));
            
            if (!isValidSub) {
                log.warn("유효하지 않은 개인비용 항목: seq={}, expenseSubSeq={}", seq, expenseSubSeq);
                return ResponseEntity.notFound().build();
            }
            
            // 첨부파일 조회
            List<Attachment> attachments = fileService.getExpenseItemAttachments(expenseSubSeq);
            if (attachments == null || attachments.isEmpty()) {
                log.warn("첨부파일이 없음: expenseSubSeq={}", expenseSubSeq);
                return ResponseEntity.notFound().build();
            }
            
            Attachment attachment = attachments.get(0);
            
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
            log.error("개인비용 항목별 첨부파일 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 개인비용 항목별 첨부파일 삭제
     *
     * @param request HTTP 요청
     * @param seq 개인비용 신청 시퀀스
     * @param expenseSubSeq 개인비용 항목 시퀀스
     * @return 삭제 결과
     */
    @DeleteMapping("/{seq}/item/{expenseSubSeq}/file")
    public ResponseEntity<ApiResponse<Object>> deleteExpenseItemFile(
            HttpServletRequest request,
            @PathVariable Long seq,
            @PathVariable Long expenseSubSeq) {
        log.info("개인비용 항목별 첨부파일 삭제 요청: seq={}, expenseSubSeq={}", seq, expenseSubSeq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // 개인비용 신청 조회 및 권한 체크 (본인만 삭제 가능)
            ExpenseClaim expenseClaim = expenseClaimService.getExpenseClaim(seq, userId);
            if (expenseClaim == null || !expenseClaim.getUserId().equals(userId)) {
                log.warn("존재하지 않는 개인비용 신청 또는 권한 없음: seq={}, userId={}", seq, userId);
                return errorResponse("403", "첨부파일 삭제 권한이 없습니다.");
            }
            
            // 항목이 해당 신청에 속하는지 확인
            List<ExpenseSub> expenseSubList = expenseClaimService.getExpenseSubList(seq);
            boolean isValidSub = expenseSubList.stream()
                    .anyMatch(sub -> sub.getSeq().equals(expenseSubSeq));
            
            if (!isValidSub) {
                log.warn("유효하지 않은 개인비용 항목: seq={}, expenseSubSeq={}", seq, expenseSubSeq);
                return errorResponse("400", "유효하지 않은 개인비용 항목입니다.");
            }
            
            // 첨부파일 삭제
            fileService.deleteExpenseItemAttachments(expenseSubSeq);
            
            return successResponse("첨부파일이 삭제되었습니다.");
        } catch (ApiException e) {
            return errorResponse("첨부파일 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("첨부파일 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 개인 비용 청구서용 서명 이미지 맵 생성
     *
     * @param applicant 신청자 정보
     * @param approvalStatus 승인 상태 (A, AM, B, RB, C, RC)
     * @param requestDate 신청일
     * @return 서명 이미지 맵
     */
    private Map<String, byte[]> createSignatureImageMapForExpense(
            ExpenseClaim expenseClaim, User applicant, String approvalStatus, LocalDate requestDate) {
        try {
            // 신청일을 "yyyy.MM.dd" 형식으로 변환
            String requestDateStr = CommonUtil.formatDateShort(requestDate);
            
            // 신청자 권한
            AuthVal applicantAuthVal = AuthVal.fromCode(applicant.getAuthVal());
            
            // 상태값에 따라 실제 승인자 ID 사용
            Long teamLeaderUserId = null;
            Long divisionHeadUserId = null;
            
            if (ApprovalStatus.TEAM_LEADER_APPROVED.getName().equals(approvalStatus)) {
                // B 상태: tj_approval_id 사용
                teamLeaderUserId = expenseClaim.getTjApprovalId();
            } else if (ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(approvalStatus) ||
                       ApprovalStatus.DONE.getName().equals(approvalStatus)) {
                // C 또는 D 상태: bb_approval_id 사용
                divisionHeadUserId = expenseClaim.getBbApprovalId();
                // B 상태도 지났으므로 tj_approval_id도 사용
                teamLeaderUserId = expenseClaim.getTjApprovalId();
            }
            
            // 승인자가 없으면 기본 팀장/본부장 조회 (fallback)
            if (teamLeaderUserId == null) {
                List<User> teamLeaders = userRepository.findByDivisionAndTeamAndAuthVal(
                        applicant.getDivision(), applicant.getTeam(), AuthVal.TEAM_LEADER.getCode());
                teamLeaderUserId = teamLeaders.isEmpty() ? null : teamLeaders.get(0).getUserId();
            }
            
            if (divisionHeadUserId == null && 
                (ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(approvalStatus) ||
                 ApprovalStatus.DONE.getName().equals(approvalStatus))) {
                List<User> divisionHeads = userRepository.findByDivisionAndAuthVal(
                        applicant.getDivision(), AuthVal.DIVISION_HEAD.getCode());
                divisionHeadUserId = divisionHeads.isEmpty() ? null : divisionHeads.get(0).getUserId();
            }
            
            // 서명 이미지 맵 생성 (승인 상태 포함)
            return fileGenerateUtil.createSignatureImageMap(
                    applicant.getUserId(),
                    teamLeaderUserId,
                    divisionHeadUserId,
                    applicantAuthVal,
                    approvalStatus,
                    requestDateStr
            );
        } catch (Exception e) {
            log.error("서명 이미지 맵 생성 실패: applicantId={}, approvalStatus={}", 
                    applicant.getUserId(), approvalStatus, e);
            // 실패 시 빈 맵 반환 (서명 없이 문서 생성)
            return new java.util.HashMap<>();
        }
    }
}

