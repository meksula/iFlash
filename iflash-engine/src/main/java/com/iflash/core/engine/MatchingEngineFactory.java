package com.iflash.core.engine;

import com.iflash.core.configuration.GlobalSettings;
import com.iflash.core.order.OrderBook;
import com.iflash.core.order.OrderBookFactory;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationAggregatorFactory;
import com.iflash.core.quotation.QuotationProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchingEngineFactory {

    public static MatchingEngine factorize(MatchingEngineType matchingEngineType) {
        return switch (matchingEngineType) {
            case SINGLE_THREAD_ENGINE -> buildSingleThreadEngine();
        };
    }

    private static MatchingEngine buildSingleThreadEngine() {
        log.info("Single Thread Matching Engine starting");

        QuotationAggregator quotationAggregator = QuotationAggregatorFactory.factorizeQuotationAggregator(GlobalSettings.QUOTATION_CALCULABLE);
        OrderBook orderBook = OrderBookFactory.factorizeOrderBook((QuotationProvider) quotationAggregator);
        SingleThreadMatchingEngine singleThreadMatchingEngine = SingleThreadMatchingEngine.create(orderBook, quotationAggregator);

        log.info("Single Thread Matching Engine successfully initialized and ready for trading");
        return singleThreadMatchingEngine;
    }
}
