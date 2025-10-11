package com.iflash.core.quotation;

import com.iflash.core.order.TransactionInfo;

import java.math.BigDecimal;

public record BoughtTransactionInfo(long volume, BigDecimal price) {

    public BoughtTransactionInfo of(TransactionInfo transactionInfo) {
        return new BoughtTransactionInfo(transactionInfo.volume(), transactionInfo.price());
    }
}
