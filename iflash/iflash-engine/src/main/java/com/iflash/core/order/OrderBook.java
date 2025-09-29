package com.iflash.core.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import com.iflash.commons.ValidateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.iflash.core.order.OrderType.MARKET;
import static java.util.Objects.*;

class OrderBook {

    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);

    private final Map<String, Queue<Order>> sellOrdersByTicker;
    private final Map<String, Long> volumeByTicker;

    OrderBook() {
        this.sellOrdersByTicker = new HashMap<>();
        this.volumeByTicker = new HashMap<>();
    }

    public List<Order> registerOrder(RegisterOrderCommand registerOrderCommand) {
        return switch (registerOrderCommand.orderDirection()) {
            case BUY -> preprocessBuyOrder(registerOrderCommand);
            case SELL -> preprocessSellOrder(registerOrderCommand);
        };
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

    private List<Order> preprocessBuyOrder(RegisterOrderCommand registerOrderCommand) {
        if (MARKET == registerOrderCommand.orderType()) {
            if (!isVolumeAvailable(registerOrderCommand.ticker(), registerOrderCommand.volume())) {
                throw OrderBookException.volumeNotAvailable(registerOrderCommand.volume(), registerOrderCommand.ticker());
            }
            Optional<Queue<Order>> ordersQueue = Optional.ofNullable(sellOrdersByTicker.get(registerOrderCommand.ticker()));
            return ordersQueue.map(orders -> {
                List<Order> ordersSoldOut = new ArrayList<>(0);
                long volumeRequested = registerOrderCommand.volume();
                long volumeBoughtInSession = 0L;

                while (volumeBoughtInSession < volumeRequested) {
                    Order order = orders.peek();
                    if (order != null) {
                        if (order.getVolume() <= volumeRequested) {
                            Order orderBought = orders.poll().bought();
                            this.decreaseVolume(registerOrderCommand.ticker(), order.getVolume());
                            ordersSoldOut.add(orderBought);
                            volumeBoughtInSession = volumeBoughtInSession + orderBought.getVolume();
                        } else {
                            long howMoreVolumeYet = volumeRequested - volumeBoughtInSession;
                            order.boughtPartially(howMoreVolumeYet);
                            volumeBoughtInSession = volumeBoughtInSession + howMoreVolumeYet;
                            this.decreaseVolume(registerOrderCommand.ticker(), howMoreVolumeYet);
                        }
                    } else {
                        throw OrderBookException.volumeNotAvailable(registerOrderCommand.volume(), registerOrderCommand.ticker());
                    }
                }
                return ordersSoldOut;
            }).orElseThrow(() -> OrderBookException.noTicker(registerOrderCommand.ticker()));
        }
        throw OrderBookException.orderTypeNotAvailable(registerOrderCommand.orderType());
    }

    private List<Order> preprocessSellOrder(RegisterOrderCommand registerOrderCommand) {
        boolean isTickerExists = sellOrdersByTicker.containsKey(registerOrderCommand.ticker());
        if (isTickerExists) {
            Queue<Order> orders = sellOrdersByTicker.get(registerOrderCommand.ticker());

            Order order = Order.factorize(registerOrderCommand);
            boolean offerResult = orders.offer(order);

            if (offerResult) {
                this.increaseVolume(registerOrderCommand.ticker(), registerOrderCommand.volume());
                return List.of(order.offerSuccessfullyRegistered());
            }
            else {
                return List.of(order.offerRegistrationFailed());
            }
        }
        else {
            return registerTicker(registerOrderCommand);
        }
    }

    private List<Order> registerTicker(RegisterOrderCommand registerOrderCommand) {
        Queue<Order> ordersQueue = new PriorityQueue<>();
        this.sellOrdersByTicker.putIfAbsent(registerOrderCommand.ticker(), ordersQueue);
        this.volumeByTicker.putIfAbsent(registerOrderCommand.ticker(), 0L);
        return preprocessSellOrder(registerOrderCommand);
    }

    private Long increaseVolume(String ticker, Long volumeSellAlready) {
        Long volumeExisting = this.volumeByTicker.get(ticker);
        Long volumeSummed = volumeExisting + volumeSellAlready;
        this.volumeByTicker.replace(ticker, volumeSummed);
        return volumeExisting;
    }

    private Long decreaseVolume(String ticker, Long volumeBoughtAlready) {
        Long volumeExisting = this.volumeByTicker.get(ticker);
        Long volumeSummed = volumeExisting - volumeBoughtAlready;
        this.volumeByTicker.replace(ticker, volumeSummed);
        return volumeExisting;
    }

}
