package com.iflash.core.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.commons.ValidateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.iflash.core.order.OrderType.LIMIT;
import static com.iflash.core.order.OrderType.MARKET;
import static java.util.Objects.*;

class SimpleOrderBook implements OrderBook {

    private static final Logger log = LoggerFactory.getLogger(SimpleOrderBook.class);

    private final Map<String, Queue<Order>> asksOrdersByTicker;
    private final Map<String, Queue<Order>> bidsOrdersByTicker;

    private SimpleOrderBook() {
        throw OrderBookException.cannotCreate();
    }

    SimpleOrderBook(Map<String, Queue<Order>> asksOrdersByTicker, Map<String, Queue<Order>> bidsOrdersByTicker) {
        this.asksOrdersByTicker = asksOrdersByTicker;
        this.bidsOrdersByTicker = bidsOrdersByTicker;
    }

    @Override
    public OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand) {
        return switch (registerOrderCommand.orderDirection()) {
            case BID -> preprocessBuyOrder(registerOrderCommand);
            case ASK -> preprocessSellOrder(registerOrderCommand);
        };
    }

    @Override
    public void registerTicker(String ticker) {
        Queue<Order> ordersQueue = new PriorityBlockingQueue<>();
        this.asksOrdersByTicker.putIfAbsent(ticker, ordersQueue);
        this.bidsOrdersByTicker.putIfAbsent(ticker, ordersQueue);
        log.info("Company with ticker: {} registered", ticker);
    }

    // todo pagination not supported
    @Override
    public Page<OrderInformation> getOrderBookSnapshot(String ticker, OrderDirection orderDirection, Pagination pagination) {
        Queue<Order> orders = select(orderDirection).get(ticker);
        if (orders == null) {
            throw OrderBookException.noTicker(ticker);
        }
        if (orders.isEmpty()) {
            return Page.of(List.of(), pagination);
        }
        List<Order> orderList = new ArrayList<>(orders);

        int orderListSize = orderList.size();
        int limit = pagination.size();
        return switch (pagination.orderBy()) {
            case ASC -> {
                if (pagination.size() <= 0) {
                    throw new IllegalStateException("Cannot find Quotations for limit value less or equal to 0");
                }
                if (pagination.size() > orderListSize) {
                    limit = orderListSize;
                }
                List<OrderInformation> orderInformationList = orderList.subList(0, limit)
                                                                       .stream()
                                                                       .map(Order::orderInformation)
                                                                       .toList();
                yield Page.of(orderInformationList, pagination);
            }
            case DESC -> {
                int fromIndex = orderList.size() - limit;
                List<OrderInformation> orderInformationList = orderList.subList(Math.max(fromIndex, 0), orderList.size())
                                                                       .stream()
                                                                       .map(Order::orderInformation)
                                                                       .collect(Collectors.toList());
                Collections.reverse(orderInformationList);
                yield Page.of(orderInformationList, pagination);
            }
        };
    }

    private Map<String, Queue<Order>> select(OrderDirection orderDirection) {
        return switch (orderDirection) {
            case BID -> bidsOrdersByTicker;
            case ASK -> asksOrdersByTicker;
        };
    }

    @Override
    public List<String> getAllTickers() {
        return asksOrdersByTicker.keySet()
                                 .stream()
                                 .sorted()
                                 .toList();
    }

    private OrderRegistrationResult preprocessBuyOrder(RegisterOrderCommand registerOrderCommand) {
        if (MARKET == registerOrderCommand.orderType()) {
            Queue<Order> asksOrdersQueue = asksOrdersByTicker.get(registerOrderCommand.ticker());
            if (asksOrdersQueue == null) {
                throw OrderBookException.noTicker(registerOrderCommand.ticker());
            }

            List<FinishedTransactionInfo> ordersSoldOut = new ArrayList<>(0);
            long volumeRequested = registerOrderCommand.volume();
            long volumeBoughtInSession = 0L;

            while (volumeBoughtInSession < volumeRequested) {
                Order order = asksOrdersQueue.poll();
                if (order != null) {
                    if (order.getVolume() <= volumeRequested) {
                        FinishedTransactionInfo boughtFinishedTransactionInfo = order.bought();
                        ordersSoldOut.add(boughtFinishedTransactionInfo);
                        volumeBoughtInSession = volumeBoughtInSession + boughtFinishedTransactionInfo.volume();
                    }
                    else {
                        long howMoreVolumeYet = volumeRequested - volumeBoughtInSession;
                        FinishedTransactionInfo boughtPartiallyFinishedTransactionInfo = order.boughtPartially(howMoreVolumeYet);
                        ordersSoldOut.add(boughtPartiallyFinishedTransactionInfo);
                        volumeBoughtInSession = volumeBoughtInSession + howMoreVolumeYet;
                        asksOrdersQueue.offer(order);
                    }
                }
                else {
                    log.info("Partially Fill occured, requested volume: {}, filled volume: {}", volumeRequested, volumeBoughtInSession);
                    // todo tutaj należy złożyć zlecenie do bids
                    return OrderRegistrationResult.partiallySuccess(ordersSoldOut, registerOrderCommand);
                }
            }
            return OrderRegistrationResult.success(ordersSoldOut);
        }
        if (LIMIT == registerOrderCommand.orderType()) {
            Queue<Order> bidsOrdersQueue = bidsOrdersByTicker.get(registerOrderCommand.ticker());
            if (bidsOrdersQueue == null) {
                throw OrderBookException.noTicker(registerOrderCommand.ticker());
            }
            Order bidOrder = Order.factorize(registerOrderCommand);
            bidsOrdersQueue.add(bidOrder);
            return OrderRegistrationResult.limitOrderSuccess(registerOrderCommand);
        }
        throw OrderBookException.orderTypeNotAvailable(registerOrderCommand.orderType());
    }

    private OrderRegistrationResult preprocessSellOrder(RegisterOrderCommand registerOrderCommand) {
        boolean isTickerExists = asksOrdersByTicker.containsKey(registerOrderCommand.ticker());
        if (isTickerExists) {
            Queue<Order> orders = asksOrdersByTicker.get(registerOrderCommand.ticker());
            Order order = Order.factorize(registerOrderCommand);
            boolean offerResult = orders.offer(order);

            if (offerResult) {
                return OrderRegistrationResult.success(List.of(order.offerSuccessfullyRegistered()
                                                                    .toTransactionInfoWithCurrentState()));
            }
            else {
                return OrderRegistrationResult.failure(List.of(order.offerRegistrationFailed()
                                                                    .toTransactionInfoWithCurrentState()), "Could not register your sell order");
            }
        }
        else {
            throw OrderBookException.noTicker(registerOrderCommand.ticker());
        }
    }

    public Queue<Order> getAsksOrderQueue(String ticker) {
        return asksOrdersByTicker.get(ticker);
    }

    public boolean isAsksVolumeAvailable(String ticker, Long volumeRequested) {
        ValidateUtils.requireNonNullOrThrow(ticker, OrderBookException.tickerNull());
        ValidateUtils.mustBePositive(volumeRequested, OrderBookException.negativeNumber(volumeRequested));

        Long volumeAvailable = getAsksVolume(ticker);
        if (isNull(volumeAvailable)) {
            return false;
        }
        return volumeAvailable >= volumeRequested;
    }

    public Long getAsksVolume(String ticker) {
        Queue<Order> orders = asksOrdersByTicker.get(ticker);
        if (orders == null) {
            return 0L;
        }
        return asksOrdersByTicker.get(ticker)
                                 .stream()
                                 .map(Order::getVolume)
                                 .reduce(Long::sum)
                                 .orElse(0L);
    }
}
