package com.iflash.brokerplatform.trading;

import com.iflash.brokerplatform.market.FillDto;
import com.iflash.brokerplatform.market.IflashApiClient;
import com.iflash.brokerplatform.market.OrderRequestDto;
import com.iflash.brokerplatform.market.OrderResultDto;
import com.iflash.brokerplatform.market.PriceDto;
import com.iflash.brokerplatform.user.User;
import com.iflash.brokerplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    private static final Long USER_ID = 1L;
    private static final String TICKER = "AAA.US";

    @Mock private UserRepository userRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private PositionLotRepository positionLotRepository;
    @Mock private IflashApiClient api;

    @InjectMocks private TradingService tradingService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("trader@example.com");
        user.setBalance(new BigDecimal("1000.0000"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Buy debits cash and increases the position by the filled quantity")
    void buySettlesCashAndPosition() {
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER)).thenReturn(Optional.empty());
        when(api.getPrice(TICKER)).thenReturn(new PriceDto(0L, TICKER, new BigDecimal("10")));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(5, new BigDecimal("10"))));

        OrderOutcome outcome = tradingService.placeOrder(USER_ID, TICKER, "BID", "MARKET", null, 5);

        assertEquals(0, user.getBalance().compareTo(new BigDecimal("950")), "balance should drop by 50");
        assertEquals(5, outcome.filledQuantity());
        ArgumentCaptor<Position> position = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(position.capture());
        assertEquals(5, position.getValue().getQuantity());
    }

    @Test
    @DisplayName("Buy is rejected up-front when the estimated cost exceeds the balance")
    void buyRejectedOnInsufficientFunds() {
        user.setBalance(new BigDecimal("10"));
        when(api.getPrice(TICKER)).thenReturn(new PriceDto(0L, TICKER, new BigDecimal("10")));

        assertThrows(TradingException.class,
                () -> tradingService.placeOrder(USER_ID, TICKER, "BID", "MARKET", null, 5));
        verify(api, never()).registerOrder(any());
    }

    @Test
    @DisplayName("Sell is rejected up-front when holdings are insufficient")
    void sellRejectedOnInsufficientHoldings() {
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER))
                .thenReturn(Optional.of(new Position(USER_ID, TICKER, 2)));

        assertThrows(TradingException.class,
                () -> tradingService.placeOrder(USER_ID, TICKER, "ASK", "MARKET", null, 5));
        verify(api, never()).registerOrder(any());
    }

    @Test
    @DisplayName("Sell credits cash and decreases the position")
    void sellSettlesCashAndPosition() {
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER))
                .thenReturn(Optional.of(new Position(USER_ID, TICKER, 10)));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(5, new BigDecimal("20"))));

        tradingService.placeOrder(USER_ID, TICKER, "ASK", "MARKET", null, 5);

        assertEquals(0, user.getBalance().compareTo(new BigDecimal("1100")), "balance should rise by 100");
        ArgumentCaptor<Position> position = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(position.capture());
        assertEquals(5, position.getValue().getQuantity());
    }

    @Test
    @DisplayName("Settles only the filled portion of a partial fill")
    void partialFillSettlesFilledOnly() {
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER)).thenReturn(Optional.empty());
        when(api.getPrice(TICKER)).thenReturn(new PriceDto(0L, TICKER, new BigDecimal("10")));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(4, new BigDecimal("10"))));

        OrderOutcome outcome = tradingService.placeOrder(USER_ID, TICKER, "BID", "MARKET", null, 10);

        assertEquals(4, outcome.filledQuantity());
        assertTrue(outcome.partiallyFilled());
        assertEquals(0, user.getBalance().compareTo(new BigDecimal("960")), "only 40 of cash spent");
    }

    @Test
    @DisplayName("MARKET buy sends a null price to the engine")
    void marketOrderSendsNullPrice() {
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER)).thenReturn(Optional.empty());
        when(api.getPrice(TICKER)).thenReturn(new PriceDto(0L, TICKER, new BigDecimal("10")));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(1, new BigDecimal("10"))));

        tradingService.placeOrder(USER_ID, TICKER, "BID", "MARKET", new BigDecimal("999"), 1);

        ArgumentCaptor<OrderRequestDto> request = ArgumentCaptor.forClass(OrderRequestDto.class);
        verify(api).registerOrder(request.capture());
        assertEquals(null, request.getValue().price());
        assertEquals(TICKER, request.getValue().ticker());
    }

    @Test
    @DisplayName("Buy opens a lot with the filled quantity and the average fill price")
    void buyOpensLot() {
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER)).thenReturn(Optional.empty());
        when(api.getPrice(TICKER)).thenReturn(new PriceDto(0L, TICKER, new BigDecimal("10")));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(2, new BigDecimal("10")),
                new FillDto(2, new BigDecimal("11"))));

        tradingService.placeOrder(USER_ID, TICKER, "BID", "MARKET", null, 4);

        ArgumentCaptor<PositionLot> lot = ArgumentCaptor.forClass(PositionLot.class);
        verify(positionLotRepository).save(lot.capture());
        assertEquals(4, lot.getValue().getQuantity());
        assertEquals(0, lot.getValue().getPurchasePrice().compareTo(new BigDecimal("10.5")), "average of 10 and 11");
    }

    @Test
    @DisplayName("Sell releases lots oldest first and deletes the closed ones")
    void sellReleasesLotsFifo() {
        PositionLot older = lot(3);
        PositionLot newer = lot(7);
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER))
                .thenReturn(Optional.of(new Position(USER_ID, TICKER, 10)));
        when(positionLotRepository.findByUserIdAndTickerOrderByPurchasedAtAscIdAsc(USER_ID, TICKER))
                .thenReturn(new ArrayList<>(List.of(older, newer)));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(5, new BigDecimal("20"))));

        tradingService.placeOrder(USER_ID, TICKER, "ASK", "MARKET", null, 5);

        verify(positionLotRepository).deleteAll(List.of(older));
        assertEquals(0, older.getQuantity());
        assertEquals(5, newer.getQuantity());
    }

    @Test
    @DisplayName("Liquidating a lot sells exactly its quantity at market and releases only that lot")
    void liquidateLotSellsItsQuantity() {
        PositionLot lot = lot(4);
        when(positionLotRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(lot));
        when(positionRepository.findByUserIdAndTicker(USER_ID, TICKER))
                .thenReturn(Optional.of(new Position(USER_ID, TICKER, 10)));
        when(api.registerOrder(any())).thenReturn(result(new FillDto(4, new BigDecimal("20"))));

        tradingService.liquidateLot(USER_ID, 9L);

        ArgumentCaptor<OrderRequestDto> request = ArgumentCaptor.forClass(OrderRequestDto.class);
        verify(api).registerOrder(request.capture());
        assertEquals("ASK", request.getValue().orderDirection());
        assertEquals("MARKET", request.getValue().orderType());
        assertEquals(4L, request.getValue().volume());
        assertEquals(0, lot.getQuantity());
        verify(positionLotRepository).deleteAll(List.of(lot));
    }

    private PositionLot lot(long quantity) {
        return new PositionLot(USER_ID, TICKER, quantity, new BigDecimal("10").multiply(BigDecimal.valueOf(quantity)),
                Instant.now());
    }

    private OrderResultDto result(FillDto... fills) {
        return new OrderResultDto(TICKER, "BID", "MARKET", null, 0L, List.of(fills));
    }
}
