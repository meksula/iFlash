package com.iflash.ratesservice.quotation;

public class QuotationException extends IllegalStateException {

    private QuotationException(String message) {
        super(message);
    }

    static QuotationException noTicker(String ticker) {
        return new QuotationException(String.format("Any Order for ticker: %s not exists", ticker));
    }
}
