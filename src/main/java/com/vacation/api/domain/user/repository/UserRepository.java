package com.vacation.api.domain.user.repository;

import com.vacation.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * 본부로 사용자 조회 (division 문자열 비교 - 하위 호환성)
     *
     * @param division 본부
     * @param authVals 권한 값 목록 (ma, bb, tj, tw)
     * @return 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.teamManagement.division = :division AND u.authVal IN :authVals ORDER BY u.createdAt DESC")
    List<User> findByDivisionAndAuthValInOrderByCreatedAtDesc(@Param("division") String division, @Param("authVals") List<String> authVals);

    /**
     * 팀으로 사용자 조회 (teamSeq 기반)
     *
     * @param teamSeq 팀 관리 시퀀스
     * @param authVals 권한 값 목록 (ma, bb, tj, tw)
     * @return 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.teamManagement.seq = :teamSeq AND u.authVal IN :authVals ORDER BY u.createdAt DESC")
    List<User> findByTeamSeqAndAuthValInOrderByCreatedAtDesc(@Param("teamSeq") Long teamSeq, @Param("authVals") List<String> authVals);

    /**
     * 본부와 팀으로 사용자 조회 (division, team 문자열 비교 - 하위 호환성)
     *
     * @param division 본부
     * @param team 팀
     * @param authVals 권한 값 목록 (ma, bb, tj, tw)
     * @return 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.teamManagement.division = :division AND u.teamManagement.team = :team AND u.authVal IN :authVals ORDER BY u.createdAt DESC")
    List<User> findByDivisionAndTeamAndAuthValInOrderByCreatedAtDesc(@Param("division") String division, @Param("team") String team, @Param("authVals") List<String> authVals);

    /**
     * 본부와 권한으로 사용자 조회 (division 문자열 비교 - 하위 호환성)
     *
     * @param division 본부
     * @param authVal 권한 값
     * @return 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.teamManagement.division = :division AND u.authVal = :authVal")
    List<User> findByDivisionAndAuthVal(@Param("division") String division, @Param("authVal") String authVal);

    /**
     * 본부와 팀과 권한으로 사용자 조회 (division, team 문자열 비교 - 하위 호환성)
     *
     * @param division 본부
     * @param team 팀
     * @param authVal 권한 값
     * @return 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.teamManagement.division = :division AND u.teamManagement.team = :team AND u.authVal = :authVal")
    List<User> findByDivisionAndTeamAndAuthVal(@Param("division") String division, @Param("team") String team, @Param("authVal") String authVal);

    /**
     * 로그인 실패 횟수 증가 (직접 UPDATE 쿼리로 캐시 문제 우회)
     * Native Query를 사용하여 확실하게 DB에 반영
     *
     * @param userId 사용자 ID
     * @return 업데이트된 행 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE tbl_users_bas SET login_failure_count = COALESCE(login_failure_count, 0) + 1 WHERE user_id = :userId", nativeQuery = true)
    int incrementLoginFailureCount(@Param("userId") Long userId);

    /**
     * 계정 잠금 (직접 UPDATE 쿼리로 캐시 문제 우회)
     * Native Query를 사용하여 확실하게 DB에 반영
     *
     * @param userId 사용자 ID
     * @param lockUntil 잠금 해제 시간
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE tbl_users_bas SET account_locked_until = :lockUntil WHERE user_id = :userId", nativeQuery = true)
    void lockAccount(@Param("userId") Long userId, @Param("lockUntil") LocalDateTime lockUntil);

    /**
     * 사용자 ID로 조회 (teamManagement 함께 fetch join)
     *
     * @param userId 사용자 ID
     * @return 사용자 Optional
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.teamManagement WHERE u.userId = :userId")
    Optional<User> findByIdWithTeamManagement(@Param("userId") Long userId);

    /**
     * 팀 관리 시퀀스로 사용자 수 조회
     *
     * @param teamSeq 팀 관리 시퀀스
     * @return 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.teamManagement.seq = :teamSeq")
    Long countByTeamManagementSeq(@Param("teamSeq") Long teamSeq);
}

