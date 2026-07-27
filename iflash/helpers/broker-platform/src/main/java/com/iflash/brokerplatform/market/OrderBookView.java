package com.iflash.brokerplatform.market;

import java.math.BigDecimal;
import java.util.List;

/** Both sides of the book for the instrument page. */
public record OrderBookView(List<Level> bids, List<Level> asks) {

    public record Level(BigDecimal price, Long volume) {
    }
}
