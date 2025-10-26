package com.iflash.core.quotation;

import com.iflash.core.order.FinishedTransactionInfo;

import java.math.BigDecimal;

public record QuotableInformation(long volume, BigDecimal price) {

    public QuotableInformation of(FinishedTransactionInfo finishedTransactionInfo) {
        return new QuotableInformation(finishedTransactionInfo.volume(), finishedTransactionInfo.price());
    }
}
