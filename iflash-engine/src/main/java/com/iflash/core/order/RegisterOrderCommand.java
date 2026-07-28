package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;

import java.math.BigDecimal;
import java.util.UUID;

import static com.iflash.core.order.OrderType.LIMIT;

public record RegisterOrderCommand(UUID orderId,
                                   OrderDirection orderDirection,
                                   OrderType orderType,
                                   String ticker,
                                   BigDecimal price,
                                   Long volume) {

    public RegisterOrderCommand withMarketPricePlusSpread(CurrentQuotation currentQuotation, BigDecimal spread) {
        return new RegisterOrderCommand(orderId, orderDirection, orderType, ticker, currentQuotation.price().add(spread), volume);
    }

    public RegisterOrderCommand createAfterPartialFillment(CurrentQuotation currentQuotation, Long volume) {
        return new RegisterOrderCommand(orderId, orderDirection, LIMIT, ticker, currentQuotation.price(), volume);
    }
}
