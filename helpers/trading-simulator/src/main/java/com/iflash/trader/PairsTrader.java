package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.QuotationHistoryResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class PairsTrader implements Trader {

    private static final Logger log = LogManager.getLogger(PairsTrader.class);

    private static final int LOOKBACK = 60;
    private static final int MIN_QUOTES = 20;
    private static final double ENTRY_Z_SCORE = 2.0;
    private static final double EXIT_Z_SCORE = 0.5;
    private static final long TRADE_VOLUME = 5;

    private final String name;
    private final ApiToolkit apiToolkit;
    private final String firstTicker;
    private final String secondTicker;
    private final TradingAccount tradingAccount;

    public PairsTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash, String firstTicker, String secondTicker) {
        this.name = name;
        this.apiToolkit = apiToolkit;
        this.firstTicker = firstTicker;
        this.secondTicker = secondTicker;
        this.tradingAccount = new TradingAccount(name, apiToolkit, initialCash);
    }

    @Override
    public void decide() {
        List<BigDecimal> firstPrices = prices(firstTicker);
        List<BigDecimal> secondPrices = prices(secondTicker);
        int size = Math.min(firstPrices.size(), secondPrices.size());
        if (size < MIN_QUOTES) {
            return;
        }
        List<Double> ratios = IntStream.range(0, size)
                                       .mapToObj(i -> firstPrices.get(firstPrices.size() - size + i)
                                                                 .divide(secondPrices.get(secondPrices.size() - size + i), 8, RoundingMode.HALF_UP)
                                                                 .doubleValue())
                                       .toList();
        double mean = ratios.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double deviation = Math.sqrt(ratios.stream().mapToDouble(ratio -> Math.pow(ratio - mean, 2)).average().orElse(0));
        if (deviation == 0) {
            return;
        }
        double zScore = (ratios.getLast() - mean) / deviation;

        if (zScore > ENTRY_Z_SCORE) {
            log.info("[{}] {} expensive against {}: z-score {}", name, firstTicker, secondTicker, zScore);
            rotate(firstTicker, secondTicker, secondPrices.getLast());
        }
        else if (zScore < -ENTRY_Z_SCORE) {
            log.info("[{}] {} cheap against {}: z-score {}", name, firstTicker, secondTicker, zScore);
            rotate(secondTicker, firstTicker, firstPrices.getLast());
        }
        else if (Math.abs(zScore) < EXIT_Z_SCORE) {
            tradingAccount.sellAll(firstTicker);
            tradingAccount.sellAll(secondTicker);
        }
    }

    private void rotate(String expensiveTicker, String cheapTicker, BigDecimal cheapPrice) {
        tradingAccount.sellAll(expensiveTicker);
        if (!tradingAccount.holds(cheapTicker)) {
            tradingAccount.buy(cheapTicker, TRADE_VOLUME, cheapPrice);
        }
    }

    private List<BigDecimal> prices(String ticker) {
        ApiToolkit.ApiResponse<QuotationHistoryResponse> response = apiToolkit.getQuotationHistory(ticker, LOOKBACK, "DESC");
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().quotations() == null) {
            return List.of();
        }
        return response.getResponseBody()
                       .quotations()
                       .elements()
                       .stream()
                       .map(QuotationHistoryResponse.Quote::price)
                       .filter(Objects::nonNull)
                       .toList()
                       .reversed();
    }
}
