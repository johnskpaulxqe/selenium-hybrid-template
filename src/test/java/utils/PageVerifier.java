package utils;

/*
  ============================================================
  FILE: PageVerifier.java
  LOCATION: src/test/java/utils/PageVerifier.java

  PURPOSE:
    Reusable page-state assertion helpers that sit above
    BasePage and CustomAssert. Combines WebDriver state checks
    with assertion logging so step definitions stay clean
    and readable.
    Responsibilities:
      1. Verify URL, title, and page heading state
      2. Verify element visibility, text, and attribute state
      3. Verify navigation and redirect outcomes
      4. Provide soft-assertion variants for multi-check steps
      5. Log every verification with PASS/FAIL status

  HOW TO USE:
    // Verify current URL contains a path fragment
    PageVerifier.verifyUrlContains(driver, "/dashboard");

    // Verify page title
    PageVerifier.verifyTitle(driver, "Dashboard - YourApp");

    // Verify element is displayed
    PageVerifier.verifyElementDisplayed(driver, Locators.Dashboard.WELCOME_MESSAGE);

    // Verify element text
    PageVerifier.verifyElementText(driver, Locators.Common.PAGE_HEADING, "Welcome");

    // Verify successful login (combined check)
    PageVerifier.verifySuccessfulLogin(driver, "admin");

  TODO (customise per project):
    - TODO-1 : Add verifySuccessfulLogin() body once you know
               your post-login URL and welcome message pattern
    - TODO-2 : Add page-specific verifiers for your key pages
               e.g. verifyDashboardLoaded(), verifyCheckoutPage()
    - TODO-3 : Add verifyNoConsoleErrors() if your team tracks
               JavaScript console errors in tests
  ============================================================
*/

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PageVerifier {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LoggerFactory.getLogger(PageVerifier.class);

    // Private constructor — static utility class
    private PageVerifier() {}


    // ═══════════════════════════════════════════════════════════
    // URL VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    // ── verifyUrlContains() ───────────────────────────────────
    /**
     * Verifies the current URL contains the expected fragment.
     * Waits up to the configured explicit wait timeout.
     * Fails with a clear message showing actual vs expected.
     *
     * Example:
     *   PageVerifier.verifyUrlContains(driver, "/dashboard");
     *   PageVerifier.verifyUrlContains(driver, "/users/profile");
     */
    public static void verifyUrlContains(WebDriver driver, String expectedFragment) {
        log.info("Verifying URL contains: [{}]", expectedFragment);
        try {
            WaitUtils.waitForUrlContains(driver, expectedFragment);
            String currentUrl = driver.getCurrentUrl();
            log.info("✔ URL verification PASSED → URL: [{}] contains [{}]",
                    currentUrl, expectedFragment);
        } catch (Exception e) {
            String actual = driver.getCurrentUrl();
            String msg = "URL verification FAILED → Expected URL to contain ["
                    + expectedFragment + "] but actual URL was [" + actual + "]";
            log.error("✖ {}", msg);
            CustomAssert.assertTrue(false, msg);
        }
    }


    // ── verifyExactUrl() ─────────────────────────────────────
    /**
     * Verifies the current URL matches exactly.
     *
     * Example:
     *   PageVerifier.verifyExactUrl(driver, "https://dev.yourapp.com/dashboard");
     */
    public static void verifyExactUrl(WebDriver driver, String expectedUrl) {
        String actual = driver.getCurrentUrl();
        log.info("Verifying exact URL → Expected: [{}] Actual: [{}]", expectedUrl, actual);
        CustomAssert.assertEquals(actual, expectedUrl, "Page URL");
    }


    // ── verifyUrlDoesNotContain() ────────────────────────────
    /**
     * Verifies the current URL does NOT contain the given fragment.
     * Use to confirm the user did NOT navigate to a restricted page.
     *
     * Example:
     *   PageVerifier.verifyUrlDoesNotContain(driver, "/admin");
     */
    public static void verifyUrlDoesNotContain(WebDriver driver, String unexpectedFragment) {
        String actual = driver.getCurrentUrl();
        log.info("Verifying URL does NOT contain: [{}] → Actual: [{}]",
                unexpectedFragment, actual);
        CustomAssert.assertNotContains(actual, unexpectedFragment,
                "URL should not contain [" + unexpectedFragment + "]");
    }


    // ═══════════════════════════════════════════════════════════
    // TITLE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    // ── verifyTitle() ─────────────────────────────────────────
    /**
     * Verifies the page title matches exactly.
     *
     * Example:
     *   PageVerifier.verifyTitle(driver, "Dashboard - YourApp");
     */
    public static void verifyTitle(WebDriver driver, String expectedTitle) {
        String actual = driver.getTitle();
        log.info("Verifying page title → Expected: [{}] Actual: [{}]",
                expectedTitle, actual);
        CustomAssert.assertEquals(actual, expectedTitle, "Page title");
    }


    // ── verifyTitleContains() ────────────────────────────────
    /**
     * Verifies the page title contains the expected text.
     * More resilient than exact match for titles with dynamic parts.
     *
     * Example:
     *   PageVerifier.verifyTitleContains(driver, "Dashboard");
     */
    public static void verifyTitleContains(WebDriver driver, String expectedFragment) {
        log.info("Verifying page title contains: [{}]", expectedFragment);
        try {
            WaitUtils.waitForTitleContains(driver, expectedFragment);
            log.info("✔ Title verification PASSED → contains [{}]", expectedFragment);
        } catch (Exception e) {
            String actual = driver.getTitle();
            String msg = "Title verification FAILED → Expected title to contain ["
                    + expectedFragment + "] but was [" + actual + "]";
            log.error("✖ {}", msg);
            CustomAssert.assertTrue(false, msg);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT VISIBILITY VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    // ── verifyElementDisplayed() ─────────────────────────────
    /**
     * Verifies an element is visible on the page.
     * Waits up to the configured explicit wait timeout.
     *
     * Example:
     *   PageVerifier.verifyElementDisplayed(driver, Locators.Dashboard.WELCOME_MESSAGE);
     *   PageVerifier.verifyElementDisplayed(driver, Locators.Common.SUCCESS_TOAST);
     */
    public static void verifyElementDisplayed(WebDriver driver, By locator) {
        log.info("Verifying element is displayed: [{}]", locator);
        try {
            WaitUtils.waitForVisible(driver, locator);
            log.info("✔ Element displayed: [{}]", locator);
        } catch (Exception e) {
            String msg = "Element not visible: [" + locator + "]";
            log.error("✖ {}", msg);
            ScreenshotUtils.captureAndSave(driver, "verify_displayed_fail");
            CustomAssert.assertTrue(false, msg);
        }
    }


    // ── verifyElementNotDisplayed() ──────────────────────────
    /**
     * Verifies an element is NOT visible on the page.
     * Does not throw if element is absent from DOM — that passes.
     *
     * Example:
     *   PageVerifier.verifyElementNotDisplayed(driver, Locators.Login.ERROR_MESSAGE);
     *   PageVerifier.verifyElementNotDisplayed(driver, Locators.Common.LOADING_SPINNER);
     */
    public static void verifyElementNotDisplayed(WebDriver driver, By locator) {
        log.info("Verifying element is NOT displayed: [{}]", locator);
        boolean visible = false;
        try {
            visible = driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            // Element absent from DOM — that's a PASS
        }
        if (visible) {
            String msg = "Element should NOT be visible but is displayed: [" + locator + "]";
            log.error("✖ {}", msg);
            CustomAssert.assertTrue(false, msg);
        } else {
            log.info("✔ Element correctly not displayed: [{}]", locator);
        }
    }


    // ── verifyElementPresent() ────────────────────────────────
    /**
     * Verifies an element is present in the DOM (may be hidden).
     *
     * Example:
     *   PageVerifier.verifyElementPresent(driver, Locators.Form.HIDDEN_FIELD);
     */
    public static void verifyElementPresent(WebDriver driver, By locator) {
        log.info("Verifying element is present in DOM: [{}]", locator);
        boolean present = !driver.findElements(locator).isEmpty();
        CustomAssert.assertTrue(present,
                "Element should be present in DOM: [" + locator + "]");
        if (present) log.info("✔ Element present: [{}]", locator);
    }


    // ── verifyElementAbsent() ─────────────────────────────────
    /**
     * Verifies an element does NOT exist anywhere in the DOM.
     *
     * Example:
     *   PageVerifier.verifyElementAbsent(driver, Locators.Common.ERROR_TOAST);
     */
    public static void verifyElementAbsent(WebDriver driver, By locator) {
        log.info("Verifying element is absent from DOM: [{}]", locator);
        boolean absent = driver.findElements(locator).isEmpty();
        CustomAssert.assertTrue(absent,
                "Element should be absent from DOM but was found: [" + locator + "]");
        if (absent) log.info("✔ Element correctly absent: [{}]", locator);
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT TEXT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    // ── verifyElementText() ──────────────────────────────────
    /**
     * Verifies an element's text matches exactly.
     *
     * Example:
     *   PageVerifier.verifyElementText(driver,
     *       Locators.Common.PAGE_HEADING, "User Dashboard");
     */
    public static void verifyElementText(WebDriver driver, By locator,
                                         String expectedText) {
        log.info("Verifying element text → [{}] expected: [{}]", locator, expectedText);
        try {
            WebElement element = WaitUtils.waitForVisible(driver, locator);
            String actual = element.getText().trim();
            log.info("Element text actual: [{}]", actual);
            CustomAssert.assertEquals(actual, expectedText,
                    "Element text for [" + locator + "]");
        } catch (Exception e) {
            String msg = "Text verification failed for [" + locator + "]: " + e.getMessage();
            log.error("✖ {}", msg);
            CustomAssert.assertTrue(false, msg);
        }
    }


    // ── verifyElementTextContains() ──────────────────────────
    /**
     * Verifies an element's text contains the expected substring.
     * More resilient than exact match for dynamic content.
     *
     * Example:
     *   PageVerifier.verifyElementTextContains(driver,
     *       Locators.Common.SUCCESS_TOAST, "saved successfully");
     */
    public static void verifyElementTextContains(WebDriver driver, By locator,
                                                 String expectedSubstring) {
        log.info("Verifying element text contains [{}] in: [{}]", expectedSubstring, locator);
        try {
            WebElement element = WaitUtils.waitForVisible(driver, locator);
            String actual = element.getText().trim();
            CustomAssert.assertContains(actual, expectedSubstring,
                    "Element text for [" + locator + "]");
            log.info("✔ Text contains [{}]: [{}]", expectedSubstring, actual);
        } catch (Exception e) {
            String msg = "Text contains check failed for [" + locator + "]: " + e.getMessage();
            log.error("✖ {}", msg);
            CustomAssert.assertTrue(false, msg);
        }
    }


    // ── verifyElementTextNotEmpty() ──────────────────────────
    /**
     * Verifies an element's text is not blank or empty.
     *
     * Example:
     *   PageVerifier.verifyElementTextNotEmpty(driver,
     *       Locators.Dashboard.USER_DISPLAY_NAME);
     */
    public static void verifyElementTextNotEmpty(WebDriver driver, By locator) {
        log.info("Verifying element text is not empty: [{}]", locator);
        WebElement element = WaitUtils.waitForVisible(driver, locator);
        String text = element.getText().trim();
        CustomAssert.assertTrue(!text.isEmpty(),
                "Element text should not be empty: [" + locator + "]");
        log.info("✔ Element text not empty: [{}]", text);
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT STATE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    // ── verifyElementEnabled() ───────────────────────────────
    /**
     * Verifies an element is enabled (not disabled).
     *
     * Example:
     *   PageVerifier.verifyElementEnabled(driver, Locators.Form.SUBMIT_BUTTON);
     */
    public static void verifyElementEnabled(WebDriver driver, By locator) {
        log.info("Verifying element is enabled: [{}]", locator);
        WebElement element = WaitUtils.waitForPresent(driver, locator);
        CustomAssert.assertTrue(element.isEnabled(),
                "Element should be enabled: [" + locator + "]");
        log.info("✔ Element is enabled: [{}]", locator);
    }


    // ── verifyElementDisabled() ──────────────────────────────
    /**
     * Verifies an element is disabled.
     *
     * Example:
     *   PageVerifier.verifyElementDisabled(driver, Locators.Form.SUBMIT_BUTTON);
     */
    public static void verifyElementDisabled(WebDriver driver, By locator) {
        log.info("Verifying element is disabled: [{}]", locator);
        WebElement element = WaitUtils.waitForPresent(driver, locator);
        CustomAssert.assertFalse(element.isEnabled(),
                "Element should be disabled: [" + locator + "]");
        log.info("✔ Element is disabled: [{}]", locator);
    }


    // ── verifyElementSelected() ──────────────────────────────
    /**
     * Verifies a checkbox or radio button is selected/checked.
     *
     * Example:
     *   PageVerifier.verifyElementSelected(driver, Locators.Form.TERMS_CHECKBOX);
     */
    public static void verifyElementSelected(WebDriver driver, By locator) {
        log.info("Verifying element is selected: [{}]", locator);
        WebElement element = WaitUtils.waitForPresent(driver, locator);
        CustomAssert.assertTrue(element.isSelected(),
                "Element should be selected: [" + locator + "]");
        log.info("✔ Element is selected: [{}]", locator);
    }


    // ── verifyAttributeValue() ────────────────────────────────
    /**
     * Verifies an element's attribute equals the expected value.
     *
     * Example:
     *   PageVerifier.verifyAttributeValue(driver,
     *       Locators.Login.USERNAME, "placeholder", "Enter your email");
     */
    public static void verifyAttributeValue(WebDriver driver, By locator,
                                            String attribute, String expectedValue) {
        log.info("Verifying attribute [{}] on [{}] → Expected: [{}]",
                attribute, locator, expectedValue);
        WebElement element = WaitUtils.waitForPresent(driver, locator);
        String actual = element.getAttribute(attribute);
        CustomAssert.assertEquals(actual, expectedValue,
                "Attribute [" + attribute + "] on [" + locator + "]");
        log.info("✔ Attribute [{}] = [{}]", attribute, actual);
    }


    // ═══════════════════════════════════════════════════════════
    // COUNT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    // ── verifyElementCount() ─────────────────────────────────
    /**
     * Verifies the number of elements matching a locator.
     *
     * Example:
     *   PageVerifier.verifyElementCount(driver, Locators.Table.DATA_ROWS, 10);
     */
    public static void verifyElementCount(WebDriver driver, By locator,
                                          int expectedCount) {
        List<WebElement> elements = driver.findElements(locator);
        int actual = elements.size();
        log.info("Verifying element count for [{}] → Expected: [{}] Actual: [{}]",
                locator, expectedCount, actual);
        CustomAssert.assertEquals(actual, expectedCount,
                "Element count for [" + locator + "]");
    }


    // ── verifyElementCountGreaterThan() ──────────────────────
    /**
     * Verifies at least a minimum number of elements exist.
     *
     * Example:
     *   PageVerifier.verifyElementCountGreaterThan(driver,
     *       Locators.Table.DATA_ROWS, 0);
     */
    public static void verifyElementCountGreaterThan(WebDriver driver,
                                                     By locator,
                                                     int minimum) {
        int actual = driver.findElements(locator).size();
        log.info("Verifying element count > {} for [{}] → Actual: [{}]",
                minimum, locator, actual);
        CustomAssert.assertGreaterThan(actual, minimum,
                "Element count for [" + locator + "] should be > " + minimum);
    }


    // ═══════════════════════════════════════════════════════════
    // COMPOSITE PAGE-LEVEL VERIFICATIONS
    // High-level helpers that combine multiple checks into one
    // ═══════════════════════════════════════════════════════════

    // ── verifyPageLoaded() ────────────────────────────────────
    /**
     * Verifies the page has fully loaded by checking:
     *   1. document.readyState is "complete"
     *   2. Loading spinner (if any) is gone
     *   3. URL contains expected fragment
     *
     * Example:
     *   PageVerifier.verifyPageLoaded(driver, "/dashboard");
     */
    public static void verifyPageLoaded(WebDriver driver, String expectedUrlFragment) {
        log.info("Verifying page loaded: [{}]", expectedUrlFragment);
        WaitUtils.waitForPageLoad(driver);
        WaitUtils.waitForInvisible(driver, Locators.Common.LOADING_SPINNER);
        verifyUrlContains(driver, expectedUrlFragment);
        log.info("✔ Page loaded successfully: [{}]", expectedUrlFragment);
    }


    // ── verifySuccessToast() ─────────────────────────────────
    /**
     * Verifies a success toast/notification is displayed
     * and contains the expected message text.
     *
     * Example:
     *   PageVerifier.verifySuccessToast(driver, "saved successfully");
     */
    public static void verifySuccessToast(WebDriver driver, String expectedMessage) {
        log.info("Verifying success toast contains: [{}]", expectedMessage);
        verifyElementDisplayed(driver, Locators.Common.SUCCESS_TOAST);
        verifyElementTextContains(driver, Locators.Common.SUCCESS_TOAST, expectedMessage);
    }


    // ── verifyErrorToast() ───────────────────────────────────
    /**
     * Verifies an error toast/notification is displayed.
     *
     * Example:
     *   PageVerifier.verifyErrorToast(driver, "Invalid credentials");
     */
    public static void verifyErrorToast(WebDriver driver, String expectedMessage) {
        log.info("Verifying error toast contains: [{}]", expectedMessage);
        verifyElementDisplayed(driver, Locators.Common.ERROR_TOAST);
        verifyElementTextContains(driver, Locators.Common.ERROR_TOAST, expectedMessage);
    }


    // ── verifyModalDisplayed() ────────────────────────────────
    /**
     * Verifies a modal dialog is open and visible.
     *
     * Example:
     *   PageVerifier.verifyModalDisplayed(driver);
     */
    public static void verifyModalDisplayed(WebDriver driver) {
        log.info("Verifying modal dialog is displayed");
        verifyElementDisplayed(driver, Locators.Common.MODAL_DIALOG);
    }


    // ── verifyModalNotDisplayed() ────────────────────────────
    /**
     * Verifies no modal dialog is open.
     *
     * Example:
     *   PageVerifier.verifyModalNotDisplayed(driver);
     */
    public static void verifyModalNotDisplayed(WebDriver driver) {
        log.info("Verifying modal dialog is NOT displayed");
        verifyElementNotDisplayed(driver, Locators.Common.MODAL_DIALOG);
    }


    // ── verifySuccessfulLogin() ───────────────────────────────
    /**
     * Verifies the user has successfully logged in by checking:
     *   1. URL has moved away from /login
     *   2. Dashboard/home URL fragment is present
     *   3. Loading spinner is gone
     *
     * TODO-1: Customise the expected URL fragment and element
     *         to match your application's post-login state.
     *
     * Example:
     *   PageVerifier.verifySuccessfulLogin(driver);
     */
    public static void verifySuccessfulLogin(WebDriver driver) {
        log.info("Verifying successful login");

        // Wait for page to finish loading
        WaitUtils.waitForPageLoad(driver);

        // TODO-1: Replace "/dashboard" with your post-login URL path
        verifyUrlDoesNotContain(driver, "/login");
        verifyUrlContains(driver, "/dashboard");

        // TODO-1: Uncomment and update once you know your welcome element
        // verifyElementDisplayed(driver, Locators.Dashboard.WELCOME_MESSAGE);

        log.info("✔ Successful login verified");
    }


    // ── verifyFailedLogin() ───────────────────────────────────
    /**
     * Verifies the login attempt failed by checking:
     *   1. URL is still on the login page
     *   2. Error message is displayed
     *
     * TODO-1: Customise to match your app's login failure state.
     *
     * Example:
     *   PageVerifier.verifyFailedLogin(driver);
     */
    public static void verifyFailedLogin(WebDriver driver) {
        log.info("Verifying failed login state");

        // TODO-1: Replace "/login" with your login page URL path
        verifyUrlContains(driver, "/login");
        verifyElementDisplayed(driver, Locators.Login.ERROR_MESSAGE);

        log.info("✔ Failed login state verified");
    }

}