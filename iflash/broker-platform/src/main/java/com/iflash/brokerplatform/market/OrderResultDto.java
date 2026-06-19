package com.iflash.brokerplatform.market;

import java.math.BigDecimal;
import java.util.List;

/** POST /api/v1/trade/order response. */
public record OrderResultDto(String ticker, String orderDirection, String orderType, BigDecimal price, Long volume,
                             List<FillDto> transactions) {
}
