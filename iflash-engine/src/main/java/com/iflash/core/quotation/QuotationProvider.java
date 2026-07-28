package com.iflash.core.quotation;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.engine.FinancialInstrumentInfo;

import java.util.List;

public interface QuotationProvider {

    CurrentQuotation getCurrentQuote(String ticker);

    Page<CurrentQuotation> getLastQuotes(String ticker, Pagination pagination);

    List<FinancialInstrumentInfo> getAllTickersWithQuotation();
}
