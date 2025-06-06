package com.opencirc.api.passport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.opencirc.api.passport.*")
public class PassportManager {


    /**
     * Main class.
     *
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(PassportManager.class, args);
    }

}
