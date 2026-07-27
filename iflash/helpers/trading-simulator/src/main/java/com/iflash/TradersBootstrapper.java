package com.iflash;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.trader.BreakoutTrader;
import com.iflash.trader.LongTermMonkeyTrader;
import com.iflash.trader.MarketMakerNoiseTrader;
import com.iflash.trader.MeanReversionTrader;
import com.iflash.trader.RandomNoiseTrader;
import com.iflash.trader.Trader;
import com.iflash.trader.TrendFollowingTrader;
import com.iflash.trader.WorldDumbestTrader;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Assembles the trader swarm. The mix is chosen to resemble a real market ecosystem:
 * <ul>
 *   <li><b>Market makers</b> rest two-sided liquidity so there is always something to trade against.</li>
 *   <li><b>Noise takers</b> add unpredictable market-order pressure — the random walk.</li>
 *   <li><b>Buy-and-hold investors</b> slowly accumulate and only sell on a comfortable profit.</li>
 *   <li><b>Technical traders</b> ("smart money") react to price patterns: trend followers chase
 *       momentum, mean-reversion traders fade extremes, breakout traders play volatility.</li>
 * </ul>
 */
public class TradersBootstrapper {

    private static final BigDecimal RETAIL_CASH = BigDecimal.valueOf(100_000.00);
    private static final BigDecimal SMART_MONEY_CASH = BigDecimal.valueOf(250_000.00);
    private static final Currency USD = Currency.getInstance("USD");

    private final ApiToolkit apiToolkit;

    public TradersBootstrapper(ApiToolkit apiToolkit) {
        this.apiToolkit = apiToolkit;
    }

    public Set<Trader> bootstrap() {
        Set<Trader> traderSet = new HashSet<>();

        // Buy-and-hold takers.
        traderSet.add(new LongTermMonkeyTrader("Top Broker", "John", "Doe", apiToolkit, RETAIL_CASH, USD, new HashMap<>()));
        traderSet.add(new LongTermMonkeyTrader("Top Broker", "Kevin", "MacAllister", apiToolkit, RETAIL_CASH, USD, new HashMap<>()));
        traderSet.add(new LongTermMonkeyTrader("Top Broker", "Amy", "Whittaker", apiToolkit, RETAIL_CASH, USD, new HashMap<>()));

        // Market-noise layer: makers provide two-sided LIMIT liquidity and cross the spread,
        // noise takers add unpredictable MARKET pressure — together they keep the price moving.
        for (int i = 1; i <= 5; i++) {
            traderSet.add(new MarketMakerNoiseTrader("MM-" + i, apiToolkit));
        }
        for (int i = 1; i <= 3; i++) {
            traderSet.add(new RandomNoiseTrader("Noise-" + i, apiToolkit));
        }

        // Smart-money layer: technical-analysis strategies reacting to the tape produced above.
        for (int i = 1; i <= 3; i++) {
            traderSet.add(new TrendFollowingTrader("Trend-" + i, apiToolkit, SMART_MONEY_CASH));
        }
        for (int i = 1; i <= 3; i++) {
            traderSet.add(new MeanReversionTrader("MeanRev-" + i, apiToolkit, SMART_MONEY_CASH));
        }
        for (int i = 1; i <= 2; i++) {
            traderSet.add(new BreakoutTrader("Breakout-" + i, apiToolkit, SMART_MONEY_CASH));
        }

        // Every market needs someone on the losing side. :)
        traderSet.add(new WorldDumbestTrader("Dumbest", apiToolkit, RETAIL_CASH));

        return traderSet;
    }

}
