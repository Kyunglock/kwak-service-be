package com.investment.portal.api.controller.log;

import com.investment.portal.application.service.log.ActivityLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

// 프로덕션 @EnableMethodSecurity 자체는 SecurityConfig에 있음 — 여기선 @PreAuthorize 규칙만 검증
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ActivityLogControllerMethodSecurityTest.Config.class)
class ActivityLogControllerMethodSecurityTest {

    @Autowired
    ActivityLogController controller;

    @Test
    @WithMockUser(roles = "USER")
    void ROLE_USER는_allLogs_접근시_AccessDeniedException() {
        assertThatThrownBy(() -> controller.allLogs(null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ROLE_ADMIN은_allLogs_접근_허용() {
        assertThatCode(() -> controller.allLogs(null, null, 0, 20))
                .doesNotThrowAnyException();
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        ActivityLogService activityLogService() {
            return mock(ActivityLogService.class);
        }

        @Bean
        ActivityLogController activityLogController(ActivityLogService activityLogService) {
            return new ActivityLogController(activityLogService);
        }
    }
}
