package com.iflash.core.order;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class OrderBookFactory {

    public static OrderBook factorizeOrderBook() {
        Map<String, Queue<Order>> sellOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();
        return new SimpleOrderBook(sellOrdersByTicker, bidsOrdersByTicker);
    }
}
