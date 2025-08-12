package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class MigrationUtils {

    /**
     * Constructor.
     */
    private MigrationUtils() {
        super();
    }

    /**
     * Loads the SQL script path from application.properties in the classpath.
     *
     * @return the relative path to the SQL script file
     * @throws IOException
     * @throws IllegalStateException
     */
    public static String loadScriptPathFromProperties() throws IOException {
        try (InputStream inputStream = MigrationUtils.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException(
                        "application.properties not found in resources");
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            String path = properties.getProperty("migration.script");
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalStateException(
                        "Property 'migration.script' not found or empty");
            }
            return path.trim();
        }
    }


    /**
     * Splits a PostgreSQL SQL script into individual statements.
     *
     * @param sql the full SQL script as a single string
     * @return a list of individual SQL statements
     */
    public static List<String> splitPostgresStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDollarQuote = false;

        for (int index = 0; index < sql.length(); index++) {
            char c = sql.charAt(index);

            if (!inSingleQuote && c == '$') {
                if ((index + 1) < sql.length() && sql.charAt(index + 1) == '$') {
                    inDollarQuote = !inDollarQuote;
                    current.append("$$");
                    index++;
                    continue;
                }
            }

            if (c == '\'' && !inDollarQuote) {
                inSingleQuote = !inSingleQuote;
            }

            if (c == ';' && !inSingleQuote && !inDollarQuote) {
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            statements.add(current.toString());
        }

        return statements;
    }

}
