package ru.sibsutis.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ClinicBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicBotApplication.class, args);
    }

}