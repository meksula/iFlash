package com.iflash.core.quotation;

import java.math.BigDecimal;

public record BoughtTransactionInfo(long volume, BigDecimal price) {
}
