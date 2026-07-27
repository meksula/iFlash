package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.math.BigDecimal;

/**
 * Classic trend follower driven by a fast/slow simple-moving-average crossover. It buys the
 * "golden cross" (fast SMA crossing above slow) and sells the "death cross" (fast crossing below),
 * so it rides sustained moves and stays flat in choppy markets.
 */
public class TrendFollowingTrader extends TechnicalTrader {

    private static final int FAST_PERIOD = 5;
    private static final int SLOW_PERIOD = 20;

    public TrendFollowingTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        super(name, apiToolkit, initialCash);
    }

    @Override
    protected Signal evaluate(BarSeries series) {
        int last = series.getEndIndex();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator fast = new SMAIndicator(closePrice, FAST_PERIOD);
        SMAIndicator slow = new SMAIndicator(closePrice, SLOW_PERIOD);

        double fastNow = fast.getValue(last).doubleValue();
        double slowNow = slow.getValue(last).doubleValue();
        double fastPrev = fast.getValue(last - 1).doubleValue();
        double slowPrev = slow.getValue(last - 1).doubleValue();

        if (fastPrev <= slowPrev && fastNow > slowNow) {
            return Signal.BUY;
        }
        if (fastPrev >= slowPrev && fastNow < slowNow) {
            return Signal.SELL;
        }
        return Signal.HOLD;
    }

    @Override
    protected int requiredBars() {
        return SLOW_PERIOD + 1;
    }
}
