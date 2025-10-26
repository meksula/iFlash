package com.iflash.core.quotation;

import com.iflash.commons.OrderBy;
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
    public List<CurrentQuotation> getLastQuotes(String ticker, int limit, OrderBy orderBy) {
        List<Quotation> quotationList = lastPriceQuotation.get(ticker);
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
