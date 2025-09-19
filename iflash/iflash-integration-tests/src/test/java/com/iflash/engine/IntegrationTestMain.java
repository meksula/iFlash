package com.iflash.engine;

import com.iflash.platform.IflashPlatformApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = IflashPlatformApplication.class)
public class IntegrationTestMain {

    @Autowired
    public TestRestTemplate restTemplate;

    public static final Logger log = LoggerFactory.getLogger(IntegrationTestMain.class);
}
