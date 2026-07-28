package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.user.User;
import com.iflash.brokerplatform.user.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
class AuthApiController {

    private final UserService userService;
    private final JwtService jwtService;

    AuthApiController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /** Passwordless: login == registration. Returns a JWT the SPA stores and sends as a bearer token. */
    @PostMapping("/login")
    LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.loginOrRegister(request.email());
        String token = jwtService.issue(user.getId(), user.getEmail());
        return new LoginResponse(token, user.getEmail());
    }

    @GetMapping("/me")
    MeResponse me(@CurrentUserId Long userId) {
        User user = userService.require(userId);
        return new MeResponse(user.getEmail(), user.getBalance());
    }

    record LoginRequest(String email) {
    }

    record LoginResponse(String token, String email) {
    }

    record MeResponse(String email, BigDecimal balance) {
    }
}
