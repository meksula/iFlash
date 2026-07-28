package com.iflash.core.quotation;

import com.iflash.core.order.FinishedTransactionInfo;
import com.iflash.core.order.OrderInformation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface QuotationAggregator {

    void calculateQuotationPostTransaction(String ticker, List<FinishedTransactionInfo> boughtFinishedTransactionInfos);

    void calculateTheoreticalQuotation(String ticker, Set<OrderInformation> topBids, Set<OrderInformation> topAsks);

    void initTicker(String ticker, BigDecimal initialPrice);
}
