package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

public record OrderBookEntryDto(String orderCreationDate, BigDecimal price, Long volume) {
}
