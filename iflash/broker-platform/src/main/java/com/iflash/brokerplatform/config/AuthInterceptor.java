package com.iflash.brokerplatform.config;

import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Redirects unauthenticated page requests to the login screen. Public paths
 * (login/logout/static assets) are excluded in {@link WebConfig}.
 */
@Component
class AuthInterceptor implements HandlerInterceptor {

    private final UserSession userSession;

    AuthInterceptor(UserSession userSession) {
        this.userSession = userSession;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (userSession.isAuthenticated(request.getSession())) {
            return true;
        }
        response.sendRedirect("/login");
        return false;
    }
}
