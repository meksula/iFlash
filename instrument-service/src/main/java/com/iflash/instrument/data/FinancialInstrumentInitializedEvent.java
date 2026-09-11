package com.iflash.instrument.data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record FinancialInstrumentInitializedEvent(UUID routingKey, Integer eventVersion, Instant timestamp,
                                                  Set<FinancialInstrument> instruments) {

    record FinancialInstrument(String ticker, BigDecimal initialPrice) {
    }

    public static FinancialInstrumentInitializedEvent create(UUID routingKey, Integer eventVersion, Instant timestamp, Set<FinancialInstrumentInitial> financialInstrumentInitialSet) {
        Set<FinancialInstrument> financialInstrumentSet = financialInstrumentInitialSet.stream()
                .map(financialInstrumentInitial -> new FinancialInstrument(financialInstrumentInitial.ticker(), financialInstrumentInitial.price())).collect(Collectors.toSet());
        return new FinancialInstrumentInitializedEvent(routingKey, eventVersion, timestamp, financialInstrumentSet);
    }
}
