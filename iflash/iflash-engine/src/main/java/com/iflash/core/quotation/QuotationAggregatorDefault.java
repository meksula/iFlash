package com.iflash.core.quotation;

import com.iflash.commons.OrderBy;
import com.iflash.core.engine.FinancialInstrumentInfo;
import com.iflash.core.order.OrderBookException;
import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.order.FinishedTransactionInfo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.iflash.core.order.OrderDirection.BID;

public class QuotationAggregatorDefault implements QuotationAggregator, QuotationProvider {

    private final QuotationCalculable quotationCalculable;
    private final Map<String, List<Quotation>> quotations;

    public QuotationAggregatorDefault(QuotationCalculable quotationCalculable, Map<String, List<Quotation>> quotations) {
        this.quotationCalculable = quotationCalculable;
        this.quotations = quotations;
    }

    @Override
    public void handle(RegisterOrderCommand registerOrderCommand, List<FinishedTransactionInfo> finishedTransactionInfos) {
        if (BID == registerOrderCommand.orderDirection()) {
            if (finishedTransactionInfos.isEmpty()) {
                return;
            }
            String ticker = registerOrderCommand.ticker();
            List<BoughtTransactionInfo> boughtTransactionInfoList = finishedTransactionInfos.stream()
                                                                                            .map(transactionInfo -> new BoughtTransactionInfo(transactionInfo.volume(),
                                                                                                                                              transactionInfo.price()))
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
    }

    @Override
    public CurrentQuotation getCurrentQuote(String ticker) {
        List<Quotation> quotationList = quotations.get(ticker);
        if (quotationList == null) {
            throw OrderBookException.noTicker(ticker);
        }
        else {
            return quotations.get(ticker)
                             .getLast()
                             .map();
        }
    }

    @Override
    public List<CurrentQuotation> getLastQuotes(String ticker, int limit, OrderBy orderBy) {
        List<Quotation> quotationList = quotations.get(ticker);
        if (quotationList == null || quotationList.isEmpty()) {
            return Collections.emptyList();
        }
        int size = quotationList.size();
        return switch (orderBy) {
            case ASC -> {
                if (limit <= 0) {
                    throw new IllegalStateException("Cannot find Quotations for limit value less or equal to 0");
                }
                if (limit > size) {
                    limit = size;
                }
                yield quotationList.subList(0, limit)
                                   .stream()
                                   .map(Quotation::map)
                                   .collect(Collectors.toList());
            }
            case DESC -> {
                int fromIndex = quotationList.size() - limit;
                List<CurrentQuotation> currentQuotations = quotationList.subList(Math.max(fromIndex, 0), quotationList.size())
                                                                        .stream()
                                                                        .map(Quotation::map)
                                                                        .collect(Collectors.toList());
                Collections.reverse(currentQuotations);
                yield currentQuotations;
            }
        };
    }

    @Override
    public void initTicker(String ticker, BigDecimal initialPrice) {
        List<Quotation> quotationList = new ArrayList<>();
        Quotation quotation = new Quotation(ticker, System.currentTimeMillis(), 0L, initialPrice);
        quotationList.add(quotation);
        quotations.putIfAbsent(ticker, quotationList);
    }

    @Override
    public List<FinancialInstrumentInfo> getAllTickersWithQuotation() {
        return quotations.entrySet()
                         .stream()
                         .map(entry -> new FinancialInstrumentInfo(entry.getKey(),
                                                                   entry.getValue()
                                                                        .getLast()
                                                                        .quotation()))
                         .sorted(Comparator.comparing(FinancialInstrumentInfo::ticker))
                         .collect(Collectors.toList());
    }
}
