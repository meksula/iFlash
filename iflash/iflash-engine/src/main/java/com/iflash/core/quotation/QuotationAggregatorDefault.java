package com.iflash.core.quotation;

import com.iflash.core.order.OrderBookException;
import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.order.TransactionInfo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.iflash.core.order.OrderDirection.BUY;
import static com.iflash.core.order.OrderDirection.SELL;

public class QuotationAggregatorDefault implements QuotationAggregator, QuotationProvider {

    private final QuotationCalculable quotationCalculable;
    private final Map<String, List<Quotation>> quotations;

    public QuotationAggregatorDefault(QuotationCalculable quotationCalculable) {
        this.quotationCalculable = quotationCalculable;
        this.quotations = new HashMap<>();
    }

    @Override
    public void handle(RegisterOrderCommand registerOrderCommand, List<TransactionInfo> transactionInfos) {
        if (SELL == registerOrderCommand.orderDirection()) {
            // todo
        }
        if (BUY == registerOrderCommand.orderDirection()) {
            String ticker = registerOrderCommand.ticker();
            List<BoughtTransactionInfo> boughtTransactionInfoList = transactionInfos.stream()
                                                                                    .map(transactionInfo -> new BoughtTransactionInfo(transactionInfo.volume(), transactionInfo.price()))
                                                                                    .toList();
            Quotation quotation = quotationCalculable.calculate(ticker, boughtTransactionInfoList);
            List<Quotation> quotationList = quotations.get(ticker);
            if (quotationList != null) {
                quotationList.add(quotation);
            } else {
                List<Quotation> quotationsNotPresent = new ArrayList<>();
                quotationsNotPresent.add(quotation);
                quotations.putIfAbsent(ticker, quotationsNotPresent);
            }
        }
    }

    @Override
    public CurrentQuote getCurrentQuote(String ticker) {
        List<Quotation> quotationList = quotations.get(ticker);
        if (quotationList == null) {
            throw OrderBookException.noTicker(ticker);
        } else {
            return quotations.get(ticker)
                             .getLast()
                             .map();
        }
    }

    @Override
    public void initTicker(String ticker, BigDecimal initialPrice) {
        List<Quotation> quotationList = new ArrayList<>();
        Quotation quotation = new Quotation(ticker, System.currentTimeMillis(), 0L, initialPrice);
        quotationList.add(quotation);
        quotations.putIfAbsent(ticker, quotationList);
    }
}
