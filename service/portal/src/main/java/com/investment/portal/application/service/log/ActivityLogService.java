package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.ActivityLogPage;
import kwak.common.application.event.ActivityEvent;

public interface ActivityLogService {

    /** 이벤트를 로그로 적재 */
    void record(ActivityEvent event);

    /** 사용자 본인 활동 조회 (페이지) */
    ActivityLogPage getMyLogs(String userId, int page, int size);

    /**
     * 관리자 전체 조회 (targetUserId/actionType 선택 필터, 페이지).
     *
     * <p>targetUserId 로 자기 자신을 콕 집어 조회한 게 아니라면, 관리자 본인의 활동은
     * 결과에서 뺀다 — 본인 활동은 /me 에서 이미 볼 수 있고, 관리자가 뭔가 조회할 때마다
     * 그 조회 행위 자체가 "전체" 목록에 쌓여 다른 사람 활동을 밀어내는 걸 막기 위함이다.
     */
    ActivityLogPage search(String requestingUserId, String targetUserId, String actionType, int page, int size);
}
