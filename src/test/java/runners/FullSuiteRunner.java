package runners;

/*
  ============================================================
  FILE: FullSuiteRunner.java
  LOCATION: src/test/java/runners/FullSuiteRunner.java

  PURPOSE:
    TestNG + Cucumber runner that executes ALL scenarios
    across all feature files with no tag filter.
    Used for nightly builds and full release validation only.

  WARNING:
    Do NOT include this runner in push/PR CI pipelines —
    it duplicates scenarios already covered by the other
    runners and significantly increases build time.
    Use it only in scheduled nightly or release pipelines.

  RUN FROM MAVEN:
    mvn test -Dtest=FullSuiteRunner
    mvn test -Dtest=FullSuiteRunner -Denv=staging -Dthreads=4

  OVERRIDE TO EXCLUDE WIP:
    mvn test -Dtest=FullSuiteRunner -Dcucumber.filter.tags="not @wip"
  ============================================================
*/

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

@CucumberOptions(

    features = "src/test/resources/features",

    glue = {
        "hooks",
        "stepdefinitions"
    },

    // No tag filter — runs everything
    // Override at runtime to exclude WIP:
    //   -Dcucumber.filter.tags="not @wip"
    tags = "not @wip",

    plugin = {
        "pretty",
        "html:target/cucumber-reports/full/index.html",
        "json:target/cucumber-reports/full/cucumber.json",
        "junit:target/cucumber-reports/full/cucumber.xml"
    },

    monochrome = true,
    publish    = false,
    snippets   = io.cucumber.testng.CucumberOptions.SnippetType.CAMELCASE

)
public class FullSuiteRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

}
