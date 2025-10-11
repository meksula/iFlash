package com.iflash.core.engine;

import com.iflash.core.order.OrderBook;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationProvider;

public class SingleThreadMatchingEngine implements MatchingEngine {

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
}
