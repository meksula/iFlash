package com.iflash.core.quotation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class WeightedAverageQuotation implements QuotationCalculable {

    @Override
    public Quotation calculate(String ticker, List<QuotableInformation> quotableInformation) {
        long weightSum = quotableInformation.stream()
                                            .mapToLong(QuotableInformation::volume)
                                            .sum();
        BigDecimal multipliedValuesSum = quotableInformation.stream()
                                                            .map(info -> info.price()
                                                                             .multiply(BigDecimal.valueOf(info.volume())))
                                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal quotationResult = multipliedValuesSum.divide(BigDecimal.valueOf(weightSum), RoundingMode.HALF_UP);

        return new Quotation(ticker, System.currentTimeMillis(), weightSum, quotationResult);
    }
}
