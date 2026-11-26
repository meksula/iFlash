package com.iflash.core.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderUtils {

    private static final Logger log = LoggerFactory.getLogger(SimpleOrderBook.class);

    public static void printOrders(SimpleOrderBook orderBook, String ticker) {
        log.debug("=== SELL ORDERS BEGIN ===");
        orderBook.getAsksOrderQueue(ticker).forEach(order -> log.debug(order.toString()));
        log.debug("=== SELL ORDERS END ===");
    }
}
