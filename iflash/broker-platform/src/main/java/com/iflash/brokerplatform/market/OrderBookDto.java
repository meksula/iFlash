package com.iflash.brokerplatform.market;

/** GET /api/v1/orderbook/{ticker} (one side per call). */
public record OrderBookDto(String ticker, String orderDirection, PageDto<OrderBookEntryDto> data) {
}
