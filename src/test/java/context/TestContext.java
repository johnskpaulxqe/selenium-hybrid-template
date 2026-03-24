package context;

/*
  ============================================================
  FILE: TestContext.java
  LOCATION: src/test/java/context/TestContext.java

  PURPOSE:
    Scenario-scoped shared state container injected into all
    step definition classes via PicoContainer dependency
    injection. Solves the problem of sharing data between
    step definition classes in the same scenario.

    Responsibilities:
      1. Hold page object instances (one per scenario)
      2. Hold scenario-level test data (tokens, IDs, responses)
      3. Hold the Cucumber Scenario reference for hooks
      4. Provide a clean, typed API for getting/setting state
      5. Reset automatically between scenarios (PicoContainer
         creates a new instance per scenario)

  WHY DO WE NEED THIS?
    Cucumber creates a new instance of each step definition
    class per scenario. If LoginSteps and DashboardSteps both
    need the LoginPage or a stored token, they cannot share
    state directly. TestContext is injected into both classes
    by PicoContainer, giving them a shared object that lives
    for exactly one scenario then is discarded.

  HOW TO USE:
    // Declare in step definition constructor
    public class LoginSteps {
        private final TestContext ctx;
        private final LoginPage   loginPage;

        public LoginSteps(TestContext ctx) {
            this.ctx       = ctx;
            this.loginPage = ctx.getLoginPage();
        }
    }

    // Store a value during a step
    ctx.setAuthToken(response.jsonPath().getString("token"));

    // Read it in a later step (same scenario)
    String token = ctx.getAuthToken();

  TODO (customise per project):
    - TODO-1 : Add page object getters for every page in your app
    - TODO-2 : Add typed fields for scenario data your steps share
               e.g. orderId, customerId, createdRecordId
    - TODO-3 : No need to manually reset — PicoContainer creates
               a fresh TestContext for every scenario automatically
  ============================================================
*/

import io.cucumber.java.Scenario;
import io.restassured.response.Response;
import pages.LoginPage;
import pages.SamplePage;

import java.util.HashMap;
import java.util.Map;

public class TestContext {

    // ═══════════════════════════════════════════════════════════
    // CUCUMBER SCENARIO REFERENCE
    // Set in Hooks.java @Before — used for logging and screenshots
    // ═══════════════════════════════════════════════════════════

    private Scenario scenario;

    /**
     * Stores the Cucumber Scenario reference.
     * Called from Hooks.java @Before hook.
     *
     * Example (in Hooks.java):
     *   context.setScenario(scenario);
     */
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * Returns the active Cucumber Scenario.
     * Used in Hooks.java @After for screenshot attachment
     * and pass/fail logging.
     */
    public Scenario getScenario() {
        return scenario;
    }

    /**
     * Returns the scenario name.
     * Convenience method — avoids null checks in hooks.
     */
    public String getScenarioName() {
        return scenario != null ? scenario.getName() : "Unknown Scenario";
    }

    /**
     * Returns true if the current scenario has failed.
     * Used in Hooks.java @After to decide whether to screenshot.
     */
    public boolean isScenarioFailed() {
        return scenario != null && scenario.isFailed();
    }


    // ═══════════════════════════════════════════════════════════
    // PAGE OBJECTS
    // Lazy-initialised — created on first access, reused within
    // the same scenario. PicoContainer discards them after.
    // TODO-1: Add a getter for every page object in your project
    // ═══════════════════════════════════════════════════════════

    private LoginPage  loginPage;
    private SamplePage samplePage;

    /**
     * Returns the LoginPage instance for this scenario.
     * Created once per scenario on first call.
     *
     * Example:
     *   LoginPage login = ctx.getLoginPage();
     */
    public LoginPage getLoginPage() {
        if (loginPage == null) {
            loginPage = new LoginPage();
        }
        return loginPage;
    }

    /**
     * Returns the SamplePage instance for this scenario.
     * TODO-1: Add similar getters for all your page objects
     *
     * Example:
     *   SamplePage sample = ctx.getSamplePage();
     */
    public SamplePage getSamplePage() {
        if (samplePage == null) {
            samplePage = new SamplePage();
        }
        return samplePage;
    }


    // ═══════════════════════════════════════════════════════════
    // AUTHENTICATION STATE
    // Stores auth tokens between login and subsequent API steps
    // ═══════════════════════════════════════════════════════════

    private String authToken;
    private String refreshToken;
    private String loggedInUsername;

    /**
     * Stores the Bearer auth token obtained during login.
     * Used by API step definitions for subsequent requests.
     *
     * Example (in LoginSteps after successful login):
     *   ctx.setAuthToken(response.jsonPath().getString("token"));
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Returns the stored auth token.
     * Used in API step definitions after login.
     *
     * Example:
     *   String token = ctx.getAuthToken();
     *   Response r = ApiUtils.getWithAuth("/api/v1/users", token);
     */
    public String getAuthToken() {
        return authToken;
    }

    /** Stores the refresh token if your API uses one. */
    public void setRefreshToken(String token) {
        this.refreshToken = token;
    }

    /** Returns the stored refresh token. */
    public String getRefreshToken() {
        return refreshToken;
    }

    /** Stores the username of the currently logged-in user. */
    public void setLoggedInUsername(String username) {
        this.loggedInUsername = username;
    }

    /** Returns the username of the currently logged-in user. */
    public String getLoggedInUsername() {
        return loggedInUsername;
    }


    // ═══════════════════════════════════════════════════════════
    // API RESPONSE STORAGE
    // Stores the last API response for use in Then steps
    // ═══════════════════════════════════════════════════════════

    private Response lastApiResponse;

    /**
     * Stores the most recent API response.
     * Set in When steps, read in Then steps.
     *
     * Example (in ApiSteps When step):
     *   ctx.setLastApiResponse(ApiUtils.get("/api/v1/users"));
     *
     * Example (in ApiSteps Then step):
     *   ResponseValidator.assertStatusCode(ctx.getLastApiResponse(), 200);
     */
    public void setLastApiResponse(Response response) {
        this.lastApiResponse = response;
    }

    /**
     * Returns the last stored API response.
     */
    public Response getLastApiResponse() {
        return lastApiResponse;
    }


    // ═══════════════════════════════════════════════════════════
    // ENTITY ID STORAGE
    // Stores IDs created during a scenario for use in later steps
    // TODO-2: Add typed fields for your entity IDs
    // ═══════════════════════════════════════════════════════════

    private String createdUserId;
    private String createdOrderId;
    private String createdRecordId;

    /**
     * Stores the ID of a user created during the scenario.
     * Used to reference the user in subsequent steps
     * (e.g. GET /users/{id}, DELETE /users/{id}).
     *
     * TODO-2: Add similar fields for your entities
     *
     * Example:
     *   ctx.setCreatedUserId(response.jsonPath().getString("id"));
     */
    public void setCreatedUserId(String userId)    { this.createdUserId    = userId; }
    public String getCreatedUserId()               { return createdUserId; }

    public void setCreatedOrderId(String orderId)  { this.createdOrderId   = orderId; }
    public String getCreatedOrderId()              { return createdOrderId; }

    /** Generic created record ID — use when entity type varies. */
    public void setCreatedRecordId(String id)      { this.createdRecordId  = id; }
    public String getCreatedRecordId()             { return createdRecordId; }


    // ═══════════════════════════════════════════════════════════
    // GENERIC KEY-VALUE STORE
    // Flexible scratch-pad for any scenario-level data that
    // does not warrant its own typed field
    // ═══════════════════════════════════════════════════════════

    private final Map<String, Object> scenarioData = new HashMap<>();

    /**
     * Stores any value by string key.
     * Use for one-off scenario data that doesn't need a typed field.
     *
     * Example:
     *   ctx.store("productSku",   "SKU-001");
     *   ctx.store("invoiceTotal", 149.99);
     *   ctx.store("dbRowCount",   5);
     */
    public void store(String key, Object value) {
        scenarioData.put(key, value);
    }

    /**
     * Retrieves a stored value by key.
     * Cast to the expected type on retrieval.
     *
     * Example:
     *   String sku   = (String)  ctx.retrieve("productSku");
     *   int    count = (Integer) ctx.retrieve("dbRowCount");
     */
    public Object retrieve(String key) {
        return scenarioData.get(key);
    }

    /**
     * Retrieves a stored value cast to String.
     * Returns null if the key is not set.
     *
     * Example:
     *   String sku = ctx.retrieveString("productSku");
     */
    public String retrieveString(String key) {
        Object value = scenarioData.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Returns true if the key exists in the scenario data store.
     *
     * Example:
     *   if (ctx.has("authToken")) { ... }
     */
    public boolean has(String key) {
        return scenarioData.containsKey(key);
    }


    // ═══════════════════════════════════════════════════════════
    // DB TEST STATE
    // Tracks database operations for cleanup in @After hooks
    // ═══════════════════════════════════════════════════════════

    private boolean dbConnectionOpened = false;
    private boolean dbTransactionStarted = false;

    /** Marks that a DB connection was opened this scenario. */
    public void markDbConnectionOpened()       { this.dbConnectionOpened    = true; }
    public boolean wasDbConnectionOpened()     { return dbConnectionOpened; }

    /** Marks that a DB transaction was started this scenario. */
    public void markDbTransactionStarted()     { this.dbTransactionStarted  = true; }
    public boolean wasDbTransactionStarted()   { return dbTransactionStarted; }

}