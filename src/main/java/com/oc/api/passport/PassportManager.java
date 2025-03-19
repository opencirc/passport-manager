package com.oc.api.passport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.oc.api.passport.*")
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
