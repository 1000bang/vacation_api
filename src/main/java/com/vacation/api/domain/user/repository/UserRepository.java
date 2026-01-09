package com.vacation.api.domain.user.repository;

import com.vacation.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-06
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 권한 값으로 사용자 조회
     *
     * @param authVals 권한 값 목록 (ma, bb, tj, tw)
     * @return 사용자 목록
     */
    List<User> findByAuthValInOrderByCreatedAtDesc(List<String> authVals);

    /**
     * 본부로 사용자 조회
     *
     * @param division 본부
     * @param authVals 권한 값 목록 (ma, bb, tj, tw)
     * @return 사용자 목록
     */
    List<User> findByDivisionAndAuthValInOrderByCreatedAtDesc(String division, List<String> authVals);

    /**
     * 본부와 팀으로 사용자 조회
     *
     * @param division 본부
     * @param team 팀
     * @param authVals 권한 값 목록 (ma, bb, tj, tw)
     * @return 사용자 목록
     */
    List<User> findByDivisionAndTeamAndAuthValInOrderByCreatedAtDesc(String division, String team, List<String> authVals);
}

