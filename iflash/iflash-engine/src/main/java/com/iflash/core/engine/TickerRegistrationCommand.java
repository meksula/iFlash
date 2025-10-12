package com.iflash.core.engine;

import java.math.BigDecimal;

public record TickerRegistrationCommand(String ticker, BigDecimal initialPrice) {
}
