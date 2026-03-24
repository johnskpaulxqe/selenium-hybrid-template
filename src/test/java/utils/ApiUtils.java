package utils;

/*
  ============================================================
  FILE: ApiUtils.java
  LOCATION: src/test/java/utils/ApiUtils.java

  PURPOSE:
    Core REST API utility built on RestAssured. Provides base
    configuration and all HTTP method helpers used across API
    step definitions. Responsibilities:
      1. Configure RestAssured base URI and default headers once
      2. Execute GET, POST, PUT, PATCH, DELETE requests
      3. Support authenticated and unauthenticated requests
      4. Log request/response details for debugging
      5. Provide response as RestAssured ValidatableResponse
         for chaining assertions in ResponseValidator

  HOW TO USE:
    // Setup (called once from Hooks.java @Before for @api scenarios)
    ApiUtils.setup();

    // Simple GET
    Response response = ApiUtils.get("/api/v1/users");

    // POST with body
    Response response = ApiUtils.post("/api/v1/users", requestBody);

    // Authenticated GET (with Bearer token)
    Response response = ApiUtils.getWithAuth("/api/v1/users", token);

    // Then validate with ResponseValidator
    ResponseValidator.assertStatusCode(response, 200);
    ResponseValidator.assertFieldEquals(response, "status", "success");

  TODO (customise per project):
    - TODO-1 : Add OAuth2 / API key auth helpers if your API
               uses a different authentication mechanism
    - TODO-2 : Add multipart/form-data helper if your API
               accepts file uploads
    - TODO-3 : Update default headers to match your API contract
  ============================================================
*/

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;

import java.util.Map;

public class ApiUtils {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(ApiUtils.class);

    // ── Base request specification ────────────────────────────
    // Built once during setup() and reused for all requests.
    // Thread-safe — RequestSpecification is immutable after build.
    private static RequestSpecification baseSpec;

    // ── Setup flag ────────────────────────────────────────────
    private static volatile boolean isSetup = false;

    // Private constructor — static utility class
    private ApiUtils() {}


    // ═══════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════

    // ── setup() ──────────────────────────────────────────────
    /**
     * Configures RestAssured with the base URI, content type,
     * default headers, and logging filters.
     * Called once from Hooks.java @Before for @api tagged scenarios.
     * Safe to call multiple times — only configures once.
     *
     * Example (in Hooks.java):
     *   ApiUtils.setup();
     */
    public static synchronized void setup() {
        if (isSetup) {
            log.debug("ApiUtils already configured — skipping setup");
            return;
        }

        // Read API base URL for the active environment
        String baseUrl = ConfigReader.get("apiBaseUrl");
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new RuntimeException(
                    "apiBaseUrl not configured in config.json for environment: ["
                            + ConfigReader.getActiveEnv() + "]");
        }

        // Set RestAssured global base URI
        RestAssured.baseURI = baseUrl;

        // Build the base request specification
        // TODO-3: Add/change default headers to match your API
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Accept", "application/json");

        // Read connection and read timeouts from config
        long connTimeout = ConfigReader.getLong("api.connectionTimeout", 10000L);
        long readTimeout = ConfigReader.getLong("api.readTimeout", 30000L);

        // Add logging filters for request and response
        // Logs full request/response at DEBUG level — visible with -Dlog.level=DEBUG
        builder.addFilter(new RequestLoggingFilter(LogDetail.ALL))
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL));

        baseSpec = builder.build();

        log.info("ApiUtils configured → baseURI: [{}]", baseUrl);
        isSetup = true;
    }


    // ── reset() ───────────────────────────────────────────────
    /**
     * Resets the setup state. Useful between test suites that
     * target different environments in the same JVM session.
     *
     * Example:
     *   ApiUtils.reset();
     *   ApiUtils.setup(); // re-configures for new environment
     */
    public static synchronized void reset() {
        isSetup   = false;
        baseSpec  = null;
        RestAssured.reset();
        log.debug("ApiUtils reset");
    }


    // ═══════════════════════════════════════════════════════════
    // GET
    // ═══════════════════════════════════════════════════════════

    // ── get() ─────────────────────────────────────────────────
    /**
     * Executes an unauthenticated GET request.
     *
     * Example:
     *   Response response = ApiUtils.get("/api/v1/users");
     *   ResponseValidator.assertStatusCode(response, 200);
     */
    public static Response get(String endpoint) {
        log.info("GET → [{}]", endpoint);
        return buildRequest()
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── get() with query params ───────────────────────────────
    /**
     * Executes a GET request with query parameters.
     *
     * Example:
     *   Response response = ApiUtils.get("/api/v1/users",
     *       Map.of("role", "ADMIN", "active", "true"));
     */
    public static Response get(String endpoint, Map<String, Object> queryParams) {
        log.info("GET → [{}] with params: {}", endpoint, queryParams);
        return buildRequest()
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── getWithAuth() ─────────────────────────────────────────
    /**
     * Executes an authenticated GET request with a Bearer token.
     *
     * Example:
     *   Response response = ApiUtils.getWithAuth("/api/v1/users", token);
     */
    public static Response getWithAuth(String endpoint, String bearerToken) {
        log.info("GET (auth) → [{}]", endpoint);
        return buildRequest()
                .header("Authorization", "Bearer " + bearerToken)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── getWithAuth() with query params ───────────────────────
    /**
     * Authenticated GET with query parameters.
     *
     * Example:
     *   Response response = ApiUtils.getWithAuth("/api/v1/users",
     *       Map.of("page", 1, "limit", 10), token);
     */
    public static Response getWithAuth(String endpoint,
                                       Map<String, Object> queryParams,
                                       String bearerToken) {
        log.info("GET (auth) → [{}] with params: {}", endpoint, queryParams);
        return buildRequest()
                .header("Authorization", "Bearer " + bearerToken)
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // POST
    // ═══════════════════════════════════════════════════════════

    // ── post() ────────────────────────────────────────────────
    /**
     * Executes an unauthenticated POST request with a JSON body.
     * Body can be a String (raw JSON), Map, or POJO.
     *
     * Example:
     *   Response response = ApiUtils.post("/api/v1/auth/login",
     *       Map.of("username", "admin", "password", "Admin@123"));
     */
    public static Response post(String endpoint, Object body) {
        log.info("POST → [{}]", endpoint);
        return buildRequest()
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── postWithAuth() ────────────────────────────────────────
    /**
     * Executes an authenticated POST request with a Bearer token.
     *
     * Example:
     *   Response response = ApiUtils.postWithAuth("/api/v1/users",
     *       requestBody, token);
     */
    public static Response postWithAuth(String endpoint, Object body, String bearerToken) {
        log.info("POST (auth) → [{}]", endpoint);
        return buildRequest()
                .header("Authorization", "Bearer " + bearerToken)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // PUT
    // ═══════════════════════════════════════════════════════════

    // ── put() ─────────────────────────────────────────────────
    /**
     * Executes a PUT request — full resource replacement.
     *
     * Example:
     *   Response response = ApiUtils.put("/api/v1/users/42", updatedUserBody);
     */
    public static Response put(String endpoint, Object body) {
        log.info("PUT → [{}]", endpoint);
        return buildRequest()
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── putWithAuth() ─────────────────────────────────────────
    /**
     * Authenticated PUT request.
     *
     * Example:
     *   Response response = ApiUtils.putWithAuth("/api/v1/users/42",
     *       updatedUserBody, token);
     */
    public static Response putWithAuth(String endpoint, Object body, String bearerToken) {
        log.info("PUT (auth) → [{}]", endpoint);
        return buildRequest()
                .header("Authorization", "Bearer " + bearerToken)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // PATCH
    // ═══════════════════════════════════════════════════════════

    // ── patch() ───────────────────────────────────────────────
    /**
     * Executes a PATCH request — partial resource update.
     *
     * Example:
     *   Response response = ApiUtils.patch("/api/v1/users/42",
     *       Map.of("firstName", "Updated"));
     */
    public static Response patch(String endpoint, Object body) {
        log.info("PATCH → [{}]", endpoint);
        return buildRequest()
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── patchWithAuth() ───────────────────────────────────────
    /**
     * Authenticated PATCH request.
     *
     * Example:
     *   Response response = ApiUtils.patchWithAuth("/api/v1/users/42",
     *       Map.of("active", false), token);
     */
    public static Response patchWithAuth(String endpoint, Object body, String bearerToken) {
        log.info("PATCH (auth) → [{}]", endpoint);
        return buildRequest()
                .header("Authorization", "Bearer " + bearerToken)
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════

    // ── delete() ──────────────────────────────────────────────
    /**
     * Executes an unauthenticated DELETE request.
     *
     * Example:
     *   Response response = ApiUtils.delete("/api/v1/users/42");
     */
    public static Response delete(String endpoint) {
        log.info("DELETE → [{}]", endpoint);
        return buildRequest()
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── deleteWithAuth() ──────────────────────────────────────
    /**
     * Authenticated DELETE request.
     *
     * Example:
     *   Response response = ApiUtils.deleteWithAuth("/api/v1/users/42", token);
     */
    public static Response deleteWithAuth(String endpoint, String bearerToken) {
        log.info("DELETE (auth) → [{}]", endpoint);
        return buildRequest()
                .header("Authorization", "Bearer " + bearerToken)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // CUSTOM REQUEST — full control
    // ═══════════════════════════════════════════════════════════

    // ── getRequestSpec() ─────────────────────────────────────
    /**
     * Returns the base RequestSpecification for building fully
     * custom requests. Use when the standard helpers above are
     * not sufficient.
     *
     * Example:
     *   Response response = ApiUtils.getRequestSpec()
     *       .header("X-Custom-Header", "value")
     *       .queryParam("filter", "active")
     *       .body(requestBody)
     *       .when()
     *       .post("/api/v1/users")
     *       .then()
     *       .extract()
     *       .response();
     */
    public static RequestSpecification getRequestSpec() {
        return buildRequest();
    }


    // ── getWithHeaders() ─────────────────────────────────────
    /**
     * Executes a GET request with custom headers map.
     * Use when you need to send non-standard headers.
     *
     * Example:
     *   Response response = ApiUtils.getWithHeaders("/api/v1/data",
     *       Map.of("X-Tenant-Id", "tenant-001", "X-Api-Key", "key123"));
     */
    public static Response getWithHeaders(String endpoint, Map<String, String> headers) {
        log.info("GET (custom headers) → [{}]", endpoint);
        return buildRequest()
                .headers(headers)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Builds a fresh RequestSpecification from the base spec.
     * Ensures setup() has been called before any request.
     */
    private static RequestSpecification buildRequest() {
        if (!isSetup || baseSpec == null) {
            log.warn("ApiUtils.setup() was not called — auto-configuring now");
            setup();
        }
        return RestAssured.given().spec(baseSpec);
    }

}