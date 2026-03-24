package utils;

/*
  ============================================================
  FILE: RequestBuilder.java
  LOCATION: src/test/java/utils/RequestBuilder.java

  PURPOSE:
    Fluent builder for constructing RestAssured RequestSpecification
    objects. Provides a clean, readable way to build complex API
    requests with headers, auth, body, query params, and path
    params — all in one chain.

    Use RequestBuilder when:
      - You need fine-grained control over headers or params
      - You are building reusable request templates
      - The standard ApiUtils helpers are not flexible enough

    Use ApiUtils when:
      - You need a quick one-liner HTTP call
      - Request is simple (no custom headers, basic auth)

  HOW TO USE:
    // Simple authenticated GET
    Response response = RequestBuilder.create()
        .withAuth(token)
        .get("/api/v1/users");

    // POST with body and custom headers
    Response response = RequestBuilder.create()
        .withAuth(token)
        .withHeader("X-Tenant-Id", "tenant-001")
        .withBody(requestBodyMap)
        .post("/api/v1/users");

    // GET with path and query params
    Response response = RequestBuilder.create()
        .withAuth(token)
        .withPathParam("id", userId)
        .withQueryParam("include", "roles")
        .get("/api/v1/users/{id}");

    // POST with form data
    Response response = RequestBuilder.create()
        .withFormParam("grant_type", "password")
        .withFormParam("username", "admin")
        .withFormParam("password", "Admin@123")
        .post("/oauth/token");

  TODO (customise per project):
    - TODO-1 : Add withApiKey() if your API uses API key auth
    - TODO-2 : Add withOAuth2() if your API uses OAuth2 client
               credentials flow
    - TODO-3 : Add withProxy() if tests run behind a proxy
    - TODO-4 : Add withSslRelaxed() for environments with
               self-signed certificates
  ============================================================
*/

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RequestBuilder {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(RequestBuilder.class);

    // ── Builder state ─────────────────────────────────────────
    private final Map<String, String>  headers      = new HashMap<>();
    private final Map<String, Object>  queryParams  = new HashMap<>();
    private final Map<String, Object>  pathParams   = new HashMap<>();
    private final Map<String, Object>  formParams   = new HashMap<>();
    private       Object               body         = null;
    private       ContentType          contentType  = ContentType.JSON;
    private       String               bearerToken  = null;
    private       String               basicUser    = null;
    private       String               basicPass    = null;
    private       boolean              logAll       = false;

    // Private — use create() factory method
    private RequestBuilder() {}


    // ═══════════════════════════════════════════════════════════
    // FACTORY
    // ═══════════════════════════════════════════════════════════

    // ── create() ──────────────────────────────────────────────
    /**
     * Creates a new RequestBuilder instance.
     * Always start here — do not use new RequestBuilder() directly.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withAuth(token)
     *       .withBody(body)
     *       .post("/api/v1/users");
     */
    public static RequestBuilder create() {
        return new RequestBuilder();
    }


    // ═══════════════════════════════════════════════════════════
    // AUTHENTICATION
    // ═══════════════════════════════════════════════════════════

    // ── withAuth() ────────────────────────────────────────────
    /**
     * Adds a Bearer token Authorization header.
     * The "Bearer " prefix is added automatically.
     *
     * Example:
     *   RequestBuilder.create().withAuth(token).get("/api/v1/users");
     */
    public RequestBuilder withAuth(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }


    // ── withBasicAuth() ───────────────────────────────────────
    /**
     * Adds HTTP Basic Authentication credentials.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withBasicAuth("admin", "Admin@123")
     *       .get("/api/v1/users");
     */
    public RequestBuilder withBasicAuth(String username, String password) {
        this.basicUser = username;
        this.basicPass = password;
        return this;
    }


    // ── withApiKey() ──────────────────────────────────────────
    /**
     * Adds an API key as a custom header.
     * TODO-1: Change the header name to match your API's key header.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withApiKey("your-api-key-here")
     *       .get("/api/v1/data");
     */
    public RequestBuilder withApiKey(String apiKey) {
        // TODO-1: Replace "X-Api-Key" with your actual API key header name
        this.headers.put("X-Api-Key", apiKey);
        return this;
    }


    // ═══════════════════════════════════════════════════════════
    // HEADERS
    // ═══════════════════════════════════════════════════════════

    // ── withHeader() ─────────────────────────────────────────
    /**
     * Adds a single custom request header.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withHeader("X-Tenant-Id", "tenant-001")
     *       .withHeader("X-Correlation-Id", UUID.randomUUID().toString())
     *       .get("/api/v1/users");
     */
    public RequestBuilder withHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }


    // ── withHeaders() ─────────────────────────────────────────
    /**
     * Adds multiple headers from a Map.
     *
     * Example:
     *   Map<String, String> headers = Map.of(
     *       "X-Tenant-Id",      "tenant-001",
     *       "X-Request-Source", "automation");
     *   RequestBuilder.create().withHeaders(headers).get("/api/v1/users");
     */
    public RequestBuilder withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }


    // ── withContentType() ─────────────────────────────────────
    /**
     * Sets the Content-Type of the request.
     * Default is ContentType.JSON — override for XML or form data.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withContentType(ContentType.XML)
     *       .withBody(xmlBody)
     *       .post("/api/v1/data");
     */
    public RequestBuilder withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }


    // ═══════════════════════════════════════════════════════════
    // BODY
    // ═══════════════════════════════════════════════════════════

    // ── withBody() ────────────────────────────────────────────
    /**
     * Sets the request body.
     * Accepts String (raw JSON), Map, or any serialisable POJO.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withBody(Map.of("username", "admin", "password", "Admin@123"))
     *       .post("/api/v1/auth/login");
     *
     *   RequestBuilder.create()
     *       .withBody("{\"username\":\"admin\"}")  // raw JSON string
     *       .post("/api/v1/auth/login");
     */
    public RequestBuilder withBody(Object body) {
        this.body = body;
        return this;
    }


    // ── withJsonBody() ────────────────────────────────────────
    /**
     * Sets a raw JSON string as the request body.
     * Alias for withBody() — makes intent explicit in test code.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withJsonBody("{\"key\":\"value\"}")
     *       .post("/api/v1/data");
     */
    public RequestBuilder withJsonBody(String jsonString) {
        this.body        = jsonString;
        this.contentType = ContentType.JSON;
        return this;
    }


    // ═══════════════════════════════════════════════════════════
    // PARAMETERS
    // ═══════════════════════════════════════════════════════════

    // ── withQueryParam() ─────────────────────────────────────
    /**
     * Adds a single query parameter.
     * Appended to the URL as ?key=value.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withQueryParam("page", 1)
     *       .withQueryParam("limit", 10)
     *       .withQueryParam("role", "ADMIN")
     *       .get("/api/v1/users");
     *   // URL: /api/v1/users?page=1&limit=10&role=ADMIN
     */
    public RequestBuilder withQueryParam(String name, Object value) {
        this.queryParams.put(name, value);
        return this;
    }


    // ── withQueryParams() ────────────────────────────────────
    /**
     * Adds multiple query parameters from a Map.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withQueryParams(Map.of("page", 1, "limit", 20))
     *       .get("/api/v1/products");
     */
    public RequestBuilder withQueryParams(Map<String, Object> params) {
        this.queryParams.putAll(params);
        return this;
    }


    // ── withPathParam() ──────────────────────────────────────
    /**
     * Adds a path parameter for URL template placeholders.
     * Use {paramName} in your endpoint path.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withAuth(token)
     *       .withPathParam("id", 42)
     *       .get("/api/v1/users/{id}");
     *   // URL: /api/v1/users/42
     */
    public RequestBuilder withPathParam(String name, Object value) {
        this.pathParams.put(name, value);
        return this;
    }


    // ── withPathParams() ─────────────────────────────────────
    /**
     * Adds multiple path parameters from a Map.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withPathParams(Map.of("userId", 42, "orderId", 99))
     *       .get("/api/v1/users/{userId}/orders/{orderId}");
     */
    public RequestBuilder withPathParams(Map<String, Object> params) {
        this.pathParams.putAll(params);
        return this;
    }


    // ── withFormParam() ──────────────────────────────────────
    /**
     * Adds a form parameter (application/x-www-form-urlencoded).
     * Automatically switches Content-Type to URLENC.
     * Use for OAuth token endpoints or legacy form-based APIs.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withFormParam("grant_type", "password")
     *       .withFormParam("username",   "admin@yourapp.com")
     *       .withFormParam("password",   "Admin@123")
     *       .post("/oauth/token");
     */
    public RequestBuilder withFormParam(String name, Object value) {
        this.formParams.put(name, value);
        this.contentType = ContentType.URLENC; // Auto-switch for form data
        return this;
    }


    // ═══════════════════════════════════════════════════════════
    // DEBUGGING
    // ═══════════════════════════════════════════════════════════

    // ── withLogging() ─────────────────────────────────────────
    /**
     * Enables full request and response logging for this request.
     * Useful for debugging a specific call without enabling
     * global logging.
     *
     * Example:
     *   RequestBuilder.create()
     *       .withAuth(token)
     *       .withLogging()
     *       .get("/api/v1/users");
     */
    public RequestBuilder withLogging() {
        this.logAll = true;
        return this;
    }


    // ═══════════════════════════════════════════════════════════
    // HTTP METHODS — execute the built request
    // ═══════════════════════════════════════════════════════════

    // ── get() ─────────────────────────────────────────────────
    /**
     * Executes the built request as a GET.
     *
     * Example:
     *   Response response = RequestBuilder.create()
     *       .withAuth(token)
     *       .withQueryParam("active", true)
     *       .get("/api/v1/users");
     */
    public Response get(String endpoint) {
        log.info("RequestBuilder GET → [{}]", endpoint);
        return buildSpec()
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── post() ────────────────────────────────────────────────
    /**
     * Executes the built request as a POST.
     *
     * Example:
     *   Response response = RequestBuilder.create()
     *       .withAuth(token)
     *       .withBody(createUserBody)
     *       .post("/api/v1/users");
     */
    public Response post(String endpoint) {
        log.info("RequestBuilder POST → [{}]", endpoint);
        return buildSpec()
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── put() ─────────────────────────────────────────────────
    /**
     * Executes the built request as a PUT.
     *
     * Example:
     *   Response response = RequestBuilder.create()
     *       .withAuth(token)
     *       .withPathParam("id", userId)
     *       .withBody(updateBody)
     *       .put("/api/v1/users/{id}");
     */
    public Response put(String endpoint) {
        log.info("RequestBuilder PUT → [{}]", endpoint);
        return buildSpec()
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── patch() ───────────────────────────────────────────────
    /**
     * Executes the built request as a PATCH.
     *
     * Example:
     *   Response response = RequestBuilder.create()
     *       .withAuth(token)
     *       .withPathParam("id", userId)
     *       .withBody(Map.of("active", false))
     *       .patch("/api/v1/users/{id}");
     */
    public Response patch(String endpoint) {
        log.info("RequestBuilder PATCH → [{}]", endpoint);
        return buildSpec()
                .when()
                .patch(endpoint)
                .then()
                .extract()
                .response();
    }


    // ── delete() ─────────────────────────────────────────────
    /**
     * Executes the built request as a DELETE.
     *
     * Example:
     *   Response response = RequestBuilder.create()
     *       .withAuth(token)
     *       .withPathParam("id", userId)
     *       .delete("/api/v1/users/{id}");
     */
    public Response delete(String endpoint) {
        log.info("RequestBuilder DELETE → [{}]", endpoint);
        return buildSpec()
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Assembles the final RequestSpecification from all
     * the configured builder properties.
     */
    private RequestSpecification buildSpec() {

        // Start from RestAssured base — picks up global baseURI
        RequestSpecification spec = RestAssured.given()
                .contentType(contentType)
                .accept(ContentType.JSON);

        // Apply Bearer token auth
        if (bearerToken != null && !bearerToken.isBlank()) {
            spec.header("Authorization", "Bearer " + bearerToken);
        }

        // Apply Basic auth
        if (basicUser != null) {
            spec.auth().preemptive().basic(basicUser, basicPass);
        }

        // Apply custom headers
        if (!headers.isEmpty()) {
            spec.headers(headers);
        }

        // Apply query parameters
        if (!queryParams.isEmpty()) {
            spec.queryParams(queryParams);
        }

        // Apply path parameters
        if (!pathParams.isEmpty()) {
            spec.pathParams(pathParams);
        }

        // Apply form parameters
        if (!formParams.isEmpty()) {
            spec.formParams(formParams);
        }

        // Apply request body (skip for form params — they ARE the body)
        if (body != null && formParams.isEmpty()) {
            spec.body(body);
        }

        // Enable full logging if requested
        if (logAll) {
            spec.log().all();
        }

        return spec;
    }

}