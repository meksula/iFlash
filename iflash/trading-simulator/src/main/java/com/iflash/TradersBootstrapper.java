package com.iflash;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.trader.LongTermMonkeyTrader;
import com.iflash.trader.MarketMakerNoiseTrader;
import com.iflash.trader.RandomNoiseTrader;
import com.iflash.trader.Trader;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TradersBootstrapper {

    private final ApiToolkit apiToolkit;

    public TradersBootstrapper(ApiToolkit apiToolkit) {
        this.apiToolkit = apiToolkit;
    }

    public Set<Trader> bootstrap() {
        Set<Trader> traderSet = new HashSet<>();

        // Buy-and-hold takers.
        traderSet.add(new LongTermMonkeyTrader("Top Broker", "John", "Doe", apiToolkit, BigDecimal.valueOf(100000.00), Currency.getInstance("USD"), new HashMap<>()));
        traderSet.add(new LongTermMonkeyTrader("Top Broker", "Kevin", "MacAllister", apiToolkit, BigDecimal.valueOf(100000.00), Currency.getInstance("USD"), new HashMap<>()));
        traderSet.add(new LongTermMonkeyTrader("Top Broker", "Amy", "Whittaker", apiToolkit, BigDecimal.valueOf(100000.00), Currency.getInstance("USD"), new HashMap<>()));

        // Market-noise layer: makers provide two-sided LIMIT liquidity and cross the spread,
        // noise takers add unpredictable MARKET pressure — together they keep the price moving.
        for (int i = 1; i <= 5; i++) {
            traderSet.add(new MarketMakerNoiseTrader("MM-" + i, apiToolkit));
        }
        for (int i = 1; i <= 3; i++) {
            traderSet.add(new RandomNoiseTrader("Noise-" + i, apiToolkit));
        }

        return traderSet;
    }

}
