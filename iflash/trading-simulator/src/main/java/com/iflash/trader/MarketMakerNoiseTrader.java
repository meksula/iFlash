package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * A noise market maker. Each tick it quotes a few random instruments on <b>both</b> sides with
 * LIMIT orders — providing the resting liquidity the engine otherwise lacks — and, with some
 * probability, crosses the spread with a MARKET order to force an actual transaction, which is
 * what moves the engine's weighted-average price. Over many ticks this yields a random walk.
 *
 * <p>Deliberately stateless (pure liquidity/noise). The engine is anonymous, so self-matching is
 * fine; quotes stay well inside the engine's ±15% price corridor.
 */
public class MarketMakerNoiseTrader implements Trader {

    private static final Random RANDOM = new Random();

    private static final int INSTRUMENTS_PER_TICK = 3;
    private static final int MAX_QUOTE_SIZE = 25;
    private static final double MIN_SPREAD = 0.004; // 0.4%
    private static final double MAX_SPREAD = 0.018; // 1.8% — safely within the engine's ±15% corridor
    private static final double CROSS_PROBABILITY = 0.6;
    private static final int MAX_CROSS_SIZE = 6;

    private final String name;
    private final ApiToolkit apiToolkit;

    public MarketMakerNoiseTrader(String name, ApiToolkit apiToolkit) {
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
        for (int i = 0; i < INSTRUMENTS_PER_TICK; i++) {
            quote(instruments.get(RANDOM.nextInt(instruments.size())));
        }
    }

    private void quote(FinancialInstrumentInfo instrument) {
        BigDecimal price = instrument.currentPrice();
        if (price == null || price.signum() <= 0) {
            return;
        }
        double mid = price.doubleValue();
        double spread = MIN_SPREAD + RANDOM.nextDouble() * (MAX_SPREAD - MIN_SPREAD);
        double bid = round2(mid * (1.0 - spread));
        double ask = round2(mid * (1.0 + spread));
        String ticker = instrument.ticker();

        // Rest passive liquidity on both sides.
        apiToolkit.placeLimitOrder("BID", ticker, 1 + RANDOM.nextInt(MAX_QUOTE_SIZE), bid);
        apiToolkit.placeLimitOrder("ASK", ticker, 1 + RANDOM.nextInt(MAX_QUOTE_SIZE), ask);

        // Occasionally take liquidity to generate a real transaction -> the price moves.
        if (RANDOM.nextDouble() < CROSS_PROBABILITY) {
            String side = RANDOM.nextBoolean() ? "BID" : "ASK";
            apiToolkit.placeMarketOrder(side, ticker, 1 + RANDOM.nextInt(MAX_CROSS_SIZE));
        }
        System.out.println("[" + name + "] quoted " + ticker + " bid=" + bid + " ask=" + ask);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
