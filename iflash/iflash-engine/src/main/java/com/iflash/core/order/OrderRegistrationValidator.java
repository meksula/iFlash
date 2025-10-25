package com.iflash.core.order;

import com.iflash.core.quotation.CurrentQuote;
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
        CurrentQuote currentQuote = quotationProvider.getCurrentQuote(ticker);
        PriceCorridor priceCorridor = calculatePriceCorridor(PRICE_TOLERANCE_PERCENTAGE, currentQuote);

        int floorPriceComparing = proposedPrice.compareTo(priceCorridor.floorPrice);
        int ceilingPriceComparing = proposedPrice.compareTo(priceCorridor.ceilingPrice);

        return floorPriceComparing >= 0 && ceilingPriceComparing <= 0;
    }

    public PriceCorridor calculatePriceCorridor(BigDecimal tolerancePercentage, CurrentQuote currentQuote) {
        BigDecimal floorPrice = currentQuote.price().subtract(currentQuote.price().multiply(tolerancePercentage)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal ceilingPrice = currentQuote.price().add(currentQuote.price().multiply(tolerancePercentage)).setScale(4, RoundingMode.HALF_UP);
        return new PriceCorridor(floorPrice, ceilingPrice);
    }

    public record PriceCorridor(BigDecimal floorPrice, BigDecimal ceilingPrice) {
    }
}
