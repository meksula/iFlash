package com.iflash.core.quotation;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.engine.FinancialInstrumentInfo;
import com.iflash.core.order.OrderBookException;
import com.iflash.core.order.OrderInformation;
import com.iflash.core.order.FinishedTransactionInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QuotationAggregatorDefault implements QuotationAggregator, QuotationProvider {

    private final QuotationCalculable quotationCalculable;
    private final Map<String, List<Quotation>> lastPriceQuotation;
    private final Map<String, List<Quotation>> theoreticalQuotation;

    public QuotationAggregatorDefault(QuotationCalculable quotationCalculable, Map<String, List<Quotation>> lastPriceQuotation, Map<String, List<Quotation>> theoreticalQuotation) {
        this.quotationCalculable = quotationCalculable;
        this.lastPriceQuotation = lastPriceQuotation;
        this.theoreticalQuotation = theoreticalQuotation;
    }

    @Override
    public void calculateQuotationPostTransaction(String ticker, List<FinishedTransactionInfo> finishedTransactionInfos) {
        if (finishedTransactionInfos.isEmpty()) {
            return;
        }
        List<QuotableInformation> quotableInformationList = finishedTransactionInfos.stream()
                                                                                    .map(transactionInfo -> new QuotableInformation(transactionInfo.volume(),
                                                                                                                                    transactionInfo.price()))
                                                                                    .toList();
        Quotation quotation = quotationCalculable.calculate(ticker, quotableInformationList);
        List<Quotation> quotationList = lastPriceQuotation.get(ticker);
        if (quotationList != null) {
            quotationList.add(quotation);
        }
        else {
            List<Quotation> quotationsNotPresent = new ArrayList<>();
            quotationsNotPresent.add(quotation);
            lastPriceQuotation.putIfAbsent(ticker, quotationsNotPresent);
        }
    }

    @Override
    public void calculateTheoreticalQuotation(String ticker, Set<OrderInformation> topBids, Set<OrderInformation> topAsks) {
        List<QuotableInformation> topBidsQuotable = topBids.stream()
                                                           .map(transactionInfo -> new QuotableInformation(transactionInfo.volume(),
                                                                                                           transactionInfo.price()))
                                                           .toList();
        List<QuotableInformation> topAsksQuotable = topAsks.stream()
                                                           .map(transactionInfo -> new QuotableInformation(transactionInfo.volume(),
                                                                                                           transactionInfo.price()))
                                                           .toList();

        Quotation topBidsQuotation = quotationCalculable.calculate(ticker, topBidsQuotable);
        Quotation topAsksQuotation = quotationCalculable.calculate(ticker, topAsksQuotable);

        BigDecimal quotation = topBidsQuotation.quotation()
                                               .setScale(4, RoundingMode.HALF_UP)
                                               .add(topAsksQuotation.quotation()
                                                                    .setScale(4, RoundingMode.HALF_UP))
                                               .divide(BigDecimal.TWO, RoundingMode.HALF_UP);
        Quotation finalQuotation = new Quotation(ticker, System.currentTimeMillis(), 0, quotation);

        List<Quotation> quotationList = theoreticalQuotation.get(ticker);
        if (quotationList != null) {
            quotationList.add(finalQuotation);
        }
        else {
            List<Quotation> quotationsNotPresent = new ArrayList<>();
            quotationsNotPresent.add(finalQuotation);
            theoreticalQuotation.putIfAbsent(ticker, quotationsNotPresent);
        }
    }

    @Override
    public CurrentQuotation getCurrentQuote(String ticker) {
        List<Quotation> quotationList = lastPriceQuotation.get(ticker);
        if (quotationList == null) {
            throw OrderBookException.noTicker(ticker);
        }
        else {
            return lastPriceQuotation.get(ticker)
                                     .getLast()
                                     .map();
        }
    }

    @Override
    public Page<CurrentQuotation> getLastQuotes(String ticker, Pagination pagination) {
        if (pagination.size() <= 0) {
            throw new IllegalStateException("Cannot get last quotes for size value less or equal to 0");
        }
        if (pagination.page() < 0) {
            throw new IllegalStateException("Cannot get last quotes for page value less than 0");
        }
        List<Quotation> quotationList = lastPriceQuotation.get(ticker);
        if (quotationList == null || quotationList.isEmpty()) {
            return Page.of(List.of(), pagination);
        }
        List<Quotation> orderedQuotations = new ArrayList<>(quotationList);

        switch (pagination.orderBy()) {
            case ASC -> {
            }
            case DESC -> Collections.reverse(orderedQuotations);
        }

        int fromIndex = pagination.page() * pagination.size();
        if (fromIndex >= orderedQuotations.size()) {
            return Page.of(List.of(), pagination);
        }

        int toIndex = Math.min(fromIndex + pagination.size(), orderedQuotations.size());
        List<CurrentQuotation> currentQuotations = orderedQuotations.subList(fromIndex, toIndex)
                                                                    .stream()
                                                                    .map(Quotation::map)
                                                                    .collect(Collectors.toList());
        return Page.of(currentQuotations, pagination);
    }

    @Override
    public void initTicker(String ticker, BigDecimal initialPrice) {
        List<Quotation> lastPriceQuotationList = new ArrayList<>();
        List<Quotation> theoreticalQuotationList = new ArrayList<>();

        Quotation lastPriceQuotation = new Quotation(ticker, System.currentTimeMillis(), 0L, initialPrice);
        Quotation theoreticalQuotation = new Quotation(ticker, System.currentTimeMillis(), 0L, initialPrice);

        lastPriceQuotationList.add(lastPriceQuotation);
        theoreticalQuotationList.add(theoreticalQuotation);

        this.lastPriceQuotation.putIfAbsent(ticker, lastPriceQuotationList);
        this.theoreticalQuotation.putIfAbsent(ticker, theoreticalQuotationList);
    }

    @Override
    public List<FinancialInstrumentInfo> getAllTickersWithQuotation() {
        return lastPriceQuotation.entrySet()
                                 .stream()
                                 .map(entry -> new FinancialInstrumentInfo(entry.getKey(),
                                                                   entry.getValue()
                                                                        .getLast()
                                                                        .quotation()))
                                 .sorted(Comparator.comparing(FinancialInstrumentInfo::ticker))
                                 .collect(Collectors.toList());
    }
}
