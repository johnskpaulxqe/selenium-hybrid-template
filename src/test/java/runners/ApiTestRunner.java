package runners;

/*
  ============================================================
  FILE: ApiTestRunner.java
  LOCATION: src/test/java/runners/ApiTestRunner.java

  PURPOSE:
    TestNG + Cucumber runner for @api tagged scenarios.
    Runs all REST API tests — no browser needed so these
    run fast and are safe to parallelise aggressively.

  RUN FROM MAVEN:
    mvn test -Dtest=ApiTestRunner
    mvn test -Dtest=ApiTestRunner -Denv=staging
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

    // Runs all @api tagged scenarios
    tags = "@api",

    plugin = {
        "pretty",
        "html:target/cucumber-reports/api/index.html",
        "json:target/cucumber-reports/api/cucumber.json",
        "junit:target/cucumber-reports/api/cucumber.xml"
    },

    monochrome = true,
    publish    = false,
    snippets   = io.cucumber.testng.CucumberOptions.SnippetType.CAMELCASE

)
public class ApiTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

}
