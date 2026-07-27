package com.iflash.brokerplatform.trading;

import java.math.BigDecimal;

/** Result of a settled order, used to build the user-facing flash message. */
public record OrderOutcome(String ticker, String direction, String orderType, long requestedVolume,
                           long filledQuantity, BigDecimal cashAmount) {

    public boolean notFilled() {
        return filledQuantity == 0;
    }

    public boolean partiallyFilled() {
        return filledQuantity > 0 && filledQuantity < requestedVolume;
    }
}
