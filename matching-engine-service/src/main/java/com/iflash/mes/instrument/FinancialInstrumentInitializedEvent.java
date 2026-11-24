package com.iflash.mes.instrument;

import com.iflash.core.engine.TickerRegistrationCommand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

record FinancialInstrumentInitializedEvent(UUID routingKey, Integer eventVersion, Instant timestamp, Set<FinancialInstrument> instruments) {

    record FinancialInstrument(String ticker, BigDecimal initialPrice) {
    }

    List<TickerRegistrationCommand> tickerRegistrationCommands() {
        return instruments.stream()
                          .map(instrument -> new TickerRegistrationCommand(instrument.ticker(), instrument.initialPrice()))
                          .toList();
    }
}
