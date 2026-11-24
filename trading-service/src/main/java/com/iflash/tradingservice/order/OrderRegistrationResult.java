package com.iflash.tradingservice.order;

import java.math.BigDecimal;
import java.util.List;

record OrderRegistrationResult(List<FinishedTransactionInfo> finishedTransactionInfoList) {

    record FinishedTransactionInfo(long volume, BigDecimal price) {
    }
}
