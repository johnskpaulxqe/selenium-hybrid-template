package utils;

/*
  ============================================================
  FILE: DriverManager.java
  LOCATION: src/test/java/utils/DriverManager.java

  PURPOSE:
    Central WebDriver factory and ThreadLocal manager.
    Responsible for:
      1. Creating the correct WebDriver instance based on the
         browser value from config.json or -Dbrowser CLI arg
      2. Storing the driver in a ThreadLocal so parallel test
         threads never share or clash on the same driver instance
      3. Providing a single getDriver() access point used by
         every Page Object and Step Definition
      4. Quitting and cleaning up the driver after each scenario

  HOW TO USE:
    // In Hooks.java @Before — create driver before each scenario
    DriverManager.initDriver();

    // In any Page Object or Step — access the driver
    WebDriver driver = DriverManager.getDriver();

    // In Hooks.java @After — quit driver after each scenario
    DriverManager.quitDriver();

  SUPPORTED BROWSERS:
    chrome   → standard Chrome window
    firefox  → standard Firefox window
    edge     → Microsoft Edge
    headless → Chrome in headless mode (default for CI)

  TODO (customise per project):
    - TODO-1 : Add any additional Chrome/Firefox options your app needs
    - TODO-2 : Add remote/grid support (Selenium Grid / BrowserStack)
    - TODO-3 : Change default browser if your team prefers Firefox
  ============================================================
*/

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

public class DriverManager {

    // ── ThreadLocal storage ───────────────────────────────────
    // Each thread (parallel scenario) gets its own WebDriver.
    // This prevents test interference in parallel execution.
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Logger — uses LogUtil wrapper (built in Step 8)
    private static final Logger log = LogUtil.getLogger(DriverManager.class);

    // Private constructor — this is a static utility class,
    // it should never be instantiated directly.
    private DriverManager() {}


    // ── initDriver() ──────────────────────────────────────────
    /**
     * Creates and stores a WebDriver instance for the current thread.
     * Called once per scenario from Hooks.java @Before method.
     *
     * Browser is resolved in this priority order:
     *   1. -Dbrowser=xxx  (Maven CLI argument)
     *   2. config.json "browser.default" value
     *   3. Falls back to "headless" if neither is set
     *
     * Example:
     *   mvn test -Dbrowser=firefox   → launches Firefox
     *   mvn test -Dbrowser=headless  → launches headless Chrome
     */
    public static void initDriver() {

        // Resolve browser from system property first, then config, then default
        String browser = System.getProperty("browser",
                        ConfigReader.get("browser.default", "headless"))
                .toLowerCase().trim();

        log.info("Initialising WebDriver → browser: [{}]", browser);

        WebDriver driver = switch (browser) {

            // ── Chrome ───────────────────────────────────────
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = buildChromeOptions(false);
                log.debug("Launching Chrome with options: {}", options.asMap());
                yield new ChromeDriver(options);
            }

            // ── Headless Chrome ───────────────────────────────
            // Default for CI pipelines — no display required
            case "headless" -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = buildChromeOptions(true);
                log.debug("Launching Headless Chrome with options: {}", options.asMap());
                yield new ChromeDriver(options);
            }

            // ── Firefox ───────────────────────────────────────
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = buildFirefoxOptions(false);
                log.debug("Launching Firefox with options: {}", options.asMap());
                yield new FirefoxDriver(options);
            }

            // ── Headless Firefox ──────────────────────────────
            case "firefox-headless" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = buildFirefoxOptions(true);
                log.debug("Launching Headless Firefox with options: {}", options.asMap());
                yield new FirefoxDriver(options);
            }

            // ── Microsoft Edge ────────────────────────────────
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions options = buildEdgeOptions();
                log.debug("Launching Edge with options: {}", options.asMap());
                yield new EdgeDriver(options);
            }

            // ── Unknown browser value ─────────────────────────
            default -> {
                log.warn("Unknown browser [{}] — falling back to headless Chrome", browser);
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver(buildChromeOptions(true));
            }
        };

        // Apply timeouts from config
        applyTimeouts(driver);

        // Store driver in ThreadLocal for this thread
        driverThreadLocal.set(driver);

        log.info("WebDriver initialised successfully for thread [{}]",
                Thread.currentThread().getName());
    }


    // ── getDriver() ───────────────────────────────────────────
    /**
     * Returns the WebDriver instance for the current thread.
     * Call this from any Page Object or Step Definition.
     *
     * Example:
     *   WebDriver driver = DriverManager.getDriver();
     *   driver.findElement(By.id("username")).sendKeys("admin");
     *
     * @throws IllegalStateException if initDriver() was not called first
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver is null. Ensure DriverManager.initDriver() " +
                            "is called in @Before hook before accessing the driver.");
        }
        return driver;
    }


    // ── quitDriver() ─────────────────────────────────────────
    /**
     * Quits the WebDriver and removes it from the ThreadLocal.
     * Must be called in Hooks.java @After to prevent browser
     * processes leaking between scenarios.
     *
     * Example:
     *   DriverManager.quitDriver();
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver quit successfully for thread [{}]",
                        Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("Exception while quitting WebDriver: {}", e.getMessage());
            } finally {
                // Always remove from ThreadLocal to prevent memory leaks
                driverThreadLocal.remove();
            }
        }
    }


    // ── isDriverInitialised() ────────────────────────────────
    /**
     * Returns true if a driver exists for the current thread.
     * Useful in @After hooks to safely check before quitting.
     *
     * Example:
     *   if (DriverManager.isDriverInitialised()) {
     *       DriverManager.quitDriver();
     *   }
     */
    public static boolean isDriverInitialised() {
        return driverThreadLocal.get() != null;
    }


    // ── buildChromeOptions() ──────────────────────────────────
    /**
     * Builds ChromeOptions with sensible defaults for both
     * headed and headless modes.
     *
     * TODO-1: Add any additional args your application needs.
     * Common additions:
     *   --disable-extensions
     *   --ignore-certificate-errors
     *   --proxy-server=http://proxy:8080
     */
    private static ChromeOptions buildChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        // Core args — required for CI stability
        List<String> args = new java.util.ArrayList<>(List.of(
                "--no-sandbox",               // Required in Docker/Linux CI
                "--disable-dev-shm-usage",    // Prevents shared memory crashes in CI
                "--disable-gpu",              // Avoids GPU rendering issues in headless
                "--disable-extensions",       // Cleaner test environment
                "--window-size=1920,1080"     // Consistent viewport for screenshots
        ));

        if (headless) {
            // New headless mode (Chrome 112+) — more stable than --headless=old
            args.add("--headless=new");
        } else {
            // Maximise browser window in headed mode
            args.add("--start-maximized");
        }

        // Read any additional args from config.json
        // TODO-1: Config key: browser.options.additionalArgs (array of strings)
        options.addArguments(args);

        // Accept insecure certificates (useful for test environments with self-signed certs)
        options.setAcceptInsecureCerts(
                ConfigReader.getBoolean("browser.options.acceptInsecureCerts", true));

        return options;
    }


    // ── buildFirefoxOptions() ─────────────────────────────────
    /**
     * Builds FirefoxOptions for headed and headless modes.
     * TODO-1: Add Firefox-specific preferences if needed.
     */
    private static FirefoxOptions buildFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
            options.addArguments("--width=1920");
            options.addArguments("--height=1080");
        } else {
            options.addArguments("--width=1920");
            options.addArguments("--height=1080");
        }

        options.setAcceptInsecureCerts(
                ConfigReader.getBoolean("browser.options.acceptInsecureCerts", true));

        return options;
    }


    // ── buildEdgeOptions() ───────────────────────────────────
    /**
     * Builds EdgeOptions with standard CI-compatible settings.
     * TODO-1: Add Edge-specific args if your project requires them.
     */
    private static EdgeOptions buildEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--start-maximized");
        options.setAcceptInsecureCerts(
                ConfigReader.getBoolean("browser.options.acceptInsecureCerts", true));
        return options;
    }


    // ── applyTimeouts() ───────────────────────────────────────
    /**
     * Applies timeout settings from config.json to the driver.
     * implicitWait is intentionally kept at 0 — we use explicit
     * waits (WaitUtils) throughout the framework for reliability.
     */
    private static void applyTimeouts(WebDriver driver) {

        // Implicit wait — keep at 0, use WaitUtils for explicit waits
        // Mixing implicit and explicit waits causes unpredictable behaviour
        int implicitWait = ConfigReader.getInt("timeouts.implicitWait", 0);
        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(implicitWait));

        // Page load timeout — how long to wait for a page to fully load
        int pageLoad = ConfigReader.getInt("timeouts.pageLoadTimeout", 30);
        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(pageLoad));

        // Script timeout — how long to wait for async JS execution
        int scriptTimeout = ConfigReader.getInt("timeouts.scriptTimeout", 30);
        driver.manage().timeouts()
                .scriptTimeout(Duration.ofSeconds(scriptTimeout));

        log.debug("Timeouts applied → implicit: {}s, pageLoad: {}s, script: {}s",
                implicitWait, pageLoad, scriptTimeout);
    }

    // ── TODO-2: Remote / Grid support ────────────────────────
    // To add Selenium Grid or BrowserStack support, add a new
    // case in initDriver() switch:
    //
    //   case "remote" -> {
    //       ChromeOptions options = buildChromeOptions(true);
    //       URL gridUrl = new URL(ConfigReader.get("grid.url", "http://localhost:4444"));
    //       yield new RemoteWebDriver(gridUrl, options);
    //   }
    //
    // And add "grid.url" to config.json under each environment.

}