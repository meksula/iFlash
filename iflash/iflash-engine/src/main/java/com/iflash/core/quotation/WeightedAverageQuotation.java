package com.iflash.core.quotation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;

public class WeightedAverageQuotation implements QuotationCalculable {

    @Override
    public Quotation calculate(String ticker, List<BoughtTransactionInfo> boughtTransactionInfos) {
        long weightSum = boughtTransactionInfos.stream()
                                               .mapToLong(BoughtTransactionInfo::volume)
                                               .sum();
        BigDecimal multipliedValuesSum = boughtTransactionInfos.stream()
                                                               .map(info -> info.price().multiply(BigDecimal.valueOf(info.volume())))
                                                               .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal quotationResult = multipliedValuesSum.divide(BigDecimal.valueOf(weightSum), RoundingMode.HALF_UP);

        return new Quotation(ticker, ZonedDateTime.now(), weightSum, quotationResult);
    }
}
