package com.iflash.core.engine;

import com.iflash.core.quotation.QuotationProvider;

public interface MatchingEngine {

    MatchingEngineState initialize();

    QuotationProvider quotationProvider();
}
