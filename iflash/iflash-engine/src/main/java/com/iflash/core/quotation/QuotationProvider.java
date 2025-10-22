package com.iflash.core.quotation;

import com.iflash.commons.OrderBy;
import com.iflash.core.engine.FinancialInstrumentInfo;

import java.util.List;

public interface QuotationProvider {

    CurrentQuote getCurrentQuote(String ticker);

    List<CurrentQuote> getLastQuotes(String ticker, int limit, OrderBy orderBy);

    List<FinancialInstrumentInfo> getAllTickersWithQuotation();
}
