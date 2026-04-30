package com.iflash.trader;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
public class Position {
    private final ZonedDateTime openTime;
    private final long amount;
    private final BigDecimal price;
}
