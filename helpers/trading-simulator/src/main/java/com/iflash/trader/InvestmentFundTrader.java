package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InvestmentFundTrader implements Trader {

    private static final Logger log = LogManager.getLogger(InvestmentFundTrader.class);

    private static final Duration WAKE_UP_INTERVAL = Duration.ofMinutes(5);
    private static final BigDecimal BUY_DISCOUNT = new BigDecimal("-0.05");
    private static final BigDecimal SELL_PREMIUM = new BigDecimal("0.05");
    private static final BigDecimal POSITION_VALUE = BigDecimal.valueOf(1_000_000);
    private static final long MAX_BLOCK_VOLUME = 500;
    private static final int MAX_BUYS_PER_WAKE_UP = 5;

    private final String name;
    private final ApiToolkit apiToolkit;
    private final TradingAccount tradingAccount;
    private final Map<String, BigDecimal> fairValues = new HashMap<>();
    private Instant nextWakeUp = Instant.MIN;

    public InvestmentFundTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        this.name = name;
        this.apiToolkit = apiToolkit;
        this.tradingAccount = new TradingAccount(name, apiToolkit, initialCash);
    }

    @Override
    public void decide() {
        Instant now = Instant.now();
        if (now.isBefore(nextWakeUp)) {
            return;
        }
        nextWakeUp = now.plus(WAKE_UP_INTERVAL);

        ApiToolkit.ApiResponse<List<FinancialInstrumentInfo>> response = apiToolkit.getInstruments();
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().isEmpty()) {
            return;
        }
        List<Valuation> valuations = response.getResponseBody()
                                             .stream()
                                             .filter(instrument -> instrument.currentPrice() != null && instrument.currentPrice().signum() > 0)
                                             .map(this::valuate)
                                             .filter(Objects::nonNull)
                                             .toList();
        log.info("[{}] wakes up, valuating {} instruments", name, valuations.size());

        valuations.stream()
                  .filter(valuation -> valuation.isOvervalued() && tradingAccount.holds(valuation.ticker()))
                  .forEach(valuation -> tradingAccount.sellAll(valuation.ticker()));
        valuations.stream()
                  .filter(Valuation::isUndervalued)
                  .filter(valuation -> !tradingAccount.holds(valuation.ticker()))
                  .sorted(Comparator.comparing(Valuation::deviation))
                  .limit(MAX_BUYS_PER_WAKE_UP)
                  .forEach(valuation -> tradingAccount.buy(valuation.ticker(), valuation.blockVolume(), valuation.currentPrice()));
    }

    private Valuation valuate(FinancialInstrumentInfo instrument) {
        BigDecimal fairValue = fairValues.putIfAbsent(instrument.ticker(), instrument.currentPrice());
        if (fairValue == null) {
            return null;
        }
        return Valuation.of(instrument.ticker(), instrument.currentPrice(), fairValue);
    }

    private record Valuation(String ticker, BigDecimal currentPrice, BigDecimal deviation) {

        static Valuation of(String ticker, BigDecimal currentPrice, BigDecimal fairValue) {
            BigDecimal deviation = currentPrice.subtract(fairValue)
                                               .divide(fairValue, 6, RoundingMode.HALF_UP);
            return new Valuation(ticker, currentPrice, deviation);
        }

        boolean isUndervalued() {
            return deviation.compareTo(BUY_DISCOUNT) <= 0;
        }

        boolean isOvervalued() {
            return deviation.compareTo(SELL_PREMIUM) >= 0;
        }

        long blockVolume() {
            long volume = POSITION_VALUE.divide(currentPrice, 0, RoundingMode.DOWN)
                                        .longValue();
            return Math.min(volume, MAX_BLOCK_VOLUME);
        }
    }
}
