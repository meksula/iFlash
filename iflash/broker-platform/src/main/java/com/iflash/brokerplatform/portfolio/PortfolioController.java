package com.iflash.brokerplatform.portfolio;

import com.iflash.brokerplatform.trading.TradingService;
import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/portfolio")
class PortfolioController {

    private final PortfolioService portfolioService;
    private final TradingService tradingService;
    private final UserSession userSession;

    PortfolioController(PortfolioService portfolioService, TradingService tradingService, UserSession userSession) {
        this.portfolioService = portfolioService;
        this.tradingService = tradingService;
        this.userSession = userSession;
    }

    @GetMapping
    String portfolio(Model model, HttpSession session) {
        Long userId = userSession.currentUserId(session);
        List<Holding> holdings = portfolioService.holdings(userId);
        model.addAttribute("holdings", holdings);
        model.addAttribute("totalValue", portfolioService.totalMarketValue(holdings));
        model.addAttribute("trades", tradingService.history(userId));
        return "portfolio";
    }
}
