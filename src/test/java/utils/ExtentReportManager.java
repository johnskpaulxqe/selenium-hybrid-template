package utils;

/*
  ============================================================
  FILE: ExtentReportManager.java
  LOCATION: src/test/java/utils/ExtentReportManager.java

  PURPOSE:
    Manages the full lifecycle of Extent Reports for the
    framework. Implements TestNG ITestListener so it hooks
    into the suite lifecycle automatically via testng.xml.
    Responsibilities:
      1. Create the ExtentReports instance once per suite
      2. Create a test node for each scenario
      3. Mark tests PASS / FAIL / SKIP with screenshots
      4. Flush (write) the report after the suite finishes
      5. Provide a ThreadLocal ExtentTest for parallel safety

  HOW IT WORKS:
    testng.xml declares this class as a listener:
      <listener class-name="utils.ExtentReportManager"/>

    Then in Hooks.java:
      ExtentReportManager.createTest(scenarioName, tags);

    In step definitions / assertions:
      ExtentReportManager.getTest().pass("Step passed");
      ExtentReportManager.getTest().fail("Step failed");

    After suite ends automatically:
      ExtentReportManager.flushReport(); (called by onFinish)

  TODO (customise per project):
    - TODO-1 : Change report title and name in createReport()
    - TODO-2 : Adjust report theme (DARK / STANDARD) to
               match your team's preference
    - TODO-3 : Add your project logo by setting a thumbnail
               on the report configuration
  ============================================================
*/

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentReportManager implements ITestListener {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LoggerFactory.getLogger(ExtentReportManager.class);

    // ── Singleton ExtentReports instance ─────────────────────
    // Created once per suite run — shared across all threads
    private static ExtentReports extent;

    // ── ThreadLocal ExtentTest ────────────────────────────────
    // Each parallel thread gets its own test node
    private static final ThreadLocal<ExtentTest> testThreadLocal = new ThreadLocal<>();

    // ── Report output path ────────────────────────────────────
    private static String reportPath;


    // ═══════════════════════════════════════════════════════════
    // TESTNG LISTENER — SUITE LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    /**
     * Called once when the TestNG suite starts.
     * Creates the ExtentReports instance and configures the reporter.
     */
    @Override
    public void onStart(ITestContext context) {
        log.info("ExtentReportManager → Suite starting: [{}]", context.getName());
        createReport();
    }


    /**
     * Called once when the TestNG suite finishes.
     * Flushes the report to disk.
     */
    @Override
    public void onFinish(ITestContext context) {
        log.info("ExtentReportManager → Suite finished. Flushing report to: [{}]", reportPath);
        flushReport();
    }


    /**
     * Called when a TestNG test method starts.
     * Creates a new test node in the Extent report.
     */
    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.debug("ExtentReportManager → Test started: [{}]", testName);
        ExtentTest test = extent.createTest(testName);
        testThreadLocal.set(test);
    }


    /**
     * Called when a TestNG test passes.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.debug("ExtentReportManager → Test passed: [{}]",
                result.getMethod().getMethodName());
        if (getTest() != null) {
            getTest().pass("Test passed");
        }
    }


    /**
     * Called when a TestNG test fails.
     * Captures and embeds screenshot automatically.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.debug("ExtentReportManager → Test failed: [{}]",
                result.getMethod().getMethodName());

        if (getTest() != null) {
            // Log the failure cause
            getTest().fail(result.getThrowable());

            // Embed screenshot if driver is active
            attachScreenshotOnFailure(result.getMethod().getMethodName());
        }
    }


    /**
     * Called when a TestNG test is skipped.
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.debug("ExtentReportManager → Test skipped: [{}]",
                result.getMethod().getMethodName());
        if (getTest() != null) {
            getTest().skip("Test skipped: " + result.getThrowable());
        }
    }


    // ═══════════════════════════════════════════════════════════
    // PUBLIC API — used by Hooks.java and step definitions
    // ═══════════════════════════════════════════════════════════

    // ── createTest() ─────────────────────────────────────────
    /**
     * Creates a new Extent test node for a Cucumber scenario.
     * Called from Hooks.java @Before hook.
     *
     * Example (in Hooks.java @Before):
     *   ExtentReportManager.createTest(scenario.getName(),
     *       scenario.getSourceTagNames().toString());
     *
     * @param scenarioName the Cucumber scenario name
     * @param tags         scenario tags as a string for the report
     */
    public static void createTest(String scenarioName, String tags) {
        if (extent == null) {
            createReport();
        }
        ExtentTest test = extent.createTest(scenarioName)
                .assignCategory(parseTags(tags));
        testThreadLocal.set(test);
        log.debug("Extent test node created: [{}]", scenarioName);
    }


    // ── getTest() ─────────────────────────────────────────────
    /**
     * Returns the ExtentTest node for the current thread.
     * Use to log steps, attach screenshots, and mark results.
     *
     * Example:
     *   ExtentReportManager.getTest().pass("Login successful");
     *   ExtentReportManager.getTest().fail("Element not found");
     *   ExtentReportManager.getTest().info("Navigating to page");
     */
    public static ExtentTest getTest() {
        return testThreadLocal.get();
    }


    // ── logPass() ─────────────────────────────────────────────
    /**
     * Logs a passing step to the current test node.
     *
     * Example:
     *   ExtentReportManager.logPass("Login button clicked");
     */
    public static void logPass(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.pass(message);
        }
    }


    // ── logFail() ─────────────────────────────────────────────
    /**
     * Logs a failing step with a message.
     *
     * Example:
     *   ExtentReportManager.logFail("Expected [200] but got [404]");
     */
    public static void logFail(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.fail(message);
        }
    }


    // ── logInfo() ─────────────────────────────────────────────
    /**
     * Logs an informational step to the current test node.
     *
     * Example:
     *   ExtentReportManager.logInfo("Navigating to dashboard");
     */
    public static void logInfo(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.info(message);
        }
    }


    // ── logScreenshot() ───────────────────────────────────────
    /**
     * Attaches a Base64 screenshot to the current test node.
     * Called from Hooks.java @After on scenario failure.
     *
     * Example:
     *   String base64 = ScreenshotUtils.captureAsBase64(driver);
     *   ExtentReportManager.logScreenshot(base64, "Failure screenshot");
     */
    public static void logScreenshot(String base64Screenshot, String title) {
        ExtentTest test = getTest();
        if (test != null && base64Screenshot != null) {
            try {
                test.addScreenCaptureFromBase64String(base64Screenshot, title);
                log.debug("Screenshot attached to Extent report: [{}]", title);
            } catch (Exception e) {
                log.warn("Could not attach screenshot to Extent report: {}", e.getMessage());
            }
        }
    }


    // ── markScenarioPassed() ──────────────────────────────────
    /**
     * Marks the current scenario as passed in the report.
     * Called from Hooks.java @After when scenario passes.
     *
     * Example (in Hooks.java @After):
     *   if (!scenario.isFailed()) {
     *       ExtentReportManager.markScenarioPassed(scenario.getName());
     *   }
     */
    public static void markScenarioPassed(String scenarioName) {
        ExtentTest test = getTest();
        if (test != null) {
            test.log(Status.PASS, "Scenario PASSED: " + scenarioName);
        }
    }


    // ── markScenarioFailed() ──────────────────────────────────
    /**
     * Marks the current scenario as failed and attaches screenshot.
     * Called from Hooks.java @After when scenario fails.
     *
     * Example (in Hooks.java @After):
     *   if (scenario.isFailed()) {
     *       String base64 = ScreenshotUtils.captureAsBase64(driver);
     *       ExtentReportManager.markScenarioFailed(scenario.getName(), base64);
     *   }
     */
    public static void markScenarioFailed(String scenarioName, String base64Screenshot) {
        ExtentTest test = getTest();
        if (test != null) {
            test.log(Status.FAIL, "Scenario FAILED: " + scenarioName);
            if (base64Screenshot != null) {
                logScreenshot(base64Screenshot, "Failure Screenshot");
            }
        }
    }


    // ── flushReport() ─────────────────────────────────────────
    /**
     * Writes the Extent report to disk.
     * Called automatically by onFinish() TestNG listener.
     * Can also be called manually for early flush if needed.
     */
    public static void flushReport() {
        if (extent != null) {
            extent.flush();
            log.info("Extent report flushed → [{}]", reportPath);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── createReport() ────────────────────────────────────────
    /**
     * Creates and configures the ExtentReports instance.
     * Runs once per suite. Thread-safe via synchronized block.
     */
    private static synchronized void createReport() {
        if (extent != null) {
            return; // Already created — don't recreate
        }

        // Build timestamped report path from config
        String baseReportPath = ConfigReader.get(
                "reporting.extentReportPath",
                "target/extent-reports/report.html");

        // Add timestamp to filename for unique reports per run
        String timestamp  = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        reportPath        = baseReportPath.replace(".html",
                "_" + timestamp + ".html");

        // Ensure output directory exists
        File reportDir = new File(reportPath).getParentFile();
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        // Create the HTML Spark reporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        configureSparkReporter(sparkReporter);

        // Create and configure the ExtentReports instance
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        // Add system / environment information to the report
        extent.setSystemInfo("Environment", ConfigReader.getActiveEnv());
        extent.setSystemInfo("Browser",     System.getProperty("browser",
                ConfigReader.get("browser.default", "headless")));
        extent.setSystemInfo("OS",          System.getProperty("os.name"));
        extent.setSystemInfo("Java",        System.getProperty("java.version"));
        extent.setSystemInfo("Base URL",    ConfigReader.get("baseUrl", "N/A"));
        extent.setSystemInfo("Tester",      System.getProperty("user.name"));

        log.info("Extent report created → [{}]", reportPath);
    }


    // ── configureSparkReporter() ──────────────────────────────
    /**
     * Applies visual and content configuration to the HTML reporter.
     * TODO-1: Change reportName and documentTitle for your project
     * TODO-2: Change Theme.DARK to Theme.STANDARD if preferred
     */
    private static void configureSparkReporter(ExtentSparkReporter reporter) {

        // Load config from extent-config.xml if it exists
        String configFilePath = "src/test/resources/extent-config.xml";
        File   configFile     = new File(configFilePath);
        if (configFile.exists()) {
            try {
                reporter.loadXMLConfig(configFilePath);
                log.debug("Loaded Extent reporter config from: [{}]", configFilePath);
                return; // Config file takes full control
            } catch (Exception e) {
                log.warn("Could not load extent-config.xml — using defaults: {}",
                        e.getMessage());
            }
        }

        // Fall back to programmatic config if XML not found
        // TODO-1: Replace with your project name
        reporter.config().setReportName("Selenium Hybrid Framework — Test Results");
        reporter.config().setDocumentTitle("Automation Test Report");

        // TODO-2: Change to Theme.STANDARD for a light theme
        reporter.config().setTheme(Theme.DARK);
        reporter.config().setEncoding("UTF-8");
        reporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        reporter.config().setTimelineEnabled(true);
    }


    // ── attachScreenshotOnFailure() ───────────────────────────
    /**
     * Attempts to capture and attach a screenshot on test failure.
     * Silently skips if no driver is active.
     */
    private static void attachScreenshotOnFailure(String testName) {
        try {
            if (DriverManager.isDriverInitialised()) {
                String base64 = ScreenshotUtils.captureAsBase64(
                        DriverManager.getDriver());
                if (base64 != null) {
                    logScreenshot(base64, "Failure: " + testName);
                }
            }
        } catch (Exception e) {
            log.warn("Could not attach failure screenshot: {}", e.getMessage());
        }
    }


    // ── parseTags() ───────────────────────────────────────────
    /**
     * Converts a Cucumber tag set string like "[smoke, regression]"
     * into a String array for Extent category assignment.
     */
    private static String[] parseTags(String tagsString) {
        if (tagsString == null || tagsString.isBlank()) {
            return new String[]{"untagged"};
        }
        // Remove brackets, split by comma, trim each tag, remove @ prefix
        return tagsString
                .replace("[", "").replace("]", "")
                .split(",")
                ;
    }

}