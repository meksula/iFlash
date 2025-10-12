package com.iflash.core.quotation;

import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.order.TransactionInfo;

import java.math.BigDecimal;
import java.util.List;

public interface QuotationAggregator {

    void handle(RegisterOrderCommand registerOrderCommand, List<TransactionInfo> boughtTransactionInfos);

    void initTicker(String ticker, BigDecimal initialPrice);
}
