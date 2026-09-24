package com.investment.portal.application.service.log;

import com.investment.portal.domain.entity.log.ActivityLog;
import com.investment.portal.domain.repository.log.ActivityLogMapper;
import kwak.common.application.event.ActivityEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActivityLogServiceImplTest {

    private final ActivityLogMapper mapper = mock(ActivityLogMapper.class);
    private final ActivityLogServiceImpl service = new ActivityLogServiceImpl(mapper);

    @Test
    void detail은_컬럼_길이_1000자로_잘라서_적재한다() {
        // AI 매매입력 문장은 최대 2000자까지 받는다 — 자르지 않으면 INSERT 가 실패하고 로그가 통째로 빠진다
        String longPrompt = "가".repeat(2000);

        service.record(ActivityEvent.of("user-1", "AI_TRADE_CAPTURE", "PORTFOLIO", "10", longPrompt));

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getDetail()).hasSize(1000);
    }
}
