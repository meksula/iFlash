package com.iflash.instrument.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class CompanyDataLoader {

    private final KafkaTemplate<String, FinancialInstrumentInitializedEvent> kafkaTemplate;
    private final String initialDataPath;

    public CompanyDataLoader(@Value("${instrument.initial-data-path}") String initialDataPath,
                             KafkaTemplate<String, FinancialInstrumentInitializedEvent> kafkaTemplate) {
        this.initialDataPath = initialDataPath;
        this.kafkaTemplate = kafkaTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCompanies() {
        log.info("Starting initialize companies information to order book from file: {}", initialDataPath);
        CsvCompanyReader csvCompanyReader = new CsvCompanyReader();
        List<Company> companies = csvCompanyReader.read(initialDataPath);

//        List<TickerRegistrationCommand> tickerRegistrationCommands = companies.stream()
//                                                                              .map(company -> new TickerRegistrationCommand(company.ticker(), company.price()))
//                                                                              .toList();
//        matchingEngine.initialize(tickerRegistrationCommands);

        FinancialInstrumentInitializedEvent event = new FinancialInstrumentInitializedEvent(UUID.randomUUID(), 1, Instant.now(), Set.of());

        kafkaTemplate.send("instrument.initialized", event.routingKey().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        throw new RuntimeException("Couldn't publish event for routingKey: " + event.routingKey(), ex);
                    } else {
                        log.info("Successfully emitted FinancialInstrumentInitializedEvent with routingKey: {} and eventVersion: {}", event.routingKey(), event.eventVersion());
                    }
                });
    }
}
