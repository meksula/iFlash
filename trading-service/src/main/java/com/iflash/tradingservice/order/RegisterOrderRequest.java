package com.iflash.tradingservice.order;

import java.math.BigDecimal;
import java.util.UUID;

record RegisterOrderRequest(UUID orderId,
                            OrderDirection orderDirection,
                            OrderType orderType,
                            String ticker,
                            BigDecimal price,
                            Long volume) {
}
