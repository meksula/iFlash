package com.iflash.platform;

import org.springframework.boot.SpringApplication;

public class TestIflashApplication {

    public static void main(String[] args) {
        SpringApplication.from(IflashPlatformApplication::main)
                         .with(TestcontainersConfiguration.class)
                         .run(args);
    }

}
