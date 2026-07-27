package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.favorite.FavoriteService;
import com.iflash.brokerplatform.market.InstrumentDto;
import com.iflash.brokerplatform.market.MarketQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            Map<String, BigDecimal> prices = market.instruments().stream()
                    .collect(Collectors.toMap(InstrumentDto::ticker, InstrumentDto::currentPrice, (a, b) -> a));
            List<InstrumentDto> favorites = favoriteTickers.stream()
                    .map(ticker -> new InstrumentDto(ticker, prices.get(ticker)))
                    .toList();
            return new DashboardResponse(favorites, false);
        } catch (RuntimeException e) {
            List<InstrumentDto> favorites = favoriteTickers.stream()
                    .map(ticker -> new InstrumentDto(ticker, null))
                    .toList();
            return new DashboardResponse(favorites, true);
        }
    }

    record DashboardResponse(List<InstrumentDto> favorites, boolean engineError) {
    }
}
