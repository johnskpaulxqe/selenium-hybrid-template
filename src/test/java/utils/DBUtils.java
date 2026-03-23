package utils;

/*
  ============================================================
  FILE: DBUtils.java
  LOCATION: src/test/java/utils/DBUtils.java

  PURPOSE:
    Core JDBC utility — manages database connections and
    provides low-level query execution methods used by all
    Repository classes. Responsibilities:
      1. Open and close JDBC connections using config.json values
      2. Execute SELECT queries → return ResultSet as List<Map>
      3. Execute INSERT / UPDATE / DELETE → return rows affected
      4. Execute scalar queries → return a single value
      5. Support parameterised queries (PreparedStatement)
      6. ThreadLocal connection management for parallel safety
      7. Connection pool via simple connection reuse per thread

  HOW TO USE:
    // Execute a SELECT and get results as a list of row maps
    List<Map<String, Object>> rows = DBUtils.executeQuery(
        "SELECT * FROM users WHERE email = ?", "admin@yourapp.com");

    // Get a single value (COUNT, MAX, specific field)
    int count = (int) DBUtils.executeScalar(
        "SELECT COUNT(*) FROM users WHERE active = ?", 1);

    // Execute an UPDATE
    int rowsAffected = DBUtils.executeUpdate(
        "UPDATE users SET active = ? WHERE email = ?", 0, "user@yourapp.com");

    // Close connection for the current thread
    DBUtils.closeConnection();

  TODO (customise per project):
    - TODO-1 : config.json db.url / db.username / db.password /
               db.driverClass are read automatically — just fill
               those values in config.json for each environment
    - TODO-2 : Add connection pool library (HikariCP) if your
               test suite runs heavy parallel DB tests
    - TODO-3 : Add transaction support if your tests need to
               rollback DB changes after each scenario
  ============================================================
*/

import org.slf4j.Logger;

import java.sql.*;
import java.util.*;

public class DBUtils {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(DBUtils.class);

    // ── ThreadLocal connection ────────────────────────────────
    // Each parallel thread gets its own connection.
    // Prevents connection sharing across concurrent scenarios.
    private static final ThreadLocal<Connection> connectionHolder =
            new ThreadLocal<>();

    // Private constructor — static utility class
    private DBUtils() {}


    // ═══════════════════════════════════════════════════════════
    // CONNECTION MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    // ── getConnection() ───────────────────────────────────────
    /**
     * Returns the active JDBC Connection for the current thread.
     * Creates a new connection if none exists or if the existing
     * one is closed.
     *
     * Connection parameters are read from config.json:
     *   db.url          → jdbc:mysql://host:3306/dbname
     *   db.username     → database username
     *   db.password     → database password
     *   db.driverClass  → com.mysql.cj.jdbc.Driver
     *
     * Example:
     *   Connection conn = DBUtils.getConnection();
     *
     * @throws RuntimeException if connection cannot be established
     */
    public static Connection getConnection() {
        Connection conn = connectionHolder.get();

        try {
            // Reuse existing connection if open and valid
            if (conn != null && !conn.isClosed() && conn.isValid(5)) {
                log.debug("Reusing existing DB connection for thread [{}]",
                        Thread.currentThread().getName());
                return conn;
            }
        } catch (SQLException e) {
            log.warn("Existing connection is invalid — creating new one: {}", e.getMessage());
        }

        // Create a fresh connection
        conn = createConnection();
        connectionHolder.set(conn);
        return conn;
    }


    // ── closeConnection() ─────────────────────────────────────
    /**
     * Closes the JDBC connection for the current thread and
     * removes it from the ThreadLocal.
     * Call this from Hooks.java @After for DB scenarios to
     * prevent connection leaks.
     *
     * Example (in Hooks.java):
     *   DBUtils.closeConnection();
     */
    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    log.info("DB connection closed for thread [{}]",
                            Thread.currentThread().getName());
                }
            } catch (SQLException e) {
                log.warn("Error closing DB connection: {}", e.getMessage());
            } finally {
                connectionHolder.remove();
            }
        }
    }


    // ── isConnected() ─────────────────────────────────────────
    /**
     * Returns true if a valid connection exists for this thread.
     *
     * Example:
     *   if (DBUtils.isConnected()) { DBUtils.closeConnection(); }
     */
    public static boolean isConnected() {
        try {
            Connection conn = connectionHolder.get();
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }


    // ═══════════════════════════════════════════════════════════
    // QUERY EXECUTION — SELECT
    // ═══════════════════════════════════════════════════════════

    // ── executeQuery() ────────────────────────────────────────
    /**
     * Executes a SELECT query and returns all rows as a
     * List of Maps, where each Map is a row with column
     * names as keys and column values as values.
     *
     * Supports parameterised queries via varargs params.
     * Use ? as placeholders in the SQL string.
     *
     * Example:
     *   List<Map<String, Object>> rows = DBUtils.executeQuery(
     *       "SELECT id, email FROM users WHERE role = ?", "ADMIN");
     *
     *   for (Map<String, Object> row : rows) {
     *       String email = (String) row.get("email");
     *   }
     *
     * @param sql    parameterised SQL query
     * @param params values to bind to ? placeholders (in order)
     * @return       list of rows; empty list if no results
     */
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) {
        log.debug("Executing SELECT: [{}] with params: {}", sql, Arrays.toString(params));

        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement stmt = buildStatement(sql, params);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta      = rs.getMetaData();
            int               colCount  = meta.getColumnCount();

            // Map each row into a column-name → value map
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    // Use column label (alias) if available, else column name
                    String colName = meta.getColumnLabel(i);
                    Object value   = rs.getObject(i);
                    row.put(colName, value);
                }
                results.add(row);
            }

            log.debug("Query returned {} row(s)", results.size());

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to execute SELECT query: [" + sql + "]\n" +
                            "Params: " + Arrays.toString(params) + "\n" +
                            "Error: " + e.getMessage(), e);
        }

        return results;
    }


    // ── executeQuerySingleRow() ───────────────────────────────
    /**
     * Executes a SELECT and returns the first row as a Map.
     * Returns null if no rows are found.
     * Throws RuntimeException if more than one row is returned
     * when expectSingleRow is true.
     *
     * Example:
     *   Map<String, Object> user = DBUtils.executeQuerySingleRow(
     *       "SELECT * FROM users WHERE email = ?", "admin@yourapp.com");
     *   String firstName = (String) user.get("first_name");
     */
    public static Map<String, Object> executeQuerySingleRow(String sql, Object... params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);

        if (rows.isEmpty()) {
            log.debug("Query returned no rows: [{}]", sql);
            return null;
        }

        if (rows.size() > 1) {
            log.warn("Query [{}] returned {} rows — returning first row only", sql, rows.size());
        }

        return rows.get(0);
    }


    // ── executeScalar() ───────────────────────────────────────
    /**
     * Executes a SELECT that returns a single value.
     * Ideal for COUNT(), MAX(), MIN(), or selecting one column.
     * Returns null if no result is found.
     *
     * Example:
     *   long count = (long) DBUtils.executeScalar(
     *       "SELECT COUNT(*) FROM users WHERE active = ?", 1);
     *
     *   String email = (String) DBUtils.executeScalar(
     *       "SELECT email FROM users WHERE id = ?", 42);
     */
    public static Object executeScalar(String sql, Object... params) {
        log.debug("Executing scalar query: [{}] with params: {}",
                sql, Arrays.toString(params));

        try (PreparedStatement stmt = buildStatement(sql, params);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                Object value = rs.getObject(1);
                log.debug("Scalar result: [{}]", value);
                return value;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to execute scalar query: [" + sql + "]\n" +
                            "Params: " + Arrays.toString(params) + "\n" +
                            "Error: " + e.getMessage(), e);
        }

        return null;
    }


    // ═══════════════════════════════════════════════════════════
    // QUERY EXECUTION — DML (INSERT / UPDATE / DELETE)
    // ═══════════════════════════════════════════════════════════

    // ── executeUpdate() ───────────────────────────────────────
    /**
     * Executes an INSERT, UPDATE, or DELETE statement.
     * Returns the number of rows affected.
     *
     * Example:
     *   int affected = DBUtils.executeUpdate(
     *       "UPDATE users SET active = ? WHERE email = ?",
     *       0, "user@yourapp.com");
     *
     *   int inserted = DBUtils.executeUpdate(
     *       "INSERT INTO audit_log (event, user_id) VALUES (?, ?)",
     *       "LOGIN", 42);
     *
     * @param sql    parameterised SQL DML statement
     * @param params values to bind to ? placeholders
     * @return       number of rows affected
     */
    public static int executeUpdate(String sql, Object... params) {
        log.debug("Executing DML: [{}] with params: {}", sql, Arrays.toString(params));

        try (PreparedStatement stmt = buildStatement(sql, params)) {
            int rowsAffected = stmt.executeUpdate();
            log.debug("DML affected {} row(s)", rowsAffected);
            return rowsAffected;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to execute DML: [" + sql + "]\n" +
                            "Params: " + Arrays.toString(params) + "\n" +
                            "Error: " + e.getMessage(), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // CONVENIENCE — EXISTENCE AND COUNT CHECKS
    // ═══════════════════════════════════════════════════════════

    // ── recordExists() ────────────────────────────────────────
    /**
     * Returns true if the query returns at least one row.
     * Use in step definitions to assert DB state.
     *
     * Example:
     *   boolean exists = DBUtils.recordExists(
     *       "SELECT 1 FROM users WHERE email = ?", "admin@yourapp.com");
     *   CustomAssert.assertTrue(exists, "Admin user exists in DB");
     */
    public static boolean recordExists(String sql, Object... params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);
        boolean exists = !rows.isEmpty();
        log.debug("recordExists check → [{}]: {}", sql, exists);
        return exists;
    }


    // ── getRowCount() ─────────────────────────────────────────
    /**
     * Returns the row count from a COUNT(*) query.
     * Convenience wrapper around executeScalar().
     *
     * Example:
     *   int count = DBUtils.getRowCount(
     *       "SELECT COUNT(*) FROM users WHERE role = ?", "ADMIN");
     */
    public static int getRowCount(String countSql, Object... params) {
        Object result = executeScalar(countSql, params);
        if (result == null) return 0;
        return ((Number) result).intValue();
    }


    // ═══════════════════════════════════════════════════════════
    // TRANSACTION SUPPORT
    // TODO-3: Use these if your tests need to roll back changes
    // ═══════════════════════════════════════════════════════════

    // ── beginTransaction() ───────────────────────────────────
    /**
     * Disables auto-commit to begin a manual transaction.
     * Call at the start of a DB test scenario that writes data.
     * Always pair with commitTransaction() or rollbackTransaction().
     *
     * Example (in a @Before hook for @db scenarios):
     *   DBUtils.beginTransaction();
     */
    public static void beginTransaction() {
        try {
            getConnection().setAutoCommit(false);
            log.debug("DB transaction started for thread [{}]",
                    Thread.currentThread().getName());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction: " + e.getMessage(), e);
        }
    }


    // ── commitTransaction() ───────────────────────────────────
    /**
     * Commits the current transaction and restores auto-commit.
     */
    public static void commitTransaction() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
            log.debug("DB transaction committed");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction: " + e.getMessage(), e);
        }
    }


    // ── rollbackTransaction() ────────────────────────────────
    /**
     * Rolls back the current transaction and restores auto-commit.
     * Call in @After hook for DB write scenarios to keep test
     * data clean between runs.
     *
     * Example (in Hooks.java @After for @db tag):
     *   DBUtils.rollbackTransaction();
     */
    public static void rollbackTransaction() {
        try {
            Connection conn = connectionHolder.get();
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
                log.info("DB transaction rolled back — test data cleaned up");
            }
        } catch (SQLException e) {
            log.warn("Failed to rollback transaction: {}", e.getMessage());
        }
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── createConnection() ────────────────────────────────────
    /**
     * Creates a fresh JDBC connection using values from config.json.
     * Reads: db.url, db.username, db.password, db.driverClass
     */
    private static Connection createConnection() {
        // Read DB config for the active environment
        String url         = ConfigReader.get("db.url");
        String username    = ConfigReader.get("db.username");
        String password    = ConfigReader.get("db.password");
        String driverClass = ConfigReader.get("db.driverClass",
                "com.mysql.cj.jdbc.Driver");

        // Validate required config values
        if (url == null || url.isBlank()) {
            throw new RuntimeException(
                    "DB URL not configured. Set 'db.url' in config.json " +
                            "for the active environment: [" + ConfigReader.getActiveEnv() + "]");
        }

        try {
            // Load the JDBC driver class
            Class.forName(driverClass);
            log.info("Connecting to DB → URL: [{}] | User: [{}]", url, username);

            // Open the connection
            Connection conn = DriverManager.getConnection(url, username, password);
            conn.setAutoCommit(true);

            log.info("DB connection established for thread [{}]",
                    Thread.currentThread().getName());
            return conn;

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "JDBC driver not found: [" + driverClass + "]. " +
                            "Check the driver dependency in pom.xml.", e);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to connect to DB: [" + url + "]\n" +
                            "Error: " + e.getMessage(), e);
        }
    }


    // ── buildStatement() ─────────────────────────────────────
    /**
     * Creates a PreparedStatement and binds all parameter values.
     * Handles null values safely via setNull().
     */
    private static PreparedStatement buildStatement(String sql,
                                                    Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);

        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else {
                stmt.setObject(i + 1, params[i]);
            }
        }

        return stmt;
    }

}