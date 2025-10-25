package com.iflash.core.engine;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.order.OrderBook;
import com.iflash.core.order.OrderBookException;
import com.iflash.core.order.OrderInformation;
import com.iflash.core.order.OrderRegistrationResult;
import com.iflash.core.order.OrderRegistrationValidator;
import com.iflash.core.order.RegisterOrderCommand;
import com.iflash.core.quotation.QuotationAggregator;
import com.iflash.core.quotation.QuotationProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.iflash.core.order.OrderDirection.SELL;

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
    public OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand) {
        // todo refactor, not good place for that logic, temporary workaround
        if (SELL == registerOrderCommand.orderDirection() && registerOrderCommand.price() == null) {
            RegisterOrderCommand registerOrderCommandWithSpread = registerOrderCommand.withMarketPricePlusSpread(quotationProvider().getCurrentQuote(registerOrderCommand.ticker()),
                                                                                                                 BigDecimal.valueOf(0.0100)
                                                                                                                           .setScale(4, RoundingMode.HALF_UP));
            return orderBook.registerOrder(registerOrderCommandWithSpread);
        }
        boolean orderRegistrationPriceValid = orderRegistrationValidator.isOrderRegistrationPriceValid(registerOrderCommand.ticker(), registerOrderCommand.price());
        if (orderRegistrationPriceValid) {
            OrderRegistrationResult orderRegistrationResult = orderBook.registerOrder(registerOrderCommand);
            CompletableFuture.runAsync(() -> quotationAggregator.handle(registerOrderCommand, orderRegistrationResult.finishedTransactionInfoList()));
            return orderRegistrationResult;
        } else {
            throw OrderBookException.cannotCreateOrder(registerOrderCommand.price());
        }
    }

    @Override
    public List<FinancialInstrumentInfo> getFinancialInstrumentInfo() {
        return quotationProvider.getAllTickersWithQuotation();
    }

    @Override
    public Page<OrderInformation> getOrderBookSnapshot(String ticker, Pagination pagination) {
        return orderBook.getOrderBookSnapshot(ticker, pagination);
    }
}
