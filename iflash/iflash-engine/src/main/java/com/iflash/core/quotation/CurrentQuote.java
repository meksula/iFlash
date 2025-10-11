package com.iflash.core.quotation;

import java.math.BigDecimal;

public record CurrentQuote(long timestamp, BigDecimal price) {
}
