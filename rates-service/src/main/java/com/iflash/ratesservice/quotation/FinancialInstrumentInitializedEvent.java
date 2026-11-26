package com.iflash.ratesservice.quotation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

record FinancialInstrumentInitializedEvent(UUID routingKey, Integer eventVersion, Instant timestamp, Set<FinancialInstrument> instruments) {

    record FinancialInstrument(String ticker, BigDecimal initialPrice) {
    }

    Quotation initialQuotation(FinancialInstrument instrument) {
        return new Quotation(timestamp.toEpochMilli(), instrument.initialPrice());
    }
}
