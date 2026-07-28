package com.iflash.core.quotation;

import java.math.BigDecimal;

public record CurrentQuotation(long timestamp, BigDecimal price) {
}
