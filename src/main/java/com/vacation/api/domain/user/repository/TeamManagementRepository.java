package com.vacation.api.domain.user.repository;

import com.vacation.api.domain.user.entity.TeamManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 팀 관리 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-22
 */
@Repository
public interface TeamManagementRepository extends JpaRepository<TeamManagement, Long> {

    /**
     * 본부와 팀으로 조회
     *
     * @param division 본부
     * @param team 팀 (null 가능)
     * @return TeamManagement
     */
    Optional<TeamManagement> findByDivisionAndTeam(String division, String team);

    /**
     * 본부로 조회 (팀이 null인 경우 포함)
     *
     * @param division 본부
     * @return TeamManagement 목록
     */
    List<TeamManagement> findByDivision(String division);

    /**
     * 본부로 조회 (팀이 null인 것만)
     *
     * @param division 본부
     * @return TeamManagement (본부만, 팀 없음)
     */
    Optional<TeamManagement> findByDivisionAndTeamIsNull(String division);

    /**
     * 팀이 null이 아닌 모든 팀 관리 정보 조회
     *
     * @return TeamManagement 목록 (team IS NOT NULL)
     */
    List<TeamManagement> findByTeamIsNotNullOrderByDivisionAscTeamAsc();
}
