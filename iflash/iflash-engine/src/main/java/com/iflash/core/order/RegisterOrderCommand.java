package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuote;

import java.math.BigDecimal;

public record RegisterOrderCommand(OrderDirection orderDirection,
                                   OrderType orderType,
                                   String ticker,
                                   BigDecimal price,
                                   Long volume) {
    public RegisterOrderCommand withMarketPricePlusSpread(CurrentQuote currentQuote, BigDecimal spread) {
        return new RegisterOrderCommand(orderDirection, orderType, ticker, currentQuote.price().add(spread), volume);
    }
}
