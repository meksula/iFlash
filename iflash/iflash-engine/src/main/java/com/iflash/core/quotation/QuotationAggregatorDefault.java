package com.iflash.core.quotation;

import java.util.List;

public class QuotationAggregatorDefault implements QuotationAggregator {

    private final QuotationCalculable quotationCalculable;

    public QuotationAggregatorDefault(QuotationCalculable quotationCalculable) {
        this.quotationCalculable = quotationCalculable;
    }

    @Override
    public void handle(String ticker, List<BoughtTransactionInfo> boughtTransactionInfos) {
        Quotation calculate = quotationCalculable.calculate(ticker, boughtTransactionInfos);
    }
}
