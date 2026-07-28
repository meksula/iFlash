package com.iflash.core.quotation;

import com.iflash.commons.OrderBy;
import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
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
        Map<String, List<Quotation>> theoreticalQuotations = new HashMap<>();
        List<Quotation> quotationList = new ArrayList<>();
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(171.9034)));
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(171.9034)));
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(171.4434)));
        quotationList.add(new Quotation(ticker, System.currentTimeMillis(), volume, BigDecimal.valueOf(170.9434)));
        quotations.put(ticker, quotationList);

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations, theoreticalQuotations);

        RegisterOrderCommand buyCommand = new RegisterOrderCommand(UUID.randomUUID(), OrderDirection.BID, OrderType.MARKET, ticker, price, volume);
        List<FinishedTransactionInfo> finishedTransactionInfos = List.of(
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.734)),
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.256)),
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.334)),
                new FinishedTransactionInfo(UUID.randomUUID(), ticker, 10, BigDecimal.valueOf(171.634))
        );

        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;
        CurrentQuotation beforeTradeCurrentQuotation = quotationProvider.getCurrentQuote(ticker);

        quotationAggregator.calculateQuotationPostTransaction(buyCommand.ticker(), finishedTransactionInfos);

        CurrentQuotation afterTradeCurrentQuotation = quotationProvider.getCurrentQuote(ticker);

        assertAll(() -> assertEquals(BigDecimal.valueOf(170.9434), beforeTradeCurrentQuotation.price()),
                  () -> assertEquals(BigDecimal.valueOf(171.4900).setScale(4, RoundingMode.HALF_UP), afterTradeCurrentQuotation.price().setScale(4, RoundingMode.HALF_UP)));
    }

    @Test
    @DisplayName("Should correctly return CurrentQuote when list is empty")
    void shouldCorrectlyReturnCurrentQuoteForEdgeCases() {
        Map<String, List<Quotation>> quotations = new HashMap<>();

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations, new HashMap<>());
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        assertAll(() -> assertDoesNotThrow(() -> quotationProvider.getLastQuotes(ticker, new Pagination(0, 10, OrderBy.ASC))),
                  () -> assertEquals(0, quotationProvider.getLastQuotes(ticker, new Pagination(0, 10, OrderBy.ASC)).getElements().size()));
    }

    @Test
    @DisplayName("Should correctly return CurrentQuote when list has less element than required")
    void shouldCorrectlyReturnCurrentQuoteWhenListHasLessElementThanRequired() {
        Map<String, List<Quotation>> quotations = new HashMap<>();
        quotations.put(ticker, List.of(new Quotation(ticker, System.currentTimeMillis(), volume, price)));

        QuotationCalculable quotationCalculable = new WeightedAverageQuotation();
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations, new HashMap<>());
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        Page<CurrentQuotation> lastQuotesAsc = quotationProvider.getLastQuotes(ticker, new Pagination(0, 2, OrderBy.ASC));
        Page<CurrentQuotation> lastQuotesDesc = quotationProvider.getLastQuotes(ticker, new Pagination(0, 2, OrderBy.DESC));

        assertAll(() -> assertEquals(1, lastQuotesAsc.getElements().size()),
                  () -> assertEquals(1, lastQuotesDesc.getElements().size()));
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
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations, new HashMap<>());
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        Page<CurrentQuotation> lastQuotesAsc = quotationProvider.getLastQuotes(ticker, new Pagination(0, 2, OrderBy.ASC));

        assertAll(() -> assertEquals(lastQuotesAsc.getElements().get(0).price(), quotations.get(ticker).get(0).quotation()),
                  () -> assertEquals(lastQuotesAsc.getElements().get(1).price(), quotations.get(ticker).get(1).quotation()),
                  () -> assertEquals(2, lastQuotesAsc.getElements().size()));
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
        QuotationAggregator quotationAggregator = new QuotationAggregatorDefault(quotationCalculable, quotations, new HashMap<>());
        QuotationProvider quotationProvider = (QuotationProvider) quotationAggregator;

        Page<CurrentQuotation> lastQuotesAsc = quotationProvider.getLastQuotes(ticker, new Pagination(0, 2, OrderBy.DESC));

        assertAll(() -> assertEquals(lastQuotesAsc.getElements().get(0).price(), quotations.get(ticker).get(2).quotation()),
                  () -> assertEquals(lastQuotesAsc.getElements().get(1).price(), quotations.get(ticker).get(1).quotation()),
                  () -> assertEquals(2, lastQuotesAsc.getElements().size()));
    }
}