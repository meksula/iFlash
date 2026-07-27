package com.iflash.brokerplatform.market;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Server-side polling proxies consumed by the (legacy Thymeleaf) browser pages. Keeping these on
 * the server hides the engine URL and avoids CORS. The Angular client uses {@code /api/market/*}.
 */
@RestController
@RequestMapping("/app")
class MarketController {

    private final MarketQueryService market;

    MarketController(MarketQueryService market) {
        this.market = market;
    }

    @GetMapping("/instruments")
    List<InstrumentDto> instruments() {
        return market.instruments();
    }

    @GetMapping("/price/{ticker}")
    PriceDto price(@PathVariable String ticker) {
        return market.price(ticker);
    }

    @GetMapping("/quotes/{ticker}")
    List<ChartPoint> quotes(@PathVariable String ticker) {
        return market.quotes(ticker);
    }

    @GetMapping("/orderbook/{ticker}")
    OrderBookView orderBook(@PathVariable String ticker) {
        return market.orderBook(ticker);
    }
}
