package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** POST /api/v1/trade/order body. {@code price} may be null for MARKET orders. */
public record OrderRequestDto(String orderDirection, String orderType, String ticker, BigDecimal price, Long volume) {
}
