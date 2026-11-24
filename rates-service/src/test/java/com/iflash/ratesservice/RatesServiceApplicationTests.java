package com.iflash.ratesservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RatesServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
