package repository;

/*
  ============================================================
  FILE: SampleRepository.java
  LOCATION: src/test/java/repository/SampleRepository.java

  PURPOSE:
    Example repository showing exactly how to extend
    BaseRepository for a specific database table/entity.
    Demonstrates all common patterns your real repositories
    should follow.

    This file covers the "users" table as a working example.
    When you add your own tables, copy this pattern:
      1. Define the TABLE constant
      2. Add entity-specific finder methods
      3. Add entity-specific assertion helpers
      4. Add any cleanup methods needed in @After hooks

  HOW TO USE:
    // Instantiate in your step definition class
    private final SampleRepository userRepo = new SampleRepository();

    // Use in step definitions
    boolean exists  = userRepo.userExistsByEmail("admin@yourapp.com");
    String  role    = userRepo.getUserRole("admin@yourapp.com");
    int     count   = userRepo.countActiveUsers();

    // Clean up test data after write scenarios
    userRepo.deleteTestUser("newuser@yourapp.com");

  TODO (customise per project):
    - TODO-1 : Rename this class and TABLE constant to match
               your first real database table
    - TODO-2 : Replace the example methods with methods that
               match your application's schema and column names
    - TODO-3 : Add a repository class per entity/table, each
               extending BaseRepository the same way
    - TODO-4 : Wire into DbSteps.java for use in @db scenarios

  EXAMPLE — adding a second repository for orders table:
    public class OrderRepository extends BaseRepository {
        private static final String TABLE = "orders";

        public boolean orderExistsById(String orderId) {
            return existsByField(TABLE, "order_id", orderId);
        }

        public String getOrderStatus(String orderId) {
            Map<String, Object> row = findOneByField(TABLE, "order_id", orderId);
            return getColumnAsString(row, "status");
        }
    }
  ============================================================
*/

import utils.DBUtils;
import utils.TestDataLoader;

import java.util.List;
import java.util.Map;

public class SampleRepository extends BaseRepository {

    // ── Table name ────────────────────────────────────────────
    // TODO-1: Change this to your actual table name
    private static final String TABLE = "users";


    // ═══════════════════════════════════════════════════════════
    // EXISTENCE CHECKS
    // ═══════════════════════════════════════════════════════════

    // ── userExistsByEmail() ───────────────────────────────────
    /**
     * Returns true if a user with the given email exists.
     *
     * Example:
     *   boolean exists = userRepo.userExistsByEmail("admin@yourapp.com");
     *   CustomAssert.assertTrue(exists, "Admin user exists in DB");
     *
     * TODO-2: Rename to match your entity, e.g. productExistsBySku()
     */
    public boolean userExistsByEmail(String email) {
        log.debug("Checking if user exists with email: [{}]", email);
        return existsByField(TABLE, "email", email);
    }


    // ── userExistsByUsername() ────────────────────────────────
    /**
     * Returns true if a user with the given username exists.
     *
     * Example:
     *   boolean exists = userRepo.userExistsByUsername("admin@yourapp.com");
     */
    public boolean userExistsByUsername(String username) {
        log.debug("Checking if user exists with username: [{}]", username);
        return existsByField(TABLE, "username", username);
    }


    // ── activeUserExistsByEmail() ─────────────────────────────
    /**
     * Returns true if an ACTIVE user with the given email exists.
     * Combines two conditions using existsByFields().
     *
     * Example:
     *   boolean active = userRepo.activeUserExistsByEmail("admin@yourapp.com");
     */
    public boolean activeUserExistsByEmail(String email) {
        log.debug("Checking if active user exists with email: [{}]", email);
        return existsByFields(TABLE, "email", email, "active", 1);
    }


    // ═══════════════════════════════════════════════════════════
    // FIND / FETCH
    // ═══════════════════════════════════════════════════════════

    // ── getUserByEmail() ─────────────────────────────────────
    /**
     * Returns a full user row as a Map by email address.
     * Returns null if not found.
     *
     * Example:
     *   Map<String, Object> user = userRepo.getUserByEmail("admin@yourapp.com");
     *   String role = userRepo.getColumnAsString(user, "role");
     *
     * TODO-2: Rename and change column names to match your schema
     */
    public Map<String, Object> getUserByEmail(String email) {
        log.debug("Fetching user by email: [{}]", email);
        return findOneByField(TABLE, "email", email);
    }


    // ── getUserById() ─────────────────────────────────────────
    /**
     * Returns a full user row as a Map by primary key ID.
     * Returns null if not found.
     *
     * Example:
     *   Map<String, Object> user = userRepo.getUserById(42);
     */
    public Map<String, Object> getUserById(int id) {
        log.debug("Fetching user by id: [{}]", id);
        return findById(TABLE, id);
    }


    // ── getAllActiveUsers() ───────────────────────────────────
    /**
     * Returns all active users as a list of row Maps.
     *
     * Example:
     *   List<Map<String, Object>> users = userRepo.getAllActiveUsers();
     *   int activeCount = users.size();
     */
    public List<Map<String, Object>> getAllActiveUsers() {
        log.debug("Fetching all active users");
        return findAllByField(TABLE, "active", 1);
    }


    // ── getUsersByRole() ──────────────────────────────────────
    /**
     * Returns all users with the given role.
     *
     * Example:
     *   List<Map<String, Object>> admins = userRepo.getUsersByRole("ADMIN");
     */
    public List<Map<String, Object>> getUsersByRole(String role) {
        log.debug("Fetching users with role: [{}]", role);
        return findAllByField(TABLE, "role", role);
    }


    // ═══════════════════════════════════════════════════════════
    // COLUMN VALUE GETTERS
    // Convenience methods that combine find + extract in one call
    // ═══════════════════════════════════════════════════════════

    // ── getUserRole() ─────────────────────────────────────────
    /**
     * Returns the role of a user identified by email.
     * Returns null if user not found.
     *
     * Example:
     *   String role = userRepo.getUserRole("admin@yourapp.com");
     *   CustomAssert.assertEquals(role, "ADMIN", "User role check");
     *
     * TODO-2: Change "role" to the actual column name in your schema
     */
    public String getUserRole(String email) {
        Map<String, Object> user = getUserByEmail(email);
        return getColumnAsString(user, "role");
    }


    // ── isUserActive() ────────────────────────────────────────
    /**
     * Returns true if the user identified by email is active.
     *
     * Example:
     *   boolean active = userRepo.isUserActive("admin@yourapp.com");
     *   CustomAssert.assertTrue(active, "Admin user is active");
     */
    public boolean isUserActive(String email) {
        Map<String, Object> user = getUserByEmail(email);
        return getColumnAsBoolean(user, "active", false);
    }


    // ── getUserFirstName() ────────────────────────────────────
    /**
     * Returns the first name of a user identified by email.
     *
     * Example:
     *   String name = userRepo.getUserFirstName("admin@yourapp.com");
     *   CustomAssert.assertEquals(name, "Admin", "First name check");
     *
     * TODO-2: Change "first_name" to the actual column name in your schema
     */
    public String getUserFirstName(String email) {
        Map<String, Object> user = getUserByEmail(email);
        return getColumnAsString(user, "first_name");
    }


    // ═══════════════════════════════════════════════════════════
    // COUNT QUERIES
    // ═══════════════════════════════════════════════════════════

    // ── countAllUsers() ───────────────────────────────────────
    /**
     * Returns the total number of users in the table.
     *
     * Example:
     *   int total = userRepo.countAllUsers();
     *   CustomAssert.assertGreaterThan(total, 0, "At least one user exists");
     */
    public int countAllUsers() {
        log.debug("Counting all users");
        return countAll(TABLE);
    }


    // ── countActiveUsers() ────────────────────────────────────
    /**
     * Returns the count of active users.
     *
     * Example:
     *   int activeCount = userRepo.countActiveUsers();
     */
    public int countActiveUsers() {
        log.debug("Counting active users");
        return countByField(TABLE, "active", 1);
    }


    // ── countUsersByRole() ────────────────────────────────────
    /**
     * Returns the count of users with the given role.
     *
     * Example:
     *   int adminCount = userRepo.countUsersByRole("ADMIN");
     *   CustomAssert.assertGreaterThan(adminCount, 0, "At least one admin exists");
     */
    public int countUsersByRole(String role) {
        log.debug("Counting users with role: [{}]", role);
        return countByField(TABLE, "role", role);
    }


    // ═══════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // Combines DB fetch + assertion in one method for clean steps
    // ═══════════════════════════════════════════════════════════

    // ── assertUserExistsInDb() ────────────────────────────────
    /**
     * Asserts that the test data admin user exists in the DB.
     * Uses expected email from dbdata.json so the assertion
     * value is not hardcoded in step definitions.
     *
     * Example:
     *   userRepo.assertAdminUserExistsInDb();
     *
     * TODO-2: Add similar assertion helpers for your own
     *         test data entries in dbdata.json
     */
    public boolean assertAdminUserExistsInDb() {
        // Read expected email from dbdata.json so it stays in one place
        String expectedEmail = TestDataLoader.getDbExpectedValue(
                "expectedValues.users.admin.email");

        log.info("Asserting admin user exists in DB with email: [{}]", expectedEmail);
        return userExistsByEmail(expectedEmail);
    }


    // ── validateUserRecord() ──────────────────────────────────
    /**
     * Fetches a user by email and returns a validation Map
     * with key DB fields for assertion in step definitions.
     * Useful for multi-field DB validations in one step.
     *
     * Example (in DbSteps.java):
     *   Map<String, Object> record = userRepo.validateUserRecord("admin@yourapp.com");
     *   CustomAssert.assertEquals(record.get("role"), "ADMIN", "Role check");
     *   CustomAssert.assertEquals(record.get("active"), true, "Active check");
     */
    public Map<String, Object> validateUserRecord(String email) {
        log.debug("Fetching full user record for validation: [{}]", email);
        Map<String, Object> user = getUserByEmail(email);

        if (user == null) {
            throw new RuntimeException(
                    "Validation failed: no user record found for email [" + email + "]");
        }

        return user;
    }


    // ═══════════════════════════════════════════════════════════
    // TEST DATA CLEANUP
    // Use in @After hooks for @db write scenarios
    // ═══════════════════════════════════════════════════════════

    // ── deleteTestUser() ─────────────────────────────────────
    /**
     * Deletes a user record by email.
     * Call from @After hooks to clean up users created during tests.
     * Returns the number of rows deleted (0 if user didn't exist).
     *
     * Example (in Hooks.java @After for @db scenarios):
     *   userRepo.deleteTestUser("newuser@yourapp.com");
     *
     * TODO-2: Add similar cleanup methods for other entities
     */
    public int deleteTestUser(String email) {
        log.info("Cleaning up test user with email: [{}]", email);
        return deleteByField(TABLE, "email", email);
    }


    // ── resetUserActive() ─────────────────────────────────────
    /**
     * Resets a user's active status to the given value.
     * Use to restore test data state after a test that
     * deactivates a user.
     *
     * Example:
     *   userRepo.resetUserActive("user@yourapp.com", 1); // re-activate
     *   userRepo.resetUserActive("user@yourapp.com", 0); // deactivate
     */
    public int resetUserActive(String email, int activeStatus) {
        log.info("Resetting active=[{}] for user: [{}]", activeStatus, email);
        return updateField(TABLE, "active", activeStatus, "email", email);
    }


    // ═══════════════════════════════════════════════════════════
    // CUSTOM QUERIES
    // For anything not covered by BaseRepository helpers
    // ═══════════════════════════════════════════════════════════

    // ── getUsersCreatedAfter() ────────────────────────────────
    /**
     * Returns all users created after a given date string.
     * Demonstrates custom SQL not covered by base helpers.
     *
     * Example:
     *   List<Map<String, Object>> newUsers =
     *       userRepo.getUsersCreatedAfter("2024-01-01");
     *
     * TODO-2: Replace with custom queries relevant to your schema.
     *         Change "created_at" to your actual timestamp column name.
     */
    public List<Map<String, Object>> getUsersCreatedAfter(String dateString) {
        log.debug("Fetching users created after: [{}]", dateString);
        String sql = "SELECT * FROM " + TABLE +
                " WHERE created_at > ? ORDER BY created_at DESC";
        return findByCustomQuery(sql, dateString);
    }


    // ── getUserCountByRoleFromNamedQuery() ────────────────────
    /**
     * Demonstrates using a named query from dbdata.json.
     * Named queries keep SQL out of Java code and in one place.
     *
     * Example:
     *   int count = userRepo.getUserCountByRoleFromNamedQuery("ADMIN");
     */
    public int getUserCountByRoleFromNamedQuery(String role) {
        // Load the SQL from dbdata.json → queries.countUsers
        String sql = TestDataLoader.getDbQuery("countUsers");
        log.debug("Executing named query [countUsers] with role: [{}]", role);
        return DBUtils.getRowCount(sql);
    }

}