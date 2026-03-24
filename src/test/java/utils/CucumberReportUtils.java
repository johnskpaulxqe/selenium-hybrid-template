package utils;

/*
  ============================================================
  FILE: CucumberReportUtils.java
  LOCATION: src/test/java/utils/CucumberReportUtils.java

  PURPOSE:
    Utility class that bridges Cucumber's built-in reporting
    with Extent Reports and provides helper methods for
    embedding evidence (screenshots, logs, JSON) into the
    Cucumber HTML report.
    Responsibilities:
      1. Attach screenshots to the Cucumber HTML report
      2. Attach plain text logs to the Cucumber HTML report
      3. Attach JSON payloads (API request/response bodies)
      4. Log step-level detail to both Cucumber and Extent
      5. Provide a summary log of scenario results

  HOW CUCUMBER REPORTING WORKS:
    Cucumber generates its own HTML report in:
      target/cucumber-reports/index.html

    Screenshots and text are attached via scenario.attach():
      scenario.attach(bytes,   "image/png", "title")  → screenshot
      scenario.attach(text,    "text/plain", "title") → plain text
      scenario.attach(json,    "application/json", "title") → JSON

    This class wraps those calls with logging and null safety.

  HOW TO USE:
    // Attach screenshot to Cucumber report
    CucumberReportUtils.attachScreenshot(scenario, driver, "Login page");

    // Attach API response body
    CucumberReportUtils.attachJson(scenario, responseBody, "API Response");

    // Log a step detail
    CucumberReportUtils.logStep(scenario, "Clicked login button");

    // Log the final scenario result
    CucumberReportUtils.logScenarioResult(scenario);

  TODO (customise per project):
    - TODO-1 : No changes needed for most projects
    - TODO-2 : Add attachPdf() if your scenarios generate
               PDF documents that need to be evidenced
    - TODO-3 : Add attachHtml() if you need to embed
               HTML snippets as report evidence
  ============================================================
*/

import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class CucumberReportUtils {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LoggerFactory.getLogger(CucumberReportUtils.class);

    // Private constructor — static utility class
    private CucumberReportUtils() {}


    // ═══════════════════════════════════════════════════════════
    // SCREENSHOT ATTACHMENT
    // ═══════════════════════════════════════════════════════════

    // ── attachScreenshot() ───────────────────────────────────
    /**
     * Captures a screenshot and attaches it to the Cucumber
     * HTML report as an embedded image.
     *
     * Called from Hooks.java @After on scenario failure, or
     * from any step that needs visual evidence.
     *
     * Example:
     *   CucumberReportUtils.attachScreenshot(scenario, driver, "After login");
     *
     * @param scenario the active Cucumber Scenario
     * @param driver   the active WebDriver
     * @param title    label shown above the screenshot in the report
     */
    public static void attachScreenshot(Scenario scenario,
                                        WebDriver driver,
                                        String title) {
        if (scenario == null || driver == null) {
            log.warn("Cannot attach screenshot — scenario or driver is null");
            return;
        }
        try {
            byte[] screenshot = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", title);
            log.debug("Screenshot attached to Cucumber report: [{}]", title);
        } catch (Exception e) {
            log.warn("Could not attach screenshot [{}] to Cucumber report: {}",
                    title, e.getMessage());
        }
    }


    // ── attachScreenshotOnFailure() ──────────────────────────
    /**
     * Attaches a screenshot only if the scenario has failed.
     * Safe to call unconditionally in @After hooks.
     *
     * Example (in Hooks.java @After):
     *   CucumberReportUtils.attachScreenshotOnFailure(scenario, driver);
     */
    public static void attachScreenshotOnFailure(Scenario scenario, WebDriver driver) {
        if (scenario != null && scenario.isFailed()) {
            attachScreenshot(scenario, driver, "Failure Screenshot — " + scenario.getName());
        }
    }


    // ── attachBase64Screenshot() ─────────────────────────────
    /**
     * Attaches a screenshot from an existing Base64 string.
     * Use when you already have a Base64 capture from
     * ScreenshotUtils.captureAsBase64().
     *
     * Example:
     *   String base64 = ScreenshotUtils.captureAsBase64(driver);
     *   CucumberReportUtils.attachBase64Screenshot(scenario, base64, "Step evidence");
     */
    public static void attachBase64Screenshot(Scenario scenario,
                                              String base64Screenshot,
                                              String title) {
        if (scenario == null || base64Screenshot == null) {
            return;
        }
        try {
            // Decode Base64 string back to bytes for attachment
            byte[] bytes = java.util.Base64.getDecoder().decode(base64Screenshot);
            scenario.attach(bytes, "image/png", title);
            log.debug("Base64 screenshot attached to Cucumber report: [{}]", title);
        } catch (Exception e) {
            log.warn("Could not attach base64 screenshot [{}]: {}", title, e.getMessage());
        }
    }


    // ═══════════════════════════════════════════════════════════
    // TEXT AND LOG ATTACHMENT
    // ═══════════════════════════════════════════════════════════

    // ── logStep() ────────────────────────────────────────────
    /**
     * Attaches a plain text step log to the Cucumber report.
     * Also logs to SLF4J at INFO level.
     * Use to add step-level detail that is not in the Gherkin.
     *
     * Example:
     *   CucumberReportUtils.logStep(scenario,
     *       "Clicked submit button — form validation passed");
     */
    public static void logStep(Scenario scenario, String message) {
        log.info(message);
        if (scenario != null) {
            try {
                scenario.attach(
                        message.getBytes(StandardCharsets.UTF_8),
                        "text/plain",
                        "Step Detail");
            } catch (Exception e) {
                log.warn("Could not attach step log to Cucumber report: {}", e.getMessage());
            }
        }
    }


    // ── attachText() ─────────────────────────────────────────
    /**
     * Attaches any plain text block to the Cucumber report.
     * Use for logging values, query results, or test data.
     *
     * Example:
     *   CucumberReportUtils.attachText(scenario,
     *       "DB Result: " + rowMap.toString(), "Database Query Result");
     */
    public static void attachText(Scenario scenario, String text, String title) {
        if (scenario == null || text == null) {
            return;
        }
        try {
            scenario.attach(
                    text.getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    title);
            log.debug("Text attached to Cucumber report: [{}]", title);
        } catch (Exception e) {
            log.warn("Could not attach text [{}] to Cucumber report: {}", title, e.getMessage());
        }
    }


    // ═══════════════════════════════════════════════════════════
    // JSON ATTACHMENT — for API request/response evidence
    // ═══════════════════════════════════════════════════════════

    // ── attachJson() ─────────────────────────────────────────
    /**
     * Attaches a JSON string to the Cucumber report.
     * Automatically pretty-prints the JSON for readability.
     * Use to embed API request bodies and response payloads.
     *
     * Example:
     *   CucumberReportUtils.attachJson(scenario,
     *       response.getBody().asString(), "API Response Body");
     *
     *   CucumberReportUtils.attachJson(scenario,
     *       JsonReader.toJson(requestBody), "API Request Body");
     */
    public static void attachJson(Scenario scenario, String json, String title) {
        if (scenario == null || json == null) {
            return;
        }
        try {
            // Pretty-print for readability in the report
            String prettyJson = JsonReader.prettyPrint(json);
            scenario.attach(
                    prettyJson.getBytes(StandardCharsets.UTF_8),
                    "application/json",
                    title);
            log.debug("JSON attached to Cucumber report: [{}]", title);
        } catch (Exception e) {
            log.warn("Could not attach JSON [{}] to Cucumber report: {}", title, e.getMessage());
        }
    }


    // ── attachRequestResponse() ──────────────────────────────
    /**
     * Attaches both a request body and response body to the
     * Cucumber report as a single combined evidence block.
     * Use in @api step definitions for full traceability.
     *
     * Example:
     *   CucumberReportUtils.attachRequestResponse(scenario,
     *       JsonReader.toJson(requestBody),
     *       response.getBody().asString(),
     *       "POST /api/v1/users");
     */
    public static void attachRequestResponse(Scenario scenario,
                                             String requestBody,
                                             String responseBody,
                                             String operationName) {
        if (scenario == null) return;

        StringBuilder combined = new StringBuilder();
        combined.append("=== REQUEST ===\n");
        combined.append(requestBody != null
                ? JsonReader.prettyPrint(requestBody)
                : "(no request body)");
        combined.append("\n\n=== RESPONSE ===\n");
        combined.append(responseBody != null
                ? JsonReader.prettyPrint(responseBody)
                : "(no response body)");

        try {
            scenario.attach(
                    combined.toString().getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    operationName + " — Request / Response");
            log.debug("Request/Response attached to report: [{}]", operationName);
        } catch (Exception e) {
            log.warn("Could not attach request/response for [{}]: {}",
                    operationName, e.getMessage());
        }
    }


    // ═══════════════════════════════════════════════════════════
    // SCENARIO RESULT LOGGING
    // ═══════════════════════════════════════════════════════════

    // ── logScenarioResult() ───────────────────────────────────
    /**
     * Logs the final scenario result summary to both the
     * Cucumber report and the SLF4J logger.
     * Call from Hooks.java @After.
     *
     * Example (in Hooks.java @After):
     *   CucumberReportUtils.logScenarioResult(scenario);
     */
    public static void logScenarioResult(Scenario scenario) {
        if (scenario == null) return;

        String status = scenario.isFailed() ? "FAILED ✖" : "PASSED ✔";
        String summary = String.format(
                "Scenario: [%s] | Status: [%s] | Tags: %s",
                scenario.getName(),
                status,
                scenario.getSourceTagNames());

        // Log to console/file via SLF4J
        if (scenario.isFailed()) {
            log.error(summary);
        } else {
            log.info(summary);
        }

        // Attach summary text to Cucumber HTML report
        attachText(scenario, summary, "Scenario Result Summary");
    }


    // ── attachEnvironmentInfo() ───────────────────────────────
    /**
     * Attaches current environment configuration as evidence.
     * Useful for debugging failures — shows exactly what
     * environment, browser, and URLs were used.
     *
     * Example (in Hooks.java @Before):
     *   CucumberReportUtils.attachEnvironmentInfo(scenario);
     */
    public static void attachEnvironmentInfo(Scenario scenario) {
        if (scenario == null) return;

        String envInfo = String.format(
                "Environment : %s%n" +
                        "Base URL    : %s%n" +
                        "Browser     : %s%n" +
                        "OS          : %s%n" +
                        "Java        : %s",
                ConfigReader.getActiveEnv(),
                ConfigReader.get("baseUrl", "N/A"),
                System.getProperty("browser",
                        ConfigReader.get("browser.default", "headless")),
                System.getProperty("os.name"),
                System.getProperty("java.version"));

        attachText(scenario, envInfo, "Test Environment");
        log.debug("Environment info attached to Cucumber report");
    }

}