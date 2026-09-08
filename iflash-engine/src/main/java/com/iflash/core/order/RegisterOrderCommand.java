package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;

import java.math.BigDecimal;
import java.util.UUID;

import static com.iflash.core.order.OrderType.LIMIT;
import static com.iflash.core.order.OrderType.MARKET;

public record RegisterOrderCommand(UUID orderId,
                                   OrderDirection orderDirection,
                                   OrderType orderType,
                                   String ticker,
                                   BigDecimal price,
                                   Long volume) {

    public RegisterOrderCommand withPrice(CurrentQuotation currentQuotation) {
        BigDecimal price = determinePrice(currentQuotation);
        return new RegisterOrderCommand(orderId, orderDirection, orderType, ticker, price, volume);
    }

    public RegisterOrderCommand createAfterPartialFillment(CurrentQuotation currentQuotation, Long volume) {
        return new RegisterOrderCommand(orderId, orderDirection, LIMIT, ticker, currentQuotation.price(), volume);
    }

    private BigDecimal determinePrice(CurrentQuotation currentQuotation) {
        if (MARKET == orderType) {
            return currentQuotation.price();
        }
        else if (LIMIT == orderType) {
            return price;
        }
        throw new RuntimeException("OrderType not supported for price determining");
    }
}
