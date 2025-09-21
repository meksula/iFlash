package com.iflash.engine.order;

import java.math.BigDecimal;

public record RegisterOrderCommand(OrderDirection orderDirection,
                                   OrderType orderType,
                                   String ticker,
                                   BigDecimal price) {
}
