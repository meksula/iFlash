package com.iflash.brokerplatform.market;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-side market queries shared by the legacy {@code /app} proxies and the {@code /api/market}
 * endpoints. Shapes the engine's raw responses into what the browser charts/book expect.
 */
@Service
public class MarketQueryService {

    private static final int CHART_POINTS = 300;
    private static final int BOOK_DEPTH = 15;

    private final IflashApiClient api;

    public MarketQueryService(IflashApiClient api) {
        this.api = api;
    }

    public List<InstrumentDto> instruments() {
        return api.listInstruments();
    }

    public PriceDto price(String ticker) {
        return api.getPrice(ticker);
    }

    public List<ChartPoint> quotes(String ticker) {
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

    public OrderBookView orderBook(String ticker) {
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
