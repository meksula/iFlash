package com.iflash.toolkit;

import java.math.BigDecimal;

public record OrderBookSnapshotResponse(String ticker, String orderDirection, Page data) {
    public record OrderBookEntry(BigDecimal price, Long volume) {}
}
