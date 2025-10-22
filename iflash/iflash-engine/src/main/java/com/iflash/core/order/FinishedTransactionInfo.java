package com.iflash.core.order;

import java.math.BigDecimal;
import java.util.UUID;

public record FinishedTransactionInfo(UUID orderUuid, String ticker, long volume, BigDecimal price) {
}
