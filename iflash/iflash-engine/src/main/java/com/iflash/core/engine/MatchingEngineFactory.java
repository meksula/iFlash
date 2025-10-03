package com.iflash.core.engine;

import com.iflash.core.configuration.GlobalSettings;
import com.iflash.core.order.OrderBook;
import com.iflash.core.order.OrderBookFactory;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationAggregatorFactory;

public class MatchingEngineFactory {

    public MatchingEngine factorize(MatchingEngineType matchingEngineType) {
        return switch (matchingEngineType) {
            case SINGLE_THREAD_ENGINE -> buildSingleThreadEngine();
        };
    }

    private MatchingEngine buildSingleThreadEngine() {
        OrderBook orderBook = OrderBookFactory.factorizeOrderBook();
        QuotationAggregator quotationAggregator = QuotationAggregatorFactory.factorizeQuotationAggregator(GlobalSettings.QUOTATION_CALCULABLE);

        return SingleThreadMatchingEngine.create(orderBook, quotationAggregator);
    }
}
