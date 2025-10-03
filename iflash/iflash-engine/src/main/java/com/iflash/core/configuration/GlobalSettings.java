package com.iflash.core.configuration;

import com.iflash.core.quotation.QuotationCalculationType;
import org.joda.money.CurrencyUnit;

public class GlobalSettings {

    public final static CurrencyUnit GLOBAL_CURRENCY = CurrencyUnit.USD;
    public final static QuotationCalculationType QUOTATION_CALCULABLE = QuotationCalculationType.WEIGHTED_AVERAGE;
}
