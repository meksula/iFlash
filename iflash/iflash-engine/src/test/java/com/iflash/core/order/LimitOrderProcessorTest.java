package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class LimitOrderProcessorTest {

    private final QuotationProvider quotationProvider = Mockito.mock(QuotationProvider.class);

    @BeforeEach
    void setUp() {
        Mockito.when(quotationProvider.getCurrentQuote(any())).thenReturn(new CurrentQuotation(System.currentTimeMillis(), BigDecimal.valueOf(171.1243)));
    }

    @Test
    @DisplayName("Should correctly add Buy Limit order and place it in bids queue when volume is not available in required price")
    void shouldCorrectlyAddBuyLimitOrderAndPlaceItInBidsQueue() {
        var ticker = "NVDA.US";

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);
        LimitOrderProcessor limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);

        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, BigDecimal.valueOf(171.9733), 10L);

        OrderRegistrationResult orderRegistrationResult = limitOrderProcessor.processLimitOrder(registerOrderCommand);

        assertAll(() -> assertEquals(1, simpleOrderBook.getBidsOrderQueue(ticker).size()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, orderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.IDLING_ON_QUEUE, orderRegistrationResult.transactionPhase()),
                  () -> assertTrue(orderRegistrationResult.finishedTransactionInfoList().isEmpty()),
                  () -> assertNull(orderRegistrationResult.errorMessage()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(0, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Limit order registered successfully", orderRegistrationResult.orderFillDetails().message()));
    }

    @Test
    @DisplayName("Should correctly add Sell Limit order and place it in asks queue when volume is not available in required price")
    void shouldCorrectlyAddBuyLimitOrderAndPlaceItInAsksQueue() {
        var ticker = "NVDA.US";

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);
        LimitOrderProcessor limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);

        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, BigDecimal.valueOf(171.9733), 10L);

        OrderRegistrationResult orderRegistrationResult = limitOrderProcessor.processLimitOrder(registerOrderCommand);

        assertAll(() -> assertEquals(1, simpleOrderBook.getAsksOrderQueue(ticker).size()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, orderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.IDLING_ON_QUEUE, orderRegistrationResult.transactionPhase()),
                  () -> assertTrue(orderRegistrationResult.finishedTransactionInfoList().isEmpty()),
                  () -> assertNull(orderRegistrationResult.errorMessage()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(0, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Limit order registered successfully", orderRegistrationResult.orderFillDetails().message()));
    }

    @Test
    @DisplayName("Should correctly fully complete Buy Limit order when there are available volume with acceptable price in Asks Queue")
    void shouldCorrectlyFullyCompleteBuyLimitOrderWhenThereAreAvailableVolumeWithAcceptablePrice() {
        var ticker = "NVDA.US";
        var buyLimit = BigDecimal.valueOf(171.9900);
        var sellLimit = BigDecimal.valueOf(171.9800);
        var volume = 10L;

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);
        LimitOrderProcessor limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);

        // Sell at a price higher than sellLimit
        RegisterOrderCommand sellRegisterOrderCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, volume);
        limitOrderProcessor.processLimitOrder(sellRegisterOrderCommand);

        // Buy at a price lower or equal to buy limit
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, volume);
        OrderRegistrationResult orderRegistrationResult = limitOrderProcessor.processLimitOrder(registerOrderCommand);

        assertAll(() -> assertTrue(simpleOrderBook.getAsksOrderQueue(ticker).isEmpty()),
                  () -> assertTrue(simpleOrderBook.getBidsOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, orderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.FULLY_COMPLETED, orderRegistrationResult.transactionPhase()),
                  () -> assertEquals(1, orderRegistrationResult.finishedTransactionInfoList().size()),
                  () -> assertNull(orderRegistrationResult.errorMessage()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(0, orderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Limit order completed successfully", orderRegistrationResult.orderFillDetails().message()));
    }

    @Test
    @DisplayName("Should correctly fully complete Buy Limit order when there are available volume with acceptable price in Asks Queue - but there are more Orders")
    void shouldCorrectlyFullyCompleteBuyLimitOrderWhenThereAreAvailableVolumeWithAcceptablePriceButThereAreMoreOrders() {
        var ticker = "NVDA.US";
        var buyLimit = BigDecimal.valueOf(171.9900);
        var sellLimit = BigDecimal.valueOf(171.9800);
        var volume = 10L;

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);
        LimitOrderProcessor limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);

        // Sell at a price higher than sellLimit
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 2L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 3L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 4L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 5L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 7L));
        registerOrderCommands.forEach(limitOrderProcessor::processLimitOrder);

        // Buy at a price lower or equal to buy limit
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, volume);
        OrderRegistrationResult orderRegistrationResult = limitOrderProcessor.processLimitOrder(registerOrderCommand);

        assertAll(() -> assertFalse(simpleOrderBook.getAsksOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(3, simpleOrderBook.getAsksOrderQueue(ticker).size()),
                  () -> assertTrue(simpleOrderBook.getBidsOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, orderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.FULLY_COMPLETED, orderRegistrationResult.transactionPhase()),
                  () -> assertEquals(3, orderRegistrationResult.finishedTransactionInfoList().size()),
                  () -> assertNull(orderRegistrationResult.errorMessage()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(10, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(0, orderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Limit order completed successfully", orderRegistrationResult.orderFillDetails().message()));
    }

    @Test
    @DisplayName("Should correctly fully complete Sell Limit order when there are available volume with acceptable price in Bids Queue")
    void shouldCorrectlyFullyCompleteSellLimitOrderWhenThereAreAvailableVolumeWithAcceptablePrice() {
        var ticker = "NVDA.US";
        var buyLimit = BigDecimal.valueOf(171.9900);
        var sellLimit = BigDecimal.valueOf(171.9800);
        var volume = 10L;

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);
        LimitOrderProcessor limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);

        // Buy Limit at a price higher than sellLimit
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, 2L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, 3L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, 4L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, 5L),
                                                                   new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, 7L));
        registerOrderCommands.forEach(limitOrderProcessor::processLimitOrder);

        // Sell at a price higher or equal to buy limit
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, volume/2);
        RegisterOrderCommand nextSameRegisterOrderCommand = new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, volume/2);
        OrderRegistrationResult orderRegistrationResult = limitOrderProcessor.processLimitOrder(registerOrderCommand);
        OrderRegistrationResult nextSameOrderRegistrationResult = limitOrderProcessor.processLimitOrder(nextSameRegisterOrderCommand);

        assertAll(() -> assertTrue(simpleOrderBook.getAsksOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(2, simpleOrderBook.getBidsOrderQueue(ticker).size()),
                  () -> assertFalse(simpleOrderBook.getBidsOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, orderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.FULLY_COMPLETED, orderRegistrationResult.transactionPhase()),
                  () -> assertEquals(2, orderRegistrationResult.finishedTransactionInfoList().size()),
                  () -> assertNull(orderRegistrationResult.errorMessage()),
                  () -> assertEquals(5, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(5, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(0, orderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Limit order completed successfully", nextSameOrderRegistrationResult.orderFillDetails().message()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, nextSameOrderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.FULLY_COMPLETED, nextSameOrderRegistrationResult.transactionPhase()),
                  () -> assertEquals(1, nextSameOrderRegistrationResult.finishedTransactionInfoList().size()),
                  () -> assertNull(nextSameOrderRegistrationResult.errorMessage()),
                  () -> assertEquals(5, nextSameOrderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(5, nextSameOrderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(0, nextSameOrderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Limit order completed successfully", nextSameOrderRegistrationResult.orderFillDetails().message()));
    }

    @Test
    @DisplayName("Should correctly partially complete Buy Limit order when there are partially available volume with acceptable price in Asks Queue")
    void shouldCorrectlyPartiallyCompleteBuyLimitOrderWhenThereArePartiallyAvailableVolumeWithAcceptablePriceInAsksQueue() {
        var ticker = "NVDA.US";
        var buyLimit = BigDecimal.valueOf(171.9900);
        var sellLimit = BigDecimal.valueOf(171.9800);

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);
        LimitOrderProcessor limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);

        // Sell at a price higher than sellLimit
        List<RegisterOrderCommand> registerOrderCommands = List.of(new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 2L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 3L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 4L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 5L),
                                                                   new RegisterOrderCommand(OrderDirection.BID, OrderType.LIMIT, ticker, sellLimit, 7L));
        registerOrderCommands.forEach(limitOrderProcessor::processLimitOrder);

        long availableVolume = registerOrderCommands.stream()
                                                    .map(RegisterOrderCommand::volume)
                                                    .reduce(Long::sum)
                                                    .get();
        long volumeRequested = availableVolume + 1; // volume higher than available

        // Buy at a price lower or equal to buy limit
        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.ASK, OrderType.LIMIT, ticker, buyLimit, volumeRequested);
        OrderRegistrationResult orderRegistrationResult = limitOrderProcessor.processLimitOrder(registerOrderCommand);

        assertAll(() -> assertTrue(simpleOrderBook.getAsksOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(0, simpleOrderBook.getAsksOrderQueue(ticker).size()),
                  () -> assertFalse(simpleOrderBook.getBidsOrderQueue(ticker).isEmpty()),
                  () -> assertEquals(1, simpleOrderBook.getBidsOrderQueue(ticker).size()),
                  () -> assertEquals(OrderRegistrationState.SUCCESS, orderRegistrationResult.orderRegistrationState()),
                  () -> assertEquals(TransactionPhase.PARTIALLY_COMPLETED, orderRegistrationResult.transactionPhase()),
                  () -> assertEquals(5, orderRegistrationResult.finishedTransactionInfoList().size()),
                  () -> assertNull(orderRegistrationResult.errorMessage()),
                  () -> assertEquals(volumeRequested, orderRegistrationResult.orderFillDetails().volumeRequested()),
                  () -> assertEquals(volumeRequested - 1, orderRegistrationResult.orderFillDetails().volumeFilled()),
                  () -> assertEquals(1, orderRegistrationResult.orderFillDetails().volumePending()),
                  () -> assertEquals("Could not filled complete full requested volume, partially filled transaction and another part of requested volume placed in queue", orderRegistrationResult.orderFillDetails().message()));
    }

    @Test
    @DisplayName("Should correctly partially complete Sell Limit order when there are partially available volume with acceptable price in Bids Queue")
    void shouldCorrectlyPartiallyCompleteSellLimitOrderWhenThereArePartiallyAvailableVolumeWithAcceptablePriceInBidsQueue() {

    }

    @Test
    @DisplayName("Should add Buy Limit order when there are many Sell Limit and Sell Market placed in Queue but with not acceptable price")
    void shouldAddBuyLimitOrderWhenThereAreManySellLimitAndSellMarketPlacedInQueueButWithNotAcceptablePrice() {

    }

    @Test
    @DisplayName("Should fulfill the scenario assumptions — many Sell Limit and Buy Limit orders placed simultaneously with different volumes and prices")
    void shouldFulfillScenarioAssumptionsManySellLimitsAndBuyLimitsAtOneMomentWithVariousVolumesAndPrices() {

    }

    @Test
    @DisplayName("Should throw exception when ticker does not exist in order book")
    void shouldThrowExceptionWhenTickerDoesNotExist() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should reject Buy Limit order with zero or negative volume")
    void shouldRejectBuyLimitOrderWithZeroOrNegativeVolume() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should reject Sell Limit order with zero or negative price")
    void shouldRejectSellLimitOrderWithZeroOrNegativePrice() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should preserve FIFO order for Buy Limit orders at the same price")
    void shouldPreserveFIFOOrderForBuyLimitOrdersAtSamePrice() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should preserve FIFO order for Sell Limit orders at the same price")
    void shouldPreserveFIFOOrderForSellLimitOrdersAtSamePrice() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should correctly execute Buy Limit order against multiple Ask orders")
    void shouldExecuteBuyLimitOrderAgainstMultipleAsks() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should correctly execute Sell Limit order against multiple Bid orders")
    void shouldExecuteSellLimitOrderAgainstMultipleBids() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should execute Buy Limit order at best available prices even if limit is higher")
    void shouldExecuteBuyLimitOrderAtBestAvailablePrice() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should execute Sell Limit order at best available prices even if limit is lower")
    void shouldExecuteSellLimitOrderAtBestAvailablePrice() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should correctly update remaining volume after partial execution")
    void shouldUpdateRemainingVolumeAfterPartialExecution() {
        // TODO: implement test
    }

    @Test
    @DisplayName("Should handle large number of orders efficiently")
    void shouldHandleLargeNumberOfOrders() {
        // TODO: implement test
    }
}