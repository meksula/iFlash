package com.iflash.engine.order;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.iflash.engine.configuration.GlobalSettings.GLOBAL_CURRENCY;

class Order implements Comparable<Order> {
    private final UUID orderUuid;
    private final ZonedDateTime orderCreationDate;
    private final String ticker;
    private final BigDecimal price;
    private final CurrencyUnit currency;
    private OrderRegistrationState orderRegistrationState;
    private OrderState currentOrderState;
    private List<OrderStateChange> orderStateHistory;

    public Order(UUID orderUuid, ZonedDateTime orderCreationDate,
                 String ticker, BigDecimal price,
                 CurrencyUnit currency, OrderRegistrationState orderRegistrationState) {
        this.orderUuid = orderUuid;
        this.orderCreationDate = orderCreationDate;
        this.ticker = ticker;
        this.price = price;
        this.currency = currency;
        this.orderRegistrationState = orderRegistrationState;
    }

    static Order factorize(RegisterOrderCommand registerOrderCommand) {
        return new Order(UUID.randomUUID(), ZonedDateTime.now(), registerOrderCommand.ticker(), registerOrderCommand.price(),
                         GLOBAL_CURRENCY, OrderRegistrationState.PENDING);
    }

    Order offerRegistrationFailed() {
        this.orderRegistrationState = OrderRegistrationState.FAILURE;
        this.currentOrderState = OrderState.CLOSED;
        return this;
    }

    Order offerSuccessfullyRegistered() {
        this.orderRegistrationState = OrderRegistrationState.PENDING;
        this.currentOrderState = OrderState.OPEN;
        return this;
    }

    public Order bought() {
        this.currentOrderState = OrderState.CLOSED;
        return this;
    }

    @Override
    public int compareTo(Order anotherOrder) {
        return Objects.requireNonNull(this.price, "Main Order.price is null")
                      .compareTo(Objects.requireNonNull(anotherOrder.price, "Other Order.price is null"));
    }

    @Override
    public String toString() {
        return "Order {" +
               "orderUuid=" + orderUuid +
               ", orderCreationDate=" + orderCreationDate +
               ", ticker='" + ticker + '\'' +
               ", price=" + price +
               ", currency=" + currency +
               ", orderState=" + orderRegistrationState +
               '}';
    }

    record OrderStateChange(ZonedDateTime changeDateTime,
                            OrderRegistrationState previousOrderRegistrationState,
                            OrderRegistrationState nextOrderRegistrationState,
                            OrderState previousOrderState,
                            OrderState nextOrderState) {
    }
}

