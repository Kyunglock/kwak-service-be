package com.investment.portal.insight;

import com.investment.portal.application.service.insight.*;
import com.investment.portal.infrastructure.messaging.InsightBuildProducer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InsightServiceRequestBuildTest {

    @Test
    void requestBuildPublishesWhenLockAcquired() {
        InsightBuildStatusService status = mock(InsightBuildStatusService.class);
        InsightBuildProducer producer = mock(InsightBuildProducer.class);
        when(status.tryAcquire("u1")).thenReturn(true);

        InsightServiceImpl svc = InsightServiceTestFactory.withAsyncDeps(status, producer);
        assertThat(svc.requestBuild("u1")).isEqualTo("PROCESSING");
        verify(producer).publish("u1");
    }

    @Test
    void requestBuildSkipsWhenAlreadyProcessing() {
        InsightBuildStatusService status = mock(InsightBuildStatusService.class);
        InsightBuildProducer producer = mock(InsightBuildProducer.class);
        when(status.tryAcquire("u1")).thenReturn(false);

        InsightServiceImpl svc = InsightServiceTestFactory.withAsyncDeps(status, producer);
        assertThat(svc.requestBuild("u1")).isEqualTo("ALREADY_PROCESSING");
        verify(producer, never()).publish(anyString());
    }
}
