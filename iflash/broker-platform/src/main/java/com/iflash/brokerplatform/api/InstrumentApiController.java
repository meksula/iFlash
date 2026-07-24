package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.favorite.FavoriteService;
import com.iflash.brokerplatform.market.InstrumentDto;
import com.iflash.brokerplatform.market.MarketQueryService;
import com.iflash.brokerplatform.market.PriceDto;
import com.iflash.brokerplatform.trading.OrderCatalog;
import com.iflash.brokerplatform.trading.TradingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/instruments")
class InstrumentApiController {

    private final MarketQueryService market;
    private final FavoriteService favoriteService;
    private final TradingService tradingService;

    InstrumentApiController(MarketQueryService market, FavoriteService favoriteService,
                            TradingService tradingService) {
        this.market = market;
        this.favoriteService = favoriteService;
        this.tradingService = tradingService;
    }

    @GetMapping
    InstrumentsResponse instruments(@CurrentUserId Long userId) {
        Set<String> favorites = new HashSet<>(favoriteService.tickers(userId));
        try {
            return new InstrumentsResponse(market.instruments(), favorites, false);
        } catch (RuntimeException e) {
            return new InstrumentsResponse(List.of(), favorites, true);
        }
    }

    @GetMapping("/{ticker}")
    InstrumentDetailResponse instrument(@PathVariable String ticker, @CurrentUserId Long userId) {
        String symbol = ticker.toUpperCase();
        BigDecimal price = null;
        boolean engineError = false;
        try {
            PriceDto dto = market.price(symbol);
            price = dto == null ? null : dto.price();
        } catch (RuntimeException e) {
            engineError = true;
        }
        boolean favorite = favoriteService.tickers(userId).contains(symbol);
        long position = tradingService.heldQuantity(userId, symbol);
        return new InstrumentDetailResponse(symbol, price, favorite, position,
                OrderCatalog.ORDER_TYPES, OrderCatalog.DIRECTIONS, engineError);
    }

    record InstrumentsResponse(List<InstrumentDto> instruments, Set<String> favorites, boolean engineError) {
    }

    record InstrumentDetailResponse(String ticker, BigDecimal price, boolean favorite, long position,
                                    List<String> orderTypes, List<OrderCatalog.Direction> directions,
                                    boolean engineError) {
    }
}
