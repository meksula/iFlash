package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;

import java.util.List;
import java.util.Random;

/**
 * Pure noise taker. Each tick it fires a handful of random MARKET orders (random side, size and
 * instrument), consuming the makers' resting liquidity and nudging prices unpredictably. This is
 * the "monkey" pressure that, combined with {@link MarketMakerNoiseTrader} liquidity, keeps the
 * tape moving.
 */
public class RandomNoiseTrader implements Trader {

    private static final Random RANDOM = new Random();

    private static final int ORDERS_PER_TICK = 4;
    private static final int MAX_SIZE = 8;

    private final String name;
    private final ApiToolkit apiToolkit;

    public RandomNoiseTrader(String name, ApiToolkit apiToolkit) {
        this.name = name;
        this.apiToolkit = apiToolkit;
    }

    @Override
    public void decide() {
        ApiToolkit.ApiResponse<List<FinancialInstrumentInfo>> response = apiToolkit.getInstruments();
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null || response.getResponseBody().isEmpty()) {
            return;
        }
        List<FinancialInstrumentInfo> instruments = response.getResponseBody();
        for (int i = 0; i < ORDERS_PER_TICK; i++) {
            FinancialInstrumentInfo instrument = instruments.get(RANDOM.nextInt(instruments.size()));
            String side = RANDOM.nextBoolean() ? "BID" : "ASK";
            apiToolkit.placeMarketOrder(side, instrument.ticker(), 1 + RANDOM.nextInt(MAX_SIZE));
        }
    }
}
