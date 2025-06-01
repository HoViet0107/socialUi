package personal.social.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for initializing stored procedures in the database.
 * <p>
 * This Spring component runs automatically at application startup (via CommandLineRunner)
 * and performs the following:
 * <p>
 * ✅ Scans all `.sql` files inside the classpath directory `procedures/`
 * ✅ Reads and parses each SQL script, including handling custom `DELIMITER` syntax if present
 * ✅ Extracts the stored procedure name either from the filename or from the SQL content
 * ✅ Drops any existing procedure of the same name to ensure fresh creation
 * ✅ Executes the SQL command to create or replace the stored procedure in the connected database
 * <p>
 * Usage:
 * Place SQL procedure files (e.g., `GetTopLevelComments.sql`) in `src/main/resources/procedures/`.
 * On application startup, they will be automatically loaded and executed.
 * <p>
 * Dependencies:
 * - Spring DataSource
 * - Spring PathMatchingResourcePatternResolver for classpath scanning
 * - JDBC Connection and Statement interfaces for executing SQL
 * - Lombok @Slf4j for structured logging
 * <p>
 * Notes:
 * - Ensure the SQL syntax is compatible with your database.
 * - This utility is sensitive to file naming and SQL syntax (especially around DELIMITER).
 */
@Component
@Slf4j
public class ProcedureInitializer implements CommandLineRunner {
    private final DataSource dataSource;

    @Autowired
    public ProcedureInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This is the entry point triggered by Spring Boot on application startup.
     *
     * @param args command-line arguments passed to the application (not used here)
     * @throws Exception if any error occurs during procedure initialization
     */
    @Override
    public void run(String... args) throws Exception {
        try {
            // Find all SQL files in procedures directory
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:procedures/*.sql");

            if (resources.length == 0) {
                log.warn("No SQL procedure files found in procedures directory!");
            }

            for (Resource resource : resources) {
                String procedureName = extractProcedureName(resource.getFilename());
                String procedureSQL = readSqlFile(resource);

                if (procedureSQL != null && !procedureSQL.isEmpty()) {
                    executeProcedure(procedureName, procedureSQL);
                }
            }

            log.info("All stored procedures created successfully");
        } catch (Exception e) {
            log.error("Error creating stored procedures: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts the procedure name from the given SQL filename.
     * Example: "GetTopLevelFeedItems.sql" → "GetTopLevelFeedItems"
     *
     * @param filename the name of the SQL file (may include .sql extension)
     * @return the procedure name without extension, or "Unknown" if filename is null
     */
    private String extractProcedureName(String filename) {
        if (filename == null) return "Unknown";
        // Get procedure name from file name (Ex: GetCommentReplies.sql -> GetTopLevelComments)
        return filename.replace(".sql", "");
    }

    /**
     * Reads the SQL content from a Resource file.
     * Handles both simple SQL files and files using custom DELIMITER syntax.
     * <p>
     * Logic:
     * ✅ Detects if the file contains `DELIMITER` commands
     * ✅ For files with DELIMITER, rewrites the body correctly for JDBC execution
     * ✅ Skips comments (`--`) and empty lines
     *
     * @param resource the Resource object representing the SQL file
     * @return the full SQL string to execute, or null if reading fails
     */
    private String readSqlFile(Resource resource) {
        StringBuilder sqlBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            boolean hasDelimiter = false;

            // First pass: check if file contains DELIMITER
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("DELIMITER")) {
                    hasDelimiter = true;
                    break;
                }
            }

            // Reset reader
            reader.close();
            BufferedReader newReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            // Second pass: process file based on whether it has DELIMITER or not
            if (hasDelimiter) {
                // Process file with DELIMITER
                boolean insideProcedureBody = false;
                while ((line = newReader.readLine()) != null) {
                    line = line.trim();

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }

                    // Handle DELIMITER lines
                    if (line.startsWith("DELIMITER")) {
                        if (line.equals("DELIMITER ;")) {
                            insideProcedureBody = false;
                        } else if (line.equals("DELIMITER //")) {
                            insideProcedureBody = true;
                        }
                        continue;
                    }

                    // Handle procedure body
                    if (insideProcedureBody && line.endsWith("//")) {
                        line = line.substring(0, line.length() - 2) + ";";
                    }

                    sqlBuilder.append(line).append("\n");
                }
            } else {
                // Process file without DELIMITER
                while ((line = newReader.readLine()) != null) {
                    sqlBuilder.append(line).append("\n");
                }
            }

            return sqlBuilder.toString();
        } catch (IOException e) {
            log.error("Error reading SQL file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Executes the given SQL script to create or replace the stored procedure.
     * First drops the existing procedure (if any), then executes the creation.
     * <p>
     * Logic:
     * ✅ Extracts the actual procedure name from the SQL body (via regex)
     * ✅ Uses JDBC connection and statement to execute DROP and CREATE commands
     * ✅ Logs both success and detailed errors
     *
     * @param procedureName the fallback name derived from filename (used if SQL parsing fails)
     * @param procedureSQL  the SQL script to execute
     */
    private void executeProcedure(String procedureName, String procedureSQL) {
        // Find actual procedure name from SQL
        Pattern pattern = Pattern.compile("CREATE\\s+PROCEDURE\\s+(\\w+)\\s*\\(",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(procedureSQL);
        String actualProcedureName = matcher.find() ? matcher.group(1) : procedureName;

        try (Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                // Delete old procedure if it exists
                stmt.execute("DROP PROCEDURE IF EXISTS " + actualProcedureName);
                log.info("Dropped existing procedure: {}", actualProcedureName);
            }

            try (Statement stmt = connection.createStatement()) {
                // Create new procedure
                stmt.execute(procedureSQL);
                log.info("Created procedure: {}", actualProcedureName);
            }
        } catch (SQLException e) {
            log.error("Error executing procedure {}: {}", actualProcedureName, e.getMessage(), e);
        }
    }
}
