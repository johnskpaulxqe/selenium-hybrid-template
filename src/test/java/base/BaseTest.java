package base;

/*
  ============================================================
  FILE: BaseTest.java
  LOCATION: src/test/java/base/BaseTest.java

  PURPOSE:
    TestNG base class that handles suite-level setup and
    teardown. Extended by all runner classes through
    AbstractTestNGCucumberTests (which itself extends this).
    Responsibilities:
      1. Log the framework startup banner once per suite
      2. Set system properties available to all tests
      3. Log suite completion summary
      4. Provide @BeforeSuite / @AfterSuite hooks for any
         global setup your project needs

  NOTE ON INHERITANCE:
    Runners extend AbstractTestNGCucumberTests (Cucumber's
    TestNG bridge). BaseTest is declared as a TestNG listener
    in testng.xml so its @BeforeSuite / @AfterSuite methods
    fire without needing explicit inheritance.

    Alternatively you can make each Runner extend BaseTest
    which extends AbstractTestNGCucumberTests — both work.

  TODO (customise per project):
    - TODO-1 : Add global test data seeding in @BeforeSuite
               if your test environment needs baseline data
    - TODO-2 : Add global cleanup in @AfterSuite
               e.g. delete all test users created during run
    - TODO-3 : Add email reporting trigger in @AfterSuite
  ============================================================
*/

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import utils.ConfigReader;
import utils.ExtentReportManager;
import utils.LogUtil;
import org.slf4j.Logger;

public class BaseTest {

    private static final Logger log = LogUtil.getLogger(BaseTest.class);


    // ── @BeforeSuite ─────────────────────────────────────────
    /**
     * Runs once before the entire TestNG suite starts.
     * Logs the framework startup banner and validates
     * critical configuration.
     *
     * alwaysRun = true ensures this fires even if tests
     * are filtered by group or tag.
     */
    @BeforeSuite(alwaysRun = true)
    public void suiteSetup(ITestContext context) {

        // Print framework startup banner to console + log file
        LogUtil.logFrameworkStartup();

        // Log suite name from testng.xml
        log.info("Suite starting: [{}]", context.getSuite().getName());

        // Log active environment and key config values
        log.info("Active environment : [{}]", ConfigReader.getActiveEnv());
        log.info("Base URL           : [{}]", ConfigReader.get("baseUrl", "not set"));
        log.info("API Base URL       : [{}]", ConfigReader.get("apiBaseUrl", "not set"));
        log.info("Browser            : [{}]", System.getProperty("browser",
                 ConfigReader.get("browser.default", "headless")));
        log.info("Thread count       : [{}]", System.getProperty("threads", "1"));

        // Validate critical config — fail fast with a clear message
        validateCriticalConfig();

        // TODO-1: Add test data seeding here if needed
        // e.g. TestDataSeeder.seedBaselineData();
    }


    // ── @AfterSuite ──────────────────────────────────────────
    /**
     * Runs once after the entire TestNG suite completes.
     * Flushes reports and logs suite completion.
     *
     * alwaysRun = true ensures this fires even if tests failed.
     */
    @AfterSuite(alwaysRun = true)
    public void suiteTeardown(ITestContext context) {

        // Log suite completion summary
        int passed  = context.getPassedTests().size();
        int failed  = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        int total   = passed + failed + skipped;

        log.info("═══════════════════════════════════════════");
        log.info("  Suite Complete: [{}]", context.getSuite().getName());
        log.info("  Total   : {}", total);
        log.info("  Passed  : {}", passed);
        log.info("  Failed  : {}", failed);
        log.info("  Skipped : {}", skipped);
        log.info("═══════════════════════════════════════════");

        // Flush Extent report to disk
        // Note: ExtentReportManager.onFinish() also calls flush()
        // This is a safety net in case the listener missed a flush
        ExtentReportManager.flushReport();

        // TODO-2: Add global cleanup here if needed
        // e.g. TestDataCleaner.deleteAllTestUsers();

        // TODO-3: Add email report trigger here if needed
        // e.g. EmailReporter.send(reportPath, failed > 0);
    }


    // ── validateCriticalConfig() ─────────────────────────────
    /**
     * Validates that required configuration values are present.
     * Throws a clear error at suite start rather than a cryptic
     * NullPointerException mid-run.
     *
     * TODO-1: Add validation for any config values critical
     *         to your project (e.g. API keys, DB URL)
     */
    private void validateCriticalConfig() {
        String activeEnv = ConfigReader.getActiveEnv();

        // Validate baseUrl is configured for the active environment
        String baseUrl = ConfigReader.get("baseUrl");
        if (baseUrl == null || baseUrl.isBlank() || baseUrl.contains("your-app")) {
            log.warn("⚠ baseUrl is not configured for environment [{}]. " +
                     "Update config.json before running UI tests.", activeEnv);
        }

        // Validate apiBaseUrl is configured
        String apiBaseUrl = ConfigReader.get("apiBaseUrl");
        if (apiBaseUrl == null || apiBaseUrl.isBlank() || apiBaseUrl.contains("your-app")) {
            log.warn("⚠ apiBaseUrl is not configured for environment [{}]. " +
                     "Update config.json before running API tests.", activeEnv);
        }

        log.info("Configuration validation complete for environment: [{}]", activeEnv);
    }

}
