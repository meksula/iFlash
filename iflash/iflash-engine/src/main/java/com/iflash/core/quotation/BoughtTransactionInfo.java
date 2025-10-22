package com.iflash.core.quotation;

import com.iflash.core.order.FinishedTransactionInfo;

import java.math.BigDecimal;

public record BoughtTransactionInfo(long volume, BigDecimal price) {

    public BoughtTransactionInfo of(FinishedTransactionInfo finishedTransactionInfo) {
        return new BoughtTransactionInfo(finishedTransactionInfo.volume(), finishedTransactionInfo.price());
    }
}
