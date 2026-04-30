package com.iflash.toolkit;

import com.iflash.core.order.OrderDirection;

import java.math.BigDecimal;

public record OrderBookSnapshotResponse(String ticker, OrderDirection orderDirection, Page data) {
    public record OrderBookEntry(BigDecimal price, Long volume) {}
}
