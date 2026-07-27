package com.iflash.brokerplatform.wallet;

import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/wallet")
class WalletController {

    private final WalletService walletService;
    private final UserSession userSession;

    WalletController(WalletService walletService, UserSession userSession) {
        this.walletService = walletService;
        this.userSession = userSession;
    }

    @GetMapping
    String wallet(Model model, HttpSession session) {
        Long userId = userSession.currentUserId(session);
        model.addAttribute("payments", walletService.history(userId));
        return "wallet";
    }

    @PostMapping("/topup")
    String topUp(@RequestParam BigDecimal amount, HttpSession session, RedirectAttributes redirect) {
        Long userId = userSession.currentUserId(session);
        try {
            walletService.topUp(userId, amount);
            redirect.addFlashAttribute("message", "Account topped up by " + amount + ".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wallet";
    }
}
