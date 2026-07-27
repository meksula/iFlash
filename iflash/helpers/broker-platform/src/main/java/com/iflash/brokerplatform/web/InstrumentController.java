package com.iflash.brokerplatform.web;

import com.iflash.brokerplatform.favorite.FavoriteService;
import com.iflash.brokerplatform.market.IflashApiClient;
import com.iflash.brokerplatform.market.PriceDto;
import com.iflash.brokerplatform.trading.OrderCatalog;
import com.iflash.brokerplatform.trading.TradingService;
import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
class InstrumentController {

    private final IflashApiClient api;
    private final FavoriteService favoriteService;
    private final TradingService tradingService;
    private final UserSession userSession;

    InstrumentController(IflashApiClient api, FavoriteService favoriteService, TradingService tradingService,
                         UserSession userSession) {
        this.api = api;
        this.favoriteService = favoriteService;
        this.tradingService = tradingService;
        this.userSession = userSession;
    }

    @GetMapping("/instruments")
    String instruments(Model model, HttpSession session) {
        Long userId = userSession.currentUserId(session);
        try {
            model.addAttribute("instruments", api.listInstruments());
        } catch (RuntimeException e) {
            model.addAttribute("instruments", List.of());
            model.addAttribute("engineError", true);
        }
        model.addAttribute("favorites", new HashSet<>(favoriteService.tickers(userId)));
        return "instruments";
    }

    @GetMapping("/instruments/{ticker}")
    String instrument(@PathVariable String ticker, Model model, HttpSession session) {
        Long userId = userSession.currentUserId(session);
        String symbol = ticker.toUpperCase();

        try {
            PriceDto price = api.getPrice(symbol);
            model.addAttribute("price", price == null ? null : price.price());
        } catch (RuntimeException e) {
            model.addAttribute("price", null);
            model.addAttribute("engineError", true);
        }

        Set<String> favorites = new HashSet<>(favoriteService.tickers(userId));
        model.addAttribute("ticker", symbol);
        model.addAttribute("favorite", favorites.contains(symbol));
        model.addAttribute("position", tradingService.heldQuantity(userId, symbol));
        model.addAttribute("orderTypes", OrderCatalog.ORDER_TYPES);
        model.addAttribute("directions", OrderCatalog.DIRECTIONS);
        return "instrument";
    }
}
