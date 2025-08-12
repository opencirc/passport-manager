package db.migration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import lombok.extern.slf4j.Slf4j;

/**
 * Flyway migration for the initial database setup.
 */
@Slf4j
public class V1__InitialSetup extends BaseJavaMigration {

    /**
     * Executes the migration by loading the SQL script from the resource path.
     *
     * @param context
     * @throws Exception
     */
    @Override
    public void migrate(Context context) throws Exception {
        log.info("Migration started");
        String scriptPath = MigrationUtils.loadScriptPathFromProperties();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(scriptPath);
                Statement stmt = context.getConnection().createStatement()) {
            if (is == null) {
                throw new IllegalStateException("SQL file not found: " + scriptPath);
            }

            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String query : MigrationUtils.splitPostgresStatements(sql)) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query);
                }
            }
        }
        log.info("Migration Completed");
    }

}
