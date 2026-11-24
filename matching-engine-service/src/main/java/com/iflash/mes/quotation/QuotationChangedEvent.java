package com.iflash.mes.quotation;

import com.iflash.core.quotation.CurrentQuotation;

import java.math.BigDecimal;

record QuotationChangedEvent(String ticker, long timestamp, BigDecimal price) {

    static QuotationChangedEvent of(String ticker, CurrentQuotation currentQuotation) {
        return new QuotationChangedEvent(ticker, currentQuotation.timestamp(), currentQuotation.price());
    }
}
