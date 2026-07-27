package com.iflash.brokerplatform.trading;

import java.util.List;

/**
 * Mirrors the engine's {@code OrderType} / {@code OrderDirection} enums (the engine module
 * is not a dependency). Directions use the engine's convention: BID = buy, ASK = sell.
 */
public final class OrderCatalog {

    public static final List<String> ORDER_TYPES =
            List.of("MARKET", "LIMIT", "STOP", "STOP_LIMIT", "ICEBERG", "FOK", "IOC", "GTC", "GTD", "AON");

    public record Direction(String code, String label) {
    }

    public static final List<Direction> DIRECTIONS =
            List.of(new Direction("BID", "Buy"), new Direction("ASK", "Sell"));

    private OrderCatalog() {
    }
}
