package com.iflash.ratesservice.quotation;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record CurrentQuoteResponse(ZonedDateTime responseZonedDateTime, long quoteTimestamp, String ticker, BigDecimal price) {

    static CurrentQuoteResponse create(Quotation quotation, String ticker) {
        return new CurrentQuoteResponse(ZonedDateTime.now(), quotation.timestamp(), ticker, quotation.price());
    }
}
