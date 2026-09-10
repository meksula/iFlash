package com.iflash.instrument.api;

import java.math.BigDecimal;

record FinancialInstrumentInfo(String ticker, BigDecimal currentPrice) {
}
