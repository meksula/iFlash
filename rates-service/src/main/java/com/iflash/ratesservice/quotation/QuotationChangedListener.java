package com.iflash.ratesservice.quotation;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class QuotationChangedListener implements ConsumerSeekAware {

    private final QuotationStore quotationStore;

    @KafkaListener(topics = "quotation.changed")
    void onQuotationChanged(QuotationChangedEvent event) {
        quotationStore.record(event.ticker(), event.quotation());
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
    }
}
