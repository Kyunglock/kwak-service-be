package com.investment.portal.application.dto.log;

import java.util.List;

/** 프론트 PageResponse<T> 형태와 동일한 페이지 응답 */
public record ActivityLogPage(
        List<ActivityLogResponse> content,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
