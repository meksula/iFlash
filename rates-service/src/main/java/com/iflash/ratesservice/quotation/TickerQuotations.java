package com.iflash.ratesservice.quotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class TickerQuotations {

    private final List<Quotation> lastPriceQuotations = new CopyOnWriteArrayList<>();
    private boolean initialized;

    synchronized void initialize(Quotation initialQuotation) {
        if (initialized) {
            return;
        }
        insertInTimeOrder(initialQuotation);
        initialized = true;
    }

    synchronized void record(Quotation quotation) {
        insertInTimeOrder(quotation);
    }

    private void insertInTimeOrder(Quotation quotation) {
        int index = lastPriceQuotations.size();
        while (index > 0 && lastPriceQuotations.get(index - 1).timestamp() > quotation.timestamp()) {
            index--;
        }
        lastPriceQuotations.add(index, quotation);
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
