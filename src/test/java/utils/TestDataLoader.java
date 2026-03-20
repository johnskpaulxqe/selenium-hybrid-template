package utils;

/*
  ============================================================
  FILE: TestDataLoader.java
  LOCATION: src/test/java/utils/TestDataLoader.java

  PURPOSE:
    Centralised test data access layer. Sits on top of JsonReader
    and provides strongly-typed, named methods for loading test
    data from the three testdata JSON files:
      - users.json    → user credentials and profiles
      - apidata.json  → API endpoints, headers, request bodies
      - dbdata.json   → expected DB state, query params

    Responsibilities:
      1. Load and cache test data files on first access
      2. Provide named getter methods for each data category
      3. Support parameterised data access (get user by role,
         get API data by scenario name, etc.)
      4. Support data-driven scenarios via TestNG DataProvider
         and Cucumber Examples tables

  HOW TO USE:
    // Get credentials for a specific user role
    String username = TestDataLoader.getUsername("admin");
    String password = TestDataLoader.getPassword("admin");

    // Get a full user node as JsonNode
    JsonNode adminUser = TestDataLoader.getUser("admin");

    // Get API endpoint by name
    String loginUrl = TestDataLoader.getApiEndpoint("login");

    // Get expected DB value
    String expectedEmail = TestDataLoader.getDbExpectedValue("users", "admin.email");

    // Get raw node for full flexibility
    JsonNode node = TestDataLoader.getUsersData();

  TODO (customise per project):
    - TODO-1 : Add your own user roles to users.json and matching
               getters here (e.g. getManagerUser(), getReadOnlyUser())
    - TODO-2 : Add your own API data sections to apidata.json and
               matching getters (e.g. getOrderPayload(), getSearchParams())
    - TODO-3 : Add your own DB expected value sections to dbdata.json
    - TODO-4 : Add a DataProvider method if you are using TestNG
               data-driven tests alongside Cucumber
  ============================================================
*/

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

public class TestDataLoader {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(TestDataLoader.class);

    // ── Test data file paths (from config.json) ───────────────
    // These paths are relative to src/test/resources
    private static final String USERS_FILE =
            ConfigReader.get("testData.usersFile", "testdata/users.json");

    private static final String API_DATA_FILE =
            ConfigReader.get("testData.apiDataFile", "testdata/apidata.json");

    private static final String DB_DATA_FILE =
            ConfigReader.get("testData.dbDataFile", "testdata/dbdata.json");

    // ── Cached root nodes ─────────────────────────────────────
    // Files are read once and cached for the entire test run.
    // Lazy-loaded on first access via getter methods below.
    private static JsonNode usersRoot   = null;
    private static JsonNode apiDataRoot = null;
    private static JsonNode dbDataRoot  = null;

    // Private constructor — static utility class
    private TestDataLoader() {}


    // ═══════════════════════════════════════════════════════════
    // RAW ROOT NODE ACCESS
    // Use these when you need full flexibility over the data
    // ═══════════════════════════════════════════════════════════

    // ── getUsersData() ────────────────────────────────────────
    /**
     * Returns the root JsonNode of users.json.
     * Cached after first load.
     *
     * Example:
     *   JsonNode root    = TestDataLoader.getUsersData();
     *   String   adminPw = root.get("admin").get("password").asText();
     */
    public static JsonNode getUsersData() {
        if (usersRoot == null) {
            log.debug("Loading users test data from: [{}]", USERS_FILE);
            usersRoot = JsonReader.readTree(USERS_FILE);
        }
        return usersRoot;
    }


    // ── getApiData() ──────────────────────────────────────────
    /**
     * Returns the root JsonNode of apidata.json.
     * Cached after first load.
     *
     * Example:
     *   JsonNode root     = TestDataLoader.getApiData();
     *   String   endpoint = root.get("endpoints").get("login").asText();
     */
    public static JsonNode getApiData() {
        if (apiDataRoot == null) {
            log.debug("Loading API test data from: [{}]", API_DATA_FILE);
            apiDataRoot = JsonReader.readTree(API_DATA_FILE);
        }
        return apiDataRoot;
    }


    // ── getDbData() ───────────────────────────────────────────
    /**
     * Returns the root JsonNode of dbdata.json.
     * Cached after first load.
     *
     * Example:
     *   JsonNode root  = TestDataLoader.getDbData();
     *   String   email = root.get("users").get("admin").get("email").asText();
     */
    public static JsonNode getDbData() {
        if (dbDataRoot == null) {
            log.debug("Loading DB test data from: [{}]", DB_DATA_FILE);
            dbDataRoot = JsonReader.readTree(DB_DATA_FILE);
        }
        return dbDataRoot;
    }


    // ═══════════════════════════════════════════════════════════
    // USER DATA — from users.json
    // ═══════════════════════════════════════════════════════════

    // ── getUser() ─────────────────────────────────────────────
    /**
     * Returns the full JsonNode for a named user role.
     * Role names match the top-level keys in users.json.
     *
     * Example:
     *   JsonNode admin = TestDataLoader.getUser("admin");
     *   String   email = admin.get("email").asText();
     *
     * TODO-1: Add roles matching your application's user types
     */
    public static JsonNode getUser(String role) {
        JsonNode user = getUsersData().get(role);
        if (user == null) {
            throw new RuntimeException(
                    "User role [" + role + "] not found in [" + USERS_FILE + "]. " +
                            "Check that the role key exists in users.json.");
        }
        log.debug("Loaded user data for role: [{}]", role);
        return user;
    }


    // ── getUsername() ─────────────────────────────────────────
    /**
     * Returns the username for the given user role.
     *
     * Example:
     *   String username = TestDataLoader.getUsername("admin");
     *   String username = TestDataLoader.getUsername("standard");
     */
    public static String getUsername(String role) {
        return getUser(role).get("username").asText();
    }


    // ── getPassword() ─────────────────────────────────────────
    /**
     * Returns the password for the given user role.
     *
     * Example:
     *   String password = TestDataLoader.getPassword("admin");
     */
    public static String getPassword(String role) {
        return getUser(role).get("password").asText();
    }


    // ── getEmail() ────────────────────────────────────────────
    /**
     * Returns the email address for the given user role.
     *
     * Example:
     *   String email = TestDataLoader.getEmail("admin");
     */
    public static String getEmail(String role) {
        JsonNode emailNode = getUser(role).get("email");
        if (emailNode == null) {
            log.warn("No email field found for user role [{}]", role);
            return null;
        }
        return emailNode.asText();
    }


    // ── getUserField() ────────────────────────────────────────
    /**
     * Returns any string field from a user's data block.
     * Use for fields beyond username/password/email.
     *
     * Example:
     *   String firstName = TestDataLoader.getUserField("admin", "firstName");
     *   String phone     = TestDataLoader.getUserField("admin", "phone");
     */
    public static String getUserField(String role, String field) {
        JsonNode user      = getUser(role);
        JsonNode fieldNode = user.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            log.warn("Field [{}] not found for user role [{}]", field, role);
            return null;
        }
        return fieldNode.asText();
    }


    // ── Convenience shortcuts for common roles ────────────────
    // TODO-1: Add/rename these to match your application's user roles

    /** Returns the admin user node */
    public static JsonNode getAdminUser() { return getUser("admin"); }

    /** Returns the standard/regular user node */
    public static JsonNode getStandardUser() { return getUser("standard"); }

    /** Returns the read-only user node */
    public static JsonNode getReadOnlyUser() { return getUser("readonly"); }


    // ═══════════════════════════════════════════════════════════
    // API DATA — from apidata.json
    // ═══════════════════════════════════════════════════════════

    // ── getApiEndpoint() ─────────────────────────────────────
    /**
     * Returns an API endpoint path by name.
     * Endpoint names match keys under "endpoints" in apidata.json.
     *
     * Example:
     *   String loginUrl = TestDataLoader.getApiEndpoint("login");
     *   // Returns: "/api/v1/auth/login"
     *
     * TODO-2: Add endpoints matching your application's API routes
     */
    public static String getApiEndpoint(String endpointName) {
        JsonNode endpoints = getApiData().get("endpoints");
        if (endpoints == null) {
            throw new RuntimeException(
                    "No 'endpoints' section found in [" + API_DATA_FILE + "]");
        }
        JsonNode endpoint = endpoints.get(endpointName);
        if (endpoint == null) {
            throw new RuntimeException(
                    "Endpoint [" + endpointName + "] not found in apidata.json endpoints section.");
        }
        log.debug("Loaded API endpoint [{}]: [{}]", endpointName, endpoint.asText());
        return endpoint.asText();
    }


    // ── getRequestBody() ─────────────────────────────────────
    /**
     * Returns a request body template node by name.
     * Matches keys under "requestBodies" in apidata.json.
     *
     * Example:
     *   JsonNode body = TestDataLoader.getRequestBody("createUser");
     *   String   json = JsonReader.toJson(body);
     *
     * TODO-2: Add request body templates for your API scenarios
     */
    public static JsonNode getRequestBody(String bodyName) {
        JsonNode bodies = getApiData().get("requestBodies");
        if (bodies == null) {
            throw new RuntimeException(
                    "No 'requestBodies' section found in [" + API_DATA_FILE + "]");
        }
        JsonNode body = bodies.get(bodyName);
        if (body == null) {
            throw new RuntimeException(
                    "Request body [" + bodyName + "] not found in apidata.json.");
        }
        log.debug("Loaded request body template: [{}]", bodyName);
        return body;
    }


    // ── getApiHeader() ────────────────────────────────────────
    /**
     * Returns a specific header value by name.
     * Matches keys under "headers" in apidata.json.
     *
     * Example:
     *   String contentType = TestDataLoader.getApiHeader("Content-Type");
     */
    public static String getApiHeader(String headerName) {
        JsonNode headers = getApiData().get("headers");
        if (headers == null || headers.get(headerName) == null) {
            log.warn("Header [{}] not found in apidata.json", headerName);
            return null;
        }
        return headers.get(headerName).asText();
    }


    // ── getApiField() ─────────────────────────────────────────
    /**
     * Returns any field from apidata.json using dot-notation.
     * Useful for accessing deeply nested API test data.
     *
     * Example:
     *   String token = TestDataLoader.getApiField("auth.staticToken");
     *   int    limit = Integer.parseInt(TestDataLoader.getApiField("pagination.defaultLimit"));
     */
    public static String getApiField(String dotPath) {
        return JsonReader.getField(API_DATA_FILE, dotPath);
    }


    // ═══════════════════════════════════════════════════════════
    // DATABASE DATA — from dbdata.json
    // ═══════════════════════════════════════════════════════════

    // ── getDbExpectedValue() ─────────────────────────────────
    /**
     * Returns an expected database value using dot-notation.
     * Use to validate DB state after operations in step definitions.
     *
     * Example:
     *   String expectedEmail = TestDataLoader.getDbExpectedValue("users.admin.email");
     *   String expectedRole  = TestDataLoader.getDbExpectedValue("users.admin.role");
     *
     * TODO-3: Add expected DB values matching your schema
     */
    public static String getDbExpectedValue(String dotPath) {
        String value = JsonReader.getField(DB_DATA_FILE, dotPath);
        if (value == null) {
            log.warn("DB expected value not found for path [{}] in [{}]",
                    dotPath, DB_DATA_FILE);
        }
        return value;
    }


    // ── getDbQuery() ─────────────────────────────────────────
    /**
     * Returns a named SQL query string from dbdata.json.
     * Matches keys under "queries" in dbdata.json.
     *
     * Example:
     *   String sql = TestDataLoader.getDbQuery("findUserByEmail");
     *   // Returns: "SELECT * FROM users WHERE email = ?"
     *
     * TODO-3: Add named queries for your DB test scenarios
     */
    public static String getDbQuery(String queryName) {
        JsonNode queries = getDbData().get("queries");
        if (queries == null) {
            throw new RuntimeException(
                    "No 'queries' section found in [" + DB_DATA_FILE + "]");
        }
        JsonNode query = queries.get(queryName);
        if (query == null) {
            throw new RuntimeException(
                    "Query [" + queryName + "] not found in dbdata.json queries section.");
        }
        log.debug("Loaded DB query [{}]: [{}]", queryName, query.asText());
        return query.asText();
    }


    // ── getDbField() ─────────────────────────────────────────
    /**
     * Returns any field from dbdata.json using dot-notation.
     *
     * Example:
     *   String tableName = TestDataLoader.getDbField("tables.users");
     */
    public static String getDbField(String dotPath) {
        return JsonReader.getField(DB_DATA_FILE, dotPath);
    }


    // ═══════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    // ── reloadAll() ──────────────────────────────────────────
    /**
     * Forces a reload of all cached test data files.
     * Call this if test data files are modified between runs
     * in the same JVM session (rare but possible in some setups).
     *
     * Example:
     *   TestDataLoader.reloadAll();
     */
    public static void reloadAll() {
        log.info("Reloading all test data files");
        usersRoot   = null;
        apiDataRoot = null;
        dbDataRoot  = null;
    }

}