package com.iflash.ratesservice.quotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class InstrumentInitializedListener implements ConsumerSeekAware {

    private final QuotationStore quotationStore;

    @KafkaListener(topics = "instrument.initialized")
    void onInstrumentInitialized(FinancialInstrumentInitializedEvent event) {
        event.instruments()
             .forEach(instrument -> quotationStore.initTicker(instrument.ticker(), event.initialQuotation(instrument)));
        log.info("Initialized quotations of {} instruments from event with routingKey: {}", event.instruments().size(), event.routingKey());
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
    }
}
