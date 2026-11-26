package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

public record InstrumentQuote(String ticker, BigDecimal currentPrice, BigDecimal dailyChangePercent) {

    public static InstrumentQuote unavailable(String ticker) {
        return new InstrumentQuote(ticker, null, null);
    }
}
