package runners;

/*
  ============================================================
  FILE: SmokeTestRunner.java
  LOCATION: src/test/java/runners/SmokeTestRunner.java

  PURPOSE:
    TestNG + Cucumber runner for @smoke tagged scenarios only.
    Designed for fast feedback — runs the critical path
    scenarios on every push and every pull request.

  RUN FROM MAVEN:
    mvn test -Dtest=SmokeTestRunner
    mvn test -Dtest=SmokeTestRunner -Dbrowser=headless
    mvn test -Dtest=SmokeTestRunner -Denv=staging

  OVERRIDE TAGS AT RUNTIME:
    mvn test -Dtest=SmokeTestRunner -Dcucumber.filter.tags="@smoke and not @wip"

  REPORTS GENERATED:
    target/cucumber-reports/smoke/index.html
    target/extent-reports/report_<timestamp>.html
    target/surefire-reports/

  TODO (customise per project):
    - TODO-1 : features path is correct — do not change unless
               you move your .feature files
    - TODO-2 : glue packages must include all packages that
               contain step definitions and hooks
    - TODO-3 : add plugin entries for any additional reporters
  ============================================================
*/

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

@CucumberOptions(

    // ── Feature files location ────────────────────────────────
    // TODO-1: Update path if you move your feature files
    features = "src/test/resources/features",

    // ── Step definitions + hooks packages ────────────────────
    // TODO-2: Add any additional packages containing steps
    glue = {
        "hooks",
        "stepdefinitions"
    },

    // ── Tag filter ────────────────────────────────────────────
    // Runs only @smoke scenarios
    // Override at runtime: -Dcucumber.filter.tags="@smoke and not @wip"
    tags = "@smoke",

    // ── Reporters ─────────────────────────────────────────────
    // pretty    → readable console output
    // html      → Cucumber HTML report
    // json      → machine-readable for CI integration
    // TODO-3: Add "rerun:target/rerun/smoke-rerun.txt" to capture
    //         failed scenarios for re-run
    plugin = {
        "pretty",
        "html:target/cucumber-reports/smoke/index.html",
        "json:target/cucumber-reports/smoke/cucumber.json",
        "junit:target/cucumber-reports/smoke/cucumber.xml"
    },

    // ── Monochrome ────────────────────────────────────────────
    // true → strips ANSI colour codes from console output
    // Set to false if your IDE supports colour output
    monochrome = true,

    // ── Publish ───────────────────────────────────────────────
    // true → publishes report to cucumber.io (requires internet)
    // Keep false for air-gapped CI environments
    publish = false,

    // ── Snippet style ─────────────────────────────────────────
    // CAMELCASE → generates camelCase method names for missing steps
    snippets = io.cucumber.testng.CucumberOptions.SnippetType.CAMELCASE

)
public class SmokeTestRunner extends AbstractTestNGCucumberTests {

    /**
     * Enables parallel scenario execution within this runner.
     * Thread count is controlled by testng.xml thread-count setting.
     *
     * @Override is required to activate parallel mode.
     * Remove @Override to run scenarios sequentially.
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

}
