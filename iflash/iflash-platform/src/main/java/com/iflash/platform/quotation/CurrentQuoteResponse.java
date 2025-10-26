package com.iflash.platform.quotation;

import com.iflash.core.quotation.CurrentQuotation;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record CurrentQuoteResponse(ZonedDateTime responseZonedDateTime, long quoteTimestamp, String ticker, BigDecimal price) {

    static CurrentQuoteResponse create(CurrentQuotation currentQuotation, String ticker) {
        return new CurrentQuoteResponse(ZonedDateTime.now(), currentQuotation.timestamp(), ticker, currentQuotation.price());
    }
}
