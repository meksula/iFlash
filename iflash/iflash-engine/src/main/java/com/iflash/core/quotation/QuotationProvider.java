package com.iflash.core.quotation;

import java.util.List;

public interface QuotationProvider {

    CurrentQuote getCurrentQuote(String ticker);

    List<CurrentQuote> getLastQuotes(String ticker, int limit, OrderBy orderBy);
}
