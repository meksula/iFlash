package com.iflash;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.trader.RandomLongTermTrader;
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
        Trader trader = new RandomLongTermTrader("Top Broker", "John", "Doe", apiToolkit, BigDecimal.valueOf(100000.00), Currency.getInstance("USD"), new HashMap<>());
        traderSet.add(trader);
        return traderSet;
    }

}
