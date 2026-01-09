package com.vacation.api.domain.vacation.repository;

import com.vacation.api.domain.vacation.entity.UserVacationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자별 연차 정보 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@Repository
public interface UserVacationInfoRepository extends JpaRepository<UserVacationInfo, Long> {

    /**
     * 사용자 ID로 연차 정보 조회
     *
     * @param userId 사용자 ID
     * @return 연차 정보
     */
    Optional<UserVacationInfo> findByUserId(Long userId);
}

