package com.iflash.brokerplatform.market;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    private static final int QUOTES_DEPTH = 5000;
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

    public InstrumentQuote quote(String ticker) {
        PriceDto price = price(ticker);
        BigDecimal currentPrice = price == null ? null : price.price();
        return new InstrumentQuote(ticker, currentPrice, PriceChange.percent(previousClose(ticker), currentPrice));
    }

    public List<ChartPoint> quotes(String ticker) {
        // lightweight-charts needs strictly ascending, unique timestamps: collapse to one point per second.
        Map<Long, BigDecimal> bySecond = new LinkedHashMap<>();
        for (QuoteDto quote : latestQuotes(ticker).reversed()) {
            bySecond.put(quote.quoteTimestamp() / 1000, quote.price());
        }
        List<ChartPoint> points = new ArrayList<>(bySecond.size());
        bySecond.forEach((time, value) -> points.add(new ChartPoint(time, value)));
        return points;
    }

    private BigDecimal previousClose(String ticker) {
        long startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        List<QuoteDto> latest = latestQuotes(ticker);
        return latest.stream()
                     .filter(quote -> quote.quoteTimestamp() < startOfDay)
                     .findFirst()
                     .or(() -> latest.stream().reduce((newer, older) -> older))
                     .map(QuoteDto::price)
                     .orElse(null);
    }

    private List<QuoteDto> latestQuotes(String ticker) {
        QuotesDto quotes;
        try {
            quotes = api.getQuotes(ticker, QUOTES_DEPTH, "DESC");
        } catch (RuntimeException e) {
            return List.of();
        }
        if (quotes == null || quotes.quotations() == null || quotes.quotations().elements() == null) {
            return List.of();
        }
        return quotes.quotations().elements();
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
