package com.investment.portal.api.controller.log;

import com.investment.portal.application.service.log.ActivityLogService;
import kwak.common.application.dto.RokResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ActivityLogControllerTest {

    @Mock ActivityLogService activityLogService;

    private Authentication authWithRole(String role) {
        return new UsernamePasswordAuthenticationToken(
                "u1", null, List.of(new SimpleGrantedAuthority(role)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(ResponseEntity<?> response) {
        return (Map<String, Object>) ((RokResponse<?>) response.getBody()).getData();
    }

    @Test
    void ROLE_ADMIN이면_isAdmin_true() {
        ActivityLogController controller = new ActivityLogController(activityLogService);

        ResponseEntity<?> response = controller.adminAccess(authWithRole("ROLE_ADMIN"));

        assertThat(dataOf(response)).containsEntry("isAdmin", true);
    }

    @Test
    void ROLE_USER면_isAdmin_false() {
        ActivityLogController controller = new ActivityLogController(activityLogService);

        ResponseEntity<?> response = controller.adminAccess(authWithRole("ROLE_USER"));

        assertThat(dataOf(response)).containsEntry("isAdmin", false);
    }
}
