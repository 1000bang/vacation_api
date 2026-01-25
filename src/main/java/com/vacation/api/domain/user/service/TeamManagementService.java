package com.vacation.api.domain.user.service;

import com.vacation.api.domain.user.entity.TeamManagement;
import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.TeamManagementRepository;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.request.TeamManagementRequest;
import com.vacation.api.domain.user.response.TeamManagementResponse;
import com.vacation.api.domain.user.response.TeamUserResponse;
import com.vacation.api.enums.AuthVal;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 팀 관리 Service
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamManagementService {

    private final TeamManagementRepository teamManagementRepository;
    private final UserRepository userRepository;

    /**
     * 전체 팀 관리 목록 조회
     *
     * @return 팀 관리 목록
     */
    @Transactional(readOnly = true)
    public List<TeamManagementResponse> getAllTeamManagement() {
        log.info("전체 팀 관리 목록 조회");
        List<TeamManagement> allTeams = teamManagementRepository.findAll();
        
        return allTeams.stream()
                .map(team -> {
                    Long userCount = userRepository.countByTeamManagementSeq(team.getSeq());
                    return TeamManagementResponse.builder()
                            .seq(team.getSeq())
                            .division(team.getDivision())
                            .team(team.getTeam())
                            .createdAt(team.getCreatedAt())
                            .userCount(userCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 팀 관리 조회 (본부별)
     *
     * @param division 본부
     * @return 팀 관리 목록
     */
    @Transactional(readOnly = true)
    public List<TeamManagementResponse> getTeamManagementByDivision(String division) {
        log.info("본부별 팀 관리 목록 조회: division={}", division);
        List<TeamManagement> teams = teamManagementRepository.findByDivision(division);
        
        return teams.stream()
                .map(team -> {
                    Long userCount = userRepository.countByTeamManagementSeq(team.getSeq());
                    return TeamManagementResponse.builder()
                            .seq(team.getSeq())
                            .division(team.getDivision())
                            .team(team.getTeam())
                            .createdAt(team.getCreatedAt())
                            .userCount(userCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 팀 관리 생성 (본부 또는 팀 추가)
     *
     * @param request 팀 관리 요청
     * @return 생성된 팀 관리 정보
     */
    @Transactional
    public TeamManagementResponse createTeamManagement(TeamManagementRequest request) {
        log.info("팀 관리 생성: division={}, team={}", request.getDivision(), request.getTeam());

        // 중복 체크
        if (request.getTeam() == null || request.getTeam().trim().isEmpty()) {
            // 본부만 생성하는 경우
            if (teamManagementRepository.findByDivisionAndTeamIsNull(request.getDivision()).isPresent()) {
                log.warn("이미 존재하는 본부: division={}", request.getDivision());
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "이미 존재하는 본부입니다.");
            }
        } else {
            // 팀 생성하는 경우
            if (teamManagementRepository.findByDivisionAndTeam(request.getDivision(), request.getTeam()).isPresent()) {
                log.warn("이미 존재하는 팀: division={}, team={}", request.getDivision(), request.getTeam());
                throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "이미 존재하는 팀입니다.");
            }
        }

        TeamManagement teamManagement = TeamManagement.builder()
                .division(request.getDivision())
                .team(request.getTeam() != null && !request.getTeam().trim().isEmpty() ? request.getTeam() : null)
                .build();

        TeamManagement saved = teamManagementRepository.save(teamManagement);
        log.info("팀 관리 생성 완료: seq={}, division={}, team={}", saved.getSeq(), saved.getDivision(), saved.getTeam());

        Long userCount = userRepository.countByTeamManagementSeq(saved.getSeq());
        return TeamManagementResponse.builder()
                .seq(saved.getSeq())
                .division(saved.getDivision())
                .team(saved.getTeam())
                .createdAt(saved.getCreatedAt())
                .userCount(userCount)
                .build();
    }

    /**
     * 팀 관리 수정
     *
     * @param seq 팀 관리 시퀀스
     * @param request 팀 관리 요청
     * @return 수정된 팀 관리 정보
     */
    @Transactional
    public TeamManagementResponse updateTeamManagement(Long seq, TeamManagementRequest request) {
        log.info("팀 관리 수정: seq={}, division={}, team={}", seq, request.getDivision(), request.getTeam());

        TeamManagement teamManagement = teamManagementRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 팀 관리: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 팀 관리입니다.");
                });

        // 중복 체크 (자기 자신 제외)
        if (request.getTeam() == null || request.getTeam().trim().isEmpty()) {
            teamManagementRepository.findByDivisionAndTeamIsNull(request.getDivision())
                    .ifPresent(existing -> {
                        if (!existing.getSeq().equals(seq)) {
                            log.warn("이미 존재하는 본부: division={}", request.getDivision());
                            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "이미 존재하는 본부입니다.");
                        }
                    });
        } else {
            teamManagementRepository.findByDivisionAndTeam(request.getDivision(), request.getTeam())
                    .ifPresent(existing -> {
                        if (!existing.getSeq().equals(seq)) {
                            log.warn("이미 존재하는 팀: division={}, team={}", request.getDivision(), request.getTeam());
                            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "이미 존재하는 팀입니다.");
                        }
                    });
        }

        teamManagement.setDivision(request.getDivision());
        teamManagement.setTeam(request.getTeam() != null && !request.getTeam().trim().isEmpty() ? request.getTeam() : null);

        TeamManagement saved = teamManagementRepository.save(teamManagement);
        log.info("팀 관리 수정 완료: seq={}, division={}, team={}", saved.getSeq(), saved.getDivision(), saved.getTeam());

        Long userCount = userRepository.countByTeamManagementSeq(saved.getSeq());
        return TeamManagementResponse.builder()
                .seq(saved.getSeq())
                .division(saved.getDivision())
                .team(saved.getTeam())
                .createdAt(saved.getCreatedAt())
                .userCount(userCount)
                .build();
    }

    /**
     * 팀 관리 삭제
     *
     * @param seq 팀 관리 시퀀스
     */
    @Transactional
    public void deleteTeamManagement(Long seq) {
        log.info("팀 관리 삭제: seq={}", seq);

        TeamManagement teamManagement = teamManagementRepository.findById(seq)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 팀 관리: seq={}", seq);
                    return new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 팀 관리입니다.");
                });

        // 사용자가 해당 팀에 속해있는지 확인
        Long userCount = userRepository.countByTeamManagementSeq(seq);
        if (userCount > 0) {
            log.warn("사용자가 속해있는 팀은 삭제할 수 없습니다: seq={}, userCount={}", seq, userCount);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "사용자가 속해있는 팀은 삭제할 수 없습니다.");
        }

        teamManagementRepository.delete(teamManagement);
        log.info("팀 관리 삭제 완료: seq={}", seq);
    }

    /**
     * 팀별 사용자 목록 조회
     *
     * @param teamSeq 팀 관리 시퀀스
     * @return 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<TeamUserResponse> getUsersByTeamSeq(Long teamSeq) {
        log.info("팀별 사용자 목록 조회: teamSeq={}", teamSeq);

        // 팀 존재 여부 확인
        if (!teamManagementRepository.existsById(teamSeq)) {
            log.warn("존재하지 않는 팀 관리: teamSeq={}", teamSeq);
            throw new ApiException(ApiErrorCode.INVALID_REQUEST_FORMAT, "존재하지 않는 팀입니다.");
        }

        List<String> authVals = List.of(AuthVal.TEAM_LEADER.getCode(), AuthVal.TEAM_MEMBER.getCode()); // 팀장과 팀원만 조회
        List<User> users = userRepository.findByTeamSeqAndAuthValInOrderByCreatedAtDesc(teamSeq, authVals);

        return users.stream()
                .map(user -> TeamUserResponse.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .position(user.getPosition())
                        .authVal(user.getAuthVal())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 본부별 사용자 목록 조회 (본부장 포함)
     *
     * @param division 본부
     * @return 사용자 목록 (본부장, 마스터)
     */
    @Transactional(readOnly = true)
    public List<TeamUserResponse> getUsersByDivision(String division) {
        log.info("본부별 사용자 목록 조회: division={}", division);

        // 본부장과 마스터 조회 (bb, ma 권한, team이 null인 TeamManagement)
        List<String> authVals = List.of(AuthVal.DIVISION_HEAD.getCode(), AuthVal.MASTER.getCode()); // 본부장과 마스터 조회
        List<User> users = userRepository.findByDivisionAndAuthValInOrderByCreatedAtDesc(division, authVals);

        return users.stream()
                .map(user -> TeamUserResponse.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .position(user.getPosition())
                        .authVal(user.getAuthVal())
                        .build())
                .collect(Collectors.toList());
    }
}
