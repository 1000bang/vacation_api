package com.vacation.api.domain.user.repository;

import com.vacation.api.domain.user.entity.UserSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 서명 정보 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-19
 */
@Repository
public interface UserSignatureRepository extends JpaRepository<UserSignature, Long> {

    /**
     * 사용자 시퀀스로 서명 정보 조회
     *
     * @param userSeq 사용자 시퀀스
     * @return 사용자 서명 정보
     */
    Optional<UserSignature> findByUserSeq(Long userSeq);

    /**
     * 사용자 시퀀스로 서명 정보 삭제
     *
     * @param userSeq 사용자 시퀀스
     */
    void deleteByUserSeq(Long userSeq);
}
