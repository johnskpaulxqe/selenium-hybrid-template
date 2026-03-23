package repository;

/*
  ============================================================
  FILE: BaseRepository.java
  LOCATION: src/test/java/repository/BaseRepository.java

  PURPOSE:
    Abstract base class for all Repository classes in the
    framework. Wraps DBUtils with higher-level, reusable query
    helper methods so individual repositories stay clean and
    focused on their own table/domain logic.

    Responsibilities:
      1. Provide typed helper methods on top of DBUtils
      2. Extract single column values from query results
      3. Provide table-level helpers (exists, count, find by field)
      4. Handle null safety and type conversion
      5. Log all operations consistently

  DESIGN PATTERN — Repository Pattern:
    BaseRepository  → generic helpers (this file)
    SampleRepository → extends BaseRepository, adds methods
                       specific to one table/entity
    DbSteps.java    → calls SampleRepository methods

  HOW TO USE:
    Extend this class in your repository classes:

    public class UserRepository extends BaseRepository {
        private static final String TABLE = "users";

        public boolean userExistsByEmail(String email) {
            return existsByField(TABLE, "email", email);
        }

        public Map<String, Object> findByEmail(String email) {
            return findOneByField(TABLE, "email", email);
        }

        public int getUserCount() {
            return countAll(TABLE);
        }
    }

  TODO (customise per project):
    - TODO-1 : No changes needed — add entity-specific methods
               in subclasses, not here
    - TODO-2 : Add getColumnAsDate() / getColumnAsTimestamp()
               if your schema uses date/time columns
    - TODO-3 : Add findAllByField() with ORDER BY support if needed
  ============================================================
*/

import utils.DBUtils;
import utils.LogUtil;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public abstract class BaseRepository {

    // ── Logger ────────────────────────────────────────────────
    // Uses the concrete subclass name for clear log output
    protected final Logger log = LogUtil.getLogger(this.getClass());


    // ═══════════════════════════════════════════════════════════
    // EXISTENCE CHECKS
    // ═══════════════════════════════════════════════════════════

    // ── existsByField() ──────────────────────────────────────
    /**
     * Returns true if at least one row exists in the table
     * where the given column equals the given value.
     *
     * Example:
     *   boolean exists = existsByField("users", "email", "admin@yourapp.com");
     *   boolean exists = existsByField("products", "sku", "SKU-001");
     */
    protected boolean existsByField(String table, String column, Object value) {
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        log.debug("existsByField → table: [{}], column: [{}], value: [{}]",
                table, column, value);
        return DBUtils.recordExists(sql, value);
    }


    // ── existsByFields() ─────────────────────────────────────
    /**
     * Returns true if a row exists matching TWO column conditions.
     * Useful for composite key lookups or multi-condition checks.
     *
     * Example:
     *   boolean exists = existsByFields(
     *       "orders", "user_id", 42, "status", "COMPLETED");
     */
    protected boolean existsByFields(String table,
                                     String column1, Object value1,
                                     String column2, Object value2) {
        String sql = "SELECT 1 FROM " + table +
                " WHERE " + column1 + " = ? AND " + column2 + " = ?";
        log.debug("existsByFields → table: [{}], {}=[{}], {}=[{}]",
                table, column1, value1, column2, value2);
        return DBUtils.recordExists(sql, value1, value2);
    }


    // ═══════════════════════════════════════════════════════════
    // COUNT QUERIES
    // ═══════════════════════════════════════════════════════════

    // ── countAll() ───────────────────────────────────────────
    /**
     * Returns the total number of rows in a table.
     *
     * Example:
     *   int totalUsers = countAll("users");
     */
    protected int countAll(String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        log.debug("countAll → table: [{}]", table);
        return DBUtils.getRowCount(sql);
    }


    // ── countByField() ────────────────────────────────────────
    /**
     * Returns the count of rows where column equals value.
     *
     * Example:
     *   int adminCount = countByField("users", "role", "ADMIN");
     *   int activeUsers = countByField("users", "active", 1);
     */
    protected int countByField(String table, String column, Object value) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        log.debug("countByField → table: [{}], column: [{}], value: [{}]",
                table, column, value);
        return DBUtils.getRowCount(sql, value);
    }


    // ═══════════════════════════════════════════════════════════
    // FIND QUERIES — return row(s) as Map
    // ═══════════════════════════════════════════════════════════

    // ── findOneByField() ─────────────────────────────────────
    /**
     * Returns the first matching row as a Map<String, Object>
     * where column equals value.
     * Returns null if no match is found.
     *
     * Example:
     *   Map<String, Object> user = findOneByField("users", "email", "admin@yourapp.com");
     *   String firstName = getColumnAsString(user, "first_name");
     */
    protected Map<String, Object> findOneByField(String table,
                                                 String column,
                                                 Object value) {
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = ?";
        log.debug("findOneByField → table: [{}], column: [{}], value: [{}]",
                table, column, value);
        return DBUtils.executeQuerySingleRow(sql, value);
    }


    // ── findAllByField() ──────────────────────────────────────
    /**
     * Returns all matching rows as a List<Map<String, Object>>
     * where column equals value.
     * Returns an empty list if no matches are found.
     *
     * Example:
     *   List<Map<String, Object>> admins = findAllByField("users", "role", "ADMIN");
     */
    protected List<Map<String, Object>> findAllByField(String table,
                                                       String column,
                                                       Object value) {
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = ?";
        log.debug("findAllByField → table: [{}], column: [{}], value: [{}]",
                table, column, value);
        return DBUtils.executeQuery(sql, value);
    }


    // ── findAll() ─────────────────────────────────────────────
    /**
     * Returns all rows from a table.
     * Use with caution on large tables — add a LIMIT in subclasses.
     *
     * Example:
     *   List<Map<String, Object>> allUsers = findAll("users");
     */
    protected List<Map<String, Object>> findAll(String table) {
        String sql = "SELECT * FROM " + table;
        log.debug("findAll → table: [{}]", table);
        return DBUtils.executeQuery(sql);
    }


    // ── findById() ────────────────────────────────────────────
    /**
     * Returns a single row by its primary key ID column.
     * Assumes the ID column is named "id" — override if needed.
     *
     * Example:
     *   Map<String, Object> user = findById("users", 42);
     *   String email = getColumnAsString(user, "email");
     */
    protected Map<String, Object> findById(String table, Object id) {
        return findOneByField(table, "id", id);
    }


    // ── findByCustomQuery() ───────────────────────────────────
    /**
     * Executes a fully custom SELECT query with parameters.
     * Use when the standard field-based finders are not enough.
     *
     * Example:
     *   List<Map<String, Object>> rows = findByCustomQuery(
     *       "SELECT * FROM orders WHERE user_id = ? AND status = ? ORDER BY created_at DESC",
     *       userId, "COMPLETED");
     */
    protected List<Map<String, Object>> findByCustomQuery(String sql, Object... params) {
        log.debug("findByCustomQuery → sql: [{}], params: {}", sql, java.util.Arrays.toString(params));
        return DBUtils.executeQuery(sql, params);
    }


    // ─── findSingleByCustomQuery() ────────────────────────────
    /**
     * Executes a custom SELECT and returns the first row.
     * Returns null if no rows found.
     *
     * Example:
     *   Map<String, Object> latest = findSingleByCustomQuery(
     *       "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 1",
     *       userId);
     */
    protected Map<String, Object> findSingleByCustomQuery(String sql, Object... params) {
        log.debug("findSingleByCustomQuery → sql: [{}]", sql);
        return DBUtils.executeQuerySingleRow(sql, params);
    }


    // ═══════════════════════════════════════════════════════════
    // COLUMN VALUE EXTRACTORS — typed getters from row Maps
    // ═══════════════════════════════════════════════════════════

    // ── getColumnAsString() ───────────────────────────────────
    /**
     * Extracts a column value from a row Map as a String.
     * Returns null if the row or column is null.
     *
     * Example:
     *   Map<String, Object> row = findOneByField("users", "id", 1);
     *   String email = getColumnAsString(row, "email");
     */
    protected String getColumnAsString(Map<String, Object> row, String column) {
        if (row == null) {
            log.warn("Cannot extract column [{}] — row is null", column);
            return null;
        }
        Object value = row.get(column);
        return value != null ? value.toString() : null;
    }


    // ── getColumnAsInt() ─────────────────────────────────────
    /**
     * Extracts a column value from a row Map as an int.
     * Returns defaultValue if the row or column is null or
     * cannot be parsed as an integer.
     *
     * Example:
     *   int age = getColumnAsInt(row, "age", 0);
     */
    protected int getColumnAsInt(Map<String, Object> row, String column, int defaultValue) {
        if (row == null || row.get(column) == null) return defaultValue;
        try {
            return ((Number) row.get(column)).intValue();
        } catch (ClassCastException e) {
            try {
                return Integer.parseInt(row.get(column).toString());
            } catch (NumberFormatException ex) {
                log.warn("Column [{}] value [{}] cannot be cast to int → using default: {}",
                        column, row.get(column), defaultValue);
                return defaultValue;
            }
        }
    }


    // ── getColumnAsLong() ────────────────────────────────────
    /**
     * Extracts a column value from a row Map as a long.
     * Useful for BIGINT primary keys or timestamp millis.
     *
     * Example:
     *   long id = getColumnAsLong(row, "id", -1L);
     */
    protected long getColumnAsLong(Map<String, Object> row, String column, long defaultValue) {
        if (row == null || row.get(column) == null) return defaultValue;
        try {
            return ((Number) row.get(column)).longValue();
        } catch (ClassCastException e) {
            try {
                return Long.parseLong(row.get(column).toString());
            } catch (NumberFormatException ex) {
                log.warn("Column [{}] value [{}] cannot be cast to long → using default: {}",
                        column, row.get(column), defaultValue);
                return defaultValue;
            }
        }
    }


    // ── getColumnAsBoolean() ─────────────────────────────────
    /**
     * Extracts a column value from a row Map as a boolean.
     * Handles MySQL TINYINT(1) booleans (0=false, 1=true)
     * and string "true"/"false" values.
     *
     * Example:
     *   boolean active = getColumnAsBoolean(row, "active", false);
     */
    protected boolean getColumnAsBoolean(Map<String, Object> row,
                                         String column,
                                         boolean defaultValue) {
        if (row == null || row.get(column) == null) return defaultValue;
        Object value = row.get(column);

        // Handle MySQL TINYINT(1) stored as Number (0 or 1)
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        // Handle string "true" / "false" / "1" / "0"
        String strValue = value.toString().toLowerCase().trim();
        return strValue.equals("true") || strValue.equals("1");
    }


    // ── getScalarValue() ─────────────────────────────────────
    /**
     * Executes a scalar query and returns the result as a String.
     * Convenience wrapper for single-value queries.
     *
     * Example:
     *   String maxId = getScalarValue(
     *       "SELECT MAX(id) FROM users", null);
     */
    protected String getScalarValue(String sql, Object... params) {
        Object result = DBUtils.executeScalar(sql, params);
        return result != null ? result.toString() : null;
    }


    // ═══════════════════════════════════════════════════════════
    // DML HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── deleteByField() ───────────────────────────────────────
    /**
     * Deletes all rows from a table where column equals value.
     * Returns the number of rows deleted.
     * Use in @After hooks to clean up test data.
     *
     * Example:
     *   int deleted = deleteByField("users", "email", "testuser@yourapp.com");
     */
    protected int deleteByField(String table, String column, Object value) {
        String sql = "DELETE FROM " + table + " WHERE " + column + " = ?";
        log.debug("deleteByField → table: [{}], column: [{}], value: [{}]",
                table, column, value);
        return DBUtils.executeUpdate(sql, value);
    }


    // ── updateField() ─────────────────────────────────────────
    /**
     * Updates a single column in all rows where a condition is met.
     * Returns the number of rows updated.
     *
     * Example:
     *   int updated = updateField("users", "active", 0, "email", "user@yourapp.com");
     *   // SQL: UPDATE users SET active = 0 WHERE email = 'user@yourapp.com'
     */
    protected int updateField(String table,
                              String setColumn,   Object setValue,
                              String whereColumn, Object whereValue) {
        String sql = "UPDATE " + table +
                " SET "   + setColumn   + " = ?" +
                " WHERE " + whereColumn + " = ?";
        log.debug("updateField → table: [{}], SET {}=[{}] WHERE {}=[{}]",
                table, setColumn, setValue, whereColumn, whereValue);
        return DBUtils.executeUpdate(sql, setValue, whereValue);
    }

}