package com.iflash.core.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.commons.ValidateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.iflash.core.order.OrderType.MARKET;
import static java.util.Objects.*;

class SimpleOrderBook implements OrderBook {

    private static final Logger log = LoggerFactory.getLogger(SimpleOrderBook.class);

    private final Map<String, Queue<Order>> sellOrdersByTicker;
    private final Map<String, Long> volumeByTicker;

    private SimpleOrderBook() {
        throw OrderBookException.cannotCreate();
    }

    SimpleOrderBook(Map<String, Queue<Order>> sellOrdersByTicker, Map<String, Long> volumeByTicker) {
        this.sellOrdersByTicker = sellOrdersByTicker;
        this.volumeByTicker = volumeByTicker;
    }

    @Override
    public OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand) {
        return switch (registerOrderCommand.orderDirection()) {
            case BUY -> preprocessBuyOrder(registerOrderCommand);
            case SELL -> preprocessSellOrder(registerOrderCommand);
        };
    }

    @Override
    public void registerTicker(String ticker) {
        Queue<Order> ordersQueue = new PriorityQueue<>();
        this.sellOrdersByTicker.putIfAbsent(ticker, ordersQueue);
        this.volumeByTicker.putIfAbsent(ticker, 0L);
        log.info("Company with ticker: {} registered", ticker);
    }

    // todo pagination not supported
    @Override
    public Page<OrderInformation> getOrderBookSnapshot(String ticker, Pagination pagination) {
        Queue<Order> orders = sellOrdersByTicker.get(ticker);
        if (orders == null) {
            throw OrderBookException.noTicker(ticker);
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

    public List<String> getAllTickers() {
        return sellOrdersByTicker.keySet()
                                 .stream()
                                 .sorted()
                                 .toList();
    }

    public Queue<Order> getOrderQueue(String ticker) {
        return sellOrdersByTicker.get(ticker);
    }

    public boolean isVolumeAvailable(String ticker, Long volumeRequested) {
        ValidateUtils.requireNonNullOrThrow(ticker, OrderBookException.tickerNull());
        ValidateUtils.mustBePositive(volumeRequested, OrderBookException.negativeNumber(volumeRequested));

        Long volumeAvailable = this.volumeByTicker.get(ticker);
        if (isNull(volumeAvailable)) {
            return false;
        }
        return volumeAvailable >= volumeRequested;
    }

    public Long getVolume(String ticker) {
        return this.volumeByTicker.getOrDefault(ticker, 0L);
    }

    private OrderRegistrationResult preprocessBuyOrder(RegisterOrderCommand registerOrderCommand) {
        if (MARKET == registerOrderCommand.orderType()) {
            if (!isVolumeAvailable(registerOrderCommand.ticker(), registerOrderCommand.volume())) {
                throw OrderBookException.volumeNotAvailable(registerOrderCommand.volume(), registerOrderCommand.ticker());
            }
            Queue<Order> ordersQueue = sellOrdersByTicker.get(registerOrderCommand.ticker());
            if (ordersQueue == null) {
                throw OrderBookException.noTicker(registerOrderCommand.ticker());
            }

            List<FinishedTransactionInfo> ordersSoldOut = new ArrayList<>(0);
            long volumeRequested = registerOrderCommand.volume();
            long volumeBoughtInSession = 0L;

            while (volumeBoughtInSession < volumeRequested) {
                Order order = ordersQueue.peek();
                if (order != null) {
                    if (order.getVolume() <= volumeRequested) {
                        FinishedTransactionInfo boughtFinishedTransactionInfo = ordersQueue.poll()
                                                                                           .bought();
                        this.decreaseVolume(registerOrderCommand.ticker(), order.getVolume());
                        ordersSoldOut.add(boughtFinishedTransactionInfo);
                        volumeBoughtInSession = volumeBoughtInSession + boughtFinishedTransactionInfo.volume();
                    }
                    else {
                        long howMoreVolumeYet = volumeRequested - volumeBoughtInSession;
                        FinishedTransactionInfo boughtPartiallyFinishedTransactionInfo = order.boughtPartially(howMoreVolumeYet);
                        ordersSoldOut.add(boughtPartiallyFinishedTransactionInfo);
                        volumeBoughtInSession = volumeBoughtInSession + howMoreVolumeYet;
                        this.decreaseVolume(registerOrderCommand.ticker(), howMoreVolumeYet);
                    }
                }
                else {
                    throw OrderBookException.volumeNotAvailable(registerOrderCommand.volume(), registerOrderCommand.ticker());
                }
            }
            return new OrderRegistrationResult(ordersSoldOut);
        }
        throw OrderBookException.orderTypeNotAvailable(registerOrderCommand.orderType());
    }

    private OrderRegistrationResult preprocessSellOrder(RegisterOrderCommand registerOrderCommand) {
        boolean isTickerExists = sellOrdersByTicker.containsKey(registerOrderCommand.ticker());
        if (isTickerExists) {
            Queue<Order> orders = sellOrdersByTicker.get(registerOrderCommand.ticker());

            Order order = Order.factorize(registerOrderCommand);
            boolean offerResult = orders.offer(order);

            if (offerResult) {
                this.increaseVolume(registerOrderCommand.ticker(), registerOrderCommand.volume());
                return new OrderRegistrationResult(List.of(order.offerSuccessfullyRegistered()
                                                                .toTransactionInfoWithCurrentState()));
            }
            else {
                return new OrderRegistrationResult(List.of(order.offerRegistrationFailed()
                                                                .toTransactionInfoWithCurrentState()));
            }
        }
        else {
            throw OrderBookException.noTicker(registerOrderCommand.ticker());
        }
    }

    private void increaseVolume(String ticker, long volume) {
        volumeByTicker.merge(ticker, volume, Long::sum);
    }

    private void decreaseVolume(String ticker, long volume) {
        volumeByTicker.merge(ticker, -volume, Long::sum);
    }
}
