package com.iflash.trader;

import com.iflash.core.engine.FinancialInstrumentInfo;
import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.OrderBookSnapshotResponse;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
public class RandomLongTermTrader implements Trader {

    private final String brokerName;
    private final String firstName;
    private final String lastName;
    private final ApiToolkit apiToolkit;

    private final BigDecimal cash;
    private final Currency currency;

    private final Map<String, Position> portfolio;

    @Override
    public void decide() {
        System.out.println("Trader " + firstName + " " + lastName + " is making a decision...");

        List<FinancialInstrumentInfo> instruments = apiToolkit.getInstruments();

        Random random = new Random();
        int i = random.nextInt(instruments.size() - 1);
        FinancialInstrumentInfo financialInstrumentInfo = instruments.get(i);
        System.out.println(financialInstrumentInfo);

        OrderBookSnapshotResponse orderBook = apiToolkit.getOrderBook(financialInstrumentInfo.ticker(), 0, 20, "DESC", "BID");

        orderBook.data()
                 .getElements()
                 .forEach(entry -> System.out.println("Order: " + entry.price() + " " + entry.volume()));
    }
}
