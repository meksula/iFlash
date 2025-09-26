package com.iflash.core.order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.iflash.core.configuration.GlobalSettings.GLOBAL_CURRENCY;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Order implements Comparable<Order> {

    private static final Logger log = LoggerFactory.getLogger(Order.class);

    private final UUID orderUuid;
    private final ZonedDateTime orderCreationDate;
    private final String ticker;
    private final BigDecimal price;
    private final CurrencyUnit currency;
    private final Long volume;

    private OrderRegistrationState orderRegistrationState;
    private OrderState currentOrderState;
    private List<OrderStateChange> orderStateHistory;

    static Order factorize(RegisterOrderCommand registerOrderCommand) {
        var newOrderRegistrationState = OrderRegistrationState.PENDING;
        var newCurrentOrderState = OrderState.PENDING;
        List<OrderStateChange> orderStateChanges = new ArrayList<>(0);
        Order order = new Order(UUID.randomUUID(), ZonedDateTime.now(), registerOrderCommand.ticker(), registerOrderCommand.price(), GLOBAL_CURRENCY, registerOrderCommand.volume(),
                                OrderRegistrationState.PENDING, OrderState.PENDING, orderStateChanges);
        orderStateChanges.add(new OrderStateChange(ZonedDateTime.now(), OrderRegistrationState.UNKNOWN, newOrderRegistrationState, OrderState.UNKNOWN, newCurrentOrderState));
        return order;
    }

    Order offerRegistrationFailed() {
        var newOrderRegistrationState = OrderRegistrationState.FAILURE;
        var newCurrentOrderState = OrderState.CLOSED;

        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, newOrderRegistrationState, currentOrderState, newCurrentOrderState));
        this.orderRegistrationState = newOrderRegistrationState;
        this.currentOrderState = newCurrentOrderState;
        return this;
    }

    Order offerSuccessfullyRegistered() {
        var newOrderRegistrationState = OrderRegistrationState.SUCCESS;
        var newCurrentOrderState = OrderState.OPEN;

        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, newOrderRegistrationState, currentOrderState, newCurrentOrderState));
        this.orderRegistrationState = newOrderRegistrationState;
        this.currentOrderState = newCurrentOrderState;
        return this;
    }

    public Order bought() {
        var newCurrentOrderState = OrderState.CLOSED;

        this.currentOrderState = newCurrentOrderState;
        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, orderRegistrationState, currentOrderState, newCurrentOrderState));
        this.currentOrderState = newCurrentOrderState;
        return this;
    }

    public void printHistory() {
        log.info("=== ORDER'S {} HISTORY BEGIN ===", orderUuid);
        this.orderStateHistory.forEach(orderStateChange -> log.info(orderStateChange.toString()));
        log.info("=== ORDER'S {} HISTORY ORDERS BEGIN ===", orderUuid);
    }

    @Override
    public int compareTo(Order anotherOrder) {
        return Objects.requireNonNull(this.price, "Main Order.price is null")
                      .compareTo(Objects.requireNonNull(anotherOrder.price, "Other Order.price is null"));
    }

    record OrderStateChange(ZonedDateTime changeDateTime,
                            OrderRegistrationState previousOrderRegistrationState,
                            OrderRegistrationState nextOrderRegistrationState,
                            OrderState previousOrderState,
                            OrderState nextOrderState) {
    }
}

