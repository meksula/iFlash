package com.iflash.brokerplatform.web;

import com.iflash.brokerplatform.favorite.FavoriteService;
import com.iflash.brokerplatform.market.IflashApiClient;
import com.iflash.brokerplatform.market.InstrumentDto;
import com.iflash.brokerplatform.user.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
class DashboardController {

    private final FavoriteService favoriteService;
    private final IflashApiClient api;
    private final UserSession userSession;

    DashboardController(FavoriteService favoriteService, IflashApiClient api, UserSession userSession) {
        this.favoriteService = favoriteService;
        this.api = api;
        this.userSession = userSession;
    }

    @GetMapping("/")
    String dashboard(Model model, HttpSession session) {
        Long userId = userSession.currentUserId(session);
        List<String> favoriteTickers = favoriteService.tickers(userId);

        Map<String, BigDecimal> prices = priceIndex(model);
        List<InstrumentDto> favorites = favoriteTickers.stream()
                                                       .map(ticker -> new InstrumentDto(ticker, prices.get(ticker)))
                                                       .toList();
        model.addAttribute("favorites", favorites);
        return "dashboard";
    }

    private Map<String, BigDecimal> priceIndex(Model model) {
        try {
            return api.listInstruments()
                      .stream()
                      .collect(Collectors.toMap(InstrumentDto::ticker, InstrumentDto::currentPrice, (a, b) -> a));
        } catch (RuntimeException e) {
            model.addAttribute("engineError", true);
            return Map.of();
        }
    }
}
