package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class MarketOrderProcessorTest {

    private final QuotationProvider quotationProvider = Mockito.mock(QuotationProvider.class);
    private final LimitOrderProcessor limitOrderProcessor = Mockito.mock(LimitOrderProcessor.class);

    @BeforeEach
    void setUp() {
        Mockito.when(quotationProvider.getCurrentQuote(any())).thenReturn(new CurrentQuotation(System.currentTimeMillis(), BigDecimal.valueOf(171.1243)));
    }

    @Test
    @DisplayName("Should correctly finish transaction and resting volume should be compared with requested instead of remaining volume")
    void shouldCorrectlyFinishTransactionAndRestingVolumeShouldBeComparedWithRequestedInsteadOfRemainingVolume() {
        final long volumeRequested = 100;

        var ticker = "NVDA.US";

        Map<String, Queue<Order>> asksOrdersByTicker = new HashMap<>();
        Map<String, Queue<Order>> bidsOrdersByTicker = new HashMap<>();

        RegisterOrderCommand buyCommandA = new RegisterOrderCommand(UUID.randomUUID(), OrderDirection.ASK, OrderType.MARKET, ticker, null, 60L);
        RegisterOrderCommand buyCommandB = new RegisterOrderCommand(UUID.randomUUID(), OrderDirection.ASK, OrderType.MARKET, ticker, null, 70L);

        SimpleOrderBook simpleOrderBook = new SimpleOrderBook(asksOrdersByTicker, bidsOrdersByTicker, quotationProvider);
        simpleOrderBook.registerTicker(ticker);

        MarketOrderProcessor marketOrderProcessor = new MarketOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker, limitOrderProcessor, quotationProvider);

        marketOrderProcessor.processMarketOrder(buyCommandA);
        marketOrderProcessor.processMarketOrder(buyCommandB);


        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(UUID.randomUUID(), OrderDirection.BID, OrderType.MARKET, ticker, BigDecimal.valueOf(171.9733), volumeRequested);
        OrderRegistrationResult orderRegistrationResult = marketOrderProcessor.processMarketOrder(registerOrderCommand);

        assertAll(() -> assertEquals(volumeRequested, orderRegistrationResult.orderFillDetails().volumeFilled()),
                () -> assertEquals(0, orderRegistrationResult.orderFillDetails().volumePending()),
                () -> assertEquals(volumeRequested, orderRegistrationResult.orderFillDetails().volumeRequested()));
    }
}