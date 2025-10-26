package com.iflash.core.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class SimpleOrderBookTest {

    @Test
    @DisplayName("Should correctly add sell Order if ticker is not exists")
    void shouldCorrectlyAddSellOrderIfTickerDoesNotExist() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);

        List<FinishedTransactionInfo> registeredTransactions = orderBook.registerOrder(registerOrderCommand)
                                                                        .finishedTransactionInfoList();
        Queue<Order> orderQueue = orderBook.getAsksOrderQueue(ticker);

        assertAll(() -> assertTrue(containsUuid(orderQueue, registeredTransactions)));
        OrderUtils.printOrders(orderBook, ticker);
    }

    @Test
    @DisplayName("Should correctly instant buy Order")
    void shouldCorrectlyInstantBuyOrder() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);

        List<FinishedTransactionInfo> registeredTransactions = orderBook.registerOrder(sellCommand)
                                                                        .finishedTransactionInfoList();
        Queue<Order> orderQueue = orderBook.getAsksOrderQueue(ticker);

        assertAll(() -> assertTrue(containsUuid(orderQueue, registeredTransactions)));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price, volume);
        List<FinishedTransactionInfo> boughtAlreadyOrders = orderBook.registerOrder(buyCommand)
                                                                     .finishedTransactionInfoList();

        assertAll(() -> assertNotNull(boughtAlreadyOrders.get(0)),
                  () -> assertEquals(0,
                                     orderBook.getAsksOrderQueue(ticker)
                                              .size()));

        OrderUtils.printOrders(orderBook, ticker);
    }

    @Test
    @DisplayName("Should reject sell order with exception if ticker not exists")
    void shouldRejectSellOrderWithExceptionIfTickerNotExists() {
        var notExistingTicker = "NOEX.IS";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, notExistingTicker, price, volume);

        assertThrows(OrderBookException.class, () -> orderBook.registerOrder(sellCommand));
    }

    @Test
    @DisplayName("Should reject sell order with exception if ticker exists but there are no active sell orders")
    void shouldRejectSellOrderWithExceptionIfTickerExistsButThereAreNoActiveSellOrders() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);

        List<FinishedTransactionInfo> registeredTransactions = orderBook.registerOrder(sellCommand)
                                                                        .finishedTransactionInfoList();
        Queue<Order> orderQueue = orderBook.getAsksOrderQueue(ticker);

        assertAll(() -> assertTrue(containsUuid(orderQueue, registeredTransactions)));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price, volume);
        orderBook.registerOrder(buyCommand);

        assertThrows(OrderBookException.class, () -> orderBook.registerOrder(buyCommand));
    }

    @Test
    @DisplayName("Should correctly buy positions from one sell order that is bigger than buy volume")
    void shouldCorrectlyBuyPositionsFromOneSellOrderThatIsBiggerThanBuyVolume() {
        var ticker = "NVDA.US";
        var volume = 100L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), volume));
        registerOrderCommands.forEach(orderBook::registerOrder);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, null, volume - 1);
        orderBook.registerOrder(buyCommand);

        assertAll(() -> assertEquals(1, orderBook.getAsksVolume(ticker)),
                  () -> assertEquals(1,
                                     orderBook.getAsksOrderQueue(ticker)
                                              .size()));
    }

    @Test
    @DisplayName("Should correctly return boolean value if volume is available or not")
    void shouldCorrectlyReturnBooleanValueIfVolumeIsAvailableOrNot() {
        var ticker = "NVDA.US";
        var tickerNotExisting = "NOEX.IS";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
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

        assertAll(() -> assertTrue(orderBook.isAsksVolumeAvailable(ticker, volume - 1)),
                  () -> assertTrue(orderBook.isAsksVolumeAvailable(ticker, volume)),
                  () -> assertFalse(orderBook.isAsksVolumeAvailable(ticker, volume + 1)),
                  () -> assertThrows(OrderBookException.class, () -> orderBook.isAsksVolumeAvailable(ticker, -1L)),
                  () -> assertThrows(OrderBookException.class, () -> orderBook.isAsksVolumeAvailable(ticker, 0L)),
                  () -> assertFalse(orderBook.isAsksVolumeAvailable(tickerNotExisting, 10L)));
    }

    @Test
    @DisplayName("Should correctly buy positions from many sell orders")
    void shouldCorrectlyBuyPositionsFromManySellOrders() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long volumeForSell = registerOrderCommands.stream()
                                                  .map(RegisterOrderCommand::volume)
                                                  .reduce(Long::sum)
                                                  .get();
        Long beforeBuyTransactionVolume = orderBook.getAsksVolume(ticker);
        assertEquals(volumeForSell, beforeBuyTransactionVolume);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, null, 6L);
        orderBook.registerOrder(buyCommand);

        OrderUtils.printOrders(orderBook, ticker);

        assertAll(() -> assertEquals(beforeBuyTransactionVolume - buyCommand.volume(), orderBook.getAsksVolume(ticker)),
                  () -> assertEquals(4,
                                     orderBook.getAsksOrderQueue(ticker)
                                              .size()),
                  () -> assertEquals(34,
                                     orderBook.getAsksOrderQueue(ticker)
                                              .peek()
                                              .getVolume()));
    }

    @Test
    @DisplayName("Should correctly buy all positions")
    void shouldCorrectlyBuyAllPositions() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long volumeForSell = registerOrderCommands.stream()
                                                  .map(RegisterOrderCommand::volume)
                                                  .reduce(Long::sum)
                                                  .get();
        Long beforeBuyTransactionVolume = orderBook.getAsksVolume(ticker);
        assertEquals(volumeForSell, beforeBuyTransactionVolume);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, null, volumeForSell);
        orderBook.registerOrder(buyCommand);

        OrderUtils.printOrders(orderBook, ticker);

        assertAll(() -> assertEquals(0, orderBook.getAsksVolume(ticker)),
                  () -> assertEquals(0,
                                     orderBook.getAsksOrderQueue(ticker)
                                              .size()),
                  () -> assertNull(orderBook.getAsksOrderQueue(ticker)
                                            .peek()));
    }

    @Test
    @DisplayName("Should not allow to selling if available volume is not enough")
    void shouldNotAllowToSellingIfAvailableVolumeIsNotEnough() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long volumeForSell = registerOrderCommands.stream()
                                                  .map(RegisterOrderCommand::volume)
                                                  .reduce(Long::sum)
                                                  .get();
        Long beforeBuyTransactionVolume = orderBook.getAsksVolume(ticker);
        assertEquals(volumeForSell, beforeBuyTransactionVolume);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, null, volumeForSell + 1);

        assertThrows(OrderBookException.class, () -> orderBook.registerOrder(buyCommand));
    }

    @Test
    @DisplayName("Should correctly process Partial fill order type")
    void shouldCorrectlyProcessPartialFillOrderType() {
        var ticker = "NVDA.US";

        Long missingLimitOrders = 10L;
        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook();
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long totalAvailableVolume = registerOrderCommands.stream()
                                                         .map(RegisterOrderCommand::volume)
                                                         .reduce(Long::sum)
                                                         .get();
        Long requestedVolumeHigherThanAvailable = totalAvailableVolume + missingLimitOrders;

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, null, requestedVolumeHigherThanAvailable);

        OrderRegistrationResult orderRegistrationResult = orderBook.registerOrder(buyCommand);

        assertAll(() -> assertEquals(0, orderBook.getAsksVolume(ticker)),
                  () -> assertEquals(requestedVolumeHigherThanAvailable, orderRegistrationResult.partialFillDetails().volumeRequested()),
                  () -> assertEquals(totalAvailableVolume, orderRegistrationResult.partialFillDetails().volumeFilled()),
                  () -> assertEquals(missingLimitOrders, orderRegistrationResult.partialFillDetails().volumePending()));
    }

    @Test
    @DisplayName("Should correctly process Partial fill order type and rest of order's volume should be put in bids queue")
    void shouldCorrectlyProcessPartialFillOrderTypeAndRestOfOrdersVolumeShouldBePutInBidsQueue() {

    }

    private boolean containsUuid(Queue<Order> orderQueue, List<FinishedTransactionInfo> registeredTransactions) {
        return orderQueue.stream()
                         .anyMatch(order -> order.getOrderUuid()
                                                 .equals(registeredTransactions.get(0)
                                                                               .orderUuid()));
    }
}