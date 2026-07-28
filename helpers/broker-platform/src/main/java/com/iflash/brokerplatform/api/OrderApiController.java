package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.trading.OrderOutcome;
import com.iflash.brokerplatform.trading.TradingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/orders")
class OrderApiController {

    private final TradingService tradingService;

    OrderApiController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping
    OrderResponse place(@RequestBody OrderRequest request, @CurrentUserId Long userId) {
        OrderOutcome outcome = tradingService.placeOrder(userId, request.ticker(), request.direction(),
                request.orderType(), request.price(), request.volume());
        return new OrderResponse(describe(outcome), outcome);
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

    record OrderRequest(String direction, String orderType, String ticker, BigDecimal price, long volume) {
    }

    record OrderResponse(String message, OrderOutcome outcome) {
    }
}
