package com.iflash.mes.instrument;

import com.iflash.core.engine.MatchingEngine;
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

    private final MatchingEngine matchingEngine;

    @KafkaListener(topics = "instrument.initialized")
    void onInstrumentInitialized(FinancialInstrumentInitializedEvent event) {
        log.info("Starting initialize {} companies in matching engine from event with routingKey: {}", event.instruments().size(), event.routingKey());
        matchingEngine.initialize(event.tickerRegistrationCommands());
        log.info("Companies initialization finished");
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
    }
}
