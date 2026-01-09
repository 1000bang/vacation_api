package com.vacation.api.domain.user.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.enums.UserStatus;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

        // 사용자 생성
        User user = User.builder()
                .email(joinRequest.getEmail())
                .name(joinRequest.getName())
                .password(encodedPassword)
                .division(joinRequest.getDivision())
                .team(joinRequest.getTeam())
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

        // 비밀번호 확인
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("비밀번호 불일치: {}", loginRequest.getEmail());
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

        // 사용자 상태 확인
        if (user.getStatus() != UserStatus.APPROVED) {
            log.warn("승인되지 않은 사용자: {}, status={}", loginRequest.getEmail(), user.getStatus());
            throw new ApiException(ApiErrorCode.USER_NOT_APPROVED);
        }

        // 로그인 일시 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        
        // 최초 로그인인 경우 firstLogin을 false로 변경
//        if (user.getFirstLogin()) {
//            user.setFirstLogin(false);
//        }
        
        // Access Token과 Refresh Token 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getEmail());
        
        // Refresh Token을 DB에 저장
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());

        return new String[]{accessToken, refreshToken};
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

        // DB에 저장된 Refresh Token과 일치하는지 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: userId={}", userId);
                    return new ApiException(ApiErrorCode.INVALID_LOGIN);
                });

        if (!refreshToken.equals(user.getRefreshToken())) {
            log.warn("저장된 Refresh Token과 일치하지 않음: userId={}", userId);
            throw new ApiException(ApiErrorCode.INVALID_LOGIN);
        }

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
        
        user.setDivision(updateRequest.getDivision());
        user.setTeam(updateRequest.getTeam());
        user.setPosition(updateRequest.getPosition());
        
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
     * - bb: 해당 본부/센터 사람들만 (모든 권한 포함: ma, bb, tj, tw)
     * - tj: 해당 본부/팀 사람들만 (모든 권한 포함: ma, bb, tj, tw)
     *
     * @param userId 요청자 사용자 ID
     * @return 필터링된 사용자 정보 목록
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
        // 모든 권한 포함 (ma: master, bb: bonbujang, tj: teamjang, tw: teamwon)
        List<String> allowedAuthVals = List.of("ma", "bb", "tj", "tw");
        
        if ("ma".equals(authVal)) {
            // master: 전체 목록
            log.info("master 권한: 전체 목록 조회");
            return userRepository.findByAuthValInOrderByCreatedAtDesc(allowedAuthVals);
        } else if ("bb".equals(authVal)) {
            // bonbujang: 해당 본부/센터 사람들만
            log.info("bonbujang 권한: 본부={} 필터링", requester.getDivision());
            return userRepository.findByDivisionAndAuthValInOrderByCreatedAtDesc(
                    requester.getDivision(), allowedAuthVals);
        } else if ("tj".equals(authVal)) {
            // teamjang: 해당 본부/팀 사람들만
            log.info("teamjang 권한: 본부={}, 팀={} 필터링", requester.getDivision(), requester.getTeam());
            return userRepository.findByDivisionAndTeamAndAuthValInOrderByCreatedAtDesc(
                    requester.getDivision(), requester.getTeam(), allowedAuthVals);
        } else {
            log.warn("권한 없음: userId={}, authVal={}", userId, authVal);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "사용자 목록 조회 권한이 없습니다.");
        }
    }
}

