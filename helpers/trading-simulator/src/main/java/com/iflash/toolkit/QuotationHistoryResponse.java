package com.iflash.toolkit;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors the {@code GET /api/v1/quotation/{ticker}/quotes} response: a paged list of historical
 * quotes (timestamp + price) wrapped in the engine's {@code Page} envelope.
 */
public record QuotationHistoryResponse(String ticker, QuotesPage quotations) {

    public record QuotesPage(List<Quote> elements) {
    }

    public record Quote(long quoteTimestamp, BigDecimal price) {
    }
}
