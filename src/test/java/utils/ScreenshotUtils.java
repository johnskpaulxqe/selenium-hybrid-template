package utils;

/*
  ============================================================
  FILE: ScreenshotUtils.java
  LOCATION: src/test/java/utils/ScreenshotUtils.java

  PURPOSE:
    Handles all screenshot capture, saving, and report
    attachment operations. Responsibilities:
      1. Capture full-page and visible-viewport screenshots
      2. Save screenshots to the configured output directory
      3. Return Base64-encoded screenshots for Extent Reports
      4. Capture element-level screenshots (specific element only)
      5. Auto-name screenshots with timestamp + scenario name
      6. Attach screenshots to Extent Reports on failure

  HOW TO USE:
    // Capture and save on failure (called from Hooks.java)
    String path = ScreenshotUtils.captureAndSave(driver, scenario.getName());

    // Capture as Base64 for embedding in Extent Report
    String base64 = ScreenshotUtils.captureAsBase64(driver);

    // Capture a specific element only
    String path = ScreenshotUtils.captureElement(driver, element, "elementName");

    // Attach to Extent Report test node
    ScreenshotUtils.attachToReport(extentTest, driver, "Step failed here");

  TODO (customise per project):
    - TODO-1 : Change SCREENSHOT_DIR if you want a different output path
    - TODO-2 : Adjust timestamp format if your CI expects a specific pattern
    - TODO-3 : Add S3/Azure Blob upload method if you need cloud screenshot storage
  ============================================================
*/

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class ScreenshotUtils {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(ScreenshotUtils.class);

    // ── Screenshot output directory ───────────────────────────
    // TODO-1: Change this path if you want screenshots elsewhere
    private static final String SCREENSHOT_DIR =
            ConfigReader.get("reporting.screenshotPath", "target/screenshots");

    // ── Timestamp format for file naming ──────────────────────
    // TODO-2: Adjust format if your CI parses screenshot filenames
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";

    // Private constructor — static utility class
    private ScreenshotUtils() {}


    // ═══════════════════════════════════════════════════════════
    // CAPTURE AND SAVE TO DISK
    // ═══════════════════════════════════════════════════════════

    // ── captureAndSave() ──────────────────────────────────────
    /**
     * Captures a screenshot and saves it to the screenshots directory.
     * Returns the absolute file path of the saved screenshot.
     * Returns null if capture fails (e.g. driver already quit).
     *
     * Called automatically from Hooks.java @After on scenario failure.
     *
     * Example:
     *   String screenshotPath = ScreenshotUtils.captureAndSave(driver, "Login_Failed");
     *
     * @param driver        the active WebDriver instance
     * @param scenarioName  used to build the screenshot filename
     * @return              absolute path of saved file, or null on failure
     */
    public static String captureAndSave(WebDriver driver, String scenarioName) {
        if (driver == null) {
            log.warn("Cannot capture screenshot — driver is null");
            return null;
        }

        try {
            // Take the screenshot
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Build a safe, unique filename
            String fileName  = buildFileName(scenarioName);
            String filePath  = SCREENSHOT_DIR + File.separator + fileName;
            File   destFile  = new File(filePath);

            // Ensure the directory exists
            FileUtils.forceMkdirParent(destFile);

            // Copy screenshot to destination
            FileUtils.copyFile(srcFile, destFile);

            log.info("Screenshot saved → [{}]", destFile.getAbsolutePath());
            return destFile.getAbsolutePath();

        } catch (IOException e) {
            log.error("Failed to save screenshot for scenario [{}]: {}",
                    scenarioName, e.getMessage(), e);
            return null;
        } catch (ClassCastException e) {
            log.error("WebDriver does not support screenshots: {}", e.getMessage());
            return null;
        }
    }


    // ── captureToFile() ───────────────────────────────────────
    /**
     * Captures a screenshot and saves it to a specific file path.
     * Use when you need precise control over the output location.
     *
     * Example:
     *   ScreenshotUtils.captureToFile(driver, "target/evidence/step1.png");
     */
    public static boolean captureToFile(WebDriver driver, String fullFilePath) {
        if (driver == null) {
            log.warn("Cannot capture screenshot — driver is null");
            return false;
        }
        try {
            File srcFile  = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destFile = new File(fullFilePath);
            FileUtils.forceMkdirParent(destFile);
            FileUtils.copyFile(srcFile, destFile);
            log.info("Screenshot saved to: [{}]", destFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("Failed to save screenshot to [{}]: {}", fullFilePath, e.getMessage(), e);
            return false;
        }
    }


    // ═══════════════════════════════════════════════════════════
    // CAPTURE AS BASE64 (for Extent Reports embedding)
    // ═══════════════════════════════════════════════════════════

    // ── captureAsBase64() ─────────────────────────────────────
    /**
     * Captures a screenshot and returns it as a Base64-encoded string.
     * Used by ExtentReportManager to embed screenshots directly
     * in the HTML report without needing a file path reference.
     *
     * Example:
     *   String base64 = ScreenshotUtils.captureAsBase64(driver);
     *   extentTest.addScreenCaptureFromBase64String(base64);
     *
     * @return Base64 string (without data URI prefix), or null on failure
     */
    public static String captureAsBase64(WebDriver driver) {
        if (driver == null) {
            log.warn("Cannot capture screenshot — driver is null");
            return null;
        }
        try {
            String base64 = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.BASE64);
            log.debug("Screenshot captured as Base64 ({} chars)", base64.length());
            return base64;
        } catch (Exception e) {
            log.error("Failed to capture screenshot as Base64: {}", e.getMessage(), e);
            return null;
        }
    }


    // ── captureAsBase64WithPrefix() ───────────────────────────
    /**
     * Captures screenshot as Base64 with the data URI prefix included.
     * Some report libraries require the full data URI format.
     *
     * Example:
     *   String dataUri = ScreenshotUtils.captureAsBase64WithPrefix(driver);
     *   // Returns: "data:image/png;base64,iVBORw0KGgo..."
     */
    public static String captureAsBase64WithPrefix(WebDriver driver) {
        String base64 = captureAsBase64(driver);
        return base64 != null ? "data:image/png;base64," + base64 : null;
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT-LEVEL SCREENSHOT
    // ═══════════════════════════════════════════════════════════

    // ── captureElement() ──────────────────────────────────────
    /**
     * Captures a screenshot of a specific WebElement only
     * (not the full page). Useful for capturing error messages,
     * form fields, or specific UI components as evidence.
     *
     * Example:
     *   String path = ScreenshotUtils.captureElement(driver, errorBanner, "ErrorBanner");
     */
    public static String captureElement(WebDriver driver, WebElement element,
                                        String elementName) {
        if (driver == null || element == null) {
            log.warn("Cannot capture element screenshot — driver or element is null");
            return null;
        }
        try {
            // Selenium 4 native element screenshot
            File srcFile  = element.getScreenshotAs(OutputType.FILE);
            String fileName  = buildFileName(elementName + "_element");
            String filePath  = SCREENSHOT_DIR + File.separator + "elements"
                    + File.separator + fileName;
            File   destFile  = new File(filePath);

            FileUtils.forceMkdirParent(destFile);
            FileUtils.copyFile(srcFile, destFile);

            log.info("Element screenshot saved → [{}]", destFile.getAbsolutePath());
            return destFile.getAbsolutePath();

        } catch (IOException e) {
            log.error("Failed to save element screenshot [{}]: {}",
                    elementName, e.getMessage(), e);
            return null;
        }
    }


    // ── captureElementAsBase64() ──────────────────────────────
    /**
     * Captures a specific element as Base64 for report embedding.
     *
     * Example:
     *   String base64 = ScreenshotUtils.captureElementAsBase64(driver, tableElement);
     */
    public static String captureElementAsBase64(WebDriver driver, WebElement element) {
        if (element == null) {
            log.warn("Cannot capture element screenshot — element is null");
            return null;
        }
        try {
            byte[] bytes  = element.getScreenshotAs(OutputType.BYTES);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("Failed to capture element as Base64: {}", e.getMessage(), e);
            return null;
        }
    }


    // ═══════════════════════════════════════════════════════════
    // CONVENIENCE — CAPTURE ON FAILURE
    // ═══════════════════════════════════════════════════════════

    // ── captureOnFailure() ────────────────────────────────────
    /**
     * Captures and saves a screenshot only if the scenario failed
     * AND screenshotOnFailure is enabled in config.json.
     *
     * This is the primary method called from Hooks.java @After.
     * Returns the file path if captured, null otherwise.
     *
     * Example (in Hooks.java):
     *   String path = ScreenshotUtils.captureOnFailure(driver, scenario);
     *
     * @param driver        active WebDriver
     * @param scenarioName  name of the failed scenario
     * @param hasFailed     true if the scenario failed
     * @return              file path of screenshot, or null
     */
    public static String captureOnFailure(WebDriver driver,
                                          String scenarioName,
                                          boolean hasFailed) {
        boolean screenshotEnabled = ConfigReader.getBoolean(
                "reporting.screenshotOnFailure", true);

        if (hasFailed && screenshotEnabled) {
            log.info("Scenario failed — capturing screenshot for: [{}]", scenarioName);
            return captureAndSave(driver, scenarioName);
        }

        // Capture on pass if configured
        boolean captureOnPass = ConfigReader.getBoolean(
                "reporting.screenshotOnPass", false);

        if (!hasFailed && captureOnPass) {
            log.debug("Capturing screenshot on pass for: [{}]", scenarioName);
            return captureAndSave(driver, scenarioName);
        }

        return null;
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── buildFileName() ───────────────────────────────────────
    /**
     * Builds a safe, unique screenshot filename from the
     * scenario name and current timestamp.
     *
     * Input:  "Login with valid credentials"
     * Output: "Login_with_valid_credentials_2024-06-15_14-32-01-123.png"
     *
     * Special characters are stripped to ensure the filename
     * is valid on Windows, Linux, and macOS.
     */
    private static String buildFileName(String scenarioName) {
        // TODO-2: Adjust timestamp format if CI parses filenames
        String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());

        // Sanitise scenario name — replace anything not alphanumeric/underscore/hyphen
        String safeName = scenarioName
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")  // Replace special chars with _
                .replaceAll("_+", "_")                  // Collapse multiple underscores
                .replaceAll("^_|_$", "")               // Trim leading/trailing underscores
                .substring(0, Math.min(scenarioName.length(), 80)); // Max 80 chars

        return safeName + "_" + timestamp + ".png";
    }

}