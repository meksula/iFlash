package com.iflash.core.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderUtils {

    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);

    public static void printOrders(OrderBook orderBook, String ticker) {
        log.info("=== SELL ORDERS BEGIN ===");
        orderBook.getOrderQueue(ticker).forEach(order -> log.info(order.toString()));
        log.info("=== SELL ORDERS END ===");
    }
}
