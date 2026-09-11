package com.iflash.instrument.data;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FinancialInstrumentDataLoader {

    private final KafkaTemplate<String, FinancialInstrumentInitializedEvent> kafkaTemplate;
    private final String initialDataPath;
    private final FinancialInstrumentRepository financialInstrumentRepository;

    public FinancialInstrumentDataLoader(@Value("${instrument.initial-data-path}") String initialDataPath,
                                         KafkaTemplate<String, FinancialInstrumentInitializedEvent> kafkaTemplate,
                                         FinancialInstrumentRepository financialInstrumentRepository) {
        this.initialDataPath = initialDataPath;
        this.kafkaTemplate = kafkaTemplate;
        this.financialInstrumentRepository = financialInstrumentRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCompanies() {
        log.info("Starting initialize companies information to order book from file: {}", initialDataPath);
        CsvFileReader csvFileReader = new CsvFileReader();
        Set<FinancialInstrumentInitial> financialInstrumentInitialSet = csvFileReader.read(initialDataPath);
        saveEntities(financialInstrumentInitialSet);

        UUID routingKey = UUID.randomUUID();
        Integer eventVersion = 0; // always as init seed
        Instant now = Instant.now();

        FinancialInstrumentInitializedEvent event = FinancialInstrumentInitializedEvent.create(routingKey, eventVersion, now, financialInstrumentInitialSet);

        kafkaTemplate.send("instrument.initialized", event.routingKey().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        throw new RuntimeException("Couldn't publish event for routingKey: " + event.routingKey(), ex);
                    } else {
                        log.info("Successfully emitted FinancialInstrumentInitializedEvent with routingKey: {} and eventVersion: {}", event.routingKey(), event.eventVersion());
                    }
                });
    }

    private void saveEntities(Set<FinancialInstrumentInitial> financialInstrumentInitialSet) {
        Set<FinancialInstrumentEntity> financialInstrumentEntities = financialInstrumentInitialSet.stream()
                .map(financialInstrumentInitial -> FinancialInstrumentEntity.createNew(financialInstrumentInitial.ticker(), financialInstrumentInitial.price()))
                .collect(Collectors.toSet());

        financialInstrumentRepository.saveAll(financialInstrumentEntities);
    }
}
