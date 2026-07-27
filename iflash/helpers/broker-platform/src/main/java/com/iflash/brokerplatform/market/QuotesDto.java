package com.iflash.brokerplatform.market;

/** GET /api/v1/quotation/{ticker}/quotes. */
public record QuotesDto(String ticker, PageDto<QuoteDto> quotations) {
}
