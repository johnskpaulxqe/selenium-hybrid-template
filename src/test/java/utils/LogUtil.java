package utils;

/*
  ============================================================
  FILE: LogUtil.java
  LOCATION: src/test/java/utils/LogUtil.java

  PURPOSE:
    Thin wrapper around SLF4J LoggerFactory. Provides a single
    consistent way to obtain a Logger in any framework class.
    Responsibilities:
      1. Return a typed SLF4J Logger for any class
      2. Return a named Logger for non-class contexts
         (e.g. feature files, utility scripts)
      3. Provide convenience static log methods for classes
         that prefer not to hold a Logger field
      4. Log framework startup info once at initialisation

  WHY A WRAPPER?
    - Single import across the whole framework (utils.LogUtil)
    - If you ever swap Logback for Log4j2, change one file only
    - Enforces consistent logger naming convention
    - Convenience methods reduce boilerplate in simple classes

  HOW TO USE:
    Option A — Logger field (recommended for most classes):
      private static final Logger log = LogUtil.getLogger(MyClass.class);
      log.info("Starting scenario: {}", scenarioName);
      log.debug("Element located: {}", locator);
      log.warn("Retry attempt {} of {}", attempt, maxRetries);
      log.error("Step failed: {}", e.getMessage(), e);

    Option B — Named logger (for shared/utility contexts):
      private static final Logger log = LogUtil.getLogger("DBLayer");
      log.info("Executing query: {}", sql);

    Option C — Quick static log (no field needed):
      LogUtil.info(MyClass.class, "Config loaded successfully");
      LogUtil.error(MyClass.class, "Connection failed: {}", e.getMessage());

  LOG LEVELS (most → least verbose):
    TRACE → DEBUG → INFO → WARN → ERROR
    Controlled via logback.xml or -Dlog.level=DEBUG at runtime

  TODO (customise per project):
    - TODO-1 : No changes needed for most projects
    - TODO-2 : Add MDC helpers here if you need thread-based
               contextual logging (e.g. scenarioId in every log line)
  ============================================================
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LogUtil {

    // Private constructor — static utility class, never instantiate
    private LogUtil() {}


    // ═══════════════════════════════════════════════════════════
    // LOGGER FACTORY METHODS
    // ═══════════════════════════════════════════════════════════

    // ── getLogger(Class) ──────────────────────────────────────
    /**
     * Returns a Logger named after the given class.
     * This is the standard and recommended way to get a logger.
     *
     * The class name becomes the logger name in logback.xml,
     * allowing per-class log level overrides if needed.
     *
     * Example:
     *   private static final Logger log = LogUtil.getLogger(LoginPage.class);
     *   // Logger name → "pages.LoginPage"
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }


    // ── getLogger(String) ─────────────────────────────────────
    /**
     * Returns a Logger with a custom string name.
     * Useful for shared contexts not tied to a specific class,
     * such as database layers, API layers, or utility scripts.
     *
     * Example:
     *   private static final Logger log = LogUtil.getLogger("APILayer");
     *   private static final Logger log = LogUtil.getLogger("DBLayer");
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }


    // ═══════════════════════════════════════════════════════════
    // CONVENIENCE STATIC METHODS (Option C usage)
    // These are for simple one-off log calls without holding
    // a Logger field. Not recommended for high-frequency paths.
    // ═══════════════════════════════════════════════════════════

    // ── info() ────────────────────────────────────────────────
    /**
     * Logs an INFO message for the given class.
     * Supports SLF4J-style {} placeholders.
     *
     * Example:
     *   LogUtil.info(ConfigReader.class, "Loaded config for env: {}", activeEnv);
     */
    public static void info(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).info(message, args);
    }


    // ── debug() ───────────────────────────────────────────────
    /**
     * Logs a DEBUG message. Only visible when log level is DEBUG or TRACE.
     *
     * Example:
     *   LogUtil.debug(WaitUtils.class, "Waiting for element: {}", locator);
     */
    public static void debug(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).debug(message, args);
    }


    // ── warn() ────────────────────────────────────────────────
    /**
     * Logs a WARN message. Use for recoverable issues or
     * unexpected-but-handled situations.
     *
     * Example:
     *   LogUtil.warn(DriverManager.class, "Unknown browser [{}] → using headless", browser);
     */
    public static void warn(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).warn(message, args);
    }


    // ── error() ───────────────────────────────────────────────
    /**
     * Logs an ERROR message. Use for failures that affect
     * test outcome. Pass the exception as the last argument
     * to include the full stack trace in the log.
     *
     * Example:
     *   LogUtil.error(DBUtils.class, "Query failed: {}", e.getMessage(), e);
     */
    public static void error(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).error(message, args);
    }


    // ── trace() ───────────────────────────────────────────────
    /**
     * Logs a TRACE message. Finest granularity — use for
     * deep debugging only. Not visible unless level=TRACE.
     *
     * Example:
     *   LogUtil.trace(WaitUtils.class, "Poll attempt {} for element {}", attempt, locator);
     */
    public static void trace(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).trace(message, args);
    }


    // ═══════════════════════════════════════════════════════════
    // MDC HELPERS — Mapped Diagnostic Context
    // MDC lets you attach key-value context to every log line
    // emitted by the current thread. Extremely useful for
    // parallel runs where you want to see which scenario or
    // thread produced each log line.
    //
    // TODO-2: Call these from Hooks.java @Before / @After
    //         to tag all logs with the current scenario name.
    // ═══════════════════════════════════════════════════════════

    // ── setScenarioContext() ──────────────────────────────────
    /**
     * Attaches the current scenario name to all log lines
     * produced by this thread until clearContext() is called.
     *
     * Call from Hooks.java @Before:
     *   LogUtil.setScenarioContext(scenario.getName());
     *
     * Then in logback.xml pattern, add %X{scenario} to see it:
     *   %d{HH:mm:ss} %-5p [%t] [%X{scenario}] %logger - %msg%n
     */
    public static void setScenarioContext(String scenarioName) {
        MDC.put("scenario", scenarioName);
        MDC.put("thread",   Thread.currentThread().getName());
    }


    // ── setContext() ──────────────────────────────────────────
    /**
     * Attaches an arbitrary key-value pair to the MDC context
     * for the current thread.
     *
     * Example:
     *   LogUtil.setContext("env", "staging");
     *   LogUtil.setContext("browser", "chrome");
     */
    public static void setContext(String key, String value) {
        MDC.put(key, value);
    }


    // ── clearContext() ────────────────────────────────────────
    /**
     * Clears all MDC context for the current thread.
     * Always call this in Hooks.java @After to prevent
     * context from leaking into the next scenario on the
     * same thread (common in parallel execution).
     *
     * Call from Hooks.java @After:
     *   LogUtil.clearContext();
     */
    public static void clearContext() {
        MDC.clear();
    }


    // ═══════════════════════════════════════════════════════════
    // FRAMEWORK STARTUP BANNER
    // Logs a visible marker at the start of each test run.
    // Called once from BaseTest or TestRunner initialisation.
    // ═══════════════════════════════════════════════════════════

    // ── logFrameworkStartup() ────────────────────────────────
    /**
     * Prints a startup banner to the log so the start of a
     * test run is clearly visible in log files and CI output.
     *
     * Call once from BaseTest.java @BeforeSuite.
     *
     * Example output:
     *   ╔══════════════════════════════════════════╗
     *   ║   Selenium Hybrid Framework Starting     ║
     *   ║   Environment : staging                  ║
     *   ║   Browser     : headless                 ║
     *   ╚══════════════════════════════════════════╝
     */
    public static void logFrameworkStartup() {
        Logger log = LoggerFactory.getLogger("Framework");

        String env     = ConfigReader.getActiveEnv();
        String browser = System.getProperty("browser",
                ConfigReader.get("browser.default", "headless"));

        log.info("╔══════════════════════════════════════════╗");
        log.info("║     Selenium Hybrid Framework Starting   ║");
        log.info("║  Environment : {:<26}║", padRight(env, 26));
        log.info("║  Browser     : {:<26}║", padRight(browser, 26));
        log.info("╚══════════════════════════════════════════╝");
    }

    // ── logScenarioStart() ───────────────────────────────────
    /**
     * Logs a visible separator before each scenario starts.
     * Call from Hooks.java @Before.
     *
     * Example:
     *   LogUtil.logScenarioStart("Successful login with valid credentials");
     */
    public static void logScenarioStart(String scenarioName) {
        Logger log = LoggerFactory.getLogger("Scenario");
        log.info("┌─────────────────────────────────────────────────────");
        log.info("│  ▶ SCENARIO: {}", scenarioName);
        log.info("└─────────────────────────────────────────────────────");
    }


    // ── logScenarioEnd() ─────────────────────────────────────
    /**
     * Logs the scenario result (PASSED / FAILED) after it ends.
     * Call from Hooks.java @After.
     *
     * Example:
     *   LogUtil.logScenarioEnd("Successful login with valid credentials", false);
     */
    public static void logScenarioEnd(String scenarioName, boolean failed) {
        Logger log = LoggerFactory.getLogger("Scenario");
        if (failed) {
            log.error("│  ✖ FAILED : {}", scenarioName);
        } else {
            log.info( "│  ✔ PASSED : {}", scenarioName);
        }
        log.info("└─────────────────────────────────────────────────────");
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Right-pads a string to a fixed width for banner alignment.
     */
    private static String padRight(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

}