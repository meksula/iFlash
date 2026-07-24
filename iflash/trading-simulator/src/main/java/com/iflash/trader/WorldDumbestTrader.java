package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.math.BigDecimal;

/**
 * The world's dumbest trader. Its entire strategy is the single most reliable way to lose money:
 * <b>buy high, sell low</b>. If the last tick went up it panic-buys (FOMO); if it went down it
 * panic-sells at the bottom. It is the perfect counterparty — someone has to be on the losing side
 * of the smart-money trades, and that someone is this guy. :)
 */
public class WorldDumbestTrader extends TechnicalTrader {

    public WorldDumbestTrader(String name, ApiToolkit apiToolkit, BigDecimal initialCash) {
        super(name, apiToolkit, initialCash);
    }

    @Override
    protected Signal evaluate(BarSeries series) {
        int last = series.getEndIndex();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        double now = closePrice.getValue(last).doubleValue();
        double previous = closePrice.getValue(last - 1).doubleValue();

        if (now > previous) {
            return Signal.BUY;  // it's going up, must buy the top!
        }
        if (now < previous) {
            return Signal.SELL; // it's going down, dump everything at the bottom!
        }
        return Signal.HOLD;
    }

    @Override
    protected int requiredBars() {
        return 2;
    }
}
