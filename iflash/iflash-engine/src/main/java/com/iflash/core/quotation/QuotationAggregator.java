package com.iflash.core.quotation;

import com.iflash.core.order.TransactionInfo;

import java.util.List;

public interface QuotationAggregator {

    void handle(String ticker, List<TransactionInfo> boughtTransactionInfos);
}
