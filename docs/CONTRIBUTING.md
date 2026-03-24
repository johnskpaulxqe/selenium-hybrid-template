# Contributing to Selenium Hybrid Framework Template

Thank you for taking the time to contribute. This document outlines the guidelines for raising issues, submitting pull requests, and maintaining code quality across the framework.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Report a Bug](#how-to-report-a-bug)
- [How to Suggest an Enhancement](#how-to-suggest-an-enhancement)
- [Development Setup](#development-setup)
- [Branching Strategy](#branching-strategy)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Commit Message Format](#commit-message-format)
- [Adding New Tests](#adding-new-tests)
- [Adding New Utilities](#adding-new-utilities)

---

## Code of Conduct

Be respectful and constructive in all interactions. Contributions of all sizes are welcome — from fixing a typo to adding a new testing layer.

---

## How to Report a Bug

Use the **Bug Report** issue template available under **Issues → New Issue**.

Before raising a new issue:
- Search existing issues to avoid duplicates
- Confirm the bug exists on the `main` branch
- Include the full stack trace from `target/logs/framework.log`

Required information:
- Java version (`java -version`)
- Maven version (`mvn -version`)
- Browser and version
- Which runner was active (`SmokeTestRunner`, `ApiTestRunner` etc.)
- Steps to reproduce
- Expected vs actual behaviour

---

## How to Suggest an Enhancement

Open an issue with the label `enhancement`. Describe:
- The problem you are solving
- The proposed solution
- Any alternatives you considered
- Whether it is a breaking change

---

## Development Setup

```bash
# 1. Fork the repository on GitHub

# 2. Clone your fork
git clone https://github.com/YOUR-USERNAME/selenium-hybrid-template.git
cd selenium-hybrid-template

# 3. Add the upstream remote
git remote add upstream https://github.com/your-org/selenium-hybrid-template.git

# 4. Create a feature branch
git checkout -b feature/your-feature-name

# 5. Configure your local environment
#    Update src/test/resources/config.json with your local URLs

# 6. Compile and verify the project builds cleanly
mvn test-compile

# 7. Run smoke tests to confirm setup
mvn test -Dtest=SmokeTestRunner -Dbrowser=headless
```

### Prerequisites

| Tool    | Minimum version |
|---------|-----------------|
| Java    | 25 (OpenJDK)    |
| Maven   | 3.8             |
| Chrome  | Latest stable   |
| Git     | 2.x             |

---

## Branching Strategy

| Branch pattern     | Purpose                                      |
|--------------------|----------------------------------------------|
| `main`             | Stable, production-ready template            |
| `develop`          | Integration branch — all features merge here |
| `feature/xxx`      | New features or enhancements                 |
| `fix/xxx`          | Bug fixes                                    |
| `docs/xxx`         | Documentation-only changes                  |
| `chore/xxx`        | Dependency upgrades, config changes          |

**Never commit directly to `main`.** All changes go through a PR targeting `develop` first.

---

## Pull Request Process

1. Ensure your branch is up to date with `develop`:
   ```bash
   git fetch upstream
   git rebase upstream/develop
   ```

2. Confirm the project compiles cleanly:
   ```bash
   mvn test-compile -DskipTests
   ```

3. Run the smoke suite and confirm it passes:
   ```bash
   mvn test -Dtest=SmokeTestRunner -Dbrowser=headless
   ```

4. Open a PR against `develop` — not `main`

5. Fill in the PR description:
   - What does this PR change?
   - Why is the change needed?
   - How was it tested?
   - Does it introduce any breaking changes?

6. At least one reviewer must approve before merging

7. Squash commits before merging to keep history clean

---

## Coding Standards

### General

- All Java files must include the purpose comment header at the top (see any existing file as a template)
- Every public method must have a Javadoc comment with an `Example:` block
- Every section of logic must have an inline comment
- All TODO markers must use the format `// TODO-N: description`
- No hardcoded URLs, credentials, or environment-specific values — use `config.json`
- No `Thread.sleep()` — use `WaitUtils` explicit waits instead
- No raw `System.out.println()` — use `LogUtil.getLogger(MyClass.class)` SLF4J logging

### Page Objects

- One class per page or major UI component
- All page classes extend `BasePage`
- No WebDriver calls in step definitions — delegate to page objects
- All locators go in `Locators.java` — never inline in page objects

### Step Definitions

- One step definition class per feature area
- Steps must read like plain English — no technical implementation detail
- All step def classes receive `TestContext` via constructor (PicoContainer)
- Never assert directly in step defs — use `CustomAssert` or page object verifiers

### Feature Files

- One feature file per feature area
- All scenarios must be tagged with at least one of: `@smoke`, `@regression`, `@api`, `@db`
- Use `Background:` for steps repeated across all scenarios in a file
- Use `Scenario Outline:` + `Examples:` for data-driven scenarios
- Avoid `@wip` in committed code — strip before raising a PR

### Naming Conventions

| Element             | Convention          | Example                      |
|---------------------|---------------------|------------------------------|
| Classes             | PascalCase          | `LoginPage`, `ApiUtils`      |
| Methods             | camelCase           | `enterUsername()`, `getUser()`|
| Constants           | UPPER_SNAKE_CASE    | `LOGIN_BUTTON`, `BASE_URL`   |
| Test data keys      | camelCase           | `adminUser`, `loginEndpoint` |
| Feature files       | kebab-case          | `user-management.feature`    |
| Branch names        | kebab-case          | `feature/add-retry-logic`    |

---

## Commit Message Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

### Types

| Type       | When to use                                      |
|------------|--------------------------------------------------|
| `feat`     | New feature or page object                       |
| `fix`      | Bug fix                                          |
| `docs`     | Documentation only                               |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test`     | Adding or updating tests                         |
| `chore`    | Dependency updates, config, build changes        |
| `ci`       | CI/CD pipeline changes                           |

### Examples

```
feat(pages): add OrderManagementPage with CRUD methods
fix(hooks): prevent NPE when driver not initialised in @After
docs(readme): update quick start section for Java 25
chore(deps): upgrade Selenium to 4.22.0
test(api): add negative scenarios for POST /users endpoint
ci(github): add db test job to nightly workflow
```

---

## Adding New Tests

### Adding a UI scenario

1. Add locators to `Locators.java` under a new or existing inner class
2. Create or update a Page Object in `src/test/java/pages/`
3. Add step definitions to an existing or new class in `stepdefinitions/`
4. Add the Gherkin scenario to the appropriate `.feature` file
5. Tag the scenario with `@smoke` and/or `@regression`
6. Run the scenario in isolation to verify it passes:
   ```bash
   mvn test -Dtest=SmokeTestRunner -Dcucumber.filter.tags="@your-new-tag"
   ```

### Adding an API scenario

1. Add the endpoint to `testdata/apidata.json` under `endpoints`
2. Add any request body templates to `testdata/apidata.json` under `requestBodies`
3. Add step definitions to `ApiSteps.java` if a new step pattern is needed
4. Add the Gherkin scenario to `api.feature` with `@api` tag

### Adding a DB scenario

1. Add named SQL queries to `testdata/dbdata.json` under `queries`
2. Add expected values to `testdata/dbdata.json` under `expectedValues`
3. Add methods to `SampleRepository.java` (or a new Repository class)
4. Add step definitions to `DbSteps.java` if needed
5. Add the Gherkin scenario to `db.feature` with `@db` tag

---

## Adding New Utilities

When adding a new utility class to `utils/`:

1. Follow the existing file header comment format
2. Make it a static utility class (private constructor, all static methods)
3. Use `LogUtil.getLogger(MyUtils.class)` for logging
4. Read configuration from `ConfigReader` — no hardcoded values
5. Add Javadoc with `Example:` blocks to every public method
6. Add a `TODO (customise per project)` section in the header comment

---

## Questions

If you are unsure about anything, open a GitHub Discussion or raise an issue labelled `question` before starting work. This avoids duplicate effort and ensures the change aligns with the template's design goals.
