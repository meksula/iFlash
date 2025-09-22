package com.iflash.core.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    @DisplayName("Should correctly create order with correct orderRegistrationState and currentOrderState and orderStateHistory")
    void shouldCorrectlyCreateOrderWithCorrectOrderRegistrationStateAndCurrentOrderStateAndOrderStateHistory() {
        var ticker = "NVDA.US";
        var price = BigDecimal.valueOf(171.9434);

        RegisterOrderCommand registerOrderCommand = new RegisterOrderCommand(OrderDirection.SELL, OrderType.MARKET, ticker, price);
        Order order = Order.factorize(registerOrderCommand);

        assertAll(() -> assertEquals(OrderRegistrationState.PENDING, order.getOrderRegistrationState()),
                  () -> assertEquals(OrderState.PENDING, order.getCurrentOrderState()),
                  () -> assertEquals(1, order.getOrderStateHistory().size()));
    }
}