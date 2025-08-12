package com.opencirc.api.passport.command;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Command(group = "Migration Commands")
@Slf4j
public class Migration {

    /**
     * Database URL.
     */
    @Value("${spring.datasource.url}")
    private String databaseUrl;

    /**
     * Database Username.
     */
    @Value("${spring.datasource.username}")
    private String databaseUsername;

    /**
     * Database password.
     */
    @Value("${spring.datasource.password}")
    private String databasePassword;

    /**
     * Shell command to run migrations.
     */
    @Command(command = "run", description = "Run database migrations")
    public void run() {
        Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, databaseUsername, databasePassword)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
    }
}

