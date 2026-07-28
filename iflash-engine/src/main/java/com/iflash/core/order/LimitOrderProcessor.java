package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Slf4j
@RequiredArgsConstructor
class LimitOrderProcessor {

    private final Map<String, Queue<Order>> asksOrdersByTicker;
    private final Map<String, Queue<Order>> bidsOrdersByTicker;
    private final QuotationProvider quotationProvider;

    OrderRegistrationResult processLimitOrder(RegisterOrderCommand registerOrderCommand) {
        if (OrderDirection.BID == registerOrderCommand.orderDirection()) {
            return processBidLimitOrder(registerOrderCommand);
        }
        else {
            return processAskLimitOrder(registerOrderCommand);
        }
    }

    private OrderRegistrationResult processBidLimitOrder(RegisterOrderCommand registerOrderCommand) {
        Queue<Order> ordersQueue = asksOrdersByTicker.get(registerOrderCommand.ticker());
        if (ordersQueue == null) {
            throw OrderBookException.noTicker(registerOrderCommand.ticker());
        }
        long volumeRequested = registerOrderCommand.volume();
        long volumeBoughtInSession = 0L;
        List<FinishedTransactionInfo> ordersSoldOut = new ArrayList<>(0);
        Order askOrder = findMatchingAskOrder(registerOrderCommand.ticker(), registerOrderCommand.price());
        if (askOrder == null) {
            Order notCompletedOrder = Order.factorize(registerOrderCommand);
            bidsOrdersByTicker.get(registerOrderCommand.ticker())
                              .offer(notCompletedOrder);
            return OrderRegistrationResult.limitOrderPlacedSuccessfully(registerOrderCommand);
        }
        else {
            while (askOrder != null && volumeBoughtInSession < volumeRequested) {
                Long askVolume = askOrder.getVolume();
                long howMoreVolumeToFillYet = volumeRequested - volumeBoughtInSession;
                if (howMoreVolumeToFillYet >= askVolume) {
                    FinishedTransactionInfo finishedTransactionInfo = askOrder.bought();
                    volumeBoughtInSession = volumeBoughtInSession + finishedTransactionInfo.volume();
                    ordersSoldOut.add(finishedTransactionInfo);
                }
                else {
                    FinishedTransactionInfo finishedTransactionInfo = askOrder.boughtPartially(howMoreVolumeToFillYet);
                    ordersSoldOut.add(finishedTransactionInfo);
                    volumeBoughtInSession = volumeBoughtInSession + howMoreVolumeToFillYet;
                    ordersQueue.offer(askOrder);
                    break;
                }
                askOrder = findMatchingAskOrder(registerOrderCommand.ticker(), registerOrderCommand.price());
            }
            if (volumeBoughtInSession < volumeRequested) {
                OrderRegistrationResult partiallyCompleted = OrderRegistrationResult.limitOrderPartiallyCompleted(ordersSoldOut, registerOrderCommand);
                CurrentQuotation currentQuote = quotationProvider.getCurrentQuote(registerOrderCommand.ticker());
                RegisterOrderCommand afterPartialFill = registerOrderCommand.createAfterPartialFillment(currentQuote, partiallyCompleted.orderFillDetails().volumePending());
                processBidLimitOrder(afterPartialFill);
                return partiallyCompleted;
            }
            else {
                return OrderRegistrationResult.limitOrderFullyCompleted(ordersSoldOut, registerOrderCommand);
            }
        }
    }

    private Order findMatchingAskOrder(String ticker, BigDecimal bidPriceLimit) {
        Queue<Order> orders = asksOrdersByTicker.get(ticker);
        if (orders == null) {
            return null;
        }
        Order bestPriceAskOrder = orders.peek();
        if (bestPriceAskOrder != null) {
            BigDecimal bestAskPrice = bestPriceAskOrder.getPrice();
            if (bidPriceLimit.compareTo(bestAskPrice) > 0) {
                return orders.poll();
            }
        }
        return null;
    }

    private OrderRegistrationResult processAskLimitOrder(RegisterOrderCommand registerOrderCommand) {
        Queue<Order> ordersQueue = bidsOrdersByTicker.get(registerOrderCommand.ticker());
        if (ordersQueue == null) {
            throw OrderBookException.noTicker(registerOrderCommand.ticker());
        }
        long volumeRequested = registerOrderCommand.volume();
        long volumeBoughtInSession = 0L;
        List<FinishedTransactionInfo> ordersSoldOut = new ArrayList<>(0);
        Order askOrder = findMatchingBidOrder(registerOrderCommand.ticker(), registerOrderCommand.price());
        if (askOrder == null) {
            Order notCompletedOrder = Order.factorize(registerOrderCommand);
            asksOrdersByTicker.get(registerOrderCommand.ticker())
                              .offer(notCompletedOrder);
            return OrderRegistrationResult.limitOrderPlacedSuccessfully(registerOrderCommand);
        }
        else {
            while (askOrder != null && volumeBoughtInSession < volumeRequested) {
                Long askVolume = askOrder.getVolume();
                long howMoreVolumeToFillYet = volumeRequested - volumeBoughtInSession;
                if (howMoreVolumeToFillYet >= askVolume) {
                    FinishedTransactionInfo finishedTransactionInfo = askOrder.bought();
                    volumeBoughtInSession = volumeBoughtInSession + finishedTransactionInfo.volume();
                    ordersSoldOut.add(finishedTransactionInfo);
                }
                else {
                    FinishedTransactionInfo finishedTransactionInfo = askOrder.boughtPartially(howMoreVolumeToFillYet);
                    ordersSoldOut.add(finishedTransactionInfo);
                    volumeBoughtInSession = volumeBoughtInSession + howMoreVolumeToFillYet;
                    ordersQueue.offer(askOrder);
                    break;
                }
                askOrder = findMatchingBidOrder(registerOrderCommand.ticker(), registerOrderCommand.price());
            }
            if (volumeBoughtInSession < volumeRequested) {
                OrderRegistrationResult partiallyCompleted = OrderRegistrationResult.limitOrderPartiallyCompleted(ordersSoldOut, registerOrderCommand);
                CurrentQuotation currentQuote = quotationProvider.getCurrentQuote(registerOrderCommand.ticker());
                RegisterOrderCommand afterPartialFill = registerOrderCommand.createAfterPartialFillment(currentQuote, partiallyCompleted.orderFillDetails()
                                                                                                                                        .volumePending());
                processAskLimitOrder(afterPartialFill);
                return partiallyCompleted;
            }
            else {
                return OrderRegistrationResult.limitOrderFullyCompleted(ordersSoldOut, registerOrderCommand);
            }
        }
    }

    private Order findMatchingBidOrder(String ticker, BigDecimal askPriceLimit) {
        Queue<Order> orders = bidsOrdersByTicker.get(ticker);
        if (orders == null) {
            return null;
        }
        Order bestPriceAskOrder = orders.peek();
        if (bestPriceAskOrder != null) {
            BigDecimal bestAskPrice = bestPriceAskOrder.getPrice();
            if (askPriceLimit.compareTo(bestAskPrice) < 0) {
                return orders.poll();
            }
        }
        return null;
    }
}
