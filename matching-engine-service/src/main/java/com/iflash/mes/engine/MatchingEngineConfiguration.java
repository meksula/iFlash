package com.iflash.mes.engine;

import com.iflash.core.configuration.GlobalSettings;
import com.iflash.core.engine.MatchingEngine;
import com.iflash.core.engine.OrderBookOperations;
import com.iflash.core.engine.SingleThreadMatchingEngine;
import com.iflash.core.engine.TradingOperations;
import com.iflash.core.order.OrderBook;
import com.iflash.core.order.OrderBookFactory;
import com.iflash.core.quotation.QuotationAggregatorFactory;
import com.iflash.mes.quotation.QuotationChangedPublisher;
import com.iflash.mes.quotation.QuotationPublishingAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
class MatchingEngineConfiguration {

    @Bean
    QuotationPublishingAggregator quotationPublishingAggregator(QuotationChangedPublisher quotationChangedPublisher) {
        return new QuotationPublishingAggregator(QuotationAggregatorFactory.factorizeQuotationAggregator(GlobalSettings.QUOTATION_CALCULABLE), quotationChangedPublisher);
    }

    @Bean
    MatchingEngine matchingEngine(QuotationPublishingAggregator quotationPublishingAggregator) {
        log.info("Single Thread Matching Engine starting");

        OrderBook orderBook = OrderBookFactory.factorizeOrderBook(quotationPublishingAggregator);
        SingleThreadMatchingEngine singleThreadMatchingEngine = SingleThreadMatchingEngine.create(orderBook, quotationPublishingAggregator);

        log.info("Single Thread Matching Engine successfully initialized and ready for trading");
        return singleThreadMatchingEngine;
    }

    @Bean
    TradingOperations tradingOperations(MatchingEngine matchingEngine) {
        return matchingEngine.tradingOperations();
    }

    @Bean
    OrderBookOperations orderBookOperations(MatchingEngine matchingEngine) {
        return matchingEngine.orderBookOperations();
    }
}
