package com.iflash.core.quotation;

public interface QuotationProvider {

    CurrentQuote getCurrentQuote(String ticker);
}
