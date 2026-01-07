package com.vacation.api.domain.user.repository;

import com.vacation.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

