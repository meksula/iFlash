package com.iflash.core.order;

public class OrderBookException extends IllegalStateException {

    private OrderBookException(String message) {
        super(message);
    }

    public static OrderBookException orderTypeNotAvailable(OrderType orderType) {
        return new OrderBookException(String.format("OrderType %s not available yet", orderType));
    }

    public static OrderBookException noTicker(String ticker) {
        return new OrderBookException(String.format("Any Order for ticker: %s not exists", ticker));
    }

    public static OrderBookException tickerNull() {
        return new OrderBookException("Ticker must not be null value");
    }

    public static OrderBookException negativeNumber(Long value) {
        return new OrderBookException(String.format("Number is %d negative", value));
    }

    public static OrderBookException volumeNotAvailable(Long volume, String ticker) {
        return new OrderBookException(String.format("Volume: %d for ticker: %s is not available", volume, ticker));
    }
}
