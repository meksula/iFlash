package com.iflash.core.quotation;

public class QuotationAggregatorFactory {

    public static QuotationAggregator factorizeQuotationAggregator(QuotationCalculationType quotationCalculationType) {
        QuotationCalculable quotationCalculable = switch (quotationCalculationType) {
            case WEIGHTED_AVERAGE ->  new WeightedAverageQuotation();
        };

        return new QuotationAggregatorDefault(quotationCalculable);
    }
}
