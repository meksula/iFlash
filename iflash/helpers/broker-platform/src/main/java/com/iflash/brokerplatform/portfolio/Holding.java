package com.iflash.brokerplatform.portfolio;

import java.math.BigDecimal;

/** A position valued at the current market price. */
public record Holding(String ticker, long quantity, BigDecimal currentPrice, BigDecimal marketValue) {
}
