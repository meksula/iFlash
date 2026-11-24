package com.iflash.ratesservice.quotation;

import java.math.BigDecimal;

record QuotationChangedEvent(String ticker, long timestamp, BigDecimal price) {

    Quotation quotation() {
        return new Quotation(timestamp, price);
    }
}
