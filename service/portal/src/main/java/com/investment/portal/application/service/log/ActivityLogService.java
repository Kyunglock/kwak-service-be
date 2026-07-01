package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.ActivityLogPage;
import com.investment.portal.application.event.ActivityEvent;

public interface ActivityLogService {

    /** 이벤트를 로그로 적재 */
    void record(ActivityEvent event);

    /** 사용자 본인 활동 조회 (페이지) */
    ActivityLogPage getMyLogs(String userId, int page, int size);

    /** 관리자 전체 조회 (userId/actionType 선택 필터, 페이지) */
    ActivityLogPage search(String userId, String actionType, int page, int size);
}
