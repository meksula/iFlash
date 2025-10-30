package com.iflash.core.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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

    private final MarketOrderProcessor marketOrderProcessor;
    private final LimitOrderProcessor limitOrderProcessor;

    private SimpleOrderBook() {
        throw OrderBookException.cannotCreate();
    }

    SimpleOrderBook(Map<String, Queue<Order>> asksOrdersByTicker, Map<String, Queue<Order>> bidsOrdersByTicker) {
        this.asksOrdersByTicker = asksOrdersByTicker;
        this.bidsOrdersByTicker = bidsOrdersByTicker;

        this.marketOrderProcessor = new MarketOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker);
        this.limitOrderProcessor = new LimitOrderProcessor(asksOrdersByTicker, bidsOrdersByTicker);
    }

    @Override
    public OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand) {
        if (MARKET == registerOrderCommand.orderType()) {
            return marketOrderProcessor.processMarketOrder(registerOrderCommand);
        }
        if (LIMIT == registerOrderCommand.orderType()) {
            return limitOrderProcessor.processLimitOrder(registerOrderCommand);
        }
        throw OrderBookException.orderTypeNotAvailable(registerOrderCommand.orderType());
    }

    @Override
    public void registerTicker(String ticker) {
        Queue<Order> askOrdersQueue = new PriorityBlockingQueue<>();
        Queue<Order> bidsOrdersQueue = new PriorityBlockingQueue<>();
        this.asksOrdersByTicker.putIfAbsent(ticker, askOrdersQueue);
        this.bidsOrdersByTicker.putIfAbsent(ticker, bidsOrdersQueue);
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

    @Override
    public Set<OrderInformation> getTopOrders(String ticker, OrderDirection orderDirection, Integer depth) {
        Queue<Order> orders = select(orderDirection).get(ticker);
        List<Order> ordersDump = List.copyOf(orders);
        if (ordersDump.size() <= depth) {
            return ordersDump.stream()
                             .map(Order::orderInformation)
                             .collect(Collectors.toSet());
        }
        return ordersDump.subList(depth, ordersDump.size())
                         .stream()
                         .map(Order::orderInformation)
                         .collect(Collectors.toSet());
    }

    public Queue<Order> getAsksOrderQueue(String ticker) {
        return asksOrdersByTicker.get(ticker);
    }

    public Queue<Order> getBidsOrderQueue(String ticker) {
        return bidsOrdersByTicker.get(ticker);
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
