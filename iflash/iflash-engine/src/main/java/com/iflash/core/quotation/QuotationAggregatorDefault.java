package com.iflash.core.quotation;

import com.iflash.core.order.TransactionInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuotationAggregatorDefault implements QuotationAggregator, QuotationProvider {

    private final QuotationCalculable quotationCalculable;
    private final Map<String, List<Quotation>> quotations;

    public QuotationAggregatorDefault(QuotationCalculable quotationCalculable) {
        this.quotationCalculable = quotationCalculable;
        this.quotations = new HashMap<>();
    }

    @Override
    public void handle(String ticker, List<TransactionInfo> transactionInfos) {
        List<BoughtTransactionInfo> boughtTransactionInfoList = transactionInfos.stream()
                                                                                .map(transactionInfo -> new BoughtTransactionInfo(transactionInfo.volume(), transactionInfo.price()))
                                                                                .toList();
        Quotation quotation = quotationCalculable.calculate(ticker, boughtTransactionInfoList);
        List<Quotation> quotationList = quotations.get(ticker);
        if (quotationList != null) {
            quotationList.add(quotation);
        }
        else {
            List<Quotation> quotationsNotPresent = new ArrayList<>();
            quotationsNotPresent.add(quotation);
            quotations.putIfAbsent(ticker, quotationsNotPresent);
        }
    }

    @Override
    public CurrentQuote getCurrentQuote(String ticker) {
        return quotations.get(ticker)
                         .getLast()
                         .map();
    }
}
