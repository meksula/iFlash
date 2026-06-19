package com.iflash.brokerplatform.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AuthController {

    private final UserService userService;
    private final UserSession userSession;

    AuthController(UserService userService, UserSession userSession) {
        this.userService = userService;
        this.userSession = userSession;
    }

    @GetMapping("/login")
    String login(HttpSession session) {
        return userSession.isAuthenticated(session) ? "redirect:/" : "login";
    }

    @PostMapping("/login")
    String doLogin(@RequestParam String email, HttpSession session, RedirectAttributes redirect) {
        try {
            User user = userService.loginOrRegister(email);
            userSession.login(session, user.getId());
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/logout")
    String logout(HttpSession session) {
        userSession.logout(session);
        session.invalidate();
        return "redirect:/login";
    }
}
