package com.iflash.ratesservice.quotation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
class QuotationStore {

    private final Map<String, TickerQuotations> quotationsByTicker = new ConcurrentHashMap<>();

    void initTicker(String ticker, BigDecimal initialPrice) {
        quotationsByTicker.computeIfAbsent(ticker, absentTicker -> new TickerQuotations())
                          .initialize(initialPrice);
    }

    void record(String ticker, Quotation quotation) {
        quotationsByTicker.computeIfAbsent(ticker, absentTicker -> new TickerQuotations())
                          .record(quotation);
    }

    Quotation currentQuote(String ticker) {
        TickerQuotations tickerQuotations = quotationsByTicker.get(ticker);
        if (tickerQuotations == null) {
            throw QuotationException.noTicker(ticker);
        }
        return tickerQuotations.current();
    }

    Page<Quotation> lastQuotes(String ticker, Pagination pagination) {
        if (pagination.size() <= 0) {
            throw new IllegalStateException("Cannot get last quotes for size value less or equal to 0");
        }
        if (pagination.page() < 0) {
            throw new IllegalStateException("Cannot get last quotes for page value less than 0");
        }
        TickerQuotations tickerQuotations = quotationsByTicker.get(ticker);
        if (tickerQuotations == null) {
            return Page.empty(pagination);
        }
        return tickerQuotations.lastQuotes(pagination);
    }
}
