package kwak.common.config.security;

import kwak.common.infrastructure.token.RedisTokenStore;
import kwak.common.infrastructure.token.UserSession;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RedisTokenStore redisTokenStore;
    @Mock FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication authenticateWith(UserSession session) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, redisTokenStore);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        when(jwtTokenProvider.validateToken("token123")).thenReturn(true);
        when(jwtTokenProvider.getSessionId("token123")).thenReturn("sid");
        when(redisTokenStore.getSession("sid")).thenReturn(session);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private List<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void role이_ADMIN이면_ROLE_ADMIN_authority를_부여한다() throws Exception {
        UserSession session = UserSession.builder().userId("u1").role("ADMIN").build();

        Authentication auth = authenticateWith(session);

        assertThat(authorities(auth)).containsExactly("ROLE_ADMIN");
    }

    @Test
    void role이_null이면_ROLE_USER로_폴백한다() throws Exception {
        UserSession session = UserSession.builder().userId("u1").build();

        Authentication auth = authenticateWith(session);

        assertThat(authorities(auth)).containsExactly("ROLE_USER");
    }
}
