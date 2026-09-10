package com.iflash.instrument.config;

import com.iflash.instrument.data.FinancialInstrumentInitializedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
class KafkaConfig {

    @Bean
    public ProducerFactory<String, FinancialInstrumentInitializedEvent> financialInstrumentInitializedProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, FinancialInstrumentInitializedEvent> financialInstrumentInitializedKafkaTemplate(ProducerFactory<String, FinancialInstrumentInitializedEvent> tradeExecutedProducerFactory) {
        return new KafkaTemplate<>(tradeExecutedProducerFactory);
    }

    @Bean
    public NewTopic instrumentInitializedTopic() {
        return TopicBuilder.name("instrument.initialized")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
