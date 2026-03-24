package pages;

/*
  ============================================================
  FILE: BasePage.java
  LOCATION: src/test/java/pages/BasePage.java

  PURPOSE:
    Abstract base class for all Page Object classes. Provides
    every page object with access to the WebDriver, common
    interaction methods, and wait helpers — without repeating
    code in every page class.
    Responsibilities:
      1. Expose the WebDriver via DriverManager.getDriver()
      2. Wrap all common element interactions (click, type,
         getText, isDisplayed, select, etc.)
      3. Integrate WaitUtils so page objects never call
         Thread.sleep() directly
      4. Integrate JavaScriptUtils for fallback interactions
      5. Provide navigation helpers (navigate, back, refresh)
      6. Log every interaction at DEBUG level

  DESIGN PATTERN — Page Object Model (POM):
    BasePage         → shared driver + interaction methods
    LoginPage        → extends BasePage, login-specific methods
    SamplePage       → extends BasePage, sample-page methods

  HOW TO USE:
    Extend this class in every page object:

    public class LoginPage extends BasePage {
        public void enterUsername(String username) {
            type(Locators.Login.USERNAME, username);
        }
        public void clickLogin() {
            click(Locators.Login.LOGIN_BUTTON);
        }
        public boolean isLoginErrorDisplayed() {
            return isDisplayed(Locators.Login.ERROR_MESSAGE);
        }
    }

  TODO (customise per project):
    - TODO-1 : Add any app-wide interactions your pages share
               (e.g. dismissCookieBanner(), waitForSpinner())
    - TODO-2 : Add getDriver() visibility if subclasses need
               direct driver access (currently protected)
    - TODO-3 : Add PageFactory.initElements() here if you prefer
               @FindBy annotation-style locators over By locators
  ============================================================
*/

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import utils.*;

import java.util.List;

public abstract class BasePage {

    // ── Logger ────────────────────────────────────────────────
    // Uses the concrete subclass name so logs show "LoginPage"
    // not "BasePage"
    protected final Logger log = LogUtil.getLogger(this.getClass());

    // ── Timeouts ──────────────────────────────────────────────
    // Read once from config — shared across all page objects
    protected final int DEFAULT_WAIT =
            ConfigReader.getInt("timeouts.explicitWait", 10);

    protected final int PAGE_LOAD_WAIT =
            ConfigReader.getInt("timeouts.pageLoadTimeout", 30);


    // ═══════════════════════════════════════════════════════════
    // DRIVER ACCESS
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns the WebDriver for the current thread.
     * All page object methods go through this — never hold
     * a driver reference in a page field.
     */
    protected WebDriver driver() {
        return DriverManager.getDriver();
    }


    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════

    // ── navigateTo() ─────────────────────────────────────────
    /**
     * Navigates to a full URL and waits for page load.
     *
     * Example:
     *   navigateTo("https://dev.yourapp.com/login");
     */
    protected void navigateTo(String url) {
        log.info("Navigating to: [{}]", url);
        driver().get(url);
        WaitUtils.waitForPageLoad(driver());
    }


    // ── navigateToPath() ─────────────────────────────────────
    /**
     * Navigates to a path relative to the configured base URL.
     *
     * Example:
     *   navigateToPath("/login");
     *   navigateToPath("/dashboard/users");
     */
    protected void navigateToPath(String path) {
        String baseUrl = ConfigReader.get("baseUrl", "");
        String fullUrl = baseUrl + path;
        log.info("Navigating to path: [{}]", fullUrl);
        driver().get(fullUrl);
        WaitUtils.waitForPageLoad(driver());
    }


    // ── goBack() ─────────────────────────────────────────────
    /**
     * Navigates back in browser history.
     *
     * Example:
     *   goBack();
     */
    protected void goBack() {
        log.debug("Navigating back");
        driver().navigate().back();
        WaitUtils.waitForPageLoad(driver());
    }


    // ── refresh() ────────────────────────────────────────────
    /**
     * Refreshes the current page.
     *
     * Example:
     *   refresh();
     */
    protected void refresh() {
        log.debug("Refreshing page");
        driver().navigate().refresh();
        WaitUtils.waitForPageLoad(driver());
    }


    // ── getCurrentUrl() ──────────────────────────────────────
    /**
     * Returns the current page URL.
     *
     * Example:
     *   String url = getCurrentUrl();
     */
    protected String getCurrentUrl() {
        return driver().getCurrentUrl();
    }


    // ── getPageTitle() ────────────────────────────────────────
    /**
     * Returns the current page title.
     *
     * Example:
     *   String title = getPageTitle();
     */
    protected String getPageTitle() {
        return driver().getTitle();
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT INTERACTIONS
    // ═══════════════════════════════════════════════════════════

    // ── click() ───────────────────────────────────────────────
    /**
     * Waits for element to be clickable then clicks it.
     * Falls back to JS click if intercepted.
     *
     * Example:
     *   click(Locators.Login.LOGIN_BUTTON);
     */
    protected void click(By locator) {
        log.debug("Clicking element: [{}]", locator);
        try {
            WaitUtils.waitForClickable(driver(), locator).click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Click intercepted on [{}] — falling back to JS click", locator);
            JavaScriptUtils.click(driver(), findElement(locator));
        }
    }


    // ── click(WebElement) ────────────────────────────────────
    /**
     * Waits for a WebElement to be clickable then clicks it.
     * Use when you already have a reference to the element.
     *
     * Example:
     *   click(submitButton);
     */
    protected void click(WebElement element) {
        log.debug("Clicking WebElement");
        try {
            WaitUtils.waitForClickable(driver(), element).click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Click intercepted — falling back to JS click");
            JavaScriptUtils.click(driver(), element);
        }
    }


    // ── type() ────────────────────────────────────────────────
    /**
     * Clears the field then types the given text.
     * Waits for the element to be visible before typing.
     *
     * Example:
     *   type(Locators.Login.USERNAME, "admin@yourapp.com");
     */
    protected void type(By locator, String text) {
        log.debug("Typing [{}] into element: [{}]", text, locator);
        WebElement element = WaitUtils.waitForVisible(driver(), locator);
        element.clear();
        element.sendKeys(text);
    }


    // ── typeWithoutClear() ────────────────────────────────────
    /**
     * Types text into a field WITHOUT clearing it first.
     * Use for fields where clear() triggers validation errors.
     *
     * Example:
     *   typeWithoutClear(Locators.Search.SEARCH_BOX, "query");
     */
    protected void typeWithoutClear(By locator, String text) {
        log.debug("Typing (no clear) [{}] into: [{}]", text, locator);
        WaitUtils.waitForVisible(driver(), locator).sendKeys(text);
    }


    // ── typeByJS() ────────────────────────────────────────────
    /**
     * Sets a field value via JavaScript.
     * Use when sendKeys() is blocked by the application.
     *
     * Example:
     *   typeByJS(Locators.Form.DATE_FIELD, "2024-12-31");
     */
    protected void typeByJS(By locator, String text) {
        log.debug("Setting value [{}] via JS on: [{}]", text, locator);
        WebElement element = findElement(locator);
        JavaScriptUtils.setValue(driver(), element, text);
    }


    // ── clearField() ─────────────────────────────────────────
    /**
     * Clears the text content of an input field.
     *
     * Example:
     *   clearField(Locators.Login.USERNAME);
     */
    protected void clearField(By locator) {
        log.debug("Clearing field: [{}]", locator);
        WaitUtils.waitForVisible(driver(), locator).clear();
    }


    // ── pressKey() ───────────────────────────────────────────
    /**
     * Sends a keyboard key to an element.
     * Useful for pressing Enter, Tab, Escape etc.
     *
     * Example:
     *   pressKey(Locators.Search.SEARCH_BOX, Keys.ENTER);
     *   pressKey(Locators.Login.PASSWORD, Keys.TAB);
     */
    protected void pressKey(By locator, Keys key) {
        log.debug("Pressing key [{}] on: [{}]", key.name(), locator);
        WaitUtils.waitForVisible(driver(), locator).sendKeys(key);
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT READING
    // ═══════════════════════════════════════════════════════════

    // ── getText() ────────────────────────────────────────────
    /**
     * Waits for element to be visible and returns its text.
     *
     * Example:
     *   String heading = getText(Locators.Dashboard.PAGE_HEADING);
     */
    protected String getText(By locator) {
        String text = WaitUtils.waitForVisible(driver(), locator).getText();
        log.debug("getText [{}] → [{}]", locator, text);
        return text;
    }


    // ── getAttributeValue() ──────────────────────────────────
    /**
     * Returns the value of an HTML attribute on an element.
     *
     * Example:
     *   String placeholder = getAttributeValue(Locators.Login.USERNAME, "placeholder");
     *   String value       = getAttributeValue(Locators.Form.INPUT, "value");
     */
    protected String getAttributeValue(By locator, String attribute) {
        String value = WaitUtils.waitForPresent(driver(), locator)
                .getAttribute(attribute);
        log.debug("getAttribute [{}][{}] → [{}]", locator, attribute, value);
        return value;
    }


    // ── getInputValue() ──────────────────────────────────────
    /**
     * Returns the current value of an input field.
     * Shorthand for getAttributeValue(locator, "value").
     *
     * Example:
     *   String email = getInputValue(Locators.Profile.EMAIL_FIELD);
     */
    protected String getInputValue(By locator) {
        return getAttributeValue(locator, "value");
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT STATE
    // ═══════════════════════════════════════════════════════════

    // ── isDisplayed() ────────────────────────────────────────
    /**
     * Returns true if the element is visible on the page.
     * Does NOT throw if element is absent — returns false.
     *
     * Example:
     *   boolean shown = isDisplayed(Locators.Login.ERROR_MESSAGE);
     */
    protected boolean isDisplayed(By locator) {
        try {
            return driver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            log.debug("isDisplayed → element not found: [{}]", locator);
            return false;
        }
    }


    // ── isEnabled() ──────────────────────────────────────────
    /**
     * Returns true if the element is enabled (not disabled).
     *
     * Example:
     *   boolean enabled = isEnabled(Locators.Form.SUBMIT_BUTTON);
     */
    protected boolean isEnabled(By locator) {
        try {
            return WaitUtils.waitForPresent(driver(), locator).isEnabled();
        } catch (Exception e) {
            log.debug("isEnabled → element not found: [{}]", locator);
            return false;
        }
    }


    // ── isSelected() ─────────────────────────────────────────
    /**
     * Returns true if a checkbox or radio button is selected.
     *
     * Example:
     *   boolean checked = isSelected(Locators.Form.TERMS_CHECKBOX);
     */
    protected boolean isSelected(By locator) {
        try {
            return WaitUtils.waitForPresent(driver(), locator).isSelected();
        } catch (Exception e) {
            log.debug("isSelected → element not found: [{}]", locator);
            return false;
        }
    }


    // ── isPresent() ──────────────────────────────────────────
    /**
     * Returns true if the element exists in the DOM
     * (regardless of visibility).
     *
     * Example:
     *   boolean exists = isPresent(Locators.Modal.DIALOG);
     */
    protected boolean isPresent(By locator) {
        return !driver().findElements(locator).isEmpty();
    }


    // ═══════════════════════════════════════════════════════════
    // WAITING
    // ═══════════════════════════════════════════════════════════

    // ── waitForVisible() ─────────────────────────────────────
    /**
     * Waits for an element to become visible.
     * Delegates to WaitUtils — no Thread.sleep() needed.
     *
     * Example:
     *   waitForVisible(Locators.Dashboard.WELCOME_MSG);
     */
    protected WebElement waitForVisible(By locator) {
        return WaitUtils.waitForVisible(driver(), locator);
    }


    // ── waitForInvisible() ───────────────────────────────────
    /**
     * Waits for an element to disappear (e.g. loading spinner).
     *
     * Example:
     *   waitForInvisible(Locators.Common.LOADING_SPINNER);
     */
    protected void waitForInvisible(By locator) {
        WaitUtils.waitForInvisible(driver(), locator);
    }


    // ── waitForText() ────────────────────────────────────────
    /**
     * Waits until the element contains the given text.
     *
     * Example:
     *   waitForText(Locators.Common.TOAST, "Saved successfully");
     */
    protected void waitForText(By locator, String text) {
        WaitUtils.waitForTextPresent(driver(), locator, text);
    }


    // ── waitForUrl() ─────────────────────────────────────────
    /**
     * Waits until the current URL contains the given fragment.
     *
     * Example:
     *   waitForUrl("/dashboard");
     */
    protected void waitForUrl(String urlFragment) {
        WaitUtils.waitForUrlContains(driver(), urlFragment);
    }


    // ── scrollTo() ───────────────────────────────────────────
    /**
     * Scrolls the given element into the visible viewport.
     *
     * Example:
     *   scrollTo(Locators.Footer.FOOTER_LINK);
     */
    protected void scrollTo(By locator) {
        WebElement element = findElement(locator);
        JavaScriptUtils.scrollIntoView(driver(), element);
    }


    // ═══════════════════════════════════════════════════════════
    // DROPDOWNS
    // ═══════════════════════════════════════════════════════════

    // ── selectByVisibleText() ────────────────────────────────
    /**
     * Selects an option from a native HTML <select> dropdown
     * by its visible text label.
     *
     * Example:
     *   selectByVisibleText(Locators.Form.COUNTRY_DROPDOWN, "United States");
     */
    protected void selectByVisibleText(By locator, String text) {
        log.debug("Selecting [{}] from dropdown: [{}]", text, locator);
        new Select(WaitUtils.waitForVisible(driver(), locator))
                .selectByVisibleText(text);
    }


    // ── selectByValue() ──────────────────────────────────────
    /**
     * Selects an option from a native HTML <select> by its value attribute.
     *
     * Example:
     *   selectByValue(Locators.Form.ROLE_DROPDOWN, "ADMIN");
     */
    protected void selectByValue(By locator, String value) {
        log.debug("Selecting value [{}] from dropdown: [{}]", value, locator);
        new Select(WaitUtils.waitForVisible(driver(), locator))
                .selectByValue(value);
    }


    // ── getSelectedOption() ───────────────────────────────────
    /**
     * Returns the currently selected option text in a <select>.
     *
     * Example:
     *   String selected = getSelectedOption(Locators.Form.COUNTRY_DROPDOWN);
     */
    protected String getSelectedOption(By locator) {
        return new Select(WaitUtils.waitForVisible(driver(), locator))
                .getFirstSelectedOption()
                .getText();
    }


    // ═══════════════════════════════════════════════════════════
    // MULTI-ELEMENT HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── findElements() ────────────────────────────────────────
    /**
     * Returns all matching elements for a locator.
     * Returns an empty list if none are found.
     *
     * Example:
     *   List<WebElement> rows = findElements(Locators.Table.DATA_ROWS);
     *   int rowCount = rows.size();
     */
    protected List<WebElement> findElements(By locator) {
        return driver().findElements(locator);
    }


    // ── getElementCount() ─────────────────────────────────────
    /**
     * Returns the count of elements matching a locator.
     *
     * Example:
     *   int rowCount = getElementCount(Locators.Table.DATA_ROWS);
     *   CustomAssert.assertEquals(rowCount, 5, "Table row count");
     */
    protected int getElementCount(By locator) {
        int count = driver().findElements(locator).size();
        log.debug("Element count for [{}] → [{}]", locator, count);
        return count;
    }


    // ═══════════════════════════════════════════════════════════
    // ALERTS
    // ═══════════════════════════════════════════════════════════

    // ── acceptAlert() ─────────────────────────────────────────
    /**
     * Waits for a JavaScript alert and accepts it.
     *
     * Example:
     *   acceptAlert();
     */
    protected void acceptAlert() {
        log.debug("Accepting alert");
        WaitUtils.waitForAlert(driver()).accept();
    }


    // ── dismissAlert() ────────────────────────────────────────
    /**
     * Waits for a JavaScript alert and dismisses it.
     *
     * Example:
     *   dismissAlert();
     */
    protected void dismissAlert() {
        log.debug("Dismissing alert");
        WaitUtils.waitForAlert(driver()).dismiss();
    }


    // ── getAlertText() ────────────────────────────────────────
    /**
     * Returns the text of a JavaScript alert without dismissing it.
     *
     * Example:
     *   String message = getAlertText();
     */
    protected String getAlertText() {
        String text = WaitUtils.waitForAlert(driver()).getText();
        log.debug("Alert text: [{}]", text);
        return text;
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Finds a single element, waiting for it to be present first.
     */
    private WebElement findElement(By locator) {
        return WaitUtils.waitForPresent(driver(), locator);
    }

}