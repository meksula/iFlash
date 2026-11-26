package com.iflash.brokerplatform.market;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceChange {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PriceChange() {
    }

    public static BigDecimal percent(BigDecimal from, BigDecimal to) {
        if (from == null || to == null || from.signum() <= 0 || to.signum() <= 0) {
            return null;
        }
        return to.subtract(from)
                 .multiply(HUNDRED)
                 .divide(from, 2, RoundingMode.HALF_UP);
    }
}
