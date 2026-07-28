package com.iflash.core.quotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeightedAverageQuotationTest {

    @Test
    @DisplayName("Should correctly calculate weighted average")
    void shouldCorrectlyCalculateWeightedAverage() {
        WeightedAverageQuotation weightedAverageQuotation = new WeightedAverageQuotation();

        List<QuotableInformation> quotableInformations = List.of(new QuotableInformation(100L, BigDecimal.valueOf(10.50D)),
                                                                 new QuotableInformation(200L, BigDecimal.valueOf(10.80D)));

        Quotation quotation = weightedAverageQuotation.calculate("NVDA.US", quotableInformations);

        assertEquals(BigDecimal.valueOf(10.70D), quotation.quotation());
    }
}