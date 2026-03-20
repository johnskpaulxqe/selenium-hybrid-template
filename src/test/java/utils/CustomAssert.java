package utils;

/*
  ============================================================
  FILE: CustomAssert.java
  LOCATION: src/test/java/utils/CustomAssert.java

  PURPOSE:
    Wraps TestNG assertions to add automatic logging and
    Extent Report integration on every assertion. Responsibilities:
      1. Log every assertion attempt (pass and fail) to SLF4J
      2. Log assertion results to the active Extent Report test node
      3. Capture a screenshot automatically on assertion failure
      4. Provide soft assertion support — collect multiple failures
         before failing the scenario (via SoftAssert)
      5. Provide all standard assertion types:
           assertEquals, assertNotEquals, assertTrue, assertFalse,
           assertNull, assertNotNull, assertContains, assertMatches

  HARD vs SOFT ASSERTIONS:
    Hard (default) — fails the test immediately on first failure
      CustomAssert.assertEquals(actual, expected, "Price check");

    Soft — collects ALL failures, reports them all at the end
      CustomAssert.softAssertEquals(actual, expected, "Price check");
      CustomAssert.softAssertTrue(condition, "Checkbox checked");
      CustomAssert.assertAll("End of checkout verification");

  HOW TO USE:
    // Hard assertion — stops on first failure
    CustomAssert.assertEquals(actual, "Welcome", "Page heading check");

    // Soft assertion — continues after failure
    CustomAssert.softAssertEquals(statusCode, 200, "API status code");
    CustomAssert.softAssertTrue(response.contains("id"), "Response has id field");
    CustomAssert.assertAll("End of API response checks");

    // With screenshot on failure (UI tests)
    CustomAssert.assertEquals(driver, actual, "Welcome", "Heading check");

  TODO (customise per project):
    - TODO-1 : No changes needed for most projects
    - TODO-2 : Add assertMatchesJsonSchema() if you need JSON schema
               assertion with automatic report logging
    - TODO-3 : Wire ExtentReportManager.getTest() once that class
               is built in Step 29 — placeholder calls are already here
  ============================================================
*/

import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import org.openqa.selenium.WebDriver;

public class CustomAssert {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(CustomAssert.class);

    // ── ThreadLocal SoftAssert ────────────────────────────────
    // Each thread (parallel scenario) gets its own SoftAssert
    // instance so soft failures don't bleed across scenarios.
    private static final ThreadLocal<SoftAssert> softAssert =
            ThreadLocal.withInitial(SoftAssert::new);

    // Private constructor — static utility class
    private CustomAssert() {}


    // ═══════════════════════════════════════════════════════════
    // HARD ASSERTIONS — fail immediately on first failure
    // ═══════════════════════════════════════════════════════════

    // ── assertEquals() ───────────────────────────────────────
    /**
     * Asserts that actual equals expected.
     * Logs PASS/FAIL and attaches to Extent Report.
     *
     * Example:
     *   CustomAssert.assertEquals(pageTitle, "Dashboard", "Page title check");
     */
    public static void assertEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertEquals(actual, expected, message);
            logPass(message, "assertEquals",
                    "Expected: [" + expected + "] Actual: [" + actual + "]");
        } catch (AssertionError e) {
            logFail(message, "assertEquals",
                    "Expected: [" + expected + "] but was: [" + actual + "]");
            throw e;
        }
    }

    /**
     * assertEquals with automatic screenshot on failure.
     * Use this variant in UI step definitions.
     *
     * Example:
     *   CustomAssert.assertEquals(driver, heading.getText(), "Welcome", "Heading check");
     */
    public static void assertEquals(WebDriver driver, Object actual,
                                    Object expected, String message) {
        try {
            Assert.assertEquals(actual, expected, message);
            logPass(message, "assertEquals",
                    "Expected: [" + expected + "] Actual: [" + actual + "]");
        } catch (AssertionError e) {
            logFail(message, "assertEquals",
                    "Expected: [" + expected + "] but was: [" + actual + "]");
            captureFailureScreenshot(driver, message);
            throw e;
        }
    }


    // ── assertNotEquals() ────────────────────────────────────
    /**
     * Asserts that actual does NOT equal unexpected.
     *
     * Example:
     *   CustomAssert.assertNotEquals(errorMsg, "", "Error message should not be empty");
     */
    public static void assertNotEquals(Object actual, Object unexpected, String message) {
        try {
            Assert.assertNotEquals(actual, unexpected, message);
            logPass(message, "assertNotEquals",
                    "Value [" + actual + "] correctly differs from [" + unexpected + "]");
        } catch (AssertionError e) {
            logFail(message, "assertNotEquals",
                    "Expected values to differ but both were: [" + actual + "]");
            throw e;
        }
    }


    // ── assertTrue() ─────────────────────────────────────────
    /**
     * Asserts that condition is true.
     *
     * Example:
     *   CustomAssert.assertTrue(element.isDisplayed(), "Submit button is visible");
     */
    public static void assertTrue(boolean condition, String message) {
        try {
            Assert.assertTrue(condition, message);
            logPass(message, "assertTrue", "Condition is true");
        } catch (AssertionError e) {
            logFail(message, "assertTrue", "Condition was false");
            throw e;
        }
    }

    public static void assertTrue(WebDriver driver, boolean condition, String message) {
        try {
            Assert.assertTrue(condition, message);
            logPass(message, "assertTrue", "Condition is true");
        } catch (AssertionError e) {
            logFail(message, "assertTrue", "Condition was false");
            captureFailureScreenshot(driver, message);
            throw e;
        }
    }


    // ── assertFalse() ────────────────────────────────────────
    /**
     * Asserts that condition is false.
     *
     * Example:
     *   CustomAssert.assertFalse(errorPanel.isDisplayed(), "Error panel not shown");
     */
    public static void assertFalse(boolean condition, String message) {
        try {
            Assert.assertFalse(condition, message);
            logPass(message, "assertFalse", "Condition is false as expected");
        } catch (AssertionError e) {
            logFail(message, "assertFalse", "Condition was true but expected false");
            throw e;
        }
    }

    public static void assertFalse(WebDriver driver, boolean condition, String message) {
        try {
            Assert.assertFalse(condition, message);
            logPass(message, "assertFalse", "Condition is false as expected");
        } catch (AssertionError e) {
            logFail(message, "assertFalse", "Condition was true but expected false");
            captureFailureScreenshot(driver, message);
            throw e;
        }
    }


    // ── assertNull() ─────────────────────────────────────────
    /**
     * Asserts that object is null.
     *
     * Example:
     *   CustomAssert.assertNull(errorMessage, "No error message on success");
     */
    public static void assertNull(Object object, String message) {
        try {
            Assert.assertNull(object, message);
            logPass(message, "assertNull", "Object is null as expected");
        } catch (AssertionError e) {
            logFail(message, "assertNull",
                    "Expected null but was: [" + object + "]");
            throw e;
        }
    }


    // ── assertNotNull() ──────────────────────────────────────
    /**
     * Asserts that object is not null.
     *
     * Example:
     *   CustomAssert.assertNotNull(responseBody, "Response body is not null");
     */
    public static void assertNotNull(Object object, String message) {
        try {
            Assert.assertNotNull(object, message);
            logPass(message, "assertNotNull", "Object is not null as expected");
        } catch (AssertionError e) {
            logFail(message, "assertNotNull", "Expected non-null value but was null");
            throw e;
        }
    }


    // ── assertContains() ─────────────────────────────────────
    /**
     * Asserts that the actual string contains the expected substring.
     * Case-sensitive by default.
     *
     * Example:
     *   CustomAssert.assertContains(responseBody, "\"status\":\"success\"",
     *                               "Response contains success status");
     */
    public static void assertContains(String actual, String expected, String message) {
        try {
            Assert.assertTrue(actual != null && actual.contains(expected),
                    message + " | Expected [" + actual + "] to contain [" + expected + "]");
            logPass(message, "assertContains",
                    "[" + actual + "] contains [" + expected + "]");
        } catch (AssertionError e) {
            logFail(message, "assertContains",
                    "[" + actual + "] does not contain [" + expected + "]");
            throw e;
        }
    }


    // ── assertNotContains() ──────────────────────────────────
    /**
     * Asserts that the actual string does NOT contain the substring.
     *
     * Example:
     *   CustomAssert.assertNotContains(pageSource, "error", "No error on page");
     */
    public static void assertNotContains(String actual, String unexpected, String message) {
        try {
            Assert.assertFalse(actual != null && actual.contains(unexpected),
                    message + " | Expected [" + actual + "] NOT to contain [" + unexpected + "]");
            logPass(message, "assertNotContains",
                    "[" + actual + "] does not contain [" + unexpected + "]");
        } catch (AssertionError e) {
            logFail(message, "assertNotContains",
                    "[" + actual + "] unexpectedly contains [" + unexpected + "]");
            throw e;
        }
    }


    // ── assertContainsIgnoreCase() ────────────────────────────
    /**
     * Asserts that actual contains expected substring, case-insensitive.
     *
     * Example:
     *   CustomAssert.assertContainsIgnoreCase(title, "dashboard", "Page title check");
     */
    public static void assertContainsIgnoreCase(String actual, String expected, String message) {
        try {
            Assert.assertTrue(
                    actual != null && actual.toLowerCase().contains(expected.toLowerCase()),
                    message + " | Expected [" + actual + "] to contain [" + expected
                            + "] (case-insensitive)");
            logPass(message, "assertContainsIgnoreCase",
                    "[" + actual + "] contains [" + expected + "] (case-insensitive)");
        } catch (AssertionError e) {
            logFail(message, "assertContainsIgnoreCase",
                    "[" + actual + "] does not contain [" + expected + "] (case-insensitive)");
            throw e;
        }
    }


    // ── assertMatches() ──────────────────────────────────────
    /**
     * Asserts that actual string matches a regex pattern.
     *
     * Example:
     *   CustomAssert.assertMatches(orderId, "^ORD-\\d{6}$", "Order ID format");
     *   CustomAssert.assertMatches(email, "^[\\w.]+@[\\w.]+\\.\\w+$", "Email format");
     */
    public static void assertMatches(String actual, String regex, String message) {
        try {
            Assert.assertTrue(actual != null && actual.matches(regex),
                    message + " | [" + actual + "] does not match pattern [" + regex + "]");
            logPass(message, "assertMatches",
                    "[" + actual + "] matches pattern [" + regex + "]");
        } catch (AssertionError e) {
            logFail(message, "assertMatches",
                    "[" + actual + "] does not match pattern [" + regex + "]");
            throw e;
        }
    }


    // ── assertGreaterThan() ───────────────────────────────────
    /**
     * Asserts that actual is greater than the minimum value.
     *
     * Example:
     *   CustomAssert.assertGreaterThan(responseTime, 0, "Response time is positive");
     *   CustomAssert.assertGreaterThan(itemCount, 5, "At least 5 items returned");
     */
    public static void assertGreaterThan(long actual, long minimum, String message) {
        try {
            Assert.assertTrue(actual > minimum,
                    message + " | Expected [" + actual + "] > [" + minimum + "]");
            logPass(message, "assertGreaterThan",
                    "[" + actual + "] > [" + minimum + "]");
        } catch (AssertionError e) {
            logFail(message, "assertGreaterThan",
                    "[" + actual + "] is NOT > [" + minimum + "]");
            throw e;
        }
    }


    // ── assertStatusCode() ───────────────────────────────────
    /**
     * Specialised assertion for HTTP status codes.
     * Provides a clearer failure message than assertEquals.
     *
     * Example:
     *   CustomAssert.assertStatusCode(response.getStatusCode(), 200);
     *   CustomAssert.assertStatusCode(response.getStatusCode(), 201);
     */
    public static void assertStatusCode(int actual, int expected) {
        String message = "HTTP Status Code";
        try {
            Assert.assertEquals(actual, expected,
                    "Expected HTTP status [" + expected + "] but received [" + actual + "]");
            logPass(message, "assertStatusCode", "Status code: " + actual);
        } catch (AssertionError e) {
            logFail(message, "assertStatusCode",
                    "Expected [" + expected + "] but was [" + actual + "]");
            throw e;
        }
    }


    // ═══════════════════════════════════════════════════════════
    // SOFT ASSERTIONS — collect failures, report at end
    // ═══════════════════════════════════════════════════════════

    // ── softAssertEquals() ───────────────────────────────────
    /**
     * Soft assertion — does NOT fail immediately.
     * Collects the failure and continues the scenario.
     * Must call assertAll() at the end to trigger failures.
     *
     * Example:
     *   CustomAssert.softAssertEquals(firstName, "John", "First name check");
     *   CustomAssert.softAssertEquals(lastName,  "Doe",  "Last name check");
     *   CustomAssert.assertAll("User profile verification");
     */
    public static void softAssertEquals(Object actual, Object expected, String message) {
        log.debug("[SOFT] assertEquals | {} | Expected: [{}] Actual: [{}]",
                message, expected, actual);
        softAssert.get().assertEquals(actual, expected, message);
    }


    // ── softAssertTrue() ─────────────────────────────────────
    /**
     * Soft assertion for boolean conditions.
     *
     * Example:
     *   CustomAssert.softAssertTrue(element.isEnabled(), "Button is enabled");
     */
    public static void softAssertTrue(boolean condition, String message) {
        log.debug("[SOFT] assertTrue | {} | Condition: {}", message, condition);
        softAssert.get().assertTrue(condition, message);
    }


    // ── softAssertFalse() ────────────────────────────────────
    /**
     * Soft assertion for false conditions.
     *
     * Example:
     *   CustomAssert.softAssertFalse(errorPanel.isDisplayed(), "No error panel shown");
     */
    public static void softAssertFalse(boolean condition, String message) {
        log.debug("[SOFT] assertFalse | {} | Condition: {}", message, condition);
        softAssert.get().assertFalse(condition, message);
    }


    // ── softAssertNotNull() ───────────────────────────────────
    /**
     * Soft assertion that object is not null.
     *
     * Example:
     *   CustomAssert.softAssertNotNull(userId, "User ID is present");
     */
    public static void softAssertNotNull(Object object, String message) {
        log.debug("[SOFT] assertNotNull | {} | Value: {}", message, object);
        softAssert.get().assertNotNull(object, message);
    }


    // ── softAssertContains() ─────────────────────────────────
    /**
     * Soft assertion that actual contains expected substring.
     *
     * Example:
     *   CustomAssert.softAssertContains(body, "\"active\":true", "User is active");
     */
    public static void softAssertContains(String actual, String expected, String message) {
        boolean result = actual != null && actual.contains(expected);
        log.debug("[SOFT] assertContains | {} | Contains: {}", message, result);
        softAssert.get().assertTrue(result,
                message + " | Expected [" + actual + "] to contain [" + expected + "]");
    }


    // ── assertAll() ──────────────────────────────────────────
    /**
     * Triggers all collected soft assertion failures at once.
     * MUST be called at the end of any test that uses soft assertions.
     * Also resets the SoftAssert instance for the next scenario.
     *
     * Example:
     *   CustomAssert.assertAll("Checkout page verification complete");
     */
    public static void assertAll(String context) {
        log.debug("assertAll triggered for context: [{}]", context);
        try {
            softAssert.get().assertAll();
        } finally {
            // Always reset for the next scenario on this thread
            resetSoftAssert();
        }
    }


    // ── resetSoftAssert() ────────────────────────────────────
    /**
     * Resets the SoftAssert instance for the current thread.
     * Called automatically by assertAll().
     * Also call from Hooks.java @After as a safety net to prevent
     * soft assertion state leaking between scenarios.
     *
     * Example (in Hooks.java @After):
     *   CustomAssert.resetSoftAssert();
     */
    public static void resetSoftAssert() {
        softAssert.set(new SoftAssert());
        log.debug("SoftAssert reset for thread [{}]", Thread.currentThread().getName());
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Logs a passing assertion at INFO level.
     * TODO-3: Add extentTest.pass() call here once ExtentReportManager
     *         is built in Step 29 and getTest() is available.
     */
    private static void logPass(String message, String assertionType, String detail) {
        log.info("✔ PASS | {} | {} | {}", assertionType, message, detail);
        // TODO-3: ExtentReportManager.getTest().pass(message + " | " + detail);
    }


    /**
     * Logs a failing assertion at ERROR level.
     * TODO-3: Add extentTest.fail() call here once ExtentReportManager
     *         is built in Step 29 and getTest() is available.
     */
    private static void logFail(String message, String assertionType, String detail) {
        log.error("✖ FAIL | {} | {} | {}", assertionType, message, detail);
        // TODO-3: ExtentReportManager.getTest().fail(message + " | " + detail);
    }


    /**
     * Captures a screenshot on assertion failure if driver is provided.
     * The screenshot is saved to disk and path is logged.
     */
    private static void captureFailureScreenshot(WebDriver driver, String assertionMessage) {
        if (driver != null) {
            String screenshotPath = ScreenshotUtils.captureAndSave(
                    driver, "ASSERT_FAIL_" + assertionMessage);
            if (screenshotPath != null) {
                log.info("Failure screenshot saved → [{}]", screenshotPath);
            }
        }
    }

}