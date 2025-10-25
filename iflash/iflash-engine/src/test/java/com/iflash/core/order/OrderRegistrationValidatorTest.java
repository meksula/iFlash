package com.iflash.core.order;

import com.iflash.commons.OrderBy;
import com.iflash.core.engine.FinancialInstrumentInfo;
import com.iflash.core.quotation.CurrentQuote;
import com.iflash.core.quotation.QuotationProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class OrderRegistrationValidatorTest {

    private final BigDecimal CURRENT_QUOTE = BigDecimal.valueOf(145.00);
    private final BigDecimal TOLERANCE_PERCENTAGE = BigDecimal.valueOf(0.15);

    @Test
    @DisplayName("Should correctly calculate Price Corridor for some financial instruments")
    void shouldCorrectlyCalculatePriceCorridorForSomeFinancialInstruments() {
        QuotationProvider quotationProvider = quotationProvider();
        OrderRegistrationValidator orderRegistrationValidator = new OrderRegistrationValidator(quotationProvider);

        OrderRegistrationValidator.PriceCorridor priceCorridor = orderRegistrationValidator.calculatePriceCorridor(TOLERANCE_PERCENTAGE, new CurrentQuote(0L, CURRENT_QUOTE));

        assertAll(() -> assertEquals(BigDecimal.valueOf(123.2500).setScale(4, RoundingMode.HALF_UP), priceCorridor.floorPrice()),
                  () -> assertEquals(BigDecimal.valueOf(166.7500).setScale(4, RoundingMode.HALF_UP), priceCorridor.ceilingPrice()));
    }

    @ParameterizedTest
    @ArgumentsSource(value = ArgsProvider.class)
    @DisplayName("Should correctly validate price and return false when price is out of corridor and true when price is in corridor")
    void shouldCorrectlyValidatePriceAndReturnFalseWhenPriceIsOutOfCorridorAndTrueWhenPriceIsInCorridor(BigDecimal proposedPrice, boolean isProposedPriceValid) {
        var ticker = "NVDA";
        QuotationProvider quotationProvider = quotationProvider();
        OrderRegistrationValidator orderRegistrationValidator = new OrderRegistrationValidator(quotationProvider);

        boolean validationResult = orderRegistrationValidator.isOrderRegistrationPriceValid(ticker, proposedPrice);

        assertAll(() -> assertEquals(validationResult, isProposedPriceValid));
    }

    static class ArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(org.junit.jupiter.api.extension.ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new BigDecimal("123.2499"), false),
                    Arguments.of(new BigDecimal("123.2500"), true),
                    Arguments.of(new BigDecimal("100.5000"), false),
                    Arguments.of(new BigDecimal("200.0000"), false),
                    Arguments.of(new BigDecimal("166.7500"), true),
                    Arguments.of(new BigDecimal("166.7501"), false));
        }
    }

    private QuotationProvider quotationProvider() {
        return new QuotationProvider() {
            @Override
            public CurrentQuote getCurrentQuote(String ticker) {
                return new CurrentQuote(0L, CURRENT_QUOTE);
            }

            @Override
            public List<CurrentQuote> getLastQuotes(String ticker, int limit, OrderBy orderBy) {
                return null;
            }

            @Override
            public List<FinancialInstrumentInfo> getAllTickersWithQuotation() {
                return null;
            }
        };
    }
}