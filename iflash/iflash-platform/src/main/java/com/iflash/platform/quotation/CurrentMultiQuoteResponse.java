package com.iflash.platform.quotation;

import com.iflash.commons.Page;
import com.iflash.core.quotation.CurrentQuotation;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record CurrentMultiQuoteResponse(ZonedDateTime responseZonedDateTime, String ticker, Page<CurrentMultiQuote> quotations) {

    static CurrentMultiQuoteResponse create(Page<CurrentQuotation> currentQuotations, String ticker) {
        Page<CurrentMultiQuote> currentMultiQuotes = currentQuotations.map(currentQuote -> new CurrentMultiQuote(currentQuote.timestamp(), currentQuote.price()));
        return new CurrentMultiQuoteResponse(ZonedDateTime.now(), ticker, currentMultiQuotes);
    }

    record CurrentMultiQuote(long quoteTimestamp, BigDecimal price) {
    }
}
