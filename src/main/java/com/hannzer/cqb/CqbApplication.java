package com.hannzer.cqb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 真實人生遊戲感任務版（Couple Quest Board）後端進入點 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CqbApplication {

    public static void main(String[] args) {
        SpringApplication.run(CqbApplication.class, args);
    }
}
