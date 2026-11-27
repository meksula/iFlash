package com.iflash.core.order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.iflash.core.configuration.GlobalSettings.GLOBAL_CURRENCY;

@Slf4j
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Order implements Comparable<Order> {

    private static final AtomicLong REGISTRATION_SEQUENCE = new AtomicLong();

    static final Comparator<Order> ASK_PRIORITY = Comparator.comparing(Order::getPrice)
                                                            .thenComparingLong(Order::getRegistrationSequence);
    static final Comparator<Order> BID_PRIORITY = Comparator.comparing(Order::getPrice, Comparator.reverseOrder())
                                                            .thenComparingLong(Order::getRegistrationSequence);

    private final UUID orderUuid;
    private final long registrationSequence;
    private final ZonedDateTime orderCreationDate;
    private final String ticker;
    private final BigDecimal price;
    private final CurrencyUnit currency;
    private Long volume;

    private OrderRegistrationState orderRegistrationState;
    private OrderState currentOrderState;
    private List<OrderStateChange> orderStateHistory;

    static Order factorize(RegisterOrderCommand registerOrderCommand) {
        var newOrderRegistrationState = OrderRegistrationState.PENDING;
        var newCurrentOrderState = OrderState.PENDING;
        List<OrderStateChange> orderStateChanges = new ArrayList<>(0);
        Order order = new Order(UUID.randomUUID(), REGISTRATION_SEQUENCE.incrementAndGet(), ZonedDateTime.now(), registerOrderCommand.ticker(), registerOrderCommand.price(), GLOBAL_CURRENCY, registerOrderCommand.volume(),
                                OrderRegistrationState.PENDING, OrderState.PENDING, orderStateChanges);
        orderStateChanges.add(new OrderStateChange(ZonedDateTime.now(), OrderRegistrationState.UNKNOWN, newOrderRegistrationState, OrderState.UNKNOWN, newCurrentOrderState, registerOrderCommand.volume(), registerOrderCommand.volume()));
        return order;
    }

    static Order factorizeWithPriceDefined(RegisterOrderCommand registerOrderCommand, BigDecimal price) {
        Order order = factorize(registerOrderCommand);
        return new Order(order.getOrderUuid(), order.getRegistrationSequence(), order.getOrderCreationDate(), order.getTicker(), price, order.getCurrency(), order.getVolume(),
                order.getOrderRegistrationState(), order.getCurrentOrderState(), order.getOrderStateHistory());
    }

    Order offerRegistrationFailed() {
        var newOrderRegistrationState = OrderRegistrationState.FAILURE;
        var newCurrentOrderState = OrderState.CLOSED;

        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, newOrderRegistrationState, currentOrderState, newCurrentOrderState, volume, volume));
        this.orderRegistrationState = newOrderRegistrationState;
        this.currentOrderState = newCurrentOrderState;
        return this;
    }

    Order offerSuccessfullyRegistered() {
        var newOrderRegistrationState = OrderRegistrationState.SUCCESS;
        var newCurrentOrderState = OrderState.OPEN;

        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, newOrderRegistrationState, currentOrderState, newCurrentOrderState, volume, volume));
        this.orderRegistrationState = newOrderRegistrationState;
        this.currentOrderState = newCurrentOrderState;
        return this;
    }

    public FinishedTransactionInfo bought() {
        var newCurrentOrderState = OrderState.CLOSED;

        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, orderRegistrationState, currentOrderState, newCurrentOrderState, volume, 0L));
        this.currentOrderState = newCurrentOrderState;

        return new FinishedTransactionInfo(orderUuid, ticker, volume, price);
    }

    public FinishedTransactionInfo boughtPartially(Long volumePartiallyBought) {
        var newCurrentOrderState = OrderState.OPEN;

        long volumeLeft = volume - volumePartiallyBought;
        this.orderStateHistory.add(new OrderStateChange(ZonedDateTime.now(), orderRegistrationState, orderRegistrationState, currentOrderState, newCurrentOrderState, volume, volumeLeft));
        this.volume = volumeLeft;
        this.currentOrderState = newCurrentOrderState;

        return new FinishedTransactionInfo(orderUuid, ticker, volumePartiallyBought, price);
    }

    public void printHistory() {
        log.debug("=== ORDER'S {} HISTORY BEGIN ===", orderUuid);
        this.orderStateHistory.forEach(orderStateChange -> log.debug(orderStateChange.toString()));
        log.debug("=== ORDER'S {} HISTORY ORDERS BEGIN ===", orderUuid);
    }

    FinishedTransactionInfo toTransactionInfoWithCurrentState() {
        return new FinishedTransactionInfo(orderUuid, ticker, volume, price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderUuid, order.orderUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderUuid);
    }

    @Override
    public int compareTo(Order anotherOrder) {
        return Objects.requireNonNull(this.price, "Main Order.price is null")
                      .compareTo(Objects.requireNonNull(anotherOrder.price, "Other Order.price is null"));
    }

    public OrderInformation orderInformation() {
        return new OrderInformation(orderCreationDate, price, volume);
    }

    record OrderStateChange(ZonedDateTime changeDateTime,
                            OrderRegistrationState previousOrderRegistrationState,
                            OrderRegistrationState nextOrderRegistrationState,
                            OrderState previousOrderState,
                            OrderState nextOrderState,
                            Long volume,
                            Long volumeAfterAction) {
    }
}

