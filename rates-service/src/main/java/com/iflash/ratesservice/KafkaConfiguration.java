package com.iflash.ratesservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;

@Configuration
class KafkaConfiguration {

    @Bean
    RecordMessageConverter recordMessageConverter() {
        return new StringJacksonJsonMessageConverter();
    }
}
