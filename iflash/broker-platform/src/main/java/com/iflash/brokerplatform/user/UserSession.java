package com.iflash.brokerplatform.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** Thin wrapper over the HTTP session holding the logged-in user's id. */
@Component
public class UserSession {

    private static final String KEY = "IBP_USER_ID";

    public void login(HttpSession session, Long userId) {
        session.setAttribute(KEY, userId);
    }

    public void logout(HttpSession session) {
        session.removeAttribute(KEY);
    }

    public Long currentUserId(HttpSession session) {
        return (Long) session.getAttribute(KEY);
    }

    public boolean isAuthenticated(HttpSession session) {
        return currentUserId(session) != null;
    }
}
