package com.iflash.core.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Slf4j
@RequiredArgsConstructor
class MarketOrderProcessor {

    private final Map<String, Queue<Order>> asksOrdersByTicker;
    private final Map<String, Queue<Order>> bidsOrdersByTicker;

    OrderRegistrationResult processMarketOrder(RegisterOrderCommand registerOrderCommand) {
        Queue<Order> ordersQueue = OrderDirection.BID == registerOrderCommand.orderDirection()
                                   ? asksOrdersByTicker.get(registerOrderCommand.ticker())
                                   : bidsOrdersByTicker.get(registerOrderCommand.ticker());
        if (ordersQueue == null) {
            throw OrderBookException.noTicker(registerOrderCommand.ticker());
        }
        List<FinishedTransactionInfo> ordersSoldOut = new ArrayList<>(0);
        long volumeRequested = registerOrderCommand.volume();
        long volumeBoughtInSession = 0L;

        while (volumeBoughtInSession < volumeRequested) {
            Order order = ordersQueue.poll();
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
                    ordersQueue.offer(order);
                }
            }
            else {
                if (volumeBoughtInSession == 0L) {
                    Queue<Order> orderQueue = OrderDirection.BID == registerOrderCommand.orderDirection()
                                              ? bidsOrdersByTicker.get(registerOrderCommand.ticker())
                                              : asksOrdersByTicker.get(registerOrderCommand.ticker());
                    Order idlingOrder = Order.factorize(registerOrderCommand);
                    orderQueue.add(idlingOrder);
                    log.info("Volume bought in session equals zero, so order have to be placed in queue");
                    return OrderRegistrationResult.limitOrderPlacedSuccessfully(registerOrderCommand);
                }
                log.info("Partially Fill occured, requested volume: {}, filled volume: {}", volumeRequested, volumeBoughtInSession);
                return OrderRegistrationResult.transactionPartiallyCompleted(ordersSoldOut, registerOrderCommand);
            }
        }
        return OrderRegistrationResult.transactionPartiallyCompleted(ordersSoldOut);
    }
}
