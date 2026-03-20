package com.iflash.core.order;

import com.iflash.commons.OrderBy;
import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class SimpleOrderBookTest {

    private final QuotationProvider quotationProvider = Mockito.mock(QuotationProvider.class);

    @BeforeEach
    void setUp() {
        Mockito.when(quotationProvider.getCurrentQuote(any())).thenReturn(new CurrentQuotation(System.currentTimeMillis(), BigDecimal.valueOf(171.1243)));
    }

    @Test
    @DisplayName("Should correctly add sell Order if ticker is not exists, if bids queue is empty we treat MARKET sell as LIMIT and put it on queue")
    void shouldCorrectlyAddSellOrderIfTickerDoesNotExist() {
        var ticker = "NVDA.US";
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, null, volume);

        List<FinishedTransactionInfo> finishedTransactions = orderBook.registerOrder(registerOrderCommand)
                                                                        .finishedTransactionInfoList();
        Queue<Order> orderQueue = orderBook.getAsksOrderQueue(ticker);

        assertAll(() -> assertEquals(0, finishedTransactions.size()),
                  () -> assertEquals(1, orderQueue.size()));
        OrderUtils.printOrders(orderBook, ticker);
    }

    @Test
    @DisplayName("Should correctly process ASK order and instant BID order after that")
    void shouldCorrectlyInstantBuyOrder() {
        var ticker = "NVDA.US";
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, null, volume);

        List<FinishedTransactionInfo> finishedTransactions = orderBook.registerOrder(sellCommand)
                                                                      .finishedTransactionInfoList();
        Queue<Order> orderQueue = orderBook.getAsksOrderQueue(ticker);

        assertAll(() -> assertEquals(0, finishedTransactions.size()),
                  () -> assertEquals(1, orderQueue.size()));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, volume);
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

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, notExistingTicker, price, volume);

        assertThrows(OrderBookException.class, () -> orderBook.registerOrder(sellCommand));
    }

    @Test
    @DisplayName("Should not reject buy order with exception if ticker exists but there are no active sell orders - buy order should be placed on bids queue")
    void shouldRejectSellOrderWithExceptionIfTickerExistsButThereAreNoActiveSellOrders() {
        var ticker = "NVDA.US";
        var volume = 1L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        RegisterOrderCommand sellCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, null, volume);

        List<FinishedTransactionInfo> finishedTransactions = orderBook.registerOrder(sellCommand)
                                                                        .finishedTransactionInfoList();
        Queue<Order> orderQueue = orderBook.getAsksOrderQueue(ticker);

        assertAll(() -> assertEquals(0, finishedTransactions.size()),
                  () -> assertEquals(1, orderQueue.size()));
        OrderUtils.printOrders(orderBook, ticker);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, volume);
        orderBook.registerOrder(buyCommand);

        assertDoesNotThrow(() -> orderBook.registerOrder(buyCommand));
    }

    @Test
    @DisplayName("Should correctly buy positions from one sell order that is bigger than buy volume")
    void shouldCorrectlyBuyPositionsFromOneSellOrderThatIsBiggerThanBuyVolume() {
        var ticker = "NVDA.US";
        var volume = 100L;

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), volume));
        registerOrderCommands.forEach(orderBook::registerOrder);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, volume - 1);
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

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
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

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long volumeForSell = registerOrderCommands.stream()
                                                  .map(RegisterOrderCommand::volume)
                                                  .reduce(Long::sum)
                                                  .get();
        Long beforeBuyTransactionVolume = orderBook.getAsksVolume(ticker);
        assertEquals(volumeForSell, beforeBuyTransactionVolume);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, 6L);
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

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long volumeForSell = registerOrderCommands.stream()
                                                  .map(RegisterOrderCommand::volume)
                                                  .reduce(Long::sum)
                                                  .get();
        Long beforeBuyTransactionVolume = orderBook.getAsksVolume(ticker);
        assertEquals(volumeForSell, beforeBuyTransactionVolume);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, volumeForSell);
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
    @DisplayName("Should correctly process Partial fill order type")
    void shouldCorrectlyProcessPartialFillOrderType() {
        var ticker = "NVDA.US";

        Long missingLimitOrders = 10L;
        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long totalAvailableVolume = registerOrderCommands.stream()
                                                         .map(RegisterOrderCommand::volume)
                                                         .reduce(Long::sum)
                                                         .get();
        Long requestedVolumeHigherThanAvailable = totalAvailableVolume + missingLimitOrders;

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, requestedVolumeHigherThanAvailable);

        OrderRegistrationResult orderRegistrationResult = orderBook.registerOrder(buyCommand);

        assertAll(() -> assertEquals(0, orderBook.getAsksVolume(ticker)),
                  () -> assertEquals(requestedVolumeHigherThanAvailable, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(totalAvailableVolume, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(missingLimitOrders, orderRegistrationResult.orderFillDetails().volumePending()));
    }

    @Test
    @DisplayName("Should correctly process Partial fill order type and rest of order's volume should be put in bids queue")
    void shouldCorrectlyProcessPartialFillOrderTypeAndRestOfOrdersVolumeShouldBePutInBidsQueue() {
        var ticker = "NVDA.US";

        Long missingLimitOrders = 10L;
        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Long totalAvailableVolume = registerOrderCommands.stream()
                                                         .map(RegisterOrderCommand::volume)
                                                         .reduce(Long::sum)
                                                         .get();
        Long requestedVolumeHigherThanAvailable = totalAvailableVolume + missingLimitOrders;

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.MARKET, ticker, null, requestedVolumeHigherThanAvailable);

        OrderRegistrationResult orderRegistrationResult = orderBook.registerOrder(buyCommand);

        assertAll(() -> assertEquals(0, orderBook.getAsksVolume(ticker)),
                  () -> assertEquals(requestedVolumeHigherThanAvailable, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(totalAvailableVolume, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(missingLimitOrders, orderRegistrationResult.orderFillDetails().volumePending()));
    }

    @Test
    @DisplayName("Should correctly paginate order book snapshot for ASC order")
    void shouldCorrectlyPaginateOrderBookSnapshotForAscOrder() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Pagination firstPage = new Pagination(0, 2, OrderBy.ASC);
        Pagination secondPage = new Pagination(1, 2, OrderBy.ASC);

        Page<OrderInformation> firstPageSnapshot = orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, firstPage);
        Page<OrderInformation> secondPageSnapshot = orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, secondPage);

        assertAll(() -> assertEquals(2, firstPageSnapshot.getElements().size()),
                  () -> assertEquals(BigDecimal.valueOf(171.1442), firstPageSnapshot.getElements().get(0).price()),
                  () -> assertEquals(BigDecimal.valueOf(171.3248), firstPageSnapshot.getElements().get(1).price()),
                  () -> assertEquals(2, secondPageSnapshot.getElements().size()),
                  () -> assertEquals(BigDecimal.valueOf(171.7202), secondPageSnapshot.getElements().get(0).price()),
                  () -> assertEquals(BigDecimal.valueOf(171.8431), secondPageSnapshot.getElements().get(1).price()));
    }

    @Test
    @DisplayName("Should correctly paginate order book snapshot for DESC order")
    void shouldCorrectlyPaginateOrderBookSnapshotForDescOrder() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Pagination firstPageDesc = new Pagination(0, 2, OrderBy.DESC);
        Pagination outOfRangePageDesc = new Pagination(3, 2, OrderBy.DESC);

        Page<OrderInformation> firstPageSnapshotDesc = orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, firstPageDesc);
        Page<OrderInformation> outOfRangeSnapshotDesc = orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, outOfRangePageDesc);

        assertAll(() -> assertEquals(2, firstPageSnapshotDesc.getElements().size()),
                  () -> assertEquals(BigDecimal.valueOf(171.9733), firstPageSnapshotDesc.getElements().get(0).price()),
                  () -> assertEquals(BigDecimal.valueOf(171.8431), firstPageSnapshotDesc.getElements().get(1).price()),
                  () -> assertEquals(0, outOfRangeSnapshotDesc.getElements().size()));
    }

    @Test
    @DisplayName("Should reject order book snapshot request if pagination values are invalid")
    void shouldRejectOrderBookSnapshotRequestIfPaginationValuesAreInvalid() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        orderBook.registerOrder(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L));

        Pagination zeroSizePagination = new Pagination(0, 0, OrderBy.ASC);
        Pagination negativePagePagination = new Pagination(-1, 1, OrderBy.ASC);

        assertAll(() -> assertThrows(IllegalStateException.class, () -> orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, zeroSizePagination)),
                  () -> assertThrows(IllegalStateException.class, () -> orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, negativePagePagination)));
    }

    @Test
    @DisplayName("Should correctly apply pagination offset for order book snapshot")
    void shouldCorrectlyApplyPaginationOffsetForOrderBookSnapshot() {
        var ticker = "NVDA.US";

        SimpleOrderBook orderBook = (SimpleOrderBook) OrderBookFactory.factorizeOrderBook(quotationProvider);
        orderBook.registerTicker(ticker);
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), 10L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.7202), 25L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.1442), 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.8431), 30L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.MARKET, ticker, BigDecimal.valueOf(171.3248), 35L));
        registerOrderCommands.forEach(orderBook::registerOrder);

        Pagination paginationWithOffset = new Pagination(1, 2, OrderBy.ASC);
        Page<OrderInformation> snapshotWithOffset = orderBook.getOrderBookSnapshot(ticker, OrderDirection.ASK, paginationWithOffset);

        assertAll(() -> assertEquals(2, snapshotWithOffset.getElements().size()),
                  () -> assertEquals(BigDecimal.valueOf(171.7202), snapshotWithOffset.getElements().get(0).price()),
                  () -> assertEquals(BigDecimal.valueOf(171.8431), snapshotWithOffset.getElements().get(1).price()));
    }
}