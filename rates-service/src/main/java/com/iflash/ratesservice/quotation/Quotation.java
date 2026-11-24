package com.iflash.ratesservice.quotation;

import java.math.BigDecimal;

record Quotation(long timestamp, BigDecimal price) {
}
