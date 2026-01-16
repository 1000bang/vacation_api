package com.vacation.api.domain.attachment.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 첨부파일 응답 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private Long seq;
    private String fileName;
    private Long fileSize;
}
