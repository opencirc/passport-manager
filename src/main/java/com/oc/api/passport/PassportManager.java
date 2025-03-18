package com.oc.api.passport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.oc.api.passport.*")
public final class PassportManager {

    private PassportManager() {
        throw new UnsupportedOperationException(
                "Main class cannot be instantiated");
    }

    /**
     * Main class.
     *
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(PassportManager.class, args);
    }

}
