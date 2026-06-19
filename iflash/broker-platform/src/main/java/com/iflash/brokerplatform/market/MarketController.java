package com.iflash.brokerplatform.market;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side polling proxies consumed by the browser. Keeping these on the
 * server hides the engine URL and avoids CORS. WebSocket can replace polling later.
 */
@RestController
@RequestMapping("/app")
class MarketController {

    private static final int CHART_POINTS = 300;
    private static final int BOOK_DEPTH = 15;

    private final IflashApiClient api;

    MarketController(IflashApiClient api) {
        this.api = api;
    }

    @GetMapping("/instruments")
    List<InstrumentDto> instruments() {
        return api.listInstruments();
    }

    @GetMapping("/price/{ticker}")
    PriceDto price(@PathVariable String ticker) {
        return api.getPrice(ticker);
    }

    @GetMapping("/quotes/{ticker}")
    List<ChartPoint> quotes(@PathVariable String ticker) {
        QuotesDto quotes;
        try {
            quotes = api.getQuotes(ticker, CHART_POINTS, "ASC");
        } catch (RuntimeException e) {
            return List.of();
        }
        if (quotes == null || quotes.quotations() == null || quotes.quotations().elements() == null) {
            return List.of();
        }
        // lightweight-charts needs strictly ascending, unique timestamps: collapse to one point per second.
        Map<Long, BigDecimal> bySecond = new LinkedHashMap<>();
        for (QuoteDto quote : quotes.quotations().elements()) {
            bySecond.put(quote.quoteTimestamp() / 1000, quote.price());
        }
        List<ChartPoint> points = new ArrayList<>(bySecond.size());
        bySecond.forEach((time, value) -> points.add(new ChartPoint(time, value)));
        return points;
    }

    @GetMapping("/orderbook/{ticker}")
    OrderBookView orderBook(@PathVariable String ticker) {
        return new OrderBookView(levels(ticker, "BID"), levels(ticker, "ASK"));
    }

    private List<OrderBookView.Level> levels(String ticker, String direction) {
        try {
            OrderBookDto book = api.getOrderBook(ticker, direction, BOOK_DEPTH);
            if (book == null || book.data() == null || book.data().elements() == null) {
                return List.of();
            }
            return book.data()
                       .elements()
                       .stream()
                       .map(entry -> new OrderBookView.Level(entry.price(), entry.volume()))
                       .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
