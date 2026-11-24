package com.iflash.ratesservice.quotation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class TickerQuotations {

    private final List<Quotation> lastPriceQuotations = new CopyOnWriteArrayList<>();
    private boolean initialized;

    synchronized void initialize(BigDecimal initialPrice) {
        if (initialized) {
            return;
        }
        lastPriceQuotations.addFirst(new Quotation(System.currentTimeMillis(), initialPrice));
        initialized = true;
    }

    synchronized void record(Quotation quotation) {
        lastPriceQuotations.add(quotation);
    }

    Quotation current() {
        return lastPriceQuotations.getLast();
    }

    Page<Quotation> lastQuotes(Pagination pagination) {
        if (lastPriceQuotations.isEmpty()) {
            return Page.empty(pagination);
        }
        List<Quotation> orderedQuotations = new ArrayList<>(lastPriceQuotations);

        switch (pagination.orderBy()) {
            case ASC -> {
            }
            case DESC -> Collections.reverse(orderedQuotations);
        }

        int fromIndex = pagination.page() * pagination.size();
        if (fromIndex >= orderedQuotations.size()) {
            return Page.empty(pagination);
        }

        int toIndex = Math.min(fromIndex + pagination.size(), orderedQuotations.size());
        return new Page<>(List.copyOf(orderedQuotations.subList(fromIndex, toIndex)), pagination);
    }
}
