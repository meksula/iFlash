package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;
import com.iflash.toolkit.OrderBookSnapshotResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class OrderBookImbalanceTrader implements Trader {

    private static final Logger log = LogManager.getLogger(OrderBookImbalanceTrader.class);

    private static final Random RANDOM = new Random();
    private static final int INSTRUMENTS_PER_TICK = 3;
    private static final int BOOK_DEPTH = 10;
    private static final double IMBALANCE_THRESHOLD = 0.4;
    private static final long TRADE_VOLUME = 5;

    private final String name;
    private final ApiToolkit apiToolkit;
    private final TradingAccount tradingAccount;

    public OrderBookImbalanceTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        this.name = name;
        this.apiToolkit = apiToolkit;
        this.tradingAccount = new TradingAccount(name, apiToolkit, initialCash);
    }

    @Override
    public void decide() {
        ApiToolkit.ApiResponse<List<FinancialInstrumentInfo>> response = apiToolkit.getInstruments();
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().isEmpty()) {
            return;
        }
        List<FinancialInstrumentInfo> instruments = response.getResponseBody();
        for (int i = 0; i < INSTRUMENTS_PER_TICK; i++) {
            evaluateAndTrade(instruments.get(RANDOM.nextInt(instruments.size())));
        }
    }

    private void evaluateAndTrade(FinancialInstrumentInfo instrument) {
        String ticker = instrument.ticker();
        Optional<BookSide> bids = bookSide(ticker, "BID", "DESC");
        Optional<BookSide> asks = bookSide(ticker, "ASK", "ASC");
        if (bids.isEmpty() || asks.isEmpty()) {
            return;
        }
        long totalVolume = bids.get().volume() + asks.get().volume();
        if (totalVolume == 0) {
            return;
        }
        double imbalance = (double) (bids.get().volume() - asks.get().volume()) / totalVolume;

        if (imbalance > IMBALANCE_THRESHOLD && asks.get().bestPrice() != null) {
            log.info("[{}] buy pressure on {}: imbalance {}", name, ticker, imbalance);
            tradingAccount.buy(ticker, TRADE_VOLUME, asks.get().bestPrice());
        }
        else if (imbalance < -IMBALANCE_THRESHOLD && tradingAccount.holds(ticker)) {
            log.info("[{}] sell pressure on {}: imbalance {}", name, ticker, imbalance);
            tradingAccount.sellAll(ticker);
        }
    }

    private Optional<BookSide> bookSide(String ticker, String orderDirection, String orderBy) {
        ApiToolkit.ApiResponse<OrderBookSnapshotResponse> response = apiToolkit.getOrderBook(ticker, 0, BOOK_DEPTH, orderBy, orderDirection);
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().data() == null) {
            return Optional.empty();
        }
        List<OrderBookSnapshotResponse.OrderBookEntry> entries = response.getResponseBody().data().getElements();
        return Optional.of(BookSide.of(entries == null ? List.of() : entries));
    }

    private record BookSide(long volume, BigDecimal bestPrice) {

        static BookSide of(List<OrderBookSnapshotResponse.OrderBookEntry> entries) {
            long volume = entries.stream()
                                 .mapToLong(OrderBookSnapshotResponse.OrderBookEntry::volume)
                                 .sum();
            BigDecimal bestPrice = entries.isEmpty() ? null : entries.getFirst().price();
            return new BookSide(volume, bestPrice);
        }
    }
}
