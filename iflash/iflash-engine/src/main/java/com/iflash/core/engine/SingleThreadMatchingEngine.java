package com.iflash.core.engine;

import com.iflash.core.order.OrderBook;
import com.iflash.core.quotation.QuotationAggregator;

public class SingleThreadMatchingEngine implements MatchingEngine {

    private final OrderBook orderBook;
    private final QuotationAggregator quotationAggregator;

    private SingleThreadMatchingEngine(OrderBook orderBook, QuotationAggregator quotationAggregator) {
        this.orderBook = orderBook;
        this.quotationAggregator = quotationAggregator;
    }

    public static SingleThreadMatchingEngine create(OrderBook orderBook, QuotationAggregator quotationAggregator) {
        return new SingleThreadMatchingEngine(orderBook, quotationAggregator);
    }

    @Override
    public MatchingEngineState initialize() {
        return null;
    }
}
