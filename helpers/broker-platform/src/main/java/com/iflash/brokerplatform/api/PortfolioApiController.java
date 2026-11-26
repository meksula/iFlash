package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.portfolio.Holding;
import com.iflash.brokerplatform.portfolio.PortfolioService;
import com.iflash.brokerplatform.trading.OrderOutcome;
import com.iflash.brokerplatform.trading.Trade;
import com.iflash.brokerplatform.trading.TradingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
class PortfolioApiController {

    private final PortfolioService portfolioService;
    private final TradingService tradingService;

    PortfolioApiController(PortfolioService portfolioService, TradingService tradingService) {
        this.portfolioService = portfolioService;
        this.tradingService = tradingService;
    }

    @GetMapping
    PortfolioResponse portfolio(@CurrentUserId Long userId) {
        List<Holding> holdings = portfolioService.holdings(userId);
        return new PortfolioResponse(holdings, portfolioService.totalMarketValue(holdings),
                tradingService.history(userId));
    }

    @PostMapping("/{ticker}/liquidate")
    OrderOutcome liquidatePosition(@PathVariable String ticker, @CurrentUserId Long userId) {
        return tradingService.liquidatePosition(userId, ticker);
    }

    @PostMapping("/lots/{lotId}/liquidate")
    OrderOutcome liquidateLot(@PathVariable Long lotId, @CurrentUserId Long userId) {
        return tradingService.liquidateLot(userId, lotId);
    }

    record PortfolioResponse(List<Holding> holdings, BigDecimal totalValue, List<Trade> trades) {
    }
}
