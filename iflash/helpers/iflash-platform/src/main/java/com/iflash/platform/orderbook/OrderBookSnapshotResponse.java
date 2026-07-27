package com.iflash.platform.orderbook;

import com.iflash.commons.Page;
import com.iflash.core.order.OrderDirection;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

record OrderBookSnapshotResponse(ZonedDateTime responseZonedDateTime, String ticker, OrderDirection orderDirection, Page<OrderBookEntry> data) {
    record OrderBookEntry(ZonedDateTime orderCreationDate, BigDecimal price, Long volume) {}
}
