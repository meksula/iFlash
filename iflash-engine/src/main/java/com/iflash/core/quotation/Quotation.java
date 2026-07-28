package com.iflash.core.quotation;

import java.math.BigDecimal;

public record Quotation(String ticker, long quotationTimestamp, long volume, BigDecimal quotation) {

    CurrentQuotation map() {
        return new CurrentQuotation(quotationTimestamp, quotation);
    }
}
