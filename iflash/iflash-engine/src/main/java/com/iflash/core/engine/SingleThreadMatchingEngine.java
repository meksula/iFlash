package com.iflash.core.engine;

import com.iflash.core.order.OrderBook;
import com.iflash.core.order.OrderRegistrationResult;
import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationProvider;

import java.util.concurrent.CompletableFuture;

public class SingleThreadMatchingEngine implements MatchingEngine, TradingOperations {

    private final OrderBook orderBook;
    private final QuotationAggregator quotationAggregator;
    private final QuotationProvider quotationProvider;

    private SingleThreadMatchingEngine(OrderBook orderBook, QuotationAggregator quotationAggregator) {
        this.orderBook = orderBook;
        this.quotationAggregator = quotationAggregator;
        this.quotationProvider = (QuotationProvider) quotationAggregator;
    }

    public static SingleThreadMatchingEngine create(OrderBook orderBook, QuotationAggregator quotationAggregator) {
        return new SingleThreadMatchingEngine(orderBook, quotationAggregator);
    }

    @Override
    public MatchingEngineState initialize() {
        // todo in near future more advanced initialization will be available
        return null;
    }

    @Override
    public QuotationProvider quotationProvider() {
        return quotationProvider;
    }

    @Override
    public TradingOperations tradingOperations() {
        return this;
    }

    @Override
    public OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand) {
        OrderRegistrationResult orderRegistrationResult = orderBook.registerOrder(registerOrderCommand);

        CompletableFuture.runAsync(() -> quotationAggregator.handle(registerOrderCommand.ticker(), orderRegistrationResult.transactionInfoList()));

        return orderRegistrationResult;
    }
}
