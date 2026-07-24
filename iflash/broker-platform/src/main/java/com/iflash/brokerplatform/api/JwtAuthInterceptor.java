package com.iflash.brokerplatform.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards {@code /api/**} with a bearer JWT. On success the resolved user id is stashed as a
 * request attribute for {@link CurrentUserIdResolver}; on failure it answers 401 (no redirects).
 */
@Component
class JwtAuthInterceptor implements HandlerInterceptor {

    static final String USER_ID_ATTRIBUTE = "ibp.userId";

    private final JwtService jwtService;

    JwtAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        Long userId = header != null && header.startsWith("Bearer ")
                ? jwtService.parseUserId(header.substring(7))
                : null;
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
