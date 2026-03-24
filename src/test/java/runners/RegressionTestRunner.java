package runners;

/*
  ============================================================
  FILE: RegressionTestRunner.java
  LOCATION: src/test/java/runners/RegressionTestRunner.java

  PURPOSE:
    TestNG + Cucumber runner for @regression tagged scenarios.
    Runs the full regression suite — used for nightly builds
    and release validation.

  RUN FROM MAVEN:
    mvn test -Dtest=RegressionTestRunner
    mvn test -Dtest=RegressionTestRunner -Denv=staging
    mvn test -Dtest=RegressionTestRunner -Dthreads=4

  OVERRIDE TAGS AT RUNTIME:
    mvn test -Dtest=RegressionTestRunner -Dcucumber.filter.tags="@regression and not @wip"
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

    // Runs all @regression scenarios
    // Excludes @wip (work-in-progress) scenarios by convention
    tags = "@regression and not @wip",

    plugin = {
        "pretty",
        "html:target/cucumber-reports/regression/index.html",
        "json:target/cucumber-reports/regression/cucumber.json",
        "junit:target/cucumber-reports/regression/cucumber.xml"
    },

    monochrome = true,
    publish    = false,
    snippets   = io.cucumber.testng.CucumberOptions.SnippetType.CAMELCASE

)
public class RegressionTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

}
