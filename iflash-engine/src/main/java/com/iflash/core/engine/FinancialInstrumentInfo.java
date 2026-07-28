package com.iflash.core.engine;

import java.math.BigDecimal;

public record FinancialInstrumentInfo(String ticker, BigDecimal currentPrice) {
}
