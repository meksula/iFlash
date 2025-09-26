package com.iflash.core.order;

public class OrderBookException extends IllegalStateException {

    public OrderBookException(String message) {
        super(message);
    }

    public static OrderBookException noTicker(String ticker) {
        return new OrderBookException(String.format("Any Order for ticker: %s not exists", ticker));
    }
}
