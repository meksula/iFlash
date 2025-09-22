package com.iflash.core.order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.iflash.core.configuration.GlobalSettings.GLOBAL_CURRENCY;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Order implements Comparable<Order> {
    private final UUID orderUuid;
    private final ZonedDateTime orderCreationDate;
    private final String ticker;
    private final BigDecimal price;
    private final CurrencyUnit currency;

    private OrderRegistrationState orderRegistrationState;
    private OrderState currentOrderState;
    private List<OrderStateChange> orderStateHistory;

    static Order factorize(RegisterOrderCommand registerOrderCommand) {
        var newOrderRegistrationState = OrderRegistrationState.PENDING;
        var newCurrentOrderState = OrderState.PENDING;
        List<OrderStateChange> orderStateChanges = new ArrayList<>(0);
        Order order = new Order(UUID.randomUUID(), ZonedDateTime.now(), registerOrderCommand.ticker(), registerOrderCommand.price(), GLOBAL_CURRENCY, OrderRegistrationState.PENDING, OrderState.PENDING, orderStateChanges);
        orderStateChanges.add(new OrderStateChange(ZonedDateTime.now(), OrderRegistrationState.UNKNOWN, newOrderRegistrationState, OrderState.UNKNOWN, newCurrentOrderState));
        return order;
    }

    Order offerRegistrationFailed() {
        var newOrderRegistrationState = OrderRegistrationState.FAILURE;
        var newCurrentOrderState = OrderState.CLOSED;

        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, newOrderRegistrationState, currentOrderState, newCurrentOrderState));
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

