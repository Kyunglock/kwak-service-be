package com.investment.portal.insight;

import com.investment.portal.application.service.insight.InsightBuildStatusService;
import com.investment.portal.application.service.insight.InsightService;
import com.investment.portal.infrastructure.messaging.InsightBuildConsumer;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class InsightBuildConsumerTest {

    @Test
    void buildsThenMarksDone() {
        InsightService service = mock(InsightService.class);
        InsightBuildStatusService status = mock(InsightBuildStatusService.class);
        new InsightBuildConsumer(service, status).onMessage("u1");
        verify(service).executeBuild("u1");
        verify(status).markDone("u1");
        verify(status, never()).markFailed(anyString());
    }

    @Test
    void marksFailedOnException() {
        InsightService service = mock(InsightService.class);
        InsightBuildStatusService status = mock(InsightBuildStatusService.class);
        doThrow(new RuntimeException("boom")).when(service).executeBuild("u1");
        new InsightBuildConsumer(service, status).onMessage("u1");
        verify(status).markFailed("u1");
        verify(status, never()).markDone(anyString());
    }
}
