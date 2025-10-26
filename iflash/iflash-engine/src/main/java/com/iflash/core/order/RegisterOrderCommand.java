package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;

import java.math.BigDecimal;

import static com.iflash.core.order.OrderType.LIMIT;

public record RegisterOrderCommand(OrderDirection orderDirection,
                                   OrderType orderType,
                                   String ticker,
                                   BigDecimal price,
                                   Long volume) {
    public RegisterOrderCommand withMarketPricePlusSpread(CurrentQuotation currentQuotation, BigDecimal spread) {
        return new RegisterOrderCommand(orderDirection, orderType, ticker, currentQuotation.price().add(spread), volume);
    }

    public RegisterOrderCommand createAfterPartiallyFilled(CurrentQuotation currentQuotation, Long volume) {
        return new RegisterOrderCommand(orderDirection, LIMIT, ticker, currentQuotation.price(), volume);
    }
}
