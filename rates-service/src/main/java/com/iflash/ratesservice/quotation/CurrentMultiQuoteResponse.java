package com.iflash.ratesservice.quotation;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record CurrentMultiQuoteResponse(ZonedDateTime responseZonedDateTime, String ticker, Page<CurrentMultiQuote> quotations) {

    static CurrentMultiQuoteResponse create(Page<Quotation> quotations, String ticker) {
        Page<CurrentMultiQuote> currentMultiQuotes = quotations.map(quotation -> new CurrentMultiQuote(quotation.timestamp(), quotation.price()));
        return new CurrentMultiQuoteResponse(ZonedDateTime.now(), ticker, currentMultiQuotes);
    }

    record CurrentMultiQuote(long quoteTimestamp, BigDecimal price) {
    }
}
