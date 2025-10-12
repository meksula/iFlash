package com.iflash.core.engine;

import com.iflash.core.quotation.QuotationProvider;

import java.util.List;

public interface MatchingEngine {

    MatchingEngineState initialize(List<TickerRegistrationCommand> companies);

    QuotationProvider quotationProvider();

    TradingOperations tradingOperations();
}
