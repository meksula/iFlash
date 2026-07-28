package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationProvider;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.iflash.core.configuration.GlobalSettings.PRICE_TOLERANCE_PERCENTAGE;

@RequiredArgsConstructor
public class OrderRegistrationValidator {

    private final QuotationProvider quotationProvider;

    public boolean isOrderRegistrationPriceValid(String ticker, BigDecimal proposedPrice) {
        if (proposedPrice == null) {
            return true;
        }
        CurrentQuotation currentQuotation = quotationProvider.getCurrentQuote(ticker);
        PriceCorridor priceCorridor = calculatePriceCorridor(PRICE_TOLERANCE_PERCENTAGE, currentQuotation);

        int floorPriceComparing = proposedPrice.compareTo(priceCorridor.floorPrice);
        int ceilingPriceComparing = proposedPrice.compareTo(priceCorridor.ceilingPrice);

        return floorPriceComparing >= 0 && ceilingPriceComparing <= 0;
    }

    public PriceCorridor calculatePriceCorridor(BigDecimal tolerancePercentage, CurrentQuotation currentQuotation) {
        BigDecimal floorPrice = currentQuotation.price().subtract(currentQuotation.price().multiply(tolerancePercentage)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal ceilingPrice = currentQuotation.price().add(currentQuotation.price().multiply(tolerancePercentage)).setScale(4, RoundingMode.HALF_UP);
        return new PriceCorridor(floorPrice, ceilingPrice);
    }

    public record PriceCorridor(BigDecimal floorPrice, BigDecimal ceilingPrice) {
    }
}
