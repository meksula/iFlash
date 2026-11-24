package com.iflash.mes.quotation;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.engine.FinancialInstrumentInfo;
import com.iflash.core.order.FinishedTransactionInfo;
import com.iflash.core.order.OrderInformation;
import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class QuotationPublishingAggregator implements QuotationAggregator, QuotationProvider {

    private final QuotationAggregator quotationAggregator;
    private final QuotationProvider quotationProvider;
    private final QuotationChangedPublisher quotationChangedPublisher;

    public QuotationPublishingAggregator(QuotationAggregator quotationAggregator, QuotationChangedPublisher quotationChangedPublisher) {
        this.quotationAggregator = quotationAggregator;
        this.quotationProvider = (QuotationProvider) quotationAggregator;
        this.quotationChangedPublisher = quotationChangedPublisher;
    }

    @Override
    public synchronized void calculateQuotationPostTransaction(String ticker, List<FinishedTransactionInfo> finishedTransactionInfos) {
        quotationAggregator.calculateQuotationPostTransaction(ticker, finishedTransactionInfos);
        if (!finishedTransactionInfos.isEmpty()) {
            quotationChangedPublisher.publish(QuotationChangedEvent.of(ticker, quotationProvider.getCurrentQuote(ticker)));
        }
    }

    @Override
    public void calculateTheoreticalQuotation(String ticker, Set<OrderInformation> topBids, Set<OrderInformation> topAsks) {
        quotationAggregator.calculateTheoreticalQuotation(ticker, topBids, topAsks);
    }

    @Override
    public void initTicker(String ticker, BigDecimal initialPrice) {
        quotationAggregator.initTicker(ticker, initialPrice);
    }

    @Override
    public CurrentQuotation getCurrentQuote(String ticker) {
        return quotationProvider.getCurrentQuote(ticker);
    }

    @Override
    public Page<CurrentQuotation> getLastQuotes(String ticker, Pagination pagination) {
        return quotationProvider.getLastQuotes(ticker, pagination);
    }

    @Override
    public List<FinancialInstrumentInfo> getAllTickersWithQuotation() {
        return quotationProvider.getAllTickersWithQuotation();
    }
}
