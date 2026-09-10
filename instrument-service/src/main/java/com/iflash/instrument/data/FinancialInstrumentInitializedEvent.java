package com.iflash.instrument.data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record FinancialInstrumentInitializedEvent(UUID routingKey,
                                                  Integer eventVersion,
                                                  Instant timestamp,
                                                  Set<FinancialInstrument> instruments) {

    public record FinancialInstrument(String ticker, BigDecimal price) {}
}
