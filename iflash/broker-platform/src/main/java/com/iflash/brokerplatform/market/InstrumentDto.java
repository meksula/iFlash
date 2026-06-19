package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** GET /api/v1/instrument element. */
public record InstrumentDto(String ticker, BigDecimal currentPrice) {
}
