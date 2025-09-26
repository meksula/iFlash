package com.iflash.core.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    @DisplayName("Should correctly create SELL order with correct orderRegistrationState and currentOrderState and orderStateHistory")
    void shouldCorrectlyCreateOrderWithCorrectOrderRegistrationStateAndCurrentOrderStateAndOrderStateHistory() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);
        Order order = Order.factorize(registerOrderCommand);

        assertAll(() -> assertEquals(OrderRegistrationState.PENDING, order.getOrderRegistrationState()),
                  () -> assertEquals(OrderState.PENDING, order.getCurrentOrderState()),
                  () -> assertEquals(1, order.getOrderStateHistory().size()));
    }

    @Test
    @DisplayName("Should correctly create BUY order with correct states and with full history log in orderStateHistory")
    void shouldCorrectlyCreateOrderWithCorrectStatesAndWithFullHistoryLogInOrderStateHistory() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        RegisterOrderCommand buyOrderCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price, volume);
        Order buyOrder = Order.factorize(buyOrderCommand);

        assertAll(() -> assertEquals(OrderRegistrationState.PENDING, buyOrder.getOrderRegistrationState()),
                  () -> assertEquals(OrderState.PENDING, buyOrder.getCurrentOrderState()),
                  () -> assertEquals(1, buyOrder.getOrderStateHistory().size()));
    }

    @Test
    @DisplayName("Should correctly create SELL order and after sell process history should be consistent")
    void shouldCorrectlyCreateSellOrderAndAfterSellProcessHistoryShouldBeConsistent() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);
        var volume = 1L;

        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price, volume);
        Order order = Order.factorize(registerOrderCommand);
        order.offerSuccessfullyRegistered();
        order.bought();

        assertAll(() -> assertEquals(OrderRegistrationState.SUCCESS, order.getOrderRegistrationState()),
                  () -> assertEquals(OrderState.CLOSED, order.getCurrentOrderState()),
                  () -> assertEquals(3, order.getOrderStateHistory().size()));

        order.printHistory();
    }
}