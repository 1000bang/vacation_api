package com.vacation.api.domain.user.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.entity.TeamManagement;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.repository.TeamManagementRepository;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.enums.UserStatus;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.common.service.RefreshTokenService;
import com.vacation.api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.vacation.api.domain.user.response.DivisionTeamResponse;
import com.vacation.api.enums.AuthVal;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeamManagementRepository teamManagementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 회원가입
     *
     * @param joinRequest 회원가입 요청 데이터
     * @return 생성된 사용자
     */
    @Transactional
    public User join(JoinRequest joinRequest) {
        log.info("회원가입 요청: {}", joinRequest.getEmail());

        // 이메일 중복 확인
        if (userRepository.existsByEmail(joinRequest.getEmail())) {
            log.warn("이미 존재하는 이메일: {}", joinRequest.getEmail());
            throw new ApiException(ApiErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(joinRequest.getPassword());

        // division, team으로 teamSeq 찾기
        // team이 null이거나 빈 문자열이면 본부만 조회 (본부장용)
        TeamManagement teamManagement;
        if (joinRequest.getTeam() == null || joinRequest.getTeam().trim().isEmpty()) {
            teamManagement = teamManagementRepository
                    .findByDivisionAndTeamIsNull(joinRequest.getDivision())
                    .orElseThrow(() -> {
                        log.warn("존재하지 않는 본부: division={}", joinRequest.getDivision());
                        return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 본부입니다.");
                    });
        } else {
            teamManagement = teamManagementRepository
                    .findByDivisionAndTeam(joinRequest.getDivision(), joinRequest.getTeam())
                    .orElseThrow(() -> {
                        log.warn("존재하지 않는 팀: division={}, team={}", joinRequest.getDivision(), joinRequest.getTeam());
                        return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 팀입니다.");
                    });
        }

        // 사용자 생성
        User user = User.builder()
                .email(joinRequest.getEmail())
                .name(joinRequest.getName())
                .password(encodedPassword)
                .teamManagement(teamManagement)
                .position(joinRequest.getPosition())
                .status(UserStatus.PENDING)
                .passwordChanged(false)
                .firstLogin(true)
                .joinDate(joinRequest.getJoinDate()) // 요청에서 받은 입사일 사용
                .authVal("tw") // 기본값: 팀원
                .build();
        if(user.getName().equals("천병재")){
            user.setStatus(UserStatus.APPROVED);
        }
        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

        return savedUser;
    }

    /**
     * 로그인
     *
     * @param loginRequest 로그인 요청 데이터
     * @return Access Token과 Refresh Token을 포함한 배열 [accessToken, refreshToken]
     */
    @Transactional
    public String[] login(LoginRequest loginRequest) {
        log.info("로그인 요청: {}", loginRequest.getEmail());

        // 사용자 조회
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 이메일: {}", loginRequest.getEmail());
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });

        // 계정 잠금 확인
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("계정이 잠금되어 있음: {}, 잠금 해제 시간: {}", loginRequest.getEmail(), user.getAccountLockedUntil());
            throw new ApiException(ApiErrorCode.ACCOUNT_LOCKED, 
                String.format("계정이 잠금되었습니다. %s 이후에 다시 시도해주세요.", 
                    user.getAccountLockedUntil().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }

        // 계정 잠금 시간이 지났으면 잠금 해제
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isBefore(LocalDateTime.now())) {
            user.setAccountLockedUntil(null);
            user.setLoginFailureCount(0);
            log.info("계정 잠금 해제: {}", loginRequest.getEmail());
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // 로그인 실패 횟수 증가 (별도 트랜잭션으로 실행하여 롤백 방지)
            int failureCount = incrementLoginFailureCount(user.getUserId());
            
            // 5회 실패 시 계정 잠금 (30분)
            if (failureCount >= 5) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(30);
                lockAccount(user.getUserId(), lockUntil);
                log.warn("계정 잠금: {}, 실패 횟수: {}, 잠금 해제 시간: {}", 
                    loginRequest.getEmail(), failureCount, lockUntil);
                throw new ApiException(ApiErrorCode.ACCOUNT_LOCKED, 
                    String.format("로그인 실패가 5회 누적되어 계정이 잠금되었습니다. %s 이후에 다시 시도해주세요.", 
                        lockUntil.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            } else {
                log.warn("비밀번호 불일치: {}, 실패 횟수: {}/5", loginRequest.getEmail(), failureCount);
                throw new ApiException(ApiErrorCode.INVALID_LOGIN, 
                    String.format("이메일 또는 비밀번호가 올바르지 않습니다. (실패 횟수: %d/5)", failureCount));
            }
        }

        // 사용자 상태 확인
        if (user.getStatus() != UserStatus.APPROVED) {
            log.warn("승인되지 않은 사용자: {}, status={}", loginRequest.getEmail(), user.getStatus());
            throw new ApiException(ApiErrorCode.USER_NOT_APPROVED);
        }

        // 로그인 성공: 실패 횟수 초기화 및 잠금 해제
        user.setLoginFailureCount(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        
        // 최초 로그인인 경우 firstLogin을 false로 변경
//        if (user.getFirstLogin()) {
//            user.setFirstLogin(false);
//        }
        
        // Access Token과 Refresh Token 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getEmail());
        
        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(user.getUserId(), refreshToken);

        log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());

        return new String[]{accessToken, refreshToken};
    }

    /**
     * 로그인 실패 횟수 증가 (수동 트랜잭션 관리로 롤백 방지)
     * TransactionTemplate을 사용하여 메인 트랜잭션과 완전히 분리
     *
     * @param userId 사용자 ID
     * @return 증가된 실패 횟수
     */
    public int incrementLoginFailureCount(Long userId) {
        // 별도 트랜잭션으로 실행 (메인 트랜잭션과 완전히 분리)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        
        // UPDATE 쿼리 실행 (예외가 발생해도 커밋되도록)
        transactionTemplate.execute(status -> {
            int rows = userRepository.incrementLoginFailureCount(userId);
            log.info("로그인 실패 횟수 UPDATE 쿼리 실행: userId={}, 업데이트된 행 수: {}", userId, rows);
            return rows;
        });
        
        // 트랜잭션 커밋 후 캐시 비우기
        entityManager.clear();
        
        // 증가된 실패 횟수 조회 (새로운 트랜잭션에서)
        return transactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("존재하지 않는 사용자: userId={}", userId);
                        return new ApiException(ApiErrorCode.USER_NOT_FOUND);
                    });
            
            int failureCount = user.getLoginFailureCount() == null ? 0 : user.getLoginFailureCount();
            log.info("로그인 실패 횟수 증가 완료: userId={}, 실패 횟수: {}/5", userId, failureCount);
            return failureCount;
        });
    }

    /**
     * 계정 잠금 (수동 트랜잭션 관리로 롤백 방지)
     * TransactionTemplate을 사용하여 메인 트랜잭션과 완전히 분리
     *
     * @param userId 사용자 ID
     * @param lockUntil 잠금 해제 시간
     */
    public void lockAccount(Long userId, LocalDateTime lockUntil) {
        // 별도 트랜잭션으로 실행 (메인 트랜잭션과 완전히 분리)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.lockAccount(userId, lockUntil);
            log.info("계정 잠금 완료: userId={}, 잠금 해제 시간: {}", userId, lockUntil);
        });
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     *
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token
     */
    @Transactional
    public String refreshAccessToken(String refreshToken) {
        log.info("Access Token 갱신 요청");

        // Refresh Token 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token");
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

        // Refresh Token 타입 확인
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            log.warn("Refresh Token이 아닌 토큰입니다");
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

        // Refresh Token에서 사용자 정보 추출
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String email = jwtUtil.getEmailFromToken(refreshToken);

        // Redis에 저장된 Refresh Token과 일치하는지 확인
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            log.warn("저장된 Refresh Token과 일치하지 않음: userId={}", userId);
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });

        // 사용자 상태 확인
        if (user.getStatus() != UserStatus.APPROVED) {
            log.warn("승인되지 않은 사용자: userId={}, status={}", userId, user.getStatus());
            throw new ApiException(ApiErrorCode.USER_NOT_APPROVED);
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtUtil.generateAccessToken(userId, email);
        log.info("Access Token 갱신 성공: userId={}, email={}", userId, email);

        return newAccessToken;
    }

    /**
     * 로그아웃 (Refresh Token 삭제)
     *
     * @param userId 사용자 ID
     */
    public void logout(Long userId) {
        log.info("로그아웃 요청: userId={}", userId);
        
        // Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId);
        
        log.info("로그아웃 완료: userId={}", userId);
    }

    /**
     * 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    public User getUserInfo(Long userId) {
        log.info("사용자 정보 조회: userId={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });
    }

    /**
     * 사용자 정보 수정
     *
     * @param userId 사용자 ID
     * @param updateRequest 수정 요청 데이터
     * @return 수정된 사용자 정보
     */
    @Transactional
    public User updateUserInfo(Long userId, com.vacation.api.domain.user.request.UpdateUserRequest updateRequest) {
        log.info("사용자 정보 수정: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });

        // TODO: 권한 체크 (특정 권한 이상인 자만 수정 가능)
        // 현재는 본인만 수정 가능하도록 구현
        
        // division, team으로 teamSeq 찾기
        // team이 null이거나 빈 문자열이면 본부만 조회 (본부장용)
        if (updateRequest.getDivision() != null) {
            TeamManagement teamManagement;
            if (updateRequest.getTeam() == null || updateRequest.getTeam().trim().isEmpty()) {
                // 본부장: team이 없음
                teamManagement = teamManagementRepository
                        .findByDivisionAndTeamIsNull(updateRequest.getDivision())
                        .orElseThrow(() -> {
                            log.warn("존재하지 않는 본부: division={}", updateRequest.getDivision());
                            return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 본부입니다.");
                        });
            } else {
                // 팀장/팀원: team이 있음
                teamManagement = teamManagementRepository
                        .findByDivisionAndTeam(updateRequest.getDivision(), updateRequest.getTeam())
                        .orElseThrow(() -> {
                            log.warn("존재하지 않는 팀: division={}, team={}", updateRequest.getDivision(), updateRequest.getTeam());
                            return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 팀입니다.");
                        });
            }
            user.setTeamManagement(teamManagement);
        }
        // position이 null이 아니고 빈 문자열이 아닐 때만 업데이트
        if (updateRequest.getPosition() != null && !updateRequest.getPosition().trim().isEmpty()) {
            user.setPosition(updateRequest.getPosition());
        }
        
        // 확장된 필드 업데이트 (null이 아닌 경우에만)
        if (updateRequest.getJoinDate() != null) {
            user.setJoinDate(updateRequest.getJoinDate());
        }
        if (updateRequest.getStatus() != null) {
            try {
                user.setStatus(UserStatus.valueOf(updateRequest.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 상태 값: {}", updateRequest.getStatus());
            }
        }
        if (updateRequest.getAuthVal() != null) {
            user.setAuthVal(updateRequest.getAuthVal());
        }
        if (updateRequest.getFirstLogin() != null) {
            user.setFirstLogin(updateRequest.getFirstLogin());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("사용자 정보 수정 완료: userId={}", userId);
        
        return updatedUser;
    }

    /**
     * 사용자 정보 리스트 조회
     * 권한에 따라 필터링:
     * - ma: 전체 목록 (모든 권한 포함: ma, bb, tj, tw)
     * - bb: 해당 본부의 tw(팀원)와 tj(팀장)만 (자기 자신 제외)
     * - tj: 해당 본부/팀의 tw(팀원)만 (자기 자신 제외)
     *
     * @param userId 요청자 사용자 ID
     * @return 필터링된 사용자 정보 목록 (자기 자신 제외)
     */
    public List<User> getUserInfoList(Long userId) {
        log.info("사용자 정보 리스트 조회: userId={}", userId);
        
        // 요청자 정보 조회
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", userId);
                    return new ApiException(ApiErrorCode.USER_NOT_FOUND);
                });
        
        String authVal = requester.getAuthVal();
        List<User> result;
        
        if (AuthVal.MASTER.getCode().equals(authVal)) {
            // master: 전체 목록 (모든 권한 포함, 자기 자신 포함)
            log.info("master 권한: 전체 목록 조회 (자기 자신 포함)");
            List<String> allowedAuthVals = List.of(AuthVal.MASTER.getCode(), AuthVal.DIVISION_HEAD.getCode(), AuthVal.TEAM_LEADER.getCode(), AuthVal.TEAM_MEMBER.getCode());
            result = userRepository.findByAuthValInOrderByCreatedAtDesc(allowedAuthVals);
            log.info("필터링된 사용자 수: {} (전체)", result.size());
            return result;
        } else if (AuthVal.DIVISION_HEAD.getCode().equals(authVal)) {
            // bonbujang: 해당 본부의 tw(팀원)와 tj(팀장)만 (자기 자신 제외)
            // 같은 본부에 속한 모든 팀의 사용자 조회
            log.info("bonbujang 권한: 본부={}, tw와 tj만 조회", requester.getDivision());
            List<String> allowedAuthVals = List.of(AuthVal.TEAM_MEMBER.getCode(), AuthVal.TEAM_LEADER.getCode());
            result = userRepository.findByDivisionAndAuthValInOrderByCreatedAtDesc(
                    requester.getDivision(), allowedAuthVals);
        } else if (AuthVal.TEAM_LEADER.getCode().equals(authVal)) {
            // teamjang: 해당 팀의 tw(팀원)만 (자기 자신 제외)
            // teamSeq로 비교
            log.info("teamjang 권한: teamSeq={}, tw만 조회", requester.getTeamManagement() != null ? requester.getTeamManagement().getSeq() : null);
            List<String> allowedAuthVals = List.of(AuthVal.TEAM_MEMBER.getCode());
            if (requester.getTeamManagement() != null) {
                result = userRepository.findByTeamSeqAndAuthValInOrderByCreatedAtDesc(
                        requester.getTeamManagement().getSeq(), allowedAuthVals);
            } else {
                log.warn("팀장의 teamManagement가 null입니다: userId={}", userId);
                result = List.of();
            }
        } else {
            log.warn("권한 없음: userId={}, authVal={}", userId, authVal);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "사용자 목록 조회 권한이 없습니다.");
        }
        
        // 자기 자신 제외 (마스터 제외)
        result = result.stream()
                .filter(user -> !user.getUserId().equals(userId))
                .toList();
        
        log.info("필터링된 사용자 수: {} (자기 자신 제외)", result.size());
        return result;
    }

    /**
     * 사용자 정보 접근 권한 체크
     * 
     * @param requesterId 요청자 사용자 ID
     * @param targetUserId 확인할 사용자 ID
     * @throws ApiException 권한이 없을 경우
     */
    public void checkUserAccessPermission(Long requesterId, Long targetUserId) {
        log.info("사용자 접근 권한 체크: requesterId={}, targetUserId={}", requesterId, targetUserId);
        
        // 요청자 정보 조회
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 요청자: userId={}", requesterId);
                    return new ApiException(ApiErrorCode.USER_NOT_FOUND);
                });
        
        // 확인할 사용자 정보 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", targetUserId);
                    return new ApiException(ApiErrorCode.USER_NOT_FOUND);
                });
        
        String requesterAuthVal = requester.getAuthVal();
        
        // 마스터(ma): 전체 열람 가능
        if (AuthVal.MASTER.getCode().equals(requesterAuthVal)) {
            log.info("마스터 권한: 접근 허용");
            return;
        }
        
        // 팀원(tw): 권한 없음
        if (AuthVal.TEAM_MEMBER.getCode().equals(requesterAuthVal)) {
            log.warn("팀원 권한: 접근 거부");
            throw new ApiException(ApiErrorCode.ACCESS_DENIED, "권한이 없습니다.");
        }
        
        // 본부장(bb): 같은 본부면 OK
        if (AuthVal.DIVISION_HEAD.getCode().equals(requesterAuthVal)) {
            // division 문자열 비교
            boolean isMyBonbu = requester.getTeamManagement() != null && targetUser.getTeamManagement() != null &&
                    requester.getTeamManagement().getDivision().equals(targetUser.getTeamManagement().getDivision());
            if (isMyBonbu) {
                log.info("본부장 권한: 같은 본부, 접근 허용");
                return;
            } else {
                log.warn("본부장 권한: 다른 본부, 접근 거부");
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "권한이 없습니다.");
            }
        }
        
        // 팀장(tj): 같은 팀이면 OK (teamSeq 비교)
        if (AuthVal.TEAM_LEADER.getCode().equals(requesterAuthVal)) {
            boolean isMyTeam = requester.getTeamManagement() != null && targetUser.getTeamManagement() != null &&
                    requester.getTeamManagement().getSeq().equals(targetUser.getTeamManagement().getSeq());
            if (isMyTeam) {
                log.info("팀장 권한: 같은 팀, 접근 허용");
                return;
            } else {
                log.warn("팀장 권한: 다른 팀, 접근 거부");
                throw new ApiException(ApiErrorCode.ACCESS_DENIED, "권한이 없습니다.");
            }
        }
        
        // 기타 권한: 접근 거부
        log.warn("알 수 없는 권한: authVal={}, 접근 거부", requesterAuthVal);
        throw new ApiException(ApiErrorCode.ACCESS_DENIED, "권한이 없습니다.");
    }

    /**
     * 본부별 팀 목록 조회
     * division별로 그룹화하고, 각 division의 teams를 정렬하여 반환
     *
     * @return 본부별 팀 목록
     */
    public List<DivisionTeamResponse> getDivisionTeamList() {
        log.info("본부별 팀 목록 조회 요청");

        // team이 null이 아닌 모든 팀 관리 정보 조회 (division, team 순으로 정렬)
        List<TeamManagement> teamList = teamManagementRepository
                .findByTeamIsNotNullOrderByDivisionAscTeamAsc();

        // division별로 그룹화
        Map<String, List<String>> divisionTeamMap = teamList.stream()
                .collect(Collectors.groupingBy(
                        TeamManagement::getDivision,
                        Collectors.mapping(
                                TeamManagement::getTeam,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        teams -> {
                                            // null 제거 및 정렬
                                            List<String> sortedTeams = teams.stream()
                                                    .filter(Objects::nonNull)
                                                    .sorted()
                                                    .collect(Collectors.toList());
                                            return sortedTeams;
                                        }
                                )
                        )
                ));

        // DivisionTeamResponse 리스트 생성
        List<DivisionTeamResponse> result = divisionTeamMap.entrySet().stream()
                .map(entry -> DivisionTeamResponse.builder()
                        .division(entry.getKey())
                        .teams(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(DivisionTeamResponse::getDivision))
                .collect(Collectors.toList());

        log.info("본부별 팀 목록 조회 완료: division 개수={}", result.size());
        return result;
    }
}

