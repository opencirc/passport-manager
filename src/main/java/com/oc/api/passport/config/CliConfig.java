package com.oc.api.passport.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CliConfig implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println("HelloWorld!");
        }

    }

}
