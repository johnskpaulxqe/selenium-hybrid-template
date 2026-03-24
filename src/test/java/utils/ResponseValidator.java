package utils;

/*
  ============================================================
  FILE: ResponseValidator.java
  LOCATION: src/test/java/utils/ResponseValidator.java

  PURPOSE:
    Centralises all API response validation logic. Wraps
    RestAssured's response extraction and assertion methods
    with clear logging and Extent Report integration.
    Responsibilities:
      1. Assert HTTP status codes
      2. Extract and assert JSON field values
      3. Assert response headers
      4. Assert response time SLAs
      5. Extract typed values from response for use in later steps
      6. Validate response body structure (not null, not empty)

  HOW TO USE:
    // Status code
    ResponseValidator.assertStatusCode(response, 200);

    // JSON field value
    ResponseValidator.assertFieldEquals(response, "status", "success");

    // Field is present and not null
    ResponseValidator.assertFieldNotNull(response, "data.id");

    // Extract value for use in next step
    String token  = ResponseValidator.extractString(response, "token");
    int    userId = ResponseValidator.extractInt(response, "data.id");

    // Response time SLA
    ResponseValidator.assertResponseTimeBelow(response, 2000);

    // Response body contains substring
    ResponseValidator.assertBodyContains(response, "\"active\":true");

  TODO (customise per project):
    - TODO-1 : Add assertMatchesJsonSchema() with your schema files
               if you need JSON schema contract validation
    - TODO-2 : Add assertHeaderContains() variants as needed
    - TODO-3 : Adjust default SLA threshold to match your API's
               performance requirements
  ============================================================
*/

import io.restassured.response.Response;
import org.slf4j.Logger;

import java.util.List;

public class ResponseValidator {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(ResponseValidator.class);

    // ── Default response time SLA (milliseconds) ──────────────
    // TODO-3: Adjust to match your API's performance requirements
    private static final long DEFAULT_SLA_MS = 5000L;

    // Private constructor — static utility class
    private ResponseValidator() {}


    // ═══════════════════════════════════════════════════════════
    // STATUS CODE ASSERTIONS
    // ═══════════════════════════════════════════════════════════

    // ── assertStatusCode() ───────────────────────────────────
    /**
     * Asserts that the response HTTP status code matches expected.
     * Logs the full endpoint and actual status on failure.
     *
     * Example:
     *   ResponseValidator.assertStatusCode(response, 200);
     *   ResponseValidator.assertStatusCode(response, 201); // Created
     *   ResponseValidator.assertStatusCode(response, 404); // Not Found
     */
    public static void assertStatusCode(Response response, int expectedStatusCode) {
        int actual = response.getStatusCode();
        log.info("Asserting status code → Expected: [{}] Actual: [{}]",
                expectedStatusCode, actual);

        if (actual != expectedStatusCode) {
            String body = response.getBody() != null
                    ? response.getBody().asString() : "(empty body)";
            log.error("Status code mismatch. Expected [{}] but got [{}]. Body: {}",
                    expectedStatusCode, actual, body);
        }

        CustomAssert.assertStatusCode(actual, expectedStatusCode);
    }


    // ── assertStatusCodeIn() ─────────────────────────────────
    /**
     * Asserts that the status code is one of several valid codes.
     * Use when multiple success codes are acceptable
     * (e.g. 200 or 204 for DELETE).
     *
     * Example:
     *   ResponseValidator.assertStatusCodeIn(response, 200, 204);
     *   ResponseValidator.assertStatusCodeIn(response, 201, 202);
     */
    public static void assertStatusCodeIn(Response response, int... validCodes) {
        int actual = response.getStatusCode();
        boolean valid = false;

        for (int code : validCodes) {
            if (actual == code) {
                valid = true;
                break;
            }
        }

        log.info("Asserting status code in {} → Actual: [{}]",
                java.util.Arrays.toString(validCodes), actual);

        CustomAssert.assertTrue(valid,
                "Expected status code in " + java.util.Arrays.toString(validCodes)
                        + " but got [" + actual + "]");
    }


    // ── assertSuccess() ───────────────────────────────────────
    /**
     * Asserts the response is a 2xx success status code.
     *
     * Example:
     *   ResponseValidator.assertSuccess(response);
     */
    public static void assertSuccess(Response response) {
        int actual = response.getStatusCode();
        log.info("Asserting 2xx success status → Actual: [{}]", actual);
        CustomAssert.assertTrue(actual >= 200 && actual < 300,
                "Expected 2xx success but got [" + actual + "]");
    }


    // ═══════════════════════════════════════════════════════════
    // JSON FIELD ASSERTIONS
    // ═══════════════════════════════════════════════════════════

    // ── assertFieldEquals() ───────────────────────────────────
    /**
     * Asserts that a JSON field in the response equals the
     * expected value. Supports dot-notation for nested fields.
     *
     * Example:
     *   ResponseValidator.assertFieldEquals(response, "status", "success");
     *   ResponseValidator.assertFieldEquals(response, "data.role", "ADMIN");
     *   ResponseValidator.assertFieldEquals(response, "data.active", true);
     */
    public static void assertFieldEquals(Response response,
                                         String jsonPath,
                                         Object expectedValue) {
        Object actual = response.jsonPath().get(jsonPath);
        log.info("Asserting JSON field [{}] → Expected: [{}] Actual: [{}]",
                jsonPath, expectedValue, actual);

        CustomAssert.assertEquals(actual, expectedValue,
                "JSON field [" + jsonPath + "]");
    }


    // ── assertFieldNotNull() ──────────────────────────────────
    /**
     * Asserts that a JSON field is present and not null.
     * Use to verify required response fields exist.
     *
     * Example:
     *   ResponseValidator.assertFieldNotNull(response, "token");
     *   ResponseValidator.assertFieldNotNull(response, "data.id");
     */
    public static void assertFieldNotNull(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        log.info("Asserting JSON field [{}] is not null → Value: [{}]", jsonPath, value);
        CustomAssert.assertNotNull(value,
                "JSON field [" + jsonPath + "] should not be null");
    }


    // ── assertFieldNull() ─────────────────────────────────────
    /**
     * Asserts that a JSON field is null or absent.
     *
     * Example:
     *   ResponseValidator.assertFieldNull(response, "error");
     */
    public static void assertFieldNull(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        log.info("Asserting JSON field [{}] is null → Value: [{}]", jsonPath, value);
        CustomAssert.assertNull(value,
                "JSON field [" + jsonPath + "] should be null");
    }


    // ── assertFieldContains() ─────────────────────────────────
    /**
     * Asserts that a JSON string field contains a substring.
     *
     * Example:
     *   ResponseValidator.assertFieldContains(response, "message", "created successfully");
     */
    public static void assertFieldContains(Response response,
                                           String jsonPath,
                                           String expectedSubstring) {
        String actual = response.jsonPath().getString(jsonPath);
        log.info("Asserting JSON field [{}] contains [{}] → Actual: [{}]",
                jsonPath, expectedSubstring, actual);
        CustomAssert.assertContains(actual, expectedSubstring,
                "JSON field [" + jsonPath + "] should contain [" + expectedSubstring + "]");
    }


    // ── assertArrayNotEmpty() ─────────────────────────────────
    /**
     * Asserts that a JSON array field is not empty.
     * Use to verify list endpoints return at least one item.
     *
     * Example:
     *   ResponseValidator.assertArrayNotEmpty(response, "data");
     *   ResponseValidator.assertArrayNotEmpty(response, "items");
     */
    public static void assertArrayNotEmpty(Response response, String jsonPath) {
        List<?> list = response.jsonPath().getList(jsonPath);
        log.info("Asserting JSON array [{}] is not empty → Size: [{}]",
                jsonPath, list != null ? list.size() : "null");
        CustomAssert.assertNotNull(list,
                "JSON array [" + jsonPath + "] should not be null");
        CustomAssert.assertTrue(!list.isEmpty(),
                "JSON array [" + jsonPath + "] should not be empty");
    }


    // ── assertArraySize() ────────────────────────────────────
    /**
     * Asserts that a JSON array has the expected number of items.
     *
     * Example:
     *   ResponseValidator.assertArraySize(response, "data", 5);
     */
    public static void assertArraySize(Response response,
                                       String jsonPath,
                                       int expectedSize) {
        List<?> list   = response.jsonPath().getList(jsonPath);
        int     actual = list != null ? list.size() : 0;
        log.info("Asserting JSON array [{}] size → Expected: [{}] Actual: [{}]",
                jsonPath, expectedSize, actual);
        CustomAssert.assertEquals(actual, expectedSize,
                "JSON array [" + jsonPath + "] size");
    }


    // ── assertArraySizeGreaterThan() ──────────────────────────
    /**
     * Asserts that a JSON array has more than the minimum items.
     *
     * Example:
     *   ResponseValidator.assertArraySizeGreaterThan(response, "data", 0);
     */
    public static void assertArraySizeGreaterThan(Response response,
                                                  String jsonPath,
                                                  int minimum) {
        List<?> list   = response.jsonPath().getList(jsonPath);
        int     actual = list != null ? list.size() : 0;
        log.info("Asserting JSON array [{}] size > {} → Actual: [{}]",
                jsonPath, minimum, actual);
        CustomAssert.assertGreaterThan(actual, minimum,
                "JSON array [" + jsonPath + "] size should be > " + minimum);
    }


    // ═══════════════════════════════════════════════════════════
    // BODY ASSERTIONS
    // ═══════════════════════════════════════════════════════════

    // ── assertBodyContains() ─────────────────────────────────
    /**
     * Asserts the raw response body string contains a substring.
     * Use for quick body checks without parsing JSON.
     *
     * Example:
     *   ResponseValidator.assertBodyContains(response, "\"active\":true");
     *   ResponseValidator.assertBodyContains(response, "success");
     */
    public static void assertBodyContains(Response response, String expectedSubstring) {
        String body = response.getBody().asString();
        log.info("Asserting response body contains: [{}]", expectedSubstring);
        CustomAssert.assertContains(body, expectedSubstring,
                "Response body should contain [" + expectedSubstring + "]");
    }


    // ── assertBodyNotEmpty() ─────────────────────────────────
    /**
     * Asserts the response body is not null and not empty.
     *
     * Example:
     *   ResponseValidator.assertBodyNotEmpty(response);
     */
    public static void assertBodyNotEmpty(Response response) {
        String body = response.getBody() != null
                ? response.getBody().asString() : null;
        log.info("Asserting response body is not empty → Length: [{}]",
                body != null ? body.length() : "null");
        CustomAssert.assertNotNull(body, "Response body should not be null");
        CustomAssert.assertTrue(!body.isBlank(), "Response body should not be empty");
    }


    // ═══════════════════════════════════════════════════════════
    // HEADER ASSERTIONS
    // ═══════════════════════════════════════════════════════════

    // ── assertHeaderEquals() ─────────────────────────────────
    /**
     * Asserts that a response header equals the expected value.
     *
     * Example:
     *   ResponseValidator.assertHeaderEquals(response, "Content-Type",
     *       "application/json; charset=utf-8");
     */
    public static void assertHeaderEquals(Response response,
                                          String headerName,
                                          String expectedValue) {
        String actual = response.getHeader(headerName);
        log.info("Asserting header [{}] → Expected: [{}] Actual: [{}]",
                headerName, expectedValue, actual);
        CustomAssert.assertEquals(actual, expectedValue,
                "Response header [" + headerName + "]");
    }


    // ── assertHeaderPresent() ────────────────────────────────
    /**
     * Asserts that a response header is present.
     *
     * Example:
     *   ResponseValidator.assertHeaderPresent(response, "Authorization");
     *   ResponseValidator.assertHeaderPresent(response, "X-Request-Id");
     */
    public static void assertHeaderPresent(Response response, String headerName) {
        String value = response.getHeader(headerName);
        log.info("Asserting header [{}] is present → Value: [{}]", headerName, value);
        CustomAssert.assertNotNull(value,
                "Response header [" + headerName + "] should be present");
    }


    // ── assertContentTypeJson() ───────────────────────────────
    /**
     * Asserts the response Content-Type is application/json.
     *
     * Example:
     *   ResponseValidator.assertContentTypeJson(response);
     */
    public static void assertContentTypeJson(Response response) {
        String contentType = response.getContentType();
        log.info("Asserting Content-Type is JSON → Actual: [{}]", contentType);
        CustomAssert.assertContains(contentType, "application/json",
                "Response Content-Type should be application/json");
    }


    // ═══════════════════════════════════════════════════════════
    // RESPONSE TIME ASSERTIONS
    // ═══════════════════════════════════════════════════════════

    // ── assertResponseTimeBelow() ─────────────────────────────
    /**
     * Asserts that the response time is below the given threshold.
     * Use to enforce API performance SLAs in tests.
     *
     * Example:
     *   ResponseValidator.assertResponseTimeBelow(response, 2000); // under 2s
     *   ResponseValidator.assertResponseTimeBelow(response, 500);  // under 500ms
     */
    public static void assertResponseTimeBelow(Response response, long thresholdMs) {
        long actual = response.getTime();
        log.info("Asserting response time < {}ms → Actual: {}ms", thresholdMs, actual);
        CustomAssert.assertTrue(actual < thresholdMs,
                "Response time [" + actual + "ms] should be below [" + thresholdMs + "ms]");
    }


    // ── assertResponseTimeWithinSla() ─────────────────────────
    /**
     * Asserts the response time is within the default SLA
     * defined at the top of this file (DEFAULT_SLA_MS).
     *
     * Example:
     *   ResponseValidator.assertResponseTimeWithinSla(response);
     */
    public static void assertResponseTimeWithinSla(Response response) {
        assertResponseTimeBelow(response, DEFAULT_SLA_MS);
    }


    // ═══════════════════════════════════════════════════════════
    // VALUE EXTRACTION — typed getters for use in later steps
    // ═══════════════════════════════════════════════════════════

    // ── extractString() ───────────────────────────────────────
    /**
     * Extracts a JSON field value as a String.
     * Use to capture values (tokens, IDs) for use in later steps.
     *
     * Example:
     *   String token  = ResponseValidator.extractString(response, "token");
     *   String userId = ResponseValidator.extractString(response, "data.id");
     */
    public static String extractString(Response response, String jsonPath) {
        String value = response.jsonPath().getString(jsonPath);
        log.debug("Extracted [{}] = [{}]", jsonPath, value);
        return value;
    }


    // ── extractInt() ──────────────────────────────────────────
    /**
     * Extracts a JSON field value as an int.
     *
     * Example:
     *   int userId = ResponseValidator.extractInt(response, "data.id");
     *   int count  = ResponseValidator.extractInt(response, "meta.total");
     */
    public static int extractInt(Response response, String jsonPath) {
        Integer value = response.jsonPath().getInt(jsonPath);
        log.debug("Extracted [{}] = [{}]", jsonPath, value);
        return value != null ? value : 0;
    }


    // ── extractBoolean() ─────────────────────────────────────
    /**
     * Extracts a JSON field value as a boolean.
     *
     * Example:
     *   boolean active = ResponseValidator.extractBoolean(response, "data.active");
     */
    public static boolean extractBoolean(Response response, String jsonPath) {
        Boolean value = response.jsonPath().getBoolean(jsonPath);
        log.debug("Extracted [{}] = [{}]", jsonPath, value);
        return value != null && value;
    }


    // ── extractList() ────────────────────────────────────────
    /**
     * Extracts a JSON array as a typed List.
     *
     * Example:
     *   List<String> roles = ResponseValidator.extractList(response, "data.roles");
     *   List<Map>    items = ResponseValidator.extractList(response, "data");
     */
    public static <T> List<T> extractList(Response response, String jsonPath) {
        List<T> list = response.jsonPath().getList(jsonPath);
        log.debug("Extracted list [{}] → size: [{}]", jsonPath,
                list != null ? list.size() : "null");
        return list;
    }


    // ── extractResponseBody() ────────────────────────────────
    /**
     * Returns the full response body as a raw String.
     * Useful for logging or passing to JsonReader for parsing.
     *
     * Example:
     *   String body = ResponseValidator.extractResponseBody(response);
     *   JsonNode node = JsonReader.parseString(body);
     */
    public static String extractResponseBody(Response response) {
        return response.getBody().asString();
    }


    // ── getStatusCode() ──────────────────────────────────────
    /**
     * Returns the HTTP status code without asserting.
     * Use when you need the code for conditional logic in steps.
     *
     * Example:
     *   int code = ResponseValidator.getStatusCode(response);
     *   if (code == 404) { ... }
     */
    public static int getStatusCode(Response response) {
        return response.getStatusCode();
    }


    // ── getResponseTime() ────────────────────────────────────
    /**
     * Returns the response time in milliseconds without asserting.
     *
     * Example:
     *   long ms = ResponseValidator.getResponseTime(response);
     *   log.info("Login response time: {}ms", ms);
     */
    public static long getResponseTime(Response response) {
        return response.getTime();
    }

}