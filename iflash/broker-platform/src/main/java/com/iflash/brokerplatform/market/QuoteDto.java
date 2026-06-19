package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** A single historical quotation point. */
public record QuoteDto(long quoteTimestamp, BigDecimal price) {
}
