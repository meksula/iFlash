package com.iflash.engine.actuator;

import com.iflash.engine.IntegrationTestMain;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorTest extends IntegrationTestMain {

    @Test
    void shouldReturnOkWhenTryingToReachActuatorEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator", String.class);

        log.info("Response: {}", response.getBody());

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }
}
