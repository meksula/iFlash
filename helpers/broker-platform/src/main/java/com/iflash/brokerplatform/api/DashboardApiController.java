package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.favorite.FavoriteService;
import com.iflash.brokerplatform.market.InstrumentQuote;
import com.iflash.brokerplatform.market.MarketQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
class DashboardApiController {

    private final FavoriteService favoriteService;
    private final MarketQueryService market;

    DashboardApiController(FavoriteService favoriteService, MarketQueryService market) {
        this.favoriteService = favoriteService;
        this.market = market;
    }

    @GetMapping
    DashboardResponse dashboard(@CurrentUserId Long userId) {
        List<String> favoriteTickers = favoriteService.tickers(userId);
        try {
            return new DashboardResponse(favoriteTickers.stream().map(market::quote).toList(), false);
        } catch (RuntimeException e) {
            return new DashboardResponse(favoriteTickers.stream().map(InstrumentQuote::unavailable).toList(), true);
        }
    }

    record DashboardResponse(List<InstrumentQuote> favorites, boolean engineError) {
    }
}
