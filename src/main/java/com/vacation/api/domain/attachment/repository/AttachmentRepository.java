package com.vacation.api.domain.attachment.repository;

import com.vacation.api.domain.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 첨부파일 Repository
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-15
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * 신청 타입과 시퀀스로 첨부파일 조회
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @return 첨부파일 목록 (파일 순서로 정렬)
     */
    List<Attachment> findByApplicationTypeAndApplicationSeqOrderByFileOrderAsc(String applicationType, Long applicationSeq);

    /**
     * 신청 타입과 시퀀스로 첫 번째 첨부파일 조회 (단일 파일용)
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     * @return 첨부파일
     */
    Optional<Attachment> findFirstByApplicationTypeAndApplicationSeqOrderByFileOrderAsc(String applicationType, Long applicationSeq);

    /**
     * 신청 타입과 시퀀스로 첨부파일 삭제
     *
     * @param applicationType 신청 타입
     * @param applicationSeq 신청 시퀀스
     */
    void deleteByApplicationTypeAndApplicationSeq(String applicationType, Long applicationSeq);
}
