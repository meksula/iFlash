package com.iflash.engine.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    @Test
    @DisplayName("Should correctly add sell Order if ticker is not exists")
    void shouldCorrectlyAddSellOrderIfTickerDoesNotExist() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);

        OrderBook orderBook = new OrderBook();
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price);

        Order order = orderBook.registerOrder(registerOrderCommand);
        Queue<Order> orderQueue = orderBook.getOrderQueue(ticker);

        assertAll(() -> assertTrue(orderQueue.contains(order)));
        OrderUtils.printOrders(orderBook, ticker);
    }

    @Test
    @DisplayName("Should correctly instant buy Order")
    void shouldCorrectlyInstantBuyOrder() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);

        OrderBook orderBook = new OrderBook();
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price);

        Order order = orderBook.registerOrder(sellCommand);
        Queue<Order> orderQueue = orderBook.getOrderQueue(ticker);

        assertAll(() -> assertTrue(orderQueue.contains(order)));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price);
        Order boughtAlreadyOrder = orderBook.registerOrder(buyCommand);

        assertAll(() -> assertNotNull(boughtAlreadyOrder),
                  () -> assertEquals(0, orderBook.getOrderQueue(ticker).size()));

        OrderUtils.printOrders(orderBook, ticker);
    }

}