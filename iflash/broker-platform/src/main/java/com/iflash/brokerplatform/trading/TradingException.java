package com.iflash.brokerplatform.trading;

/** Business-rule rejection (insufficient funds/holdings, bad input) shown back to the user. */
public class TradingException extends RuntimeException {

    public TradingException(String message) {
        super(message);
    }
}
