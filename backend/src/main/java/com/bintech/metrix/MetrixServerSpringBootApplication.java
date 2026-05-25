package com.bintech.metrix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class MetrixServerSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetrixServerSpringBootApplication.class, args);
        log.info("Metrix Server Started");
    }
}
