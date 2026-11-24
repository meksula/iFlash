package com.iflash.mes.quotation;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class QuotationChangedPublisher {

    static final String QUOTATION_CHANGED_TOPIC = "quotation.changed";

    private final KafkaTemplate<String, QuotationChangedEvent> kafkaTemplate;
    private final ExecutorService publishingExecutor;

    QuotationChangedPublisher(KafkaTemplate<String, QuotationChangedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.publishingExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "quotation-changed-publisher");
            thread.setContextClassLoader(QuotationChangedPublisher.class.getClassLoader());
            return thread;
        });
    }

    void publish(QuotationChangedEvent event) {
        publishingExecutor.execute(() -> send(event));
    }

    private void send(QuotationChangedEvent event) {
        kafkaTemplate.send(QUOTATION_CHANGED_TOPIC, event.ticker(), event)
                     .whenComplete((result, ex) -> {
                         if (ex != null) {
                             log.error("Couldn't publish QuotationChangedEvent for ticker: {}", event.ticker(), ex);
                         }
                     });
    }

    @PreDestroy
    void shutdown() {
        publishingExecutor.shutdown();
    }
}
