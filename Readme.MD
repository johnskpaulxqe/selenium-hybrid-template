# 🧪 Selenium Cucumber Hybrid Framework Template

> **A production-ready, cloneable BDD test automation framework template.**
> Clone → configure → test. Designed to cover UI, API, and Database testing under one roof.

---

## 📋 Table of Contents

- [What This Is](#what-this-is)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Test Runners](#test-runners)
- [Running Tests](#running-tests)
- [Configuration](#configuration)
- [Reporting](#reporting)
- [CI/CD](#cicd)
- [Customising for Your Project](#customising-for-your-project)
- [Contributing](#contributing)

---

## What This Is

This is a **hybrid test automation framework template** built on Cucumber + TestNG + Selenium + RestAssured.
It is designed to be cloned once and adapted to any project with minimal effort.

**Covers three testing layers out of the box:**

| Layer    | Technology             | Purpose                            |
|----------|------------------------|------------------------------------|
| UI       | Selenium WebDriver 4   | Browser-based end-to-end tests     |
| API      | RestAssured            | REST API contract & integration    |
| Database | JDBC                   | Data validation & DB state checks  |

---

## Tech Stack

| Concern         | Library / Tool                        | Version  |
|-----------------|---------------------------------------|----------|
| BDD             | Cucumber                              | 7.x      |
| Test Framework  | TestNG                                | 7.x      |
| UI Automation   | Selenium WebDriver                    | 4.x      |
| API Testing     | RestAssured                           | 5.x      |
| Database        | JDBC (pluggable driver)               | —        |
| Build Tool      | Maven                                 | 3.8+     |
| Reporting       | Extent Reports + Cucumber HTML        | 5.x      |
| Logging         | SLF4J + Logback                       | 1.x      |
| CI/CD           | GitHub Actions + Azure Pipelines      | —        |

---

## Project Structure

```
selenium-hybrid-template/
│
├── .github/
│   ├── workflows/
│   │   └── github-actions.yml          # GitHub Actions CI pipeline
│   └── ISSUE_TEMPLATE/
│       └── bug_report.md               # Standardised bug report template
│
├── docs/
│   ├── README.md                       # This file
│   ├── CONTRIBUTING.md                 # How to contribute to this template
│   └── HOW_TO_USE.md                   # Step-by-step adoption guide
│
├── src/
│   └── test/
│       ├── java/
│       │   ├── base/
│       │   │   └── BaseTest.java               # TestNG base — driver lifecycle
│       │   │
│       │   ├── context/
│       │   │   └── TestContext.java            # Cucumber scenario-scoped shared state
│       │   │
│       │   ├── hooks/
│       │   │   └── Hooks.java                  # @Before / @After Cucumber hooks
│       │   │
│       │   ├── pages/
│       │   │   ├── BasePage.java               # Shared page methods + driver access
│       │   │   ├── LoginPage.java              # Example: login page POM
│       │   │   └── SamplePage.java             # Example: generic sample page POM
│       │   │
│       │   ├── repository/
│       │   │   ├── BaseRepository.java         # JDBC query helpers
│       │   │   └── SampleRepository.java       # Example: DB query class
│       │   │
│       │   ├── runners/
│       │   │   ├── SmokeTestRunner.java        # Runs @smoke tagged scenarios
│       │   │   ├── RegressionTestRunner.java   # Runs @regression tagged scenarios
│       │   │   ├── ApiTestRunner.java          # Runs @api tagged scenarios
│       │   │   ├── DbTestRunner.java           # Runs @db tagged scenarios
│       │   │   └── FullSuiteRunner.java        # Runs all scenarios (no tag filter)
│       │   │
│       │   ├── stepdefinitions/
│       │   │   ├── LoginSteps.java             # Steps for login.feature
│       │   │   ├── ApiSteps.java               # Steps for api.feature
│       │   │   ├── DbSteps.java                # Steps for db.feature
│       │   │   └── SampleSteps.java            # Steps for ui.feature
│       │   │
│       │   └── utils/
│       │         ├── ApiUtils.java             # RestAssured base helpers
│       │         ├── ConfigReader.java         # Reads config.json
│       │         ├── CustomAssert.java         # Wrapped assertions with logging
│       │         ├── DBUtils.java              # JDBC connection + execution
│       │         ├── DriverManager.java        # ThreadLocal WebDriver factory
│       │         ├── ExtentReportManager.java  # Extent Reports lifecycle
│       │         ├── JavaScriptUtils.java      # JS executor helpers
│       │         ├── JsonReader.java           # Generic JSON file reader
│       │         ├── Locators.java             # Central By locator store
│       │         ├── LogUtil.java              # SLF4J logger factory wrapper
│       │         ├── PageVerifier.java         # Reusable page-state assertions
│       │         ├── RequestBuilder.java       # Builds RestAssured RequestSpec
│       │         ├── ResponseValidator.java    # RestAssured response checks
│       │         ├── ScreenshotUtils.java      # Screenshot capture + attachment
│       │         ├── TestDataLoader.java       # Loads test data from JSON files
│       │         └── WaitUtils.java            # Explicit / fluent wait helpers
│       │
│       └── resources/
│             ├── config.json                   # Single config source for all environments
│             ├── logback.xml                   # Logback logging configuration
│             ├── extent-config.xml             # Extent Reports theme + settings
│             ├── features/
│             │     ├── login.feature           # Login scenarios (@smoke, @regression)
│             │     ├── ui.feature              # UI scenarios (@smoke, @regression)
│             │     ├── api.feature             # API scenarios (@api)
│             │     └── db.feature              # DB scenarios (@db)
│             └── testdata/
│                   ├── users.json              # User credential test data
│                   ├── apidata.json            # API request/response test data
│                   └── dbdata.json             # Expected DB state test data
│
├── pom.xml                                     # Maven build + all dependencies
├── testng.xml                                  # TestNG suite — wires all runners
└── azure-pipelines.yml                         # Azure DevOps CI pipeline
```

---

## Quick Start

### Prerequisites

| Tool        | Minimum Version |
|-------------|-----------------|
| Java        | 11              |
| Maven       | 3.8             |
| Chrome      | Latest stable   |
| Git         | 2.x             |

### Clone and configure

```bash
# 1. Clone the template
git clone https://github.com/your-org/selenium-hybrid-template.git
cd selenium-hybrid-template

# 2. Set your base URL and environment in config.json
#    (src/test/resources/config.json)

# 3. Run smoke tests to verify setup
mvn test -Dcucumber.filter.tags="@smoke"
```

---

## Test Runners

This framework provides **five dedicated runners**. Each runner maps to a tag,
making it trivial to run exactly the suite you need — locally or in CI.

| Runner Class            | Tag Filter      | Purpose                                  |
|-------------------------|-----------------|------------------------------------------|
| `SmokeTestRunner`       | `@smoke`        | Fast sanity checks — critical path only  |
| `RegressionTestRunner`  | `@regression`   | Full regression suite                    |
| `ApiTestRunner`         | `@api`          | All API contract tests                   |
| `DbTestRunner`          | `@db`           | All database validation tests            |
| `FullSuiteRunner`       | *(none)*        | Entire test suite — used in nightly CI   |

### Tag your scenarios

```gherkin
@smoke @regression
Scenario: Successful login with valid credentials
  Given I am on the login page
  When I enter valid credentials
  Then I should be on the dashboard

@api
Scenario: GET /users returns 200
  Given the API base URL is configured
  When I send a GET request to "/users"
  Then the response status should be 200

@db
Scenario: User record exists in database
  Given a database connection is established
  When I query the users table for "testuser@example.com"
  Then the record should exist
```

### Running a specific runner via Maven

```bash
# Smoke suite only
mvn test -Dtest=SmokeTestRunner

# API suite only
mvn test -Dtest=ApiTestRunner

# Database suite only
mvn test -Dtest=DbTestRunner

# Regression suite only
mvn test -Dtest=RegressionTestRunner

# Full suite
mvn test -Dtest=FullSuiteRunner

# CLI tag override (works on any runner, overrides its default tag)
mvn test -Dcucumber.filter.tags="@smoke and @regression"
```

---

## Running Tests

### Run by tag (without specifying a runner)

```bash
mvn test -Dcucumber.filter.tags="@smoke"
mvn test -Dcucumber.filter.tags="@api"
mvn test -Dcucumber.filter.tags="@smoke or @api"
mvn test -Dcucumber.filter.tags="@regression and not @wip"
```

### Run with a specific browser

```bash
mvn test -Dbrowser=chrome
mvn test -Dbrowser=firefox
mvn test -Dbrowser=edge
mvn test -Dbrowser=headless   # Headless Chrome — default in CI
```

### Run with a specific environment

```bash
mvn test -Denv=dev
mvn test -Denv=staging
mvn test -Denv=prod
```

### Run in parallel (thread count controlled in testng.xml)

```bash
mvn test -Dthreads=4
```

---

## Configuration

All environment and framework settings live in a **single file**:

```
src/test/resources/config.json
```

```json
{
  "environments": {
    "dev": {
      "baseUrl": "https://dev.your-app.com",
      "apiBaseUrl": "https://api.dev.your-app.com",
      "dbUrl": "jdbc:postgresql://dev-db:5432/yourdb",
      "dbUser": "dev_user",
      "dbPassword": "dev_password"
    },
    "staging": {
      "baseUrl": "https://staging.your-app.com",
      "apiBaseUrl": "https://api.staging.your-app.com",
      "dbUrl": "jdbc:postgresql://staging-db:5432/yourdb",
      "dbUser": "staging_user",
      "dbPassword": "staging_password"
    }
  },
  "browser": "headless",
  "activeEnv": "dev",
  "implicitWait": 0,
  "explicitWait": 10,
  "screenshotOnFailure": true,
  "extentReportPath": "target/extent-reports/report.html"
}
```

> **TODO:** Replace all placeholder URLs, credentials, and DB connection strings with your project values.  
> **Security note:** Never commit real credentials. Use environment variables or a secrets manager in CI.

---

## Reporting

Three report types are generated automatically after every test run:

| Report              | Location                                     | When generated        |
|---------------------|----------------------------------------------|-----------------------|
| Extent HTML Report  | `target/extent-reports/report.html`          | After every run       |
| Cucumber HTML       | `target/cucumber-reports/index.html`         | After every run       |
| TestNG Report       | `target/surefire-reports/`                   | After every run       |

Screenshots of failing scenarios are **automatically embedded** in the Extent Report.

---

## CI/CD

### GitHub Actions

Push to `main` or open a PR → smoke tests run automatically.

```yaml
# Triggered on: push to main, pull requests
# Runs: SmokeTestRunner (fast feedback)
# Full regression: nightly schedule
```

See: `.github/workflows/github-actions.yml`

### Azure Pipelines

```yaml
# Triggered on: PR to main
# Stages: Build → Smoke → Regression
# Publishes: Cucumber HTML + Extent reports as pipeline artifacts
```

See: `azure-pipelines.yml`

---

## Customising for Your Project

When you clone this template, work through this checklist:

- [ ] Update `config.json` with your application's URLs and DB details
- [ ] Replace `LoginPage.java` and `SamplePage.java` with your own page objects
- [ ] Replace example `.feature` files with your own scenarios
- [ ] Tag your scenarios with `@smoke`, `@regression`, `@api`, `@db` as appropriate
- [ ] Update `testdata/users.json` with your own test users
- [ ] Update `Locators.java` with your application's actual element locators
- [ ] Set the correct JDBC driver dependency in `pom.xml` for your database
- [ ] Configure secrets (DB passwords, API keys) as CI environment variables
- [ ] Update `README.md` with your project name and details

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on raising issues, submitting PRs, and the branching strategy.

---

## License

MIT — free to use, adapt, and distribute.

---

*Built with ❤️ as a reusable template. Replace this footer with your team/project name.*