package com.iflash.core.quotation;

import com.iflash.commons.OrderBy;
import com.iflash.commons.ValidateUtils;
import com.iflash.core.order.OrderDirection;
import com.iflash.core.order.OrderType;
import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.order.FinishedTransactionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuotationAggregatorDefaultTest {

    private final String ticker = "NVDA.US";
    private final BigDecimal price = BigDecimal.valueOf(171.9434);
    private final long volume = 1L;

    @Test
    @DisplayName("Should correctly calculate Quotation for Financial Instrument when buy order finished")
    void shouldCorrectlyCalculateQuotationForFinancialInstrumentWhenBuyOrderFinished() {
        Map<String, List<Quotation>> quotations = new HashMap<>();
        List<Quotation> quotationList = new ArrayList<>();
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(171.9034)));
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(171.9034)));
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(171.4434)));
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(170.9434)));
        quotations.put(ticker, quotationList);

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(OrderDirection.BUY, OrderType.MARKET, ticker, price, volume);
        List<FinishedTransactionInfo> finishedTransactionInfos = List.of(
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.734)),
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.256)),
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.334)),
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.634))
        );

        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;
        CurrentQuote beforeTradeCurrentQuote = quotationProvider.getCurrentQuote(ticker);

        quotationAggregator.handle(buyCommand, finishedTransactionInfos);

        CurrentQuote afterTradeCurrentQuote = quotationProvider.getCurrentQuote(ticker);

        assertAll(() -> assertEquals(BigDecimal.valueOf(170.9434), beforeTradeCurrentQuote.price()),
                  () -> assertEquals(BigDecimal.valueOf(171.4900).setScale(4, RoundingMode.HALF_UP), afterTradeCurrentQuote.price().setScale(4, RoundingMode.HALF_UP)));
    }

    @Test
    @DisplayName("Should correctly return CurrentQuote when list is empty")
    void shouldCorrectlyReturnCurrentQuoteForEdgeCases() {
        Map<String, List<Quotation>> quotations = new HashMap<>();

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations);
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        assertAll(() -> assertDoesNotThrow(() -> quotationProvider.getLastQuotes(ticker, 10, OrderBy.ASC)),
                  () -> assertEquals(0, quotationProvider.getLastQuotes(ticker, 10, OrderBy.ASC).size()));
    }

    @Test
    @DisplayName("Should correctly return CurrentQuote when list has less element than required")
    void shouldCorrectlyReturnCurrentQuoteWhenListHasLessElementThanRequired() {
        Map<String, List<Quotation>> quotations = new HashMap<>();
        quotations.put(ticker, List.of(new Quotation(ticker, System.currentTimeMillis(), volume, price)));

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations);
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        List<CurrentQuote> lastQuotesAsc = quotationProvider.getLastQuotes(ticker, 2, OrderBy.ASC);
        List<CurrentQuote> lastQuotesDesc = quotationProvider.getLastQuotes(ticker, 2, OrderBy.DESC);

        assertAll(() -> assertEquals(1, lastQuotesAsc.size()),
                  () -> assertEquals(1, lastQuotesDesc.size()));
    }

    @Test
    @DisplayName("Should correctly return CurrentQuote for ASC order")
    void shouldCorrectlyReturnCurrentQuoteForAscOrder() {
        Map<String, List<Quotation>> quotations = new HashMap<>();
        quotations.put(ticker, List.of(
                new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(1)),
                new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(2)),
                new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(3))));

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations);
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        List<CurrentQuote> lastQuotesAsc = quotationProvider.getLastQuotes(ticker, 2, OrderBy.ASC);

        assertAll(() -> assertEquals(lastQuotesAsc.get(0).price(), quotations.get(ticker).get(0).quotation()),
                  () -> assertEquals(lastQuotesAsc.get(1).price(), quotations.get(ticker).get(1).quotation()),
                  () -> assertEquals(2, lastQuotesAsc.size()));
    }

    @Test
    @DisplayName("Should correctly return CurrentQuote for DESC order")
    void shouldCorrectlyReturnCurrentQuoteForDescOrder() {
        Map<String, List<Quotation>> quotations = new HashMap<>();
        quotations.put(ticker, List.of(
                new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(1)),
                new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(2)),
                new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(3))));

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations);
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        List<CurrentQuote> lastQuotesAsc = quotationProvider.getLastQuotes(ticker, 2, OrderBy.DESC);

        assertAll(() -> assertEquals(lastQuotesAsc.get(0).price(), quotations.get(ticker).get(2).quotation()),
                  () -> assertEquals(lastQuotesAsc.get(1).price(), quotations.get(ticker).get(1).quotation()),
                  () -> assertEquals(2, lastQuotesAsc.size()));
    }
}