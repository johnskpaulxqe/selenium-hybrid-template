package stepdefinitions;

/*
  ============================================================
  FILE: LoginSteps.java
  LOCATION: src/test/java/stepdefinitions/LoginSteps.java

  PURPOSE:
    Step definitions for all login-related Gherkin steps.
    Maps Gherkin steps in login.feature to LoginPage and
    PageVerifier actions. Demonstrates the clean step
    definition pattern using TestContext for state sharing.

  HOW IT WORKS:
    1. PicoContainer injects TestContext into the constructor
    2. Page objects are retrieved from TestContext
    3. Steps delegate directly to page object methods
    4. No WebDriver or assertion logic lives here — only
       orchestration calls to page objects and verifiers

  TODO (customise per project):
    - TODO-1 : Update step text to match your Gherkin wording
    - TODO-2 : Add steps for any additional login flows your
               app has (SSO, MFA, social login etc.)
  ============================================================
*/

import context.TestContext;
import io.cucumber.java.en.*;
import pages.LoginPage;
import utils.CustomAssert;
import utils.DriverManager;
import utils.PageVerifier;
import utils.TestDataLoader;

public class LoginSteps {

    private final TestContext ctx;
    private final LoginPage   loginPage;

    // PicoContainer injects TestContext automatically
    public LoginSteps(TestContext ctx) {
        this.ctx       = ctx;
        this.loginPage = ctx.getLoginPage();
    }


    // ── GIVEN steps ──────────────────────────────────────────

    @Given("I am on the login page")
    public void iAmOnTheLoginPage() {
        loginPage.navigateToLoginPage();
    }

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String userRole) {
        loginPage.navigateToLoginPage();
        loginPage.loginAs(userRole);
        loginPage.verifyLoginSuccess();
        // Store logged in username for later steps
        ctx.setLoggedInUsername(TestDataLoader.getUsername(userRole));
    }


    // ── WHEN steps ────────────────────────────────────────────

    @When("I enter valid credentials for {string}")
    public void iEnterValidCredentialsFor(String userRole) {
        loginPage.enterUsername(TestDataLoader.getUsername(userRole));
        loginPage.enterPassword(TestDataLoader.getPassword(userRole));
    }

    @When("I enter username {string} and password {string}")
    public void iEnterUsernameAndPassword(String username, String password) {
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }

    @When("I enter invalid credentials")
    public void iEnterInvalidCredentials() {
        // Uses the invalidCredentials entry from users.json
        loginPage.enterUsername(TestDataLoader.getUsername("invalidCredentials"));
        loginPage.enterPassword(TestDataLoader.getPassword("invalidCredentials"));
    }

    @When("I click the login button")
    public void iClickTheLoginButton() {
        loginPage.clickLoginButton();
    }

    @When("I login as {string}")
    public void iLoginAs(String userRole) {
        loginPage.loginAs(userRole);
    }

    @When("I click forgot password")
    public void iClickForgotPassword() {
        loginPage.clickForgotPassword();
    }


    // ── THEN steps ────────────────────────────────────────────

    @Then("I should be on the dashboard")
    public void iShouldBeOnTheDashboard() {
        loginPage.verifyLoginSuccess();
    }

    @Then("I should see a login error message")
    public void iShouldSeeALoginErrorMessage() {
        CustomAssert.assertTrue(
                loginPage.isErrorDisplayed(),
                "Login error message should be displayed");
    }

    @Then("the login error message should contain {string}")
    public void theLoginErrorMessageShouldContain(String expectedText) {
        loginPage.verifyErrorMessage(expectedText);
    }

    @Then("I should remain on the login page")
    public void iShouldRemainOnTheLoginPage() {
        PageVerifier.verifyUrlContains(DriverManager.getDriver(), "/login");
    }

    @Then("the login button should be enabled")
    public void theLoginButtonShouldBeEnabled() {
        CustomAssert.assertTrue(
                loginPage.isLoginButtonEnabled(),
                "Login button should be enabled");
    }

}