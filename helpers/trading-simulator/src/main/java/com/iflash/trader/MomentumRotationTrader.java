package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;
import com.iflash.toolkit.QuotationHistoryResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MomentumRotationTrader implements Trader {

    private static final Logger log = LogManager.getLogger(MomentumRotationTrader.class);

    private static final int LOOKBACK = 30;
    private static final int MIN_QUOTES = 10;
    private static final int TOP_N = 3;
    private static final long TRADE_VOLUME = 5;

    private final String name;
    private final ApiToolkit apiToolkit;
    private final TradingAccount tradingAccount;

    public MomentumRotationTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
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
        List<Momentum> leaders = response.getResponseBody()
                                         .stream()
                                         .map(instrument -> momentum(instrument.ticker()))
                                         .flatMap(Optional::stream)
                                         .filter(Momentum::isPositive)
                                         .sorted(Comparator.comparing(Momentum::rateOfReturn).reversed())
                                         .limit(TOP_N)
                                         .toList();
        Set<String> leaderTickers = leaders.stream()
                                           .map(Momentum::ticker)
                                           .collect(Collectors.toSet());
        log.info("[{}] momentum leaders: {}", name, leaderTickers);

        tradingAccount.heldTickers()
                      .stream()
                      .filter(ticker -> !leaderTickers.contains(ticker))
                      .forEach(tradingAccount::sellAll);
        leaders.stream()
               .filter(leader -> !tradingAccount.holds(leader.ticker()))
               .forEach(leader -> tradingAccount.buy(leader.ticker(), TRADE_VOLUME, leader.lastPrice()));
    }

    private Optional<Momentum> momentum(String ticker) {
        ApiToolkit.ApiResponse<QuotationHistoryResponse> response = apiToolkit.getQuotationHistory(ticker, LOOKBACK, "DESC");
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().quotations() == null) {
            return Optional.empty();
        }
        List<BigDecimal> prices = response.getResponseBody()
                                          .quotations()
                                          .elements()
                                          .stream()
                                          .map(QuotationHistoryResponse.Quote::price)
                                          .filter(Objects::nonNull)
                                          .toList();
        if (prices.size() < MIN_QUOTES) {
            return Optional.empty();
        }
        return Optional.of(Momentum.of(ticker, prices.getFirst(), prices.getLast()));
    }

    private record Momentum(String ticker, BigDecimal lastPrice, BigDecimal rateOfReturn) {

        static Momentum of(String ticker, BigDecimal lastPrice, BigDecimal oldestPrice) {
            BigDecimal rateOfReturn = lastPrice.subtract(oldestPrice)
                                               .divide(oldestPrice, 6, RoundingMode.HALF_UP);
            return new Momentum(ticker, lastPrice, rateOfReturn);
        }

        boolean isPositive() {
            return rateOfReturn.signum() > 0;
        }
    }
}
