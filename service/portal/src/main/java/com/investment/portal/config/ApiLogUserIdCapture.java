package com.investment.portal.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ApiRequestLoggingFilter 는 Security 필터보다 앞에서 실행되어(미인증 공격 시도 포착 목적)
 * SecurityContext 를 직접 읽을 수 없다. 대신 이 인터셉터가 컨트롤러 진입 시점에
 * 인증된 userId 를 request attribute 로 넘겨 필터가 로그에 채우게 한다.
 */
@Configuration
public class ApiLogUserIdCapture implements WebMvcConfigurer, HandlerInterceptor {

    public static final String USER_ID_ATTR = "apiLogUserId";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof String userId
                && !"anonymousUser".equals(userId)) {
            request.setAttribute(USER_ID_ATTR, userId);
        }
        return true;
    }
}
