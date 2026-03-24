package pages;

/*
  ============================================================
  FILE: LoginPage.java
  LOCATION: src/test/java/pages/LoginPage.java

  PURPOSE:
    Page Object for the Login page. Demonstrates the full
    POM pattern using BasePage, Locators, and PageVerifier.
    Every UI interaction with the login page goes through
    this class — step definitions never touch WebDriver directly.

  HOW TO USE:
    // Instantiate in step definition or TestContext
    LoginPage loginPage = new LoginPage();

    // Use in step definitions
    loginPage.navigateToLoginPage();
    loginPage.login("admin@yourapp.com", "Admin@123");
    loginPage.verifyLoginSuccess();

  TODO (customise per project):
    - TODO-1 : Update navigateToLoginPage() path to match your app
    - TODO-2 : Update verifyLoginSuccess() URL fragment
    - TODO-3 : Add any additional login page interactions your
               app requires (SSO, MFA, captcha bypass etc.)
  ============================================================
*/

import utils.ConfigReader;
import utils.Locators;
import utils.LogUtil;
import utils.PageVerifier;
import utils.TestDataLoader;
import utils.WaitUtils;

public class LoginPage extends BasePage {

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Navigates directly to the login page.
     * TODO-1: Update "/login" to your app's login path
     */
    public void navigateToLoginPage() {
        log.info("Navigating to login page");
        // TODO-1: Replace "/login" with your actual login path
        navigateToPath("/login");
    }


    // ═══════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Enters the username into the username field.
     *
     * Example:
     *   loginPage.enterUsername("admin@yourapp.com");
     */
    public void enterUsername(String username) {
        log.debug("Entering username: [{}]", username);
        type(Locators.Login.USERNAME, username);
    }


    /**
     * Enters the password into the password field.
     */
    public void enterPassword(String password) {
        log.debug("Entering password");
        type(Locators.Login.PASSWORD, password);
    }


    /**
     * Clicks the login / sign-in button.
     */
    public void clickLoginButton() {
        log.debug("Clicking login button");
        click(Locators.Login.LOGIN_BUTTON);
    }


    /**
     * Performs a full login — enters credentials and clicks submit.
     * The most commonly used method in login step definitions.
     *
     * Example:
     *   loginPage.login("admin@yourapp.com", "Admin@123");
     */
    public void login(String username, String password) {
        log.info("Logging in as: [{}]", username);
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
    }


    /**
     * Logs in using credentials from users.json for the given role.
     * Keeps step definitions clean — no credential strings in steps.
     *
     * Example:
     *   loginPage.loginAs("admin");
     *   loginPage.loginAs("standard");
     */
    public void loginAs(String userRole) {
        String username = TestDataLoader.getUsername(userRole);
        String password = TestDataLoader.getPassword(userRole);
        log.info("Logging in as role: [{}] username: [{}]", userRole, username);
        login(username, password);
    }


    /**
     * Clicks the Forgot Password link.
     */
    public void clickForgotPassword() {
        log.debug("Clicking forgot password link");
        click(Locators.Login.FORGOT_PASSWORD_LINK);
    }


    /**
     * Ticks the Remember Me checkbox.
     */
    public void tickRememberMe() {
        log.debug("Ticking remember me checkbox");
        click(Locators.Login.REMEMBER_ME_CHECKBOX);
    }


    // ═══════════════════════════════════════════════════════════
    // STATE CHECKS
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns true if the login error message is displayed.
     * Use for negative test assertions.
     *
     * Example:
     *   boolean errorShown = loginPage.isErrorDisplayed();
     */
    public boolean isErrorDisplayed() {
        return isDisplayed(Locators.Login.ERROR_MESSAGE);
    }


    /**
     * Returns the text of the login error message.
     *
     * Example:
     *   String errorText = loginPage.getErrorMessage();
     */
    public String getErrorMessage() {
        return getText(Locators.Login.ERROR_MESSAGE);
    }


    /**
     * Returns true if the login button is enabled.
     */
    public boolean isLoginButtonEnabled() {
        return isEnabled(Locators.Login.LOGIN_BUTTON);
    }


    // ═══════════════════════════════════════════════════════════
    // VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Verifies login succeeded — URL moved away from login page
     * and dashboard is loaded.
     * TODO-2: Update "/dashboard" to your post-login URL fragment
     */
    public void verifyLoginSuccess() {
        log.info("Verifying login success");
        PageVerifier.verifySuccessfulLogin(driver());
    }


    /**
     * Verifies login failed — still on login page with error shown.
     */
    public void verifyLoginFailed() {
        log.info("Verifying login failure");
        PageVerifier.verifyFailedLogin(driver());
    }


    /**
     * Verifies the error message text matches expected.
     *
     * Example:
     *   loginPage.verifyErrorMessage("Invalid username or password");
     */
    public void verifyErrorMessage(String expectedMessage) {
        PageVerifier.verifyElementTextContains(driver(),
                Locators.Login.ERROR_MESSAGE, expectedMessage);
    }

}