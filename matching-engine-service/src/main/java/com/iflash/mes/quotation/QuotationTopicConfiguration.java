package com.iflash.mes.quotation;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
class QuotationTopicConfiguration {

    @Bean
    NewTopic quotationChangedTopic() {
        return TopicBuilder.name(QuotationChangedPublisher.QUOTATION_CHANGED_TOPIC)
                           .partitions(1)
                           .replicas(1)
                           .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                           .build();
    }
}
