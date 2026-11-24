package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** GET /api/v1/instrument element (reference data, no current price). */
record InstrumentReferenceDto(String ticker, BigDecimal initialPrice) {
}
