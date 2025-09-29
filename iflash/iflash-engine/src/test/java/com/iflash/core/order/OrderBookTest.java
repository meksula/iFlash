package com.iflash.core.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private static final Random RANDOM = new Random();

    @Test
    @DisplayName("Should correctly add sell Order if ticker is not exists")
    void shouldCorrectlyAddSellOrderIfTickerDoesNotExist() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        OrderBook orderBook = new OrderBook();
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);

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
        var volume = 1L;

        OrderBook orderBook = new OrderBook();
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);

        Order order = orderBook.registerOrder(sellCommand);
        Queue<Order> orderQueue = orderBook.getOrderQueue(ticker);

        assertAll(() -> assertTrue(orderQueue.contains(order)));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price, volume);
        Order boughtAlreadyOrder = orderBook.registerOrder(buyCommand);

        assertAll(() -> assertNotNull(boughtAlreadyOrder),
                  () -> assertEquals(0, orderBook.getOrderQueue(ticker).size()));

        OrderUtils.printOrders(orderBook, ticker);
    }

    @Test
    @DisplayName("Should reject sell order with exception if ticker not exists")
    void shouldRejectSellOrderWithExceptionIfTickerNotExists() {
        var notExistingTicker = "DUPA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        OrderBook orderBook = new OrderBook();
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, notExistingTicker, price, volume);

        assertThrows(OrderBookException.class, () -> orderBook.registerOrder(sellCommand));
    }

    @Test
    @DisplayName("Should reject sell order with exception if ticker exists but there are no active sell orders")
    void shouldRejectSellOrderWithExceptionIfTickerExistsButThereAreNoActiveSellOrders() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        OrderBook orderBook = new OrderBook();
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);

        Order order = orderBook.registerOrder(sellCommand);
        Queue<Order> orderQueue = orderBook.getOrderQueue(ticker);

        assertAll(() -> assertTrue(orderQueue.contains(order)));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price, volume);
        orderBook.registerOrder(buyCommand);

        assertThrows(OrderBookException.class, () -> orderBook.registerOrder(buyCommand));
    }

    @Test
    @DisplayName("Should ensure orders idempotency - cannot register multiple orders with the same uuid")
    void shouldEnsureOrdersIdempotencyCannotRegisterMultipleOrdersWithTheSameUuid() {
        // todo
    }

    @Test
    @DisplayName("Should correctly buy positions from one sell order that is bigger than buy volume")
    void shouldCorrectlyBuyPositionsFromOneSellOrderThatIsBiggerThanBuyVolume() {

    }

    @Test
    @DisplayName("Should correctly return boolean value if volume is available or not")
    void shouldCorrectlyReturnBooleanValueIfVolumeIsAvailableOrNot() {
        var ticker = "NVDA.US";
        var tickerNotExisting = "NOEX.IS";

        OrderBook orderBook = new OrderBook();
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long volume = registerOrderCommands.stream()
                                      .map(RegisterOrderCommand::volume)
                                      .reduce(Long::sum)
                                      .get();

        assertAll(() -> assertTrue(orderBook.isVolumeAvailable(ticker, volume - 1)),
                  () -> assertTrue(orderBook.isVolumeAvailable(ticker, volume)),
                  () -> assertFalse(orderBook.isVolumeAvailable(ticker, volume + 1)),
                  () -> assertThrows(OrderBookException.class, () -> orderBook.isVolumeAvailable(ticker, -1L)),
                  () -> assertThrows(OrderBookException.class, () -> orderBook.isVolumeAvailable(ticker, 0L)),
                  () -> assertFalse(orderBook.isVolumeAvailable(tickerNotExisting, 10L)));
    }

    @Test
    @DisplayName("Should correctly buy positions from many sell orders")
    void shouldCorrectlyBuyPositionsFromManySellOrders() {
        var ticker = "NVDA.US";

        OrderBook orderBook = new OrderBook();
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, null, 50L);

    }


}