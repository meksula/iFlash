package com.iflash.core.quotation;

import java.util.List;

public interface QuotationAggregator {

    void handle(String ticker, List<BoughtTransactionInfo> boughtTransactionInfos);
}
