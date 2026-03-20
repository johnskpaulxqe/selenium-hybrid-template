package utils;

/*
  ============================================================
  FILE: JavaScriptUtils.java
  LOCATION: src/test/java/utils/JavaScriptUtils.java

  PURPOSE:
    Wraps Selenium's JavascriptExecutor to provide clean,
    reusable JS-based interactions. Used when standard
    Selenium WebElement methods are unreliable or blocked.
    Responsibilities:
      1. Click elements that are overlapped or intercepted
      2. Scroll to elements and page positions
      3. Highlight elements during debugging
      4. Get/set element values and attributes via JS
      5. Execute async JavaScript
      6. Manipulate browser state (localStorage, sessionStorage)

  WHEN TO USE JS INSTEAD OF SELENIUM:
    - Element is obscured by an overlay/modal
    - Element is inside a shadow DOM
    - Standard click() throws ElementClickInterceptedException
    - You need to scroll to a specific pixel position
    - You need to read/write localStorage or sessionStorage
    - Standard sendKeys() is blocked by the application

  HOW TO USE:
    // JS click when standard click is intercepted
    JavaScriptUtils.click(driver, element);

    // Scroll element into view before interacting
    JavaScriptUtils.scrollIntoView(driver, element);

    // Set a value in a field that blocks sendKeys
    JavaScriptUtils.setValue(driver, element, "test@example.com");

    // Highlight element for visual debugging
    JavaScriptUtils.highlight(driver, element);

    // Read from localStorage
    String token = JavaScriptUtils.getLocalStorage(driver, "authToken");

  TODO (customise per project):
    - TODO-1 : Add any app-specific JS helpers your project needs
    - TODO-2 : Extend shadow DOM support if your app uses web components
    - TODO-3 : Add JS helpers for your specific SPA framework if needed
               (Angular, React, Vue have framework-specific JS APIs)
  ============================================================
*/

import org.openqa.selenium.*;
import org.slf4j.Logger;

public class JavaScriptUtils {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(JavaScriptUtils.class);

    // ── Highlight colour used during debugging ─────────────────
    private static final String HIGHLIGHT_STYLE  =
            "arguments[0].style.border='3px solid red'";
    private static final String RESTORE_STYLE    =
            "arguments[0].style.border=''";

    // Private constructor — static utility class
    private JavaScriptUtils() {}


    // ═══════════════════════════════════════════════════════════
    // CLICK
    // ═══════════════════════════════════════════════════════════

    // ── click() ───────────────────────────────────────────────
    /**
     * Clicks an element using JavaScript.
     * Use when standard WebElement.click() throws:
     *   - ElementClickInterceptedException
     *   - ElementNotInteractableException
     *
     * Example:
     *   JavaScriptUtils.click(driver, element);
     */
    public static void click(WebDriver driver, WebElement element) {
        log.debug("JS click on element: [{}]", describeElement(element));
        try {
            getExecutor(driver).executeScript("arguments[0].click();", element);
        } catch (JavascriptException e) {
            throw new RuntimeException("JS click failed on element: " +
                    describeElement(element), e);
        }
    }


    // ── clickBySelector() ─────────────────────────────────────
    /**
     * Clicks an element found via a CSS selector string.
     * Useful when you have the selector but not the WebElement.
     *
     * Example:
     *   JavaScriptUtils.clickBySelector(driver, "#submitBtn");
     */
    public static void clickBySelector(WebDriver driver, String cssSelector) {
        log.debug("JS click by CSS selector: [{}]", cssSelector);
        try {
            getExecutor(driver).executeScript(
                    "document.querySelector(arguments[0]).click();", cssSelector);
        } catch (JavascriptException e) {
            throw new RuntimeException("JS click by selector failed: [" + cssSelector + "]", e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // SCROLL
    // ═══════════════════════════════════════════════════════════

    // ── scrollIntoView() ──────────────────────────────────────
    /**
     * Scrolls the element into the visible viewport.
     * Always call this before interacting with elements
     * that might be off-screen (especially in long pages).
     *
     * Example:
     *   JavaScriptUtils.scrollIntoView(driver, footerElement);
     *   footerElement.click();
     */
    public static void scrollIntoView(WebDriver driver, WebElement element) {
        log.debug("Scrolling element into view: [{}]", describeElement(element));
        getExecutor(driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                element);

        // Brief pause to let smooth scroll animation complete
        pause(300);
    }


    // ── scrollToTop() ─────────────────────────────────────────
    /**
     * Scrolls the page back to the very top (0, 0).
     *
     * Example:
     *   JavaScriptUtils.scrollToTop(driver);
     */
    public static void scrollToTop(WebDriver driver) {
        log.debug("Scrolling page to top");
        getExecutor(driver).executeScript("window.scrollTo(0, 0);");
    }


    // ── scrollToBottom() ──────────────────────────────────────
    /**
     * Scrolls to the very bottom of the page.
     * Useful for infinite-scroll pages or lazy-loaded content.
     *
     * Example:
     *   JavaScriptUtils.scrollToBottom(driver);
     */
    public static void scrollToBottom(WebDriver driver) {
        log.debug("Scrolling page to bottom");
        getExecutor(driver).executeScript(
                "window.scrollTo(0, document.body.scrollHeight);");
    }


    // ── scrollByPixels() ──────────────────────────────────────
    /**
     * Scrolls the page by a given number of pixels.
     * Positive y scrolls down, negative scrolls up.
     *
     * Example:
     *   JavaScriptUtils.scrollByPixels(driver, 0, 500);  // scroll down 500px
     *   JavaScriptUtils.scrollByPixels(driver, 0, -200); // scroll up 200px
     */
    public static void scrollByPixels(WebDriver driver, int x, int y) {
        log.debug("Scrolling by pixels → x: {}, y: {}", x, y);
        getExecutor(driver).executeScript(
                "window.scrollBy(arguments[0], arguments[1]);", x, y);
    }


    // ── scrollToPosition() ────────────────────────────────────
    /**
     * Scrolls to an exact pixel coordinate on the page.
     *
     * Example:
     *   JavaScriptUtils.scrollToPosition(driver, 0, 1200);
     */
    public static void scrollToPosition(WebDriver driver, int x, int y) {
        log.debug("Scrolling to position → x: {}, y: {}", x, y);
        getExecutor(driver).executeScript(
                "window.scrollTo(arguments[0], arguments[1]);", x, y);
    }


    // ═══════════════════════════════════════════════════════════
    // ELEMENT VALUE AND ATTRIBUTE
    // ═══════════════════════════════════════════════════════════

    // ── setValue() ────────────────────────────────────────────
    /**
     * Sets the value of an input field via JavaScript.
     * Use when sendKeys() is blocked by the application
     * (e.g. React/Angular controlled inputs, read-only fields
     * that need value injection for testing).
     *
     * Example:
     *   JavaScriptUtils.setValue(driver, dateField, "2024-12-31");
     */
    public static void setValue(WebDriver driver, WebElement element, String value) {
        log.debug("Setting value [{}] on element via JS: [{}]",
                value, describeElement(element));
        getExecutor(driver).executeScript(
                "arguments[0].value = arguments[1];", element, value);
    }


    // ── getValue() ────────────────────────────────────────────
    /**
     * Gets the current value of an input field via JavaScript.
     *
     * Example:
     *   String currentVal = JavaScriptUtils.getValue(driver, inputField);
     */
    public static String getValue(WebDriver driver, WebElement element) {
        Object result = getExecutor(driver).executeScript(
                "return arguments[0].value;", element);
        return result != null ? result.toString() : "";
    }


    // ── getAttribute() ────────────────────────────────────────
    /**
     * Gets any attribute of an element via JavaScript.
     * More reliable than WebElement.getAttribute() for some
     * dynamic attribute states.
     *
     * Example:
     *   String placeholder = JavaScriptUtils.getAttribute(driver, field, "placeholder");
     *   String dataId      = JavaScriptUtils.getAttribute(driver, row, "data-id");
     */
    public static String getAttribute(WebDriver driver, WebElement element, String attribute) {
        Object result = getExecutor(driver).executeScript(
                "return arguments[0].getAttribute(arguments[1]);", element, attribute);
        return result != null ? result.toString() : "";
    }


    // ── setAttribute() ────────────────────────────────────────
    /**
     * Sets any attribute on an element via JavaScript.
     * Useful for removing 'readonly', 'disabled', or 'hidden'
     * attributes in test environments.
     *
     * Example:
     *   // Remove readonly from a date field
     *   JavaScriptUtils.setAttribute(driver, dateField, "readonly", "false");
     *
     *   // Make a hidden element visible
     *   JavaScriptUtils.setAttribute(driver, hiddenDiv, "style", "display:block");
     */
    public static void setAttribute(WebDriver driver, WebElement element,
                                    String attribute, String value) {
        log.debug("Setting attribute [{}={}] on element: [{}]",
                attribute, value, describeElement(element));
        getExecutor(driver).executeScript(
                "arguments[0].setAttribute(arguments[1], arguments[2]);",
                element, attribute, value);
    }


    // ── removeAttribute() ────────────────────────────────────
    /**
     * Removes an attribute from an element.
     *
     * Example:
     *   // Remove disabled attribute to force-enable a button
     *   JavaScriptUtils.removeAttribute(driver, submitBtn, "disabled");
     */
    public static void removeAttribute(WebDriver driver, WebElement element, String attribute) {
        log.debug("Removing attribute [{}] from element: [{}]",
                attribute, describeElement(element));
        getExecutor(driver).executeScript(
                "arguments[0].removeAttribute(arguments[1]);", element, attribute);
    }


    // ── getInnerText() ────────────────────────────────────────
    /**
     * Gets the visible text of an element via JS innerText.
     * More reliable than element.getText() for some dynamic content.
     *
     * Example:
     *   String text = JavaScriptUtils.getInnerText(driver, labelElement);
     */
    public static String getInnerText(WebDriver driver, WebElement element) {
        Object result = getExecutor(driver).executeScript(
                "return arguments[0].innerText;", element);
        return result != null ? result.toString().trim() : "";
    }


    // ═══════════════════════════════════════════════════════════
    // BROWSER STORAGE
    // ═══════════════════════════════════════════════════════════

    // ── getLocalStorage() ────────────────────────────────────
    /**
     * Reads a value from the browser's localStorage.
     *
     * Example:
     *   String token = JavaScriptUtils.getLocalStorage(driver, "authToken");
     */
    public static String getLocalStorage(WebDriver driver, String key) {
        Object result = getExecutor(driver).executeScript(
                "return window.localStorage.getItem(arguments[0]);", key);
        return result != null ? result.toString() : null;
    }


    // ── setLocalStorage() ────────────────────────────────────
    /**
     * Sets a value in the browser's localStorage.
     * Useful for injecting auth tokens in test setup.
     *
     * Example:
     *   JavaScriptUtils.setLocalStorage(driver, "authToken", "Bearer abc123");
     */
    public static void setLocalStorage(WebDriver driver, String key, String value) {
        log.debug("Setting localStorage key [{}]", key);
        getExecutor(driver).executeScript(
                "window.localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }


    // ── clearLocalStorage() ──────────────────────────────────
    /**
     * Clears all entries from localStorage.
     * Call from Hooks.java @After for clean state between scenarios.
     *
     * Example:
     *   JavaScriptUtils.clearLocalStorage(driver);
     */
    public static void clearLocalStorage(WebDriver driver) {
        log.debug("Clearing localStorage");
        getExecutor(driver).executeScript("window.localStorage.clear();");
    }


    // ── getSessionStorage() ───────────────────────────────────
    /**
     * Reads a value from the browser's sessionStorage.
     *
     * Example:
     *   String sessionId = JavaScriptUtils.getSessionStorage(driver, "sessionId");
     */
    public static String getSessionStorage(WebDriver driver, String key) {
        Object result = getExecutor(driver).executeScript(
                "return window.sessionStorage.getItem(arguments[0]);", key);
        return result != null ? result.toString() : null;
    }


    // ═══════════════════════════════════════════════════════════
    // PAGE INFORMATION
    // ═══════════════════════════════════════════════════════════

    // ── getPageTitle() ────────────────────────────────────────
    /**
     * Returns the page title via JS.
     *
     * Example:
     *   String title = JavaScriptUtils.getPageTitle(driver);
     */
    public static String getPageTitle(WebDriver driver) {
        Object result = getExecutor(driver).executeScript("return document.title;");
        return result != null ? result.toString() : "";
    }


    // ── getPageUrl() ──────────────────────────────────────────
    /**
     * Returns the current page URL via JS.
     *
     * Example:
     *   String url = JavaScriptUtils.getPageUrl(driver);
     */
    public static String getPageUrl(WebDriver driver) {
        Object result = getExecutor(driver).executeScript("return window.location.href;");
        return result != null ? result.toString() : "";
    }


    // ── isElementInViewport() ────────────────────────────────
    /**
     * Returns true if the element is currently within the
     * visible viewport (not scrolled off screen).
     *
     * Example:
     *   boolean visible = JavaScriptUtils.isElementInViewport(driver, element);
     */
    public static boolean isElementInViewport(WebDriver driver, WebElement element) {
        Object result = getExecutor(driver).executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                        "return (" +
                        "  rect.top >= 0 &&" +
                        "  rect.left >= 0 &&" +
                        "  rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&" +
                        "  rect.right <= (window.innerWidth || document.documentElement.clientWidth)" +
                        ");",
                element);
        return Boolean.TRUE.equals(result);
    }


    // ═══════════════════════════════════════════════════════════
    // DEBUGGING HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── highlight() ──────────────────────────────────────────
    /**
     * Adds a red border around an element for visual debugging.
     * The highlight is removed after a short delay.
     * Only useful in headed (non-headless) browser runs.
     *
     * Example:
     *   JavaScriptUtils.highlight(driver, problematicElement);
     */
    public static void highlight(WebDriver driver, WebElement element) {
        log.debug("Highlighting element: [{}]", describeElement(element));
        try {
            // Apply red border
            getExecutor(driver).executeScript(HIGHLIGHT_STYLE, element);
            // Wait briefly so it's visible
            pause(500);
            // Remove the border
            getExecutor(driver).executeScript(RESTORE_STYLE, element);
        } catch (Exception e) {
            // Highlighting is non-critical — log and continue
            log.warn("Could not highlight element: {}", e.getMessage());
        }
    }


    // ── executeScript() ───────────────────────────────────────
    /**
     * Executes arbitrary JavaScript and returns the result.
     * Use for one-off JS calls not covered by the methods above.
     *
     * Example:
     *   Object result = JavaScriptUtils.executeScript(driver,
     *       "return document.querySelectorAll('li.active').length;");
     */
    public static Object executeScript(WebDriver driver, String script, Object... args) {
        log.debug("Executing JS: [{}]", script);
        return getExecutor(driver).executeScript(script, args);
    }


    // ── executeAsyncScript() ─────────────────────────────────
    /**
     * Executes asynchronous JavaScript (e.g. for callbacks/Promises).
     * The script must call the callback provided as the last argument.
     *
     * Example:
     *   Object result = JavaScriptUtils.executeAsyncScript(driver,
     *       "var callback = arguments[arguments.length - 1];" +
     *       "setTimeout(function(){ callback('done'); }, 1000);");
     */
    public static Object executeAsyncScript(WebDriver driver, String script, Object... args) {
        log.debug("Executing async JS: [{}]", script);
        return getExecutor(driver).executeAsyncScript(script, args);
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Casts the WebDriver to JavascriptExecutor.
     * All modern WebDriver implementations support this.
     */
    private static JavascriptExecutor getExecutor(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException(
                    "WebDriver does not support JavaScript execution: " +
                            driver.getClass().getName());
        }
        return (JavascriptExecutor) driver;
    }

    /**
     * Returns a short descriptive string for an element for logging.
     * Tries tag name first, falls back to class name on error.
     */
    private static String describeElement(WebElement element) {
        try {
            return element.getTagName() + "[" + element.getAttribute("id") + "]";
        } catch (Exception e) {
            return element.getClass().getSimpleName();
        }
    }

    /**
     * Pauses the current thread briefly.
     * Only used in non-critical visual helpers (highlight, scrollIntoView).
     * Never use this as a replacement for proper explicit waits.
     */
    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}