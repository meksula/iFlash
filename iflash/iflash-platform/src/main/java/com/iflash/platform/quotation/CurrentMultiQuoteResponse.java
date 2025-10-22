package com.iflash.platform.quotation;

import com.iflash.core.quotation.CurrentQuote;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

record CurrentMultiQuoteResponse(ZonedDateTime responseZonedDateTime, String ticker, List<CurrentMultiQuote> quotations) {

    static CurrentMultiQuoteResponse create(List<CurrentQuote> currentQuotes, String ticker) {
        List<CurrentMultiQuote> currentMultiQuotes = currentQuotes.stream()
                                                         .map(currentQuote -> new CurrentMultiQuote(currentQuote.timestamp(), currentQuote.price()))
                                                         .collect(Collectors.toList());
        return new CurrentMultiQuoteResponse(ZonedDateTime.now(), ticker, currentMultiQuotes);
    }

    record CurrentMultiQuote(long quoteTimestamp, BigDecimal price) {
    }
}
