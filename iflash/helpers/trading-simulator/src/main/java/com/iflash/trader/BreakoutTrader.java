package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;

/**
 * Volatility breakout trader built on Bollinger Bands. It buys when price pushes above the upper
 * band (an upside breakout worth riding) and sells when price breaks below the lower band (a
 * breakdown worth exiting). Together with the mean-reversion trader — which reads the same
 * stretched moves the opposite way — it makes the swarm's reaction to volatility two-sided.
 */
public class BreakoutTrader extends TechnicalTrader {

    private static final int PERIOD = 20;
    private static final int DEVIATION_MULTIPLIER = 2;

    public BreakoutTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        super(name, apiToolkit, initialCash);
    }

    @Override
    protected Signal evaluate(BarSeries series) {
        int last = series.getEndIndex();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, PERIOD);
        StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, PERIOD);

        BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(sma);
        Num multiplier = series.numOf(DEVIATION_MULTIPLIER);
        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, standardDeviation, multiplier);
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, standardDeviation, multiplier);

        double price = closePrice.getValue(last).doubleValue();
        if (price > upper.getValue(last).doubleValue()) {
            return Signal.BUY;
        }
        if (price < lower.getValue(last).doubleValue()) {
            return Signal.SELL;
        }
        return Signal.HOLD;
    }

    @Override
    protected int requiredBars() {
        return PERIOD + 1;
    }
}
