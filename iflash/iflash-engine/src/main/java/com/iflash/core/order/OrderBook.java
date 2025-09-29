package com.iflash.core.order;

import java.util.HashMap;
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

//    private final Queue<Order> sellOrdersQueue;
//    private final Queue<Order> buyOrdersQueue;

    private final Map<String, Queue<Order>> sellOrdersByTicker;
    private final Map<String, Long> volumeByTicker;
//    private final Map<String, Queue<Order>> buyOrdersByTicker;

    OrderBook() {
//        this.sellOrdersQueue = new PriorityQueue<>();
//        this.buyOrdersQueue = new PriorityQueue<>();

        this.sellOrdersByTicker = new HashMap<>();
        this.volumeByTicker = new HashMap<>();
//        this.buyOrdersByTicker = new HashMap<>();
    }

    public Order registerOrder(RegisterOrderCommand registerOrderCommand) {
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

    private Order preprocessBuyOrder(RegisterOrderCommand registerOrderCommand) {
        if (MARKET == registerOrderCommand.orderType()) {
            Optional<Queue<Order>> ordersQueue = Optional.ofNullable(sellOrdersByTicker.get(registerOrderCommand.ticker()));
            return ordersQueue.map(orders -> {
                                  Order order = orders.poll();
                                  if (order != null) {
                                      return order.bought();
                                  }
                                  else {
                                      throw OrderBookException.noTicker(registerOrderCommand.ticker());
                                  }
                              })
                              .orElseThrow(() -> OrderBookException.noTicker(registerOrderCommand.ticker()));
        }
        throw new OrderBookException("Not implemented yet");
    }

    private Order preprocessSellOrder(RegisterOrderCommand registerOrderCommand) {
        boolean isTickerExists = sellOrdersByTicker.containsKey(registerOrderCommand.ticker());
        if (isTickerExists) {
            Queue<Order> orders = sellOrdersByTicker.get(registerOrderCommand.ticker());

            Order order = Order.factorize(registerOrderCommand);
            boolean offerResult = orders.offer(order);

            if (offerResult) {
                this.increaseVolume(registerOrderCommand.ticker(), registerOrderCommand.volume());
                return order.offerSuccessfullyRegistered();
            }
            else {
                return order.offerRegistrationFailed();
            }
        }
        else {
            return registerTicker(registerOrderCommand);
        }
    }

    private Order registerTicker(RegisterOrderCommand registerOrderCommand) {
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

}
