package com.iflash.core.engine;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.order.FinishedTransactionInfo;
import com.iflash.core.order.OrderBook;
import com.iflash.core.order.OrderBookException;
import com.iflash.core.order.OrderDirection;
import com.iflash.core.order.OrderInformation;
import com.iflash.core.order.OrderRegistrationResult;
import com.iflash.core.order.OrderRegistrationValidator;
import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationProvider;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.iflash.core.configuration.GlobalSettings.QUOTATION_CALCULATE_DEPTH;
import static com.iflash.core.order.OrderDirection.ASK;
import static com.iflash.core.order.OrderDirection.BID;

@Slf4j
public class SingleThreadMatchingEngine implements MatchingEngine, TradingOperations, OrderBookOperations {

    private final OrderBook orderBook;
    private final QuotationAggregator quotationAggregator;
    private final QuotationProvider quotationProvider;
    private final OrderRegistrationValidator orderRegistrationValidator;

    private SingleThreadMatchingEngine(OrderBook orderBook, QuotationAggregator quotationAggregator) {
        this.orderBook = orderBook;
        this.quotationAggregator = quotationAggregator;
        this.quotationProvider = (QuotationProvider) quotationAggregator;
        this.orderRegistrationValidator = new OrderRegistrationValidator(quotationProvider);
    }

    public static SingleThreadMatchingEngine create(OrderBook orderBook, QuotationAggregator quotationAggregator) {
        return new SingleThreadMatchingEngine(orderBook, quotationAggregator);
    }

    @Override
    public MatchingEngineState initialize(List<TickerRegistrationCommand> tickerRegistrationCommandList) {
        tickerRegistrationCommandList.forEach(tickerRegistrationCommand -> {
            orderBook.registerTicker(tickerRegistrationCommand.ticker());
            quotationAggregator.initTicker(tickerRegistrationCommand.ticker(), tickerRegistrationCommand.initialPrice());
        });
        return MatchingEngineState.RUNNING;
    }

    @Override
    public QuotationProvider quotationProvider() {
        return quotationProvider;
    }

    @Override
    public TradingOperations tradingOperations() {
        return this;
    }

    @Override
    public OrderBookOperations orderBookOperations() {
        return this;
    }

    @Override
    public OrderRegistrationResult registerOrder(RegisterOrderCommand incomingRegisterOrderCommand) {
        RegisterOrderCommand registerOrderCommand = incomingRegisterOrderCommand.withMarketPricePlusSpread(quotationProvider().getCurrentQuote(incomingRegisterOrderCommand.ticker()),
                                                                                                           BigDecimal.valueOf(0.0100).setScale(4, RoundingMode.HALF_UP));
        boolean orderRegistrationPriceValid = orderRegistrationValidator.isOrderRegistrationPriceValid(registerOrderCommand.ticker(), registerOrderCommand.price());
        if (orderRegistrationPriceValid) {
            OrderRegistrationResult orderRegistrationResult = orderBook.registerOrder(registerOrderCommand);
            switch (orderRegistrationResult.transactionPhase()) {
                case FULLY_COMPLETED, PARTIALLY_COMPLETED -> {
                    List<FinishedTransactionInfo> finishedTransactionInfos = orderRegistrationResult.finishedTransactionInfoList();
                    CompletableFuture.runAsync(() -> quotationAggregator.calculateQuotationPostTransaction(registerOrderCommand.ticker(), finishedTransactionInfos));
                }
                case IDLING_ON_QUEUE -> {
                    Set<OrderInformation> topBids = orderBook.getTopOrders(registerOrderCommand.ticker(), BID, QUOTATION_CALCULATE_DEPTH);
                    Set<OrderInformation> topAsks = orderBook.getTopOrders(registerOrderCommand.ticker(), ASK, QUOTATION_CALCULATE_DEPTH);
                    CompletableFuture.runAsync(() -> quotationAggregator.calculateTheoreticalQuotation(registerOrderCommand.ticker(), topBids, topAsks));
                }
                case REJECTED -> log.warn("Order is rejected");
            }
            return orderRegistrationResult;
        }
        else {
            throw OrderBookException.cannotCreateOrder(registerOrderCommand.price());
        }
    }

    @Override
    public List<FinancialInstrumentInfo> getFinancialInstrumentInfo() {
        return quotationProvider.getAllTickersWithQuotation();
    }

    @Override
    public Page<OrderInformation> getOrderBookSnapshot(String ticker, OrderDirection orderDirection, Pagination pagination) {
        return orderBook.getOrderBookSnapshot(ticker, orderDirection, pagination);
    }
}
