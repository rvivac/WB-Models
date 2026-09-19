package com.wbscouting.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class WbScoutingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WbScoutingApiApplication.class, args);
    }
}
