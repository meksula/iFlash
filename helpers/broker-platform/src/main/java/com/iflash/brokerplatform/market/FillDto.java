package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** A single executed transaction returned by the engine. */
public record FillDto(long volume, BigDecimal price) {
}
