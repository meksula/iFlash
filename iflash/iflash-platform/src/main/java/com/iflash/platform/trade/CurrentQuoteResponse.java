package com.iflash.platform.trade;

import com.iflash.core.quotation.CurrentQuote;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record CurrentQuoteResponse(ZonedDateTime responseZonedDateTime, long quoteTimestamp, String ticker, BigDecimal price) {

    static CurrentQuoteResponse create(CurrentQuote currentQuote, String ticker) {
        return new CurrentQuoteResponse(ZonedDateTime.now(), currentQuote.timestamp(), ticker, currentQuote.price());
    }
}
