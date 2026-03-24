package hooks;

/*
  ============================================================
  FILE: Hooks.java
  LOCATION: src/test/java/hooks/Hooks.java

  PURPOSE:
    Cucumber lifecycle hooks that run before and after every
    scenario. Manages WebDriver, logging, screenshots,
    reporting, and DB connection lifecycle.
    Responsibilities:
      1. @Before  → initialise WebDriver for UI scenarios
      2. @Before  → set up API for API scenarios
      3. @Before  → log scenario start + set MDC context
      4. @After   → capture screenshot on failure
      5. @After   → attach screenshot to Extent Report
      6. @After   → quit WebDriver for UI scenarios
      7. @After   → close DB connection for DB scenarios
      8. @After   → log scenario result (PASS/FAIL)
      9. @After   → reset soft assertions

  HOW CUCUMBER TAGS CONTROL HOOKS:
    @Before                   → runs before EVERY scenario
    @Before("@ui")            → runs only before @ui scenarios
    @Before("@api")           → runs only before @api scenarios
    @Before("@db")            → runs only before @db scenarios
    @After                    → runs after EVERY scenario
    @After(order = 1)         → runs last (higher = earlier)

  ORDER OF EXECUTION:
    @Before(order=1) runs FIRST (lowest number first for Before)
    @After(order=1)  runs LAST  (lowest number last for After)

  TODO (customise per project):
    - TODO-1 : Add @Before for any additional setup your
               scenarios need (e.g. test data seeding)
    - TODO-2 : Add @After cleanup for resources specific
               to your project (e.g. API token invalidation)
    - TODO-3 : Wire ExtentReportManager.getTest() once built
               in Phase 7 — placeholder comments mark each spot
  ============================================================
*/

import context.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;
import utils.*;

public class Hooks {

    // ── TestContext injected by PicoContainer ─────────────────
    // PicoContainer injects the same TestContext instance that
    // all step definition classes in this scenario share.
    private final TestContext ctx;

    public Hooks(TestContext ctx) {
        this.ctx = ctx;
    }


    // ═══════════════════════════════════════════════════════════
    // @BEFORE HOOKS
    // ═══════════════════════════════════════════════════════════

    // ── Global @Before — runs before EVERY scenario ───────────
    /**
     * Runs before every scenario regardless of tags.
     * Sets up logging context and stores the Scenario reference.
     * order = 1 → runs first among all @Before hooks
     */
    @Before(order = 1)
    public void beforeScenario(Scenario scenario) {
        // Store scenario in context for hooks and step definitions
        ctx.setScenario(scenario);

        // Set MDC context so all log lines show the scenario name
        LogUtil.setScenarioContext(scenario.getName());
        LogUtil.setContext("tags", scenario.getSourceTagNames().toString());

        // Log visible separator in console/log file
        LogUtil.logScenarioStart(scenario.getName());
    }


    // ── @Before for UI scenarios ──────────────────────────────
    /**
     * Runs before every scenario tagged @ui or @smoke or @regression.
     * Initialises the WebDriver for the current thread.
     * order = 2 → runs after global @Before
     *
     * Browser is resolved from:
     *   1. -Dbrowser=xxx Maven CLI argument
     *   2. config.json browser.default
     *   3. Falls back to headless
     */
    @Before(value = "@ui or @smoke or @regression", order = 2)
    public void beforeUiScenario() {
        String scenarioName = ctx.getScenarioName();
        LogUtil.getLogger(Hooks.class)
                .info("Setting up WebDriver for UI scenario: [{}]", scenarioName);

        // Initialise driver — stored in ThreadLocal inside DriverManager
        DriverManager.initDriver();

        // TODO-3: Initialise Extent Report test node here once
        //         ExtentReportManager is built in Phase 7
        // ExtentReportManager.createTest(scenarioName, tags);
    }


    // ── @Before for API scenarios ─────────────────────────────
    /**
     * Runs before every scenario tagged @api.
     * Configures RestAssured base URI and default headers.
     * order = 2 → runs after global @Before
     */
    @Before(value = "@api", order = 2)
    public void beforeApiScenario() {
        LogUtil.getLogger(Hooks.class)
                .info("Setting up API for scenario: [{}]", ctx.getScenarioName());

        // Configure RestAssured once per scenario
        // ApiUtils.setup() is idempotent — safe to call multiple times
        ApiUtils.setup();
    }


    // ── @Before for DB scenarios ──────────────────────────────
    /**
     * Runs before every scenario tagged @db.
     * Opens the JDBC connection for this thread.
     * order = 2 → runs after global @Before
     */
    @Before(value = "@db", order = 2)
    public void beforeDbScenario() {
        LogUtil.getLogger(Hooks.class)
                .info("Opening DB connection for scenario: [{}]", ctx.getScenarioName());

        // Open connection — stored in ThreadLocal inside DBUtils
        DBUtils.getConnection();
        ctx.markDbConnectionOpened();
    }


    // ═══════════════════════════════════════════════════════════
    // @AFTER HOOKS
    // ═══════════════════════════════════════════════════════════

    // ── @After for UI scenarios ───────────────────────────────
    /**
     * Runs after every @ui / @smoke / @regression scenario.
     * Captures screenshot on failure, attaches to report,
     * then quits the WebDriver.
     * order = 2 → runs before the global @After (order=1)
     */
    @After(value = "@ui or @smoke or @regression", order = 2)
    public void afterUiScenario() {
        Scenario scenario = ctx.getScenario();

        // Only proceed if driver was actually initialised
        if (!DriverManager.isDriverInitialised()) {
            return;
        }

        WebDriver driver = DriverManager.getDriver();

        // Capture screenshot if scenario failed (or if pass capture enabled)
        String screenshotPath = ScreenshotUtils.captureOnFailure(
                driver,
                ctx.getScenarioName(),
                scenario.isFailed());

        // Attach screenshot to Cucumber HTML report as Base64 embed
        if (scenario.isFailed() && screenshotPath != null) {
            try {
                byte[] screenshotBytes =
                        ((org.openqa.selenium.TakesScreenshot) driver)
                                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                scenario.attach(screenshotBytes, "image/png", ctx.getScenarioName());

                LogUtil.getLogger(Hooks.class)
                        .info("Screenshot attached to Cucumber report");
            } catch (Exception e) {
                LogUtil.getLogger(Hooks.class)
                        .warn("Could not attach screenshot to report: {}", e.getMessage());
            }
        }

        // TODO-3: Attach screenshot to Extent Report once ExtentReportManager is built
        // if (scenario.isFailed()) {
        //     String base64 = ScreenshotUtils.captureAsBase64(driver);
        //     ExtentReportManager.getTest().fail("Scenario failed")
        //         .addScreenCaptureFromBase64String(base64);
        // }

        // Quit WebDriver — removes from ThreadLocal
        DriverManager.quitDriver();
    }


    // ── @After for API scenarios ──────────────────────────────
    /**
     * Runs after every @api scenario.
     * Resets any per-scenario API state stored in TestContext.
     * order = 2 → runs before the global @After
     */
    @After(value = "@api", order = 2)
    public void afterApiScenario() {
        LogUtil.getLogger(Hooks.class)
                .debug("API scenario cleanup: [{}]", ctx.getScenarioName());

        // TODO-2: Add token invalidation here if your API requires it
        // e.g. ApiUtils.post("/auth/logout", ctx.getAuthToken());
    }


    // ── @After for DB scenarios ───────────────────────────────
    /**
     * Runs after every @db scenario.
     * Rolls back any open transaction and closes the connection.
     * order = 2 → runs before the global @After
     */
    @After(value = "@db", order = 2)
    public void afterDbScenario() {
        LogUtil.getLogger(Hooks.class)
                .debug("DB scenario cleanup: [{}]", ctx.getScenarioName());

        // Rollback any open transaction to keep DB clean
        if (ctx.wasDbTransactionStarted()) {
            DBUtils.rollbackTransaction();
        }

        // Close the JDBC connection for this thread
        if (ctx.wasDbConnectionOpened()) {
            DBUtils.closeConnection();
        }
    }


    // ── Global @After — runs after EVERY scenario ─────────────
    /**
     * Runs after every scenario regardless of tags.
     * Logs the final result, clears MDC context,
     * and resets soft assertions.
     * order = 1 → runs last (after all other @After hooks)
     */
    @After(order = 1)
    public void afterScenario() {
        Scenario scenario = ctx.getScenario();

        if (scenario != null) {
            // Log final PASSED / FAILED result
            LogUtil.logScenarioEnd(scenario.getName(), scenario.isFailed());

            // TODO-3: Mark Extent Report test node pass/fail
            // if (scenario.isFailed()) {
            //     ExtentReportManager.getTest().fail("Scenario Failed");
            // } else {
            //     ExtentReportManager.getTest().pass("Scenario Passed");
            // }
        }

        // Reset soft assertions — prevents state leaking to next scenario
        CustomAssert.resetSoftAssert();

        // Clear MDC logging context for this thread
        LogUtil.clearContext();
    }


    // ═══════════════════════════════════════════════════════════
    // @AFTERSTEP — runs after every STEP (not just scenario end)
    // ═══════════════════════════════════════════════════════════

    /**
     * Runs after every individual step.
     * Currently used to attach step-level screenshots for
     * failing steps to the Cucumber report.
     *
     * Only captures if driver is active AND the step failed.
     * This avoids screenshot overhead on passing steps.
     *
     * TODO-3: Hook into Extent Report step node here once built.
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Only capture on step failure AND only for UI scenarios
        if (scenario.isFailed() && DriverManager.isDriverInitialised()) {
            try {
                byte[] screenshot =
                        ((org.openqa.selenium.TakesScreenshot) DriverManager.getDriver())
                                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);

                // Attach step failure screenshot to Cucumber HTML report
                scenario.attach(screenshot, "image/png",
                        "Step failed: " + scenario.getName());

                LogUtil.getLogger(Hooks.class)
                        .debug("Step failure screenshot attached");
            } catch (Exception e) {
                LogUtil.getLogger(Hooks.class)
                        .warn("Could not capture step screenshot: {}", e.getMessage());
            }
        }
    }

}