package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.ActivityLogResponse;
import com.investment.portal.domain.entity.log.ActivityLog;
import com.investment.portal.domain.repository.log.ActivityLogMapper;
import kwak.common.application.event.ActivityEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    // ── 전체 조회에서 관리자 본인 활동 제외 ────────────────────────────────────────

    @Test
    void targetUserId를_지정하지_않으면_요청자_본인을_결과에서_뺀다() {
        service.search("admin-1", null, null, 0, 20);

        verify(mapper).search(isNull(), isNull(), eq("admin-1"), eq(0), eq(20));
        verify(mapper).countSearch(isNull(), isNull(), eq("admin-1"));
    }

    @Test
    void targetUserId가_요청자_본인이_아니면_요청자를_결과에서_뺀다() {
        service.search("admin-1", "other-user", "LOGIN", 0, 20);

        verify(mapper).search(eq("other-user"), eq("LOGIN"), eq("admin-1"), eq(0), eq(20));
        verify(mapper).countSearch(eq("other-user"), eq("LOGIN"), eq("admin-1"));
    }

    @Test
    void targetUserId로_요청자_본인을_명시하면_제외하지_않는다() {
        service.search("admin-1", "admin-1", null, 0, 20);

        verify(mapper).search(eq("admin-1"), isNull(), isNull(), eq(0), eq(20));
        verify(mapper).countSearch(eq("admin-1"), isNull(), isNull());
    }

    @Test
    void 조회_결과는_응답_DTO로_변환된다() {
        when(mapper.countSearch(isNull(), isNull(), eq("admin-1"))).thenReturn(1L);
        when(mapper.search(isNull(), isNull(), eq("admin-1"), eq(0), eq(20)))
                .thenReturn(List.of(ActivityLog.builder().logId(1L).userId("user-2").actionType("LOGIN").build()));

        List<ActivityLogResponse> content = service.search("admin-1", null, null, 0, 20).content();

        assertThat(content).hasSize(1);
        assertThat(content.get(0).userId()).isEqualTo("user-2");
    }
}
