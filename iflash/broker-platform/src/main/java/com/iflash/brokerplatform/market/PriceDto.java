package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** GET /api/v1/quotation/{ticker}/price (unknown fields ignored by Jackson). */
public record PriceDto(long quoteTimestamp, String ticker, BigDecimal price) {
}
