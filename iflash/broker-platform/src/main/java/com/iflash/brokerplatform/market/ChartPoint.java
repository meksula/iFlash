package com.iflash.brokerplatform.market;

import java.math.BigDecimal;

/** A point for the price chart: {@code time} in epoch seconds (lightweight-charts UTCTimestamp). */
public record ChartPoint(long time, BigDecimal value) {
}
