# HOW TO USE — Adoption Guide

This guide walks you through adapting this template for a brand-new project from scratch. Follow the steps in order.

---

## Table of Contents

- [Step 1 — Clone and Rename](#step-1--clone-and-rename)
- [Step 2 — Configure Your Environment](#step-2--configure-your-environment)
- [Step 3 — Configure the Database](#step-3--configure-the-database)
- [Step 4 — Replace Test Data](#step-4--replace-test-data)
- [Step 5 — Add Your Locators](#step-5--add-your-locators)
- [Step 6 — Build Your Page Objects](#step-6--build-your-page-objects)
- [Step 7 — Write Your Feature Files](#step-7--write-your-feature-files)
- [Step 8 — Run Your First Test](#step-8--run-your-first-test)
- [Step 9 — Set Up CI/CD](#step-9--set-up-cicd)
- [Step 10 — Tagging Strategy](#step-10--tagging-strategy)
- [Troubleshooting](#troubleshooting)
- [Quick Reference](#quick-reference)

---

## Step 1 — Clone and Rename

```bash
# Clone the template
git clone https://github.com/your-org/selenium-hybrid-template.git your-project-name
cd your-project-name

# Remove the template's git history and start fresh
rm -rf .git
git init
git remote add origin https://github.com/your-org/your-project-name.git
```

Open `pom.xml` and update the project identity:

```xml
<!-- TODO-1 in pom.xml -->
<groupId>com.yourcompany</groupId>
<artifactId>your-project-name</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

Open `logback.xml` and update the package name:
```xml
<!-- Match your groupId -->
<logger name="com.yourcompany" level="${LOG_LEVEL}" ...>
```

---

## Step 2 — Configure Your Environment

Open `src/test/resources/config.json` and replace all placeholder values:

```json
{
  "activeEnv": "dev",
  "environments": {
    "dev": {
      "baseUrl":    "https://dev.your-real-app.com",
      "apiBaseUrl": "https://api.dev.your-real-app.com",
      "db": {
        "url":         "jdbc:mysql://localhost:3306/your_dev_db",
        "username":    "dev_user",
        "password":    "dev_password",
        "driverClass": "com.mysql.cj.jdbc.Driver"
      }
    },
    "staging": {
      "baseUrl":    "https://staging.your-real-app.com",
      "apiBaseUrl": "https://api.staging.your-real-app.com",
      "db": {
        "url":      "jdbc:mysql://staging-db:3306/your_staging_db",
        "username": "staging_user",
        "password": "staging_password"
      }
    }
  }
}
```

**Security rule:** Never commit real passwords. For CI, use `$DB_PASS` as the value and inject it via environment variables.


---

### The concept

Instead of putting your real password in `config.json`, you put a placeholder starting with `$`:

```json
"db": {
  "url":      "$DB_URL",
  "username": "$DB_USER",
  "password": "$DB_PASS"
}
```

Our `ConfigReader.java` already handles this — when it sees a value starting with `$`, it automatically reads it from the system environment instead:

```java
// Already in ConfigReader.java resolveEnvVar() method
if (value.startsWith("$")) {
    String varName = value.substring(1);       // strips the $
    return System.getenv(varName);             // reads from environment
}
```

---

### Setting it up in each environment

**On your local Mac — set in your `~/.zshrc`:**
```bash
export DB_URL=jdbc:mysql://localhost:3306/fredweb_dev
export DB_USER=your_db_username
export DB_PASS=your_real_password

# Reload
source ~/.zshrc
```

Verify it worked:
```bash
echo $DB_PASS
# should print your password
```

---

**On GitHub Actions — add as repository secrets:**

1. Go to your repo on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** for each one:

```
Name:  DB_URL
Value: jdbc:mysql://your-db-host:3306/fredweb_dev

Name:  DB_USER
Value: your_db_username

Name:  DB_PASS
Value: your_real_password
```

The pipeline already passes them in via `-DDB_URL=${{ secrets.DB_URL }}` in `github-actions.yml` — so nothing else needs changing there.

---

**On Azure DevOps — add as pipeline variables:**

1. Open your pipeline → **Edit** → **Variables**
2. Add each one and tick **Keep this value secret**:
```
DB_URL  = jdbc:mysql://your-db-host:3306/fredweb_dev
DB_USER = your_db_username
DB_PASS = your_real_password
```

---

### So your `config.json` for prod looks like this

```json
"prod": {
  "baseUrl":    "https://fredweb.phdl.pitt.edu",
  "apiBaseUrl": "https://api.fredweb.phdl.pitt.edu",
  "db": {
    "url":         "$DB_URL",
    "username":    "$DB_USER",
    "password":    "$DB_PASS",
    "driverClass": "com.mysql.cj.jdbc.Driver"
  }
}
```

This way the file is safe to commit — there are no real credentials in it anywhere.

---

### The flow at runtime

```
config.json value "$DB_PASS"
        │
        ▼
ConfigReader.resolveEnvVar()
        │
        ├── Local Mac   → reads System.getenv("DB_PASS") from ~/.zshrc
        ├── GitHub CI   → reads from repository secret DB_PASS
        └── Azure CI    → reads from pipeline variable DB_PASS
```

---

## Step 3 — Configure the Database

### If you are using MySQL (already configured)
Just update the connection values in `config.json` — the MySQL driver is already in `pom.xml`.

### If you are using a different database
Replace the MySQL driver in `pom.xml` with yours:

**PostgreSQL:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

**Oracle:**
```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <version>23.4.0.24.05</version>
</dependency>
```

**SQL Server:**
```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.6.2.jre11</version>
</dependency>
```

Then update `driverClass` in `config.json`:
```json
"driverClass": "org.postgresql.Driver"
```

---

## Step 4 — Replace Test Data

### users.json
Replace the placeholder users with your application's real test accounts:

```json
{
  "admin": {
    "username": "admin@your-real-app.com",
    "password": "YourRealPassword",
    "email":    "admin@your-real-app.com",
    "role":     "ADMIN"
  },
  "standard": {
    "username": "user@your-real-app.com",
    "password": "YourRealPassword",
    "email":    "user@your-real-app.com",
    "role":     "USER"
  }
}
```

### apidata.json
Replace the placeholder endpoints with your real API paths:

```json
{
  "endpoints": {
    "login":     "/your/real/auth/endpoint",
    "getUsers":  "/your/real/users/endpoint",
    "createUser":"/your/real/users/endpoint"
  }
}
```

### dbdata.json
Replace placeholder values with your real schema and expected data:

```json
{
  "tables": {
    "users": "your_actual_users_table"
  },
  "queries": {
    "findUserByEmail": "SELECT * FROM your_actual_users_table WHERE email = ?"
  }
}
```

---

## Step 5 — Add Your Locators

Open `src/test/java/utils/Locators.java`.

For each page in your application, add a static inner class:

```java
// Example: adding locators for a User Management page
public static class UserManagement {

    public static final By ADD_USER_BUTTON =
            By.cssSelector("[data-testid='add-user-btn']");

    public static final By USER_TABLE_ROWS =
            By.cssSelector("table.users-table tbody tr");

    public static final By SEARCH_INPUT =
            By.id("userSearch");
}
```

### Finding the right locator

Priority order (most stable to least):

1. `By.id("elementId")` — use whenever available
2. `By.cssSelector("[data-testid='value']")` — ask your devs to add `data-testid` attributes
3. `By.cssSelector(".className")` — for stable class names
4. `By.xpath("//button[text()='Submit']")` — last resort only

---

## Step 6 — Build Your Page Objects

For each page in your application, create a new file in `src/test/java/pages/`.

Copy `SamplePage.java` as a starting template and rename it:

```java
package pages;

public class UserManagementPage extends BasePage {

    // Navigation
    public void navigateToPage() {
        navigateToPath("/admin/users");
        waitForInvisible(Locators.Common.LOADING_SPINNER);
    }

    // Actions
    public void clickAddUser() {
        click(Locators.UserManagement.ADD_USER_BUTTON);
    }

    public void searchForUser(String email) {
        type(Locators.UserManagement.SEARCH_INPUT, email);
    }

    // State checks
    public int getUserTableRowCount() {
        return getElementCount(Locators.UserManagement.USER_TABLE_ROWS);
    }

    // Verifications
    public void verifyUserInTable(String email) {
        PageVerifier.verifyElementDisplayed(driver(),
                Locators.rowContainingText(email));
    }
}
```

Then register it in `TestContext.java`:

```java
// In TestContext.java
private UserManagementPage userManagementPage;

public UserManagementPage getUserManagementPage() {
    if (userManagementPage == null) {
        userManagementPage = new UserManagementPage();
    }
    return userManagementPage;
}
```

---

## Step 7 — Write Your Feature Files

Create a new `.feature` file in `src/test/resources/features/` for each feature area:

```gherkin
@ui
Feature: User Management
  As an admin user
  I want to manage application users
  So that I can control access to the system

  Background:
    Given I am logged in as "admin"

  @smoke @regression
  Scenario: Admin can view user list
    When I navigate to the user management page
    Then the user table should be displayed

  @regression
  Scenario: Admin can search for a user
    When I search for user "john@example.com"
    Then the user "john@example.com" should appear in the results
```

For each new Gherkin step, add the matching step definition method to the appropriate class in `stepdefinitions/`.

---

## Step 8 — Run Your First Test

```bash
# 1. Compile first — fix any import errors before running
mvn test-compile

# 2. Run a single scenario by tag to test your setup
mvn test -Dtest=SmokeTestRunner -Dbrowser=headless -Denv=dev

# 3. Check the reports
#    Cucumber HTML: target/cucumber-reports/smoke/index.html
#    Extent HTML:   target/extent-reports/report_<timestamp>.html
#    Log file:      target/logs/framework.log
```

### Common first-run issues

| Error | Likely cause | Fix |
|---|---|---|
| `config.json not found` | Wrong resource path | Check `src/test/resources/config.json` exists |
| `baseUrl not configured` | Placeholder still in config | Update `config.json` with real URLs |
| `WebDriver null` | Driver not initialised | Check scenario is tagged `@smoke` or `@ui` |
| `StaleElementReferenceException` | Page reloaded between find and use | Use `WaitUtils.fluentWait()` instead |
| `NoSuchElementException` | Wrong locator | Verify in browser DevTools, update `Locators.java` |
| `Connection refused` DB | DB not reachable | Check `db.url` in `config.json` |

---

## Step 9 — Set Up CI/CD

### GitHub Actions

1. Push your code to GitHub
2. Go to Settings → Secrets → Actions
3. Add these secrets:
   ```
   DB_URL   = jdbc:mysql://your-db:3306/your_db
   DB_USER  = your_username
   DB_PASS  = your_password
   ```
4. The workflow triggers automatically on push to `main`

### Azure DevOps

1. Create a new pipeline in Azure DevOps
2. Point it to `azure-pipelines.yml` in your repo root
3. Go to Pipeline → Variables and add:
   ```
   DB_URL   (mark as secret)
   DB_USER  (mark as secret)
   DB_PASS  (mark as secret)
   ```
4. Run the pipeline manually first to verify

---

## Step 10 — Tagging Strategy

Tag every scenario with at least one tag so it is picked up by the correct runner.

| Tag | Runner | When it runs |
|---|---|---|
| `@smoke` | `SmokeTestRunner` | Every push, every PR |
| `@regression` | `RegressionTestRunner` | Nightly |
| `@api` | `ApiTestRunner` | Every push, every PR |
| `@db` | `DbTestRunner` | Nightly |
| `@wip` | None | Never — excluded from all runners |

### Tagging guidelines

- New scenarios start life as `@wip` during development
- Remove `@wip` and add real tags before raising a PR
- Login and core navigation → `@smoke @regression`
- Full feature coverage → `@regression`
- API contract tests → `@api`
- DB state validation → `@db`
- A scenario can have multiple tags: `@smoke @regression` is valid

---

## Quick Reference

```bash
# Run by runner
mvn test -Dtest=SmokeTestRunner
mvn test -Dtest=RegressionTestRunner
mvn test -Dtest=ApiTestRunner
mvn test -Dtest=DbTestRunner
mvn test -Dtest=FullSuiteRunner

# Run by tag
mvn test -Dcucumber.filter.tags="@smoke"
mvn test -Dcucumber.filter.tags="@regression and not @wip"
mvn test -Dcucumber.filter.tags="@smoke or @api"

# Run with options
mvn test -Dtest=SmokeTestRunner -Dbrowser=chrome
mvn test -Dtest=SmokeTestRunner -Dbrowser=headless
mvn test -Dtest=SmokeTestRunner -Denv=staging
mvn test -Dtest=SmokeTestRunner -Dlog.level=DEBUG

# Compile only
mvn test-compile -DskipTests

# View reports after run
open target/cucumber-reports/smoke/index.html
open target/extent-reports/report_*.html
```

---

## File Locations Reference

```
src/test/
  ├── java/
  │   ├── base/
  │   │   └── BaseTest.java              ← TestNG suite lifecycle
  │   ├── context/
  │   │   └── TestContext.java           ← Scenario-scoped shared state
  │   ├── hooks/
  │   │   └── Hooks.java                 ← @Before / @After
  │   ├── pages/
  │   │   ├── BasePage.java              ← Base for all page objects
  │   │   ├── LoginPage.java             ← Login page interactions
  │   │   └── SamplePage.java            ← Template for your pages
  │   ├── repository/
  │   │   ├── BaseRepository.java        ← DB query helpers
  │   │   └── SampleRepository.java      ← Template for your repos
  │   ├── runners/
  │   │   ├── SmokeTestRunner.java       ← @smoke
  │   │   ├── RegressionTestRunner.java  ← @regression
  │   │   ├── ApiTestRunner.java         ← @api
  │   │   ├── DbTestRunner.java          ← @db
  │   │   └── FullSuiteRunner.java       ← all
  │   ├── stepdefinitions/
  │   │   ├── LoginSteps.java
  │   │   ├── ApiSteps.java
  │   │   ├── DbSteps.java
  │   │   └── SampleSteps.java
  │   └── utils/
  │       ├── ApiUtils.java              ← RestAssured HTTP helpers
  │       ├── ConfigReader.java          ← Reads config.json
  │       ├── CucumberReportUtils.java   ← Cucumber report attachment
  │       ├── CustomAssert.java          ← Logged assertions
  │       ├── DBUtils.java               ← JDBC connection + queries
  │       ├── DriverManager.java         ← ThreadLocal WebDriver
  │       ├── ExtentReportManager.java   ← Extent Reports lifecycle
  │       ├── JavaScriptUtils.java       ← JS executor helpers
  │       ├── JsonReader.java            ← JSON file parsing
  │       ├── Locators.java              ← All By locators
  │       ├── LogUtil.java               ← SLF4J logger wrapper
  │       ├── PageVerifier.java          ← Page state assertions
  │       ├── RequestBuilder.java        ← Fluent API request builder
  │       ├── ResponseValidator.java     ← API response assertions
  │       ├── ScreenshotUtils.java       ← Screenshot capture
  │       ├── TestDataLoader.java        ← Test data access
  │       └── WaitUtils.java             ← Explicit waits
  └── resources/
      ├── config.json                    ← All environment config
      ├── extent-config.xml              ← Report theme settings
      ├── logback.xml                    ← Logging config
      ├── features/
      │   ├── login.feature
      │   ├── ui.feature
      │   ├── api.feature
      │   └── db.feature
      └── testdata/
          ├── users.json
          ├── apidata.json
          └── dbdata.json
```
