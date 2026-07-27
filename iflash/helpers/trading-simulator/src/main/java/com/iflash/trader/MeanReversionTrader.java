package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;

/**
 * Contrarian mean-reversion trader driven by the Relative Strength Index. It buys when the market
 * is oversold (RSI below {@value #OVERSOLD}) and sells when it is overbought (RSI above
 * {@value #OVERBOUGHT}), betting that stretched prices snap back toward the mean.
 */
public class MeanReversionTrader extends TechnicalTrader {

    private static final int RSI_PERIOD = 14;
    private static final double OVERSOLD = 30.0;
    private static final double OVERBOUGHT = 70.0;

    public MeanReversionTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        super(name, apiToolkit, initialCash);
    }

    @Override
    protected Signal evaluate(BarSeries series) {
        int last = series.getEndIndex();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, RSI_PERIOD);

        Num value = rsi.getValue(last);
        if (value.isNaN()) {
            return Signal.HOLD;
        }
        double rsiValue = value.doubleValue();

        if (rsiValue < OVERSOLD) {
            return Signal.BUY;
        }
        if (rsiValue > OVERBOUGHT) {
            return Signal.SELL;
        }
        return Signal.HOLD;
    }

    @Override
    protected int requiredBars() {
        return RSI_PERIOD + 1;
    }
}
