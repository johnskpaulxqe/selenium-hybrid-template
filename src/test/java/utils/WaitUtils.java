package utils;

/*
  ============================================================
  FILE: WaitUtils.java
  LOCATION: src/test/java/utils/WaitUtils.java

  PURPOSE:
    Centralises all Selenium waiting strategies in one place.
    Provides explicit and fluent wait helpers that are used
    throughout every Page Object in the framework.
    Responsibilities:
      1. Explicit waits — wait for specific element conditions
      2. Fluent waits  — poll at intervals with custom ignore list
      3. Page-level waits — wait for page load, JS, Ajax (jQuery)
      4. Custom condition waits — URL, title, alert, frame

  WHY NO IMPLICIT WAITS?
    Implicit waits are set to 0 in DriverManager intentionally.
    Mixing implicit and explicit waits causes unpredictable
    double-wait behaviour. All waiting is done here explicitly.

  HOW TO USE:
    // Wait for element to be clickable then click
    WebElement btn = WaitUtils.waitForClickable(driver, By.id("submit"));
    btn.click();

    // Wait for element to be visible
    WebElement el = WaitUtils.waitForVisible(driver, By.id("username"));

    // Wait for text to appear in element
    WaitUtils.waitForTextPresent(driver, By.id("message"), "Success");

    // Wait for page to fully load
    WaitUtils.waitForPageLoad(driver);

    // Custom timeout override
    WebElement el = WaitUtils.waitForVisible(driver, By.id("loader"), 30);

  TODO (customise per project):
    - TODO-1 : Adjust DEFAULT_TIMEOUT if your app is slower/faster
    - TODO-2 : Add waitForAjax() with your framework's JS pattern
               if not using jQuery (e.g. Angular, React, custom)
    - TODO-3 : Add waitForElement(By, customCondition) if needed
  ============================================================
*/

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class WaitUtils {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(WaitUtils.class);

    // ── Default timeouts ──────────────────────────────────────
    // TODO-1: Adjust these to match your application's response time
    private static final int DEFAULT_TIMEOUT =
            ConfigReader.getInt("timeouts.explicitWait", 10);

    private static final int PAGE_LOAD_TIMEOUT =
            ConfigReader.getInt("timeouts.pageLoadTimeout", 30);

    private static final int POLLING_INTERVAL =
            ConfigReader.getInt("timeouts.fluentWaitPollingInterval", 500);

    // Private constructor — static utility class
    private WaitUtils() {}


    // ═══════════════════════════════════════════════════════════
    // EXPLICIT WAIT — ELEMENT CONDITIONS
    // ═══════════════════════════════════════════════════════════

    // ── waitForVisible() ──────────────────────────────────────
    /**
     * Waits until the element located by the given By is visible
     * in the DOM and has non-zero dimensions.
     *
     * Example:
     *   WebElement msg = WaitUtils.waitForVisible(driver, By.id("successMsg"));
     */
    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return waitForVisible(driver, locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisible(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be visible: [{}] (timeout: {}s)", locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element not visible after %ds: [%s]", timeoutSeconds, locator), e);
        }
    }


    // ── waitForVisible(WebElement) ────────────────────────────
    /**
     * Waits until an already-located WebElement becomes visible.
     * Use when you already have a WebElement reference.
     *
     * Example:
     *   WebElement el = driver.findElement(By.id("banner"));
     *   WaitUtils.waitForVisible(driver, el);
     */
    public static WebElement waitForVisible(WebDriver driver, WebElement element) {
        return waitForVisible(driver, element, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisible(WebDriver driver, WebElement element, int timeoutSeconds) {
        log.debug("Waiting for WebElement to be visible (timeout: {}s)", timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.visibilityOf(element));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("WebElement not visible after %ds", timeoutSeconds), e);
        }
    }


    // ── waitForClickable() ────────────────────────────────────
    /**
     * Waits until the element is visible AND enabled (clickable).
     * Use before every click() in Page Objects.
     *
     * Example:
     *   WaitUtils.waitForClickable(driver, By.id("loginBtn")).click();
     */
    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return waitForClickable(driver, locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickable(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be clickable: [{}] (timeout: {}s)", locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element not clickable after %ds: [%s]", timeoutSeconds, locator), e);
        }
    }

    public static WebElement waitForClickable(WebDriver driver, WebElement element) {
        return waitForClickable(driver, element, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickable(WebDriver driver, WebElement element, int timeoutSeconds) {
        log.debug("Waiting for WebElement to be clickable (timeout: {}s)", timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.elementToBeClickable(element));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("WebElement not clickable after %ds", timeoutSeconds), e);
        }
    }


    // ── waitForPresent() ──────────────────────────────────────
    /**
     * Waits until the element is present in the DOM.
     * Note: present does NOT mean visible — element may be hidden.
     * Use waitForVisible() if you need it to be seen on screen.
     *
     * Example:
     *   WebElement hidden = WaitUtils.waitForPresent(driver, By.id("hiddenField"));
     */
    public static WebElement waitForPresent(WebDriver driver, By locator) {
        return waitForPresent(driver, locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForPresent(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be present in DOM: [{}] (timeout: {}s)",
                locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element not present after %ds: [%s]", timeoutSeconds, locator), e);
        }
    }


    // ── waitForInvisible() ────────────────────────────────────
    /**
     * Waits until the element is no longer visible or is removed
     * from the DOM. Useful for waiting for loaders/spinners to disappear.
     *
     * Example:
     *   WaitUtils.waitForInvisible(driver, By.id("loadingSpinner"));
     */
    public static boolean waitForInvisible(WebDriver driver, By locator) {
        return waitForInvisible(driver, locator, DEFAULT_TIMEOUT);
    }

    public static boolean waitForInvisible(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be invisible: [{}] (timeout: {}s)",
                locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element still visible after %ds: [%s]", timeoutSeconds, locator), e);
        }
    }


    // ── waitForAllVisible() ───────────────────────────────────
    /**
     * Waits until ALL elements matching the locator are visible.
     * Returns the list of visible elements.
     *
     * Example:
     *   List<WebElement> rows = WaitUtils.waitForAllVisible(driver, By.cssSelector("tr.data-row"));
     */
    public static List<WebElement> waitForAllVisible(WebDriver driver, By locator) {
        return waitForAllVisible(driver, locator, DEFAULT_TIMEOUT);
    }

    public static List<WebElement> waitForAllVisible(WebDriver driver, By locator,
                                                     int timeoutSeconds) {
        log.debug("Waiting for all elements to be visible: [{}] (timeout: {}s)",
                locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Not all elements visible after %ds: [%s]", timeoutSeconds, locator), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // EXPLICIT WAIT — TEXT AND ATTRIBUTE CONDITIONS
    // ═══════════════════════════════════════════════════════════

    // ── waitForTextPresent() ──────────────────────────────────
    /**
     * Waits until the element contains the expected text.
     *
     * Example:
     *   WaitUtils.waitForTextPresent(driver, By.id("statusMsg"), "Login successful");
     */
    public static boolean waitForTextPresent(WebDriver driver, By locator, String text) {
        return waitForTextPresent(driver, locator, text, DEFAULT_TIMEOUT);
    }

    public static boolean waitForTextPresent(WebDriver driver, By locator,
                                             String text, int timeoutSeconds) {
        log.debug("Waiting for text [{}] in element [{}] (timeout: {}s)",
                text, locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Text [%s] not present in [%s] after %ds",
                            text, locator, timeoutSeconds), e);
        }
    }


    // ── waitForAttributeContains() ────────────────────────────
    /**
     * Waits until an element's attribute contains the given value.
     * Useful for waiting on CSS class changes or disabled states.
     *
     * Example:
     *   // Wait for button to become disabled
     *   WaitUtils.waitForAttributeContains(driver, By.id("submitBtn"), "class", "disabled");
     */
    public static boolean waitForAttributeContains(WebDriver driver, By locator,
                                                   String attribute, String value) {
        return waitForAttributeContains(driver, locator, attribute, value, DEFAULT_TIMEOUT);
    }

    public static boolean waitForAttributeContains(WebDriver driver, By locator,
                                                   String attribute, String value,
                                                   int timeoutSeconds) {
        log.debug("Waiting for attribute [{}] to contain [{}] on [{}] (timeout: {}s)",
                attribute, value, locator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.attributeContains(locator, attribute, value));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Attribute [%s] did not contain [%s] after %ds",
                            attribute, value, timeoutSeconds), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // EXPLICIT WAIT — PAGE AND URL CONDITIONS
    // ═══════════════════════════════════════════════════════════

    // ── waitForUrlContains() ─────────────────────────────────
    /**
     * Waits until the current URL contains the given fragment.
     * Useful after navigation or redirects.
     *
     * Example:
     *   WaitUtils.waitForUrlContains(driver, "/dashboard");
     */
    public static boolean waitForUrlContains(WebDriver driver, String urlFragment) {
        return waitForUrlContains(driver, urlFragment, DEFAULT_TIMEOUT);
    }

    public static boolean waitForUrlContains(WebDriver driver, String urlFragment,
                                             int timeoutSeconds) {
        log.debug("Waiting for URL to contain [{}] (timeout: {}s)", urlFragment, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.urlContains(urlFragment));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("URL did not contain [%s] after %ds. Current URL: [%s]",
                            urlFragment, timeoutSeconds, driver.getCurrentUrl()), e);
        }
    }


    // ── waitForTitleContains() ────────────────────────────────
    /**
     * Waits until the page title contains the given text.
     *
     * Example:
     *   WaitUtils.waitForTitleContains(driver, "Dashboard");
     */
    public static boolean waitForTitleContains(WebDriver driver, String titleFragment) {
        return waitForTitleContains(driver, titleFragment, DEFAULT_TIMEOUT);
    }

    public static boolean waitForTitleContains(WebDriver driver, String titleFragment,
                                               int timeoutSeconds) {
        log.debug("Waiting for title to contain [{}] (timeout: {}s)", titleFragment, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.titleContains(titleFragment));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Title did not contain [%s] after %ds. Current title: [%s]",
                            titleFragment, timeoutSeconds, driver.getTitle()), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // EXPLICIT WAIT — ALERTS AND FRAMES
    // ═══════════════════════════════════════════════════════════

    // ── waitForAlert() ────────────────────────────────────────
    /**
     * Waits until a JavaScript alert/confirm/prompt is present.
     * Returns the Alert so it can be accepted or dismissed.
     *
     * Example:
     *   Alert alert = WaitUtils.waitForAlert(driver);
     *   alert.accept();
     */
    public static Alert waitForAlert(WebDriver driver) {
        return waitForAlert(driver, DEFAULT_TIMEOUT);
    }

    public static Alert waitForAlert(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting for alert (timeout: {}s)", timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.alertIsPresent());
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Alert not present after %ds", timeoutSeconds), e);
        }
    }


    // ── waitForFrameAndSwitch() ───────────────────────────────
    /**
     * Waits until the iframe is available then switches to it.
     *
     * Example:
     *   WaitUtils.waitForFrameAndSwitch(driver, By.id("paymentFrame"));
     */
    public static WebDriver waitForFrameAndSwitch(WebDriver driver, By frameLocator) {
        return waitForFrameAndSwitch(driver, frameLocator, DEFAULT_TIMEOUT);
    }

    public static WebDriver waitForFrameAndSwitch(WebDriver driver, By frameLocator,
                                                  int timeoutSeconds) {
        log.debug("Waiting for frame [{}] and switching (timeout: {}s)",
                frameLocator, timeoutSeconds);
        try {
            return buildWait(driver, timeoutSeconds)
                    .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Frame [%s] not available after %ds", frameLocator, timeoutSeconds), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // PAGE LOAD WAITS
    // ═══════════════════════════════════════════════════════════

    // ── waitForPageLoad() ─────────────────────────────────────
    /**
     * Waits until document.readyState === "complete".
     * Call this after driver.get() or any navigation that
     * triggers a full page reload.
     *
     * Example:
     *   driver.get(url);
     *   WaitUtils.waitForPageLoad(driver);
     */
    public static void waitForPageLoad(WebDriver driver) {
        waitForPageLoad(driver, PAGE_LOAD_TIMEOUT);
    }

    public static void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting for page load complete (timeout: {}s)", timeoutSeconds);
        try {
            buildWait(driver, timeoutSeconds).until(webDriver ->
                    ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState")
                            .equals("complete"));
            log.debug("Page load complete");
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Page did not load within %ds", timeoutSeconds), e);
        }
    }


    // ── waitForJQueryAjax() ───────────────────────────────────
    /**
     * Waits until all jQuery AJAX calls have completed.
     * Only works on pages that use jQuery.
     * Silently skips if jQuery is not present on the page.
     *
     * TODO-2: Replace with your framework's AJAX completion
     *         check if not using jQuery.
     *
     * Example:
     *   WaitUtils.waitForJQueryAjax(driver);
     */
    public static void waitForJQueryAjax(WebDriver driver) {
        waitForJQueryAjax(driver, DEFAULT_TIMEOUT);
    }

    public static void waitForJQueryAjax(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting for jQuery AJAX to complete (timeout: {}s)", timeoutSeconds);
        try {
            buildWait(driver, timeoutSeconds).until(webDriver -> {
                JavascriptExecutor js = (JavascriptExecutor) webDriver;
                // Check jQuery is defined before calling $.active
                Object jQueryDefined = js.executeScript(
                        "return typeof jQuery !== 'undefined'");
                if (Boolean.FALSE.equals(jQueryDefined)) {
                    return true; // jQuery not present — nothing to wait for
                }
                Object activeRequests = js.executeScript("return jQuery.active");
                return activeRequests != null && activeRequests.toString().equals("0");
            });
        } catch (TimeoutException e) {
            log.warn("jQuery AJAX did not complete within {}s — continuing", timeoutSeconds);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // FLUENT WAIT
    // ═══════════════════════════════════════════════════════════

    // ── fluentWait() ──────────────────────────────────────────
    /**
     * Creates a FluentWait that polls every POLLING_INTERVAL ms,
     * ignoring StaleElementReferenceException and NoSuchElementException.
     * Use for elements that appear/disappear unpredictably.
     *
     * Example:
     *   WebElement el = WaitUtils.fluentWait(driver, By.id("dynamicEl"), 15);
     */
    public static WebElement fluentWait(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("FluentWait for element [{}] (timeout: {}s, poll: {}ms)",
                locator, timeoutSeconds, POLLING_INTERVAL);

        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(POLLING_INTERVAL))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        try {
            return wait.until(d -> d.findElement(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Element [%s] not found after %ds with fluent wait",
                            locator, timeoutSeconds), e);
        }
    }


    // ── fluentWaitForCondition() ──────────────────────────────
    /**
     * Fluent wait with a fully custom condition function.
     * Use when none of the standard ExpectedConditions apply.
     *
     * Example — wait for a list to have more than 5 items:
     *   WaitUtils.fluentWaitForCondition(driver,
     *       d -> d.findElements(By.cssSelector("li.result")).size() > 5,
     *       10, "Result list to have more than 5 items");
     */
    public static <T> T fluentWaitForCondition(WebDriver driver,
                                               Function<WebDriver, T> condition,
                                               int timeoutSeconds,
                                               String conditionDescription) {
        log.debug("FluentWait for condition [{}] (timeout: {}s)", conditionDescription, timeoutSeconds);

        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(POLLING_INTERVAL))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .withMessage("Condition not met: " + conditionDescription);

        return wait.until(condition);
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── buildWait() ───────────────────────────────────────────
    /**
     * Builds a standard WebDriverWait with the given timeout.
     * All explicit wait methods use this internally.
     */
    private static WebDriverWait buildWait(WebDriver driver, int timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

}