package runners;

/*
  ============================================================
  FILE: DbTestRunner.java
  LOCATION: src/test/java/runners/DbTestRunner.java

  PURPOSE:
    TestNG + Cucumber runner for @db tagged scenarios.
    Runs all database validation tests via JDBC.
    Thread count kept low (see testng.xml) to avoid
    exhausting the DB connection pool.

  RUN FROM MAVEN:
    mvn test -Dtest=DbTestRunner
    mvn test -Dtest=DbTestRunner -Denv=staging
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

    // Runs all @db tagged scenarios
    tags = "@db",

    plugin = {
        "pretty",
        "html:target/cucumber-reports/db/index.html",
        "json:target/cucumber-reports/db/cucumber.json",
        "junit:target/cucumber-reports/db/cucumber.xml"
    },

    monochrome = true,
    publish    = false,
    snippets   = io.cucumber.testng.CucumberOptions.SnippetType.CAMELCASE

)
public class DbTestRunner extends AbstractTestNGCucumberTests {

    // parallel = true but thread-count in testng.xml is kept at 2
    // to avoid DB connection pool exhaustion on shared environments
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

}
