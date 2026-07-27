package com.iflash.toolkit;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public record FinancialInstrumentInfo(String ticker,
                                      @JsonAlias({"price"}) BigDecimal currentPrice) {
}
