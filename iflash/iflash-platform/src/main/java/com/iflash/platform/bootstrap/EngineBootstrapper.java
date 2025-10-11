package com.iflash.platform.bootstrap;

import com.iflash.core.engine.MatchingEngine;
import com.iflash.core.engine.MatchingEngineFactory;
import com.iflash.core.engine.MatchingEngineType;
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
}
