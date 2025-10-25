package com.iflash.core.configuration;

import com.iflash.core.quotation.QuotationCalculationType;
import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

public class GlobalSettings {

    public final static CurrencyUnit GLOBAL_CURRENCY = CurrencyUnit.USD;
    public final static QuotationCalculationType QUOTATION_CALCULABLE = QuotationCalculationType.WEIGHTED_AVERAGE;
    public final static BigDecimal PRICE_TOLERANCE_PERCENTAGE = BigDecimal.valueOf(0.15D); // 1,5% max tolerance
}
