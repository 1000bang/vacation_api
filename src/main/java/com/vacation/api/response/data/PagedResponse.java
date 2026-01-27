package com.vacation.api.response.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이징 응답 공통 DTO
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> list;
    private Long totalCount;
}
