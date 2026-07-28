package com.iflash.platform.bootstrap;

import com.iflash.core.engine.MatchingEngine;
import com.iflash.core.engine.MatchingEngineFactory;
import com.iflash.core.engine.MatchingEngineType;
import com.iflash.core.engine.OrderBookOperations;
import com.iflash.core.engine.TradingOperations;
import com.iflash.core.quotation.QuotationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineBootstrapper {

    @Value("${engine.type}")
    private MatchingEngineType matchingEngineType;

    @Bean(name = "matchingEngine")
    public MatchingEngine bootstrapMatchingEngine() {
        return MatchingEngineFactory.factorize(matchingEngineType);
    }

    @Bean(name = "quotationProvider")
    public QuotationProvider quotationProvider(MatchingEngine matchingEngine) {
        return matchingEngine.quotationProvider();
    }

    @Bean(name = "tradingOperations")
    public TradingOperations tradingOperations(MatchingEngine matchingEngine) {
        return matchingEngine.tradingOperations();
    }

    @Bean(name = "orderBookOperations")
    public OrderBookOperations orderBookOperations(MatchingEngine matchingEngine) {
        return matchingEngine.orderBookOperations();
    }
}
