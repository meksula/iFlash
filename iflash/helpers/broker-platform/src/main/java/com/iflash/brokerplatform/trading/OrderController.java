package com.iflash.brokerplatform.trading;

import com.iflash.brokerplatform.market.EngineException;
import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
class OrderController {

    private final TradingService tradingService;
    private final UserSession userSession;

    OrderController(TradingService tradingService, UserSession userSession) {
        this.tradingService = tradingService;
        this.userSession = userSession;
    }

    @PostMapping("/orders")
    String place(@RequestParam String direction,
                 @RequestParam String orderType,
                 @RequestParam String ticker,
                 @RequestParam(required = false) BigDecimal price,
                 @RequestParam long volume,
                 HttpSession session,
                 RedirectAttributes redirect) {
        Long userId = userSession.currentUserId(session);
        try {
            OrderOutcome outcome = tradingService.placeOrder(userId, ticker, direction, orderType, price, volume);
            redirect.addFlashAttribute("message", describe(outcome));
        } catch (TradingException | EngineException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instruments/" + ticker.toUpperCase();
    }

    private String describe(OrderOutcome outcome) {
        String side = "BID".equals(outcome.direction()) ? "Bought" : "Sold";
        if (outcome.notFilled()) {
            return "Order placed but not filled — no matching liquidity right now.";
        }
        BigDecimal avg = outcome.cashAmount().divide(BigDecimal.valueOf(outcome.filledQuantity()), 4, RoundingMode.HALF_UP);
        String base = side + " " + outcome.filledQuantity() + " " + outcome.ticker()
                + " @ avg " + avg + " (cash " + outcome.cashAmount() + ")";
        return outcome.partiallyFilled()
                ? base + " — partially filled of " + outcome.requestedVolume() + "."
                : base + ".";
    }
}
