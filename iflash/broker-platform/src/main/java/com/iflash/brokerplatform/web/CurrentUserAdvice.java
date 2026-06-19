package com.iflash.brokerplatform.web;

import com.iflash.brokerplatform.user.User;
import com.iflash.brokerplatform.user.UserRepository;
import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes the logged-in user's email and balance to the page header on every view. */
@ControllerAdvice(basePackages = "com.iflash.brokerplatform")
class CurrentUserAdvice {

    private final UserRepository userRepository;
    private final UserSession userSession;

    CurrentUserAdvice(UserRepository userRepository, UserSession userSession) {
        this.userRepository = userRepository;
        this.userSession = userSession;
    }

    @ModelAttribute
    void currentUser(Model model, HttpSession session) {
        Long userId = userSession.currentUserId(session);
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            model.addAttribute("currentEmail", user.getEmail());
            model.addAttribute("balance", user.getBalance());
        }
    }
}
