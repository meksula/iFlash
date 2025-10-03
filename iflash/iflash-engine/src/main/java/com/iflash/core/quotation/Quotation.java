package com.iflash.core.quotation;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record Quotation(String ticker, ZonedDateTime quotationDate, long volume, BigDecimal quotation) {
}
