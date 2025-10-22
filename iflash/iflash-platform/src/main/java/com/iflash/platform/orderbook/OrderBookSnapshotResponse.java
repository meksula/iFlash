package com.iflash.platform.orderbook;

import com.iflash.commons.Page;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record OrderBookSnapshotResponse(ZonedDateTime responseZonedDateTime, String ticker, Page<OrderBookEntry> asks) {
    record OrderBookEntry(ZonedDateTime orderCreationDate, BigDecimal price, Long volume) {}
}
