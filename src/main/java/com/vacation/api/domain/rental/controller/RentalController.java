package com.vacation.api.domain.rental.controller;

import com.vacation.api.common.BaseController;
import com.vacation.api.common.PagedResponse;
import com.vacation.api.domain.attachment.entity.Attachment;
import com.vacation.api.domain.attachment.service.FileService;
import com.vacation.api.domain.rental.entity.RentalProposal;
import com.vacation.api.domain.rental.entity.RentalSupport;
import com.vacation.api.domain.rental.request.RentalProposalRequest;
import com.vacation.api.domain.rental.request.RentalSupportRequest;
import com.vacation.api.domain.rental.response.RentalProposalResponse;
import com.vacation.api.domain.rental.response.RentalSupportResponse;
import com.vacation.api.domain.rental.service.RentalService;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.service.UserService;
import com.vacation.api.enums.ApplicationType;
import com.vacation.api.response.data.ApiResponse;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.util.FileGenerateUtil;
import com.vacation.api.util.ResponseMapper;
import com.vacation.api.util.ZipFileUtil;
import com.vacation.api.util.SignatureFileUtil;
import com.vacation.api.util.SignatureImageUtil;
import com.vacation.api.util.CommonUtil;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.enums.ApprovalStatus;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.repository.UserSignatureRepository;
import com.vacation.api.exception.ApiException;
import com.vacation.api.vo.RentalSupportApplicationVO;
import com.vacation.api.vo.RentalSupportProposalVO;
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

    private final RentalService rentalService;
    private final UserService userService;
    private final ResponseMapper responseMapper;
    private final FileService fileService;
    private final ZipFileUtil zipFileUtil;
    private final SignatureFileUtil signatureFileUtil;
    private final SignatureImageUtil signatureImageUtil;
    private final UserRepository userRepository;
    private final UserSignatureRepository userSignatureRepository;

    public RentalController(RentalService rentalService, UserService userService, 
                           ResponseMapper responseMapper, FileService fileService,
                           TransactionIDCreator transactionIDCreator,
                           ZipFileUtil zipFileUtil,
                           SignatureFileUtil signatureFileUtil,
                           SignatureImageUtil signatureImageUtil,
                           UserRepository userRepository,
                           UserSignatureRepository userSignatureRepository) {
        super(transactionIDCreator);
        this.rentalService = rentalService;
        this.userService = userService;
        this.responseMapper = responseMapper;
        this.fileService = fileService;
        this.zipFileUtil = zipFileUtil;
        this.signatureFileUtil = signatureFileUtil;
        this.signatureImageUtil = signatureImageUtil;
        this.userRepository = userRepository;
        this.userSignatureRepository = userSignatureRepository;
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
            List<RentalProposal> rentalProposalList = rentalService.getRentalSupportList(userId);
            
            // 각 항목에 applicant 추가하여 Response VO로 변환
            List<RentalProposalResponse> responseList = responseMapper.toRentalProposalResponseList(
                    rentalProposalList, 
                    userService::getUserInfo
            );
            
            return successResponse(responseList);
        } catch (ApiException e) {
            return errorResponse("월세 품의 정보 목록 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 품의 정보 목록 조회에 실패했습니다.", e);
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
            RentalProposal rentalProposal = rentalService.getRentalSupport(seq, userId);
            
            if (rentalProposal == null) {
                return successResponse(null);
            }
            
            // 첨부파일 정보 조회
            Attachment attachment = fileService.getAttachment(ApplicationType.RENTAL_PROPOSAL.getCode(), seq);
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(rentalProposal.getUserId());
            String applicantName = user != null ? user.getName() : null;
            
            // Response VO로 변환
            RentalProposalResponse response = responseMapper.toRentalProposalResponse(
                    rentalProposal, applicantName, attachment);
            
            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("월세 품의 정보 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 품의 정보 조회에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 정보 생성
     *
     * @param request HTTP 요청
     * @param rentalApprovalRequest 월세 지원 정보 요청 데이터
     * @param file 첨부파일 (선택)
     * @return 생성된 월세 지원 정보
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> createRentalSupport(
            HttpServletRequest request,
            @RequestPart("rentalProposalRequest") @Valid RentalProposalRequest rentalProposalRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("월세 품의 정보 생성 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalProposal rentalProposal = rentalService.createRentalSupport(userId, rentalProposalRequest);
            
            // 파일이 있으면 업로드
            if (file != null && !file.isEmpty()) {
                try {
                    fileService.uploadFile(file, ApplicationType.RENTAL_PROPOSAL.getCode(), rentalProposal.getSeq(), userId);
                } catch (Exception e) {
                    log.error("파일 업로드 실패: seq={}", rentalProposal.getSeq(), e);
                    // 파일 업로드 실패해도 신청은 성공으로 처리
                }
            }
            User applicant = userService.getUserInfo(userId);
            RentalProposalResponse resp = responseMapper.toRentalProposalResponse(
                    rentalProposal, applicant != null ? applicant.getName() : null, null);
            return createdResponse(resp);
        } catch (ApiException e) {
            return errorResponse("월세 품의 정보 생성에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 품의 정보 생성에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 정보 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param rentalApprovalRequest 월세 지원 정보 요청 데이터
     * @param file 첨부파일 (선택)
     * @return 수정된 월세 지원 정보
     */
    @PutMapping(value = "/{seq}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> updateRentalSupport(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestPart("rentalProposalRequest") @Valid RentalProposalRequest rentalProposalRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("월세 품의 정보 수정 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalProposal rentalProposal = rentalService.updateRentalSupport(seq, userId, rentalProposalRequest);
            
            // 파일이 있으면 업로드
            if (file != null && !file.isEmpty()) {
                try {
                    fileService.uploadFile(file, ApplicationType.RENTAL_PROPOSAL.getCode(), rentalProposal.getSeq(), userId);
                } catch (Exception e) {
                    log.error("파일 업로드 실패: seq={}", rentalProposal.getSeq(), e);
                    // 파일 업로드 실패해도 수정은 성공으로 처리
                }
            }
            
            return successResponse(rentalProposal);
        } catch (ApiException e) {
            return errorResponse("월세 품의 정보 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 품의 정보 수정에 실패했습니다.", e);
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            rentalService.deleteRentalSupport(seq, userId);
            return successResponse("삭제되었습니다.");
        } catch (ApiException e) {
            return errorResponse("월세 지원 정보 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 정보 삭제에 실패했습니다.", e);
        }
    }

    // ========== 월세 지원 신청 (청구서) 관련 엔드포인트 ==========

    /**
     * 월세 지원 신청 목록 조회 (페이징)
     *
     * @param request HTTP 요청
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 5)
     * @return 월세 지원 신청 목록
     */
    @GetMapping("/application")
    public ResponseEntity<ApiResponse<Object>> getRentalSupportApplicationList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        log.info("월세 지원 신청 목록 조회 요청: page={}, size={}", page, size);

        try {
            Long userId = (Long) request.getAttribute("userId");
            
            // totalCount 조회 (COUNT 쿼리)
            long totalCount = rentalService.getRentalSupportApplicationCount(userId);
            
            // 페이징된 목록 조회
            List<RentalSupport> rentalSupportList = rentalService.getRentalSupportApplicationList(userId, page, size);
            
            // 각 항목에 applicant 추가하여 Response VO로 변환
            List<RentalSupportResponse> responseList = responseMapper.toRentalSupportResponseList(
                    rentalSupportList, 
                    userService::getUserInfo
            );
            
            // totalCount 포함 응답 생성
            PagedResponse<RentalSupportResponse> responseData = PagedResponse.<RentalSupportResponse>builder()
                    .list(responseList)
                    .totalCount(totalCount)
                    .build();
            
            return successResponse(responseData);
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalSupport rentalSupport = rentalService.getRentalSupportApplication(seq, userId);
            
            if (rentalSupport == null) {
                return successResponse(null);
            }
            
            // 첨부파일 정보 조회
            List<Attachment> attachments = fileService.getAttachments(ApplicationType.RENTAL.getCode(), seq);
            log.info("월세 지원 신청 첨부파일 조회: seq={}, applicationType=RENTAL, attachmentCount={}", seq, attachments != null ? attachments.size() : 0);
            Attachment attachment = null;
            if (attachments != null && !attachments.isEmpty()) {
                attachment = attachments.get(0);
                log.info("월세 지원 신청 첨부파일 정보: attachmentSeq={}, fileName={}, fileSize={}", 
                        attachment.getSeq(), attachment.getFileName(), attachment.getFileSize());
            } else {
                log.warn("월세 지원 신청 첨부파일 없음: seq={}, applicationType=RENTAL", seq);
            }
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(rentalSupport.getUserId());
            String applicantName = user != null ? user.getName() : null;
            
            // Response VO로 변환
            RentalSupportResponse response = responseMapper.toRentalSupportResponse(
                    rentalSupport, applicantName, attachment);
            
            return successResponse(response);
        } catch (ApiException e) {
            return errorResponse("월세 지원 신청 조회에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 신청 조회에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 신청 생성
     *
     * @param request HTTP 요청
     * @param rentalSupportRequest 월세 지원 신청 요청 데이터
     * @return 생성된 월세 지원 신청 정보
     */
    @PostMapping(value = "/application", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> createRentalSupportApplication(
            HttpServletRequest request,
            @RequestPart("rentalSupportRequest") @Valid RentalSupportRequest rentalSupportRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("월세 지원 신청 생성 요청");

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalSupport rentalSupport = rentalService.createRentalSupportApplication(userId, rentalSupportRequest);
            
            // 파일이 있으면 업로드
            if (file != null && !file.isEmpty()) {
                try {
                    log.info("월세 지원 신청 파일 업로드 시작: seq={}, fileName={}, fileSize={}", 
                            rentalSupport.getSeq(), file.getOriginalFilename(), file.getSize());
                    Attachment attachment = fileService.uploadFile(file, ApplicationType.RENTAL.getCode(), rentalSupport.getSeq(), userId);
                    log.info("월세 지원 신청 파일 업로드 완료: attachmentSeq={}, applicationType=RENTAL, applicationSeq={}", 
                            attachment.getSeq(), rentalSupport.getSeq());
                } catch (Exception e) {
                    log.error("파일 업로드 실패: seq={}, fileName={}", rentalSupport.getSeq(), file.getOriginalFilename(), e);
                    // 파일 업로드 실패해도 신청은 성공으로 처리
                }
            }
            User applicant = userService.getUserInfo(userId);
            RentalSupportResponse resp = responseMapper.toRentalSupportResponse(
                    rentalSupport, applicant != null ? applicant.getName() : null, null);
            return createdResponse(resp);
        } catch (ApiException e) {
            return errorResponse("월세 지원 신청 생성에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 신청 생성에 실패했습니다.", e);
        }
    }

    /**
     * 월세 지원 신청 수정
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @param rentalSupportRequest 월세 지원 신청 요청 데이터
     * @param file 첨부파일 (선택)
     * @return 수정된 월세 지원 신청 정보
     */
    @PutMapping(value = "/application/{seq}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> updateRentalSupportApplication(
            HttpServletRequest request,
            @PathVariable Long seq,
            @RequestPart("rentalSupportRequest") @Valid RentalSupportRequest rentalSupportRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("월세 지원 신청 수정 요청: seq={}", seq);

        try {
            Long userId = (Long) request.getAttribute("userId");
            RentalSupport rentalSupport = rentalService.updateRentalSupportApplication(seq, userId, rentalSupportRequest);
            
            // 파일이 있으면 업로드 (기존 파일은 자동 삭제됨)
            if (file != null && !file.isEmpty()) {
                try {
                    log.info("월세 지원 신청 파일 수정 업로드 시작: seq={}, fileName={}, fileSize={}", 
                            rentalSupport.getSeq(), file.getOriginalFilename(), file.getSize());
                    Attachment attachment = fileService.uploadFile(file, ApplicationType.RENTAL.getCode(), rentalSupport.getSeq(), userId);
                    log.info("월세 지원 신청 파일 수정 업로드 완료: attachmentSeq={}, applicationType=RENTAL, applicationSeq={}", 
                            attachment.getSeq(), rentalSupport.getSeq());
                } catch (Exception e) {
                    log.error("파일 업로드 실패: seq={}, fileName={}", rentalSupport.getSeq(), file.getOriginalFilename(), e);
                    // 파일 업로드 실패해도 수정은 성공으로 처리
                }
            }
            
            return successResponse(rentalSupport);
        } catch (ApiException e) {
            return errorResponse("월세 지원 신청 수정에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 신청 수정에 실패했습니다.", e);
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

        try {
            Long userId = (Long) request.getAttribute("userId");
            rentalService.deleteRentalSupportApplication(seq, userId);
            return successResponse("삭제되었습니다.");
        } catch (ApiException e) {
            return errorResponse("월세 지원 신청 삭제에 실패했습니다.", e);
        } catch (Exception e) {
            return errorResponse("월세 지원 신청 삭제에 실패했습니다.", e);
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
            Long requesterId = (Long) request.getAttribute("userId");
            User requester = userService.getUserInfo(requesterId);
            
            // 월세 지원 신청 조회 (seq만으로 조회)
            RentalSupport rentalSupport = rentalService.getRentalSupportApplicationById(seq);
            if (rentalSupport == null) {
                log.warn("존재하지 않는 월세 지원 신청: seq={}", seq);
                return ResponseEntity.notFound().build();
            }
            
            // 권한 체크: 본인 신청서이거나 결재 권한이 있는 경우만 다운로드 가능
            Long applicantId = rentalSupport.getUserId();
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
            
            // 신청자 정보 조회
            User applicant = userService.getUserInfo(applicantId);
            
            // VO 생성
            RentalSupportApplicationVO vo = rentalService.createRentalSupportApplicationVO(
                    rentalSupport, applicant);
            
            // 서명 이미지 맵 생성 (승인 상태 포함)
            String approvalStatus = rentalSupport.getApprovalStatus();
            if (approvalStatus == null) {
                approvalStatus = ApprovalStatus.INITIAL.getName();
            }
            Map<String, byte[]> signatureImageMap = createSignatureImageMapForRental(
                    rentalSupport, applicant, approvalStatus, vo.getRequestDate());
            
            // XLSX 생성 (서명 이미지 맵 전달)
            byte[] excelBytes = FileGenerateUtil.generateRentalSupportApplicationExcel(vo, signatureImageMap);
            
            // 파일명 생성 (오늘 날짜 사용)
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String documentFileName = "월세지원신청서_" + applicant.getName() + "_" + dateStr + ".xlsx";
            
            // 첨부파일 조회
            List<Attachment> attachments = 
                    fileService.getAttachments(ApplicationType.RENTAL.getCode(), seq);
            
            // 첨부파일이 있으면 ZIP으로 묶기
            if (attachments != null && !attachments.isEmpty()) {
                byte[] zipBytes = zipFileUtil.createZipWithDocumentAndAttachments(
                        excelBytes, documentFileName, attachments);
                
                String zipFileName = documentFileName.replace(".xlsx", ".zip");
                String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/zip"));
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);
                headers.setContentLength(zipBytes.length);
                
                log.info("월세 지원 신청서 ZIP 생성 완료. 크기: {} bytes, 첨부파일: {}개", zipBytes.length, attachments.size());
                
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
                
                log.info("월세 지원 신청서 Excel 생성 완료. 크기: {} bytes", excelBytes.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(excelBytes);
            }
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
            
            // 월세 품의 정보 조회
            RentalProposal rentalProposal = rentalService.getRentalSupport(seq, userId);
            if (rentalProposal == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 사용자 정보 조회
            User user = userService.getUserInfo(userId);
            
            // VO 생성
            RentalSupportProposalVO vo = rentalService.createRentalSupportProposalVO(
                    rentalProposal, user);
            
            // DOCX 생성 (서명은 null로 전달하여 빈 문자열로 처리)
            byte[] docBytes = FileGenerateUtil.generateRentalSupportProposalDoc(vo, null);
            
            // 파일명 생성
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String documentFileName = "월세지원품의서_" + user.getName() + "_" + dateStr + ".docx";
            
            // 첨부파일 조회
            List<Attachment> attachments = 
                    fileService.getAttachments(ApplicationType.RENTAL_PROPOSAL.getCode(), seq);
            
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
                
                log.info("월세 지원 품의서 ZIP 생성 완료. 크기: {} bytes, 첨부파일: {}개", zipBytes.length, attachments.size());
                
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
                
                log.info("월세 지원 품의서 DOCX 생성 완료. 크기: {} bytes", docBytes.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(docBytes);
            }
        } catch (Exception e) {
            log.error("월세 지원 품의서 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월세 지원 품의서 첨부파일 다운로드
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 첨부파일
     */
    @GetMapping("/{seq}/attachment")
    public ResponseEntity<Resource> downloadRentalApprovalAttachment(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 품의서 첨부파일 다운로드 요청: seq={}", seq);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            
            // 월세 품의서 조회 (권한 체크)
            RentalProposal rentalProposal = rentalService.getRentalSupport(seq, requesterId);
            if (rentalProposal == null) {
                log.warn("존재하지 않는 월세 품의서 또는 권한 없음: seq={}, requesterId={}", seq, requesterId);
                return ResponseEntity.notFound().build();
            }
            
            // 첨부파일 조회
            Attachment attachment = fileService.getAttachment(ApplicationType.RENTAL_PROPOSAL.getCode(), seq);
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
            log.error("월세 지원 품의서 첨부파일 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월세 지원 신청 첨부파일 다운로드
     *
     * @param request HTTP 요청
     * @param seq 시퀀스
     * @return 첨부파일
     */
    @GetMapping("/application/{seq}/attachment")
    public ResponseEntity<Resource> downloadRentalSupportAttachment(
            HttpServletRequest request,
            @PathVariable Long seq) {
        log.info("월세 지원 신청 첨부파일 다운로드 요청: seq={}", seq);

        try {
            Long requesterId = (Long) request.getAttribute("userId");
            
            // 월세 지원 신청 조회 (권한 체크)
            RentalSupport rentalSupport = rentalService.getRentalSupportApplication(seq, requesterId);
            if (rentalSupport == null) {
                log.warn("존재하지 않는 월세 지원 신청 또는 권한 없음: seq={}, requesterId={}", seq, requesterId);
                return ResponseEntity.notFound().build();
            }
            
            // 첨부파일 조회
            List<Attachment> attachments = fileService.getAttachments(ApplicationType.RENTAL.getCode(), seq);
            if (attachments == null || attachments.isEmpty()) {
                log.warn("첨부파일이 없음: seq={}", seq);
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
            log.error("월세 지원 신청 첨부파일 다운로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월세 지원 신청서용 서명 이미지 맵 생성
     *
     * @param applicant 신청자 정보
     * @param approvalStatus 승인 상태 (A, AM, B, RB, C, RC)
     * @param requestDate 신청일
     * @return 서명 이미지 맵
     */
    private Map<String, byte[]> createSignatureImageMapForRental(
            RentalSupport rentalSupport, User applicant, String approvalStatus, LocalDate requestDate) {
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
                teamLeaderUserId = rentalSupport.getTjApprovalId();
            } else if (ApprovalStatus.DIVISION_HEAD_APPROVED.getName().equals(approvalStatus) ||
                       ApprovalStatus.DONE.getName().equals(approvalStatus)) {
                // C 또는 D 상태: bb_approval_id 사용
                divisionHeadUserId = rentalSupport.getBbApprovalId();
                // B 상태도 지났으므로 tj_approval_id도 사용
                teamLeaderUserId = rentalSupport.getTjApprovalId();
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
            return FileGenerateUtil.createSignatureImageMap(
                    applicant.getUserId(),
                    teamLeaderUserId,
                    divisionHeadUserId,
                    applicantAuthVal,
                    approvalStatus,
                    requestDateStr,
                    signatureFileUtil,
                    signatureImageUtil,
                    userSignatureRepository
            );
        } catch (Exception e) {
            log.error("서명 이미지 맵 생성 실패: applicantId={}, approvalStatus={}", 
                    applicant.getUserId(), approvalStatus, e);
            // 실패 시 빈 맵 반환 (서명 없이 문서 생성)
            return new java.util.HashMap<>();
        }
    }
}

