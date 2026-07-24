package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.user.User;
import com.iflash.brokerplatform.user.UserService;
import com.iflash.brokerplatform.wallet.Payment;
import com.iflash.brokerplatform.wallet.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
class WalletApiController {

    private final WalletService walletService;
    private final UserService userService;

    WalletApiController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping
    WalletResponse wallet(@CurrentUserId Long userId) {
        User user = userService.require(userId);
        return new WalletResponse(user.getBalance(), walletService.history(userId));
    }

    @PostMapping("/topup")
    WalletResponse topUp(@RequestBody TopUpRequest request, @CurrentUserId Long userId) {
        walletService.topUp(userId, request.amount());
        User user = userService.require(userId);
        return new WalletResponse(user.getBalance(), walletService.history(userId));
    }

    record TopUpRequest(BigDecimal amount) {
    }

    record WalletResponse(BigDecimal balance, List<Payment> payments) {
    }
}
