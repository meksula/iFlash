package com.iflash.core.order;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionInfo(UUID orderUuid, String ticker, long volume, BigDecimal price) {
}
