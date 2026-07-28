package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;
import com.iflash.toolkit.QuotationHistoryResponse;
import com.iflash.toolkit.TransactionResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Base for "smart money" traders that act on technical-analysis signals computed from an
 * instrument's recent price history. It owns the boring parts — pulling quotes, building the
 * {@link BarSeries}, sizing orders and keeping cash/holdings in sync — so each subclass only has to
 * turn a price series into a {@link Signal}.
 *
 * <p>Cash and holdings are tracked locally for the trader's own bookkeeping; the engine remains the
 * source of truth for fills.
 */
abstract class TechnicalTrader implements Trader {

    private static final Logger log = LogManager.getLogger(TechnicalTrader.class);

    protected enum Signal {BUY, SELL, HOLD}

    private static final Random RANDOM = new Random();
    private static final int HISTORY_SIZE = 120;
    private static final int INSTRUMENTS_PER_TICK = 2;
    private static final long TRADE_VOLUME = 5;

    protected final String name;
    protected final ApiToolkit apiToolkit;
    protected BigDecimal cash;
    protected final Map<String, Long> holdings = new HashMap<>();

    protected TechnicalTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        this.name = name;
        this.apiToolkit = apiToolkit;
        this.cash = initialCash;
    }

    @Override
    public final void decide() {
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
        BarSeries series = buildSeries(ticker);
        if (series == null || series.getBarCount() < requiredBars()) {
            return;
        }

        Signal signal = evaluate(series);
        if (signal == Signal.HOLD) {
            return;
        }
        log.info("[{}] {} signal for {} @ {}", name, signal, ticker, instrument.currentPrice());

        if (signal == Signal.BUY) {
            buy(ticker, instrument.currentPrice());
        } else {
            sell(ticker);
        }
    }

    /** Turn the recent price series into a trading signal. */
    protected abstract Signal evaluate(BarSeries series);

    /** Minimum number of bars the strategy needs before it can produce a meaningful signal. */
    protected abstract int requiredBars();

    private BarSeries buildSeries(String ticker) {
        ApiToolkit.ApiResponse<QuotationHistoryResponse> response = apiToolkit.getQuotationHistory(ticker, HISTORY_SIZE, "ASC");
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().quotations() == null) {
            return null;
        }
        List<QuotationHistoryResponse.Quote> quotes = response.getResponseBody().quotations().elements();
        if (quotes == null || quotes.isEmpty()) {
            return null;
        }

        BarSeries series = new BaseBarSeriesBuilder().withName(ticker).build();
        // The engine timestamps are not guaranteed strictly increasing; synthetic 1s-spaced bars
        // keep ta4j happy while preserving chronological order, which is all the indicators need.
        ZonedDateTime barTime = ZonedDateTime.now().minusSeconds(quotes.size() + 1L);
        for (QuotationHistoryResponse.Quote quote : quotes) {
            if (quote.price() == null || quote.price().signum() <= 0) {
                continue;
            }
            barTime = barTime.plusSeconds(1);
            series.addBar(barTime, quote.price(), quote.price(), quote.price(), quote.price());
        }
        return series;
    }

    private void buy(String ticker, BigDecimal currentPrice) {
        if (currentPrice != null && currentPrice.signum() > 0) {
            BigDecimal estimatedCost = currentPrice.multiply(BigDecimal.valueOf(TRADE_VOLUME));
            if (cash.compareTo(estimatedCost) < 0) {
                log.info("[{}] not enough cash ({}) to buy {} of {}", name, cash, TRADE_VOLUME, ticker);
                return;
            }
        }

        ApiToolkit.ApiResponse<TransactionResponse> response = apiToolkit.placeMarketOrder("BID", ticker, TRADE_VOLUME);
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null) {
            log.warn("[{}] buy order failed for {}. HTTP status: {}", name, ticker, response.getHttpStatus());
            return;
        }

        TransactionResponse transaction = response.getResponseBody();
        long filled = executedVolume(transaction);
        if (filled <= 0) {
            log.info("[{}] buy order for {} was not filled (no liquidity)", name, ticker);
            return;
        }

        cash = cash.subtract(executedValue(transaction));
        holdings.merge(ticker, filled, Long::sum);
        log.info("[{}] BOUGHT {} {} — holdings now {}, cash {}", name, filled, ticker, holdings.get(ticker), cash);
    }

    private void sell(String ticker) {
        long held = holdings.getOrDefault(ticker, 0L);
        if (held <= 0) {
            return;
        }

        ApiToolkit.ApiResponse<TransactionResponse> response = apiToolkit.placeMarketOrder("ASK", ticker, held);
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null) {
            log.warn("[{}] sell order failed for {}. HTTP status: {}", name, ticker, response.getHttpStatus());
            return;
        }

        TransactionResponse transaction = response.getResponseBody();
        long filled = executedVolume(transaction);
        if (filled <= 0) {
            log.info("[{}] sell order for {} was not filled (no bids)", name, ticker);
            return;
        }

        cash = cash.add(executedValue(transaction));
        long remaining = held - filled;
        if (remaining > 0) {
            holdings.put(ticker, remaining);
        } else {
            holdings.remove(ticker);
        }
        log.info("[{}] SOLD {} {} — holdings now {}, cash {}", name, filled, ticker, remaining, cash);
    }

    private long executedVolume(TransactionResponse transaction) {
        return transaction.getVolume() == null ? 0L : transaction.getVolume();
    }

    private BigDecimal executedValue(TransactionResponse transaction) {
        List<TransactionResponse.TransactionInfoResponse> transactions = transaction.getTransactions();
        if (transactions != null && !transactions.isEmpty()) {
            return transactions.stream()
                               .map(t -> t.price().multiply(BigDecimal.valueOf(t.volume())))
                               .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (transaction.getPrice() != null && transaction.getVolume() != null) {
            return transaction.getPrice().multiply(BigDecimal.valueOf(transaction.getVolume()));
        }
        return BigDecimal.ZERO;
    }
}
