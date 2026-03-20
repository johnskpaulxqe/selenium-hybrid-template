package utils;

/*
  ============================================================
  FILE: ConfigReader.java
  LOCATION: src/test/java/utils/ConfigReader.java

  PURPOSE:
    Single access point for all configuration values defined
    in config.json. Responsibilities:
      1. Load config.json once at startup using Jackson
      2. Resolve the active environment (dev / staging / prod)
         from -Denv CLI arg or config "activeEnv" field
      3. Provide typed getters: get(), getInt(), getBoolean()
      4. Support dot-notation key paths:
           ConfigReader.get("browser.default")
           ConfigReader.get("timeouts.explicitWait")
      5. Resolve environment-specific values automatically:
           ConfigReader.get("baseUrl") → reads from
           environments.<activeEnv>.baseUrl in config.json
      6. Resolve CI environment variable references:
           If a config value starts with "$", read from
           System.getenv() instead (for prod secrets)

  HOW TO USE:
    // Simple string value
    String url = ConfigReader.get("baseUrl");

    // Integer value with fallback default
    int timeout = ConfigReader.getInt("timeouts.explicitWait", 10);

    // Boolean value
    boolean headless = ConfigReader.getBoolean("browser.options.headless", true);

    // Top-level (non-environment) value
    String reportPath = ConfigReader.get("reporting.extentReportPath");

  KEY RESOLUTION ORDER:
    1. Check environments.<activeEnv>.<key>  (env-specific first)
    2. Fall back to top-level <key>          (shared config)
    3. Return provided default if not found

  TODO (customise per project):
    - TODO-1 : config.json path is on classpath — no change needed
               unless you rename or move the file
    - TODO-2 : Add getList() if you need array values from config
    - TODO-3 : Extend resolveEnvVar() if your CI uses a different
               secret injection pattern
  ============================================================
*/

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ConfigReader {

    // ── Logger ────────────────────────────────────────────────
    // Using LoggerFactory directly here (not LogUtil) because
    // LogUtil itself depends on ConfigReader — avoids circular init
    private static final Logger log = LoggerFactory.getLogger(ConfigReader.class);

    // ── Constants ─────────────────────────────────────────────
    // TODO-1: Update this path if you rename or move config.json
    private static final String CONFIG_FILE = "config.json";

    // ── Singleton state ───────────────────────────────────────
    // Config is loaded once when the class is first accessed.
    // All subsequent calls reuse the same parsed JsonNode tree.
    private static final JsonNode rootNode;
    private static final JsonNode envNode;
    private static final String   activeEnv;

    // ── Static initialiser — runs once on first class access ──
    static {
        rootNode  = loadConfig();
        activeEnv = resolveActiveEnv(rootNode);
        envNode   = resolveEnvNode(rootNode, activeEnv);
        log.info("ConfigReader initialised → active environment: [{}]", activeEnv);
    }

    // Private constructor — static utility class, never instantiate
    private ConfigReader() {}


    // ═══════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════

    // ── get(key) ──────────────────────────────────────────────
    /**
     * Returns a config value as a String.
     * Supports dot-notation paths: "browser.default", "timeouts.explicitWait"
     * Checks env-specific node first, then falls back to root.
     * Returns null if not found.
     *
     * Example:
     *   String url     = ConfigReader.get("baseUrl");
     *   String browser = ConfigReader.get("browser.default");
     */
    public static String get(String key) {
        return get(key, null);
    }


    // ── get(key, defaultValue) ────────────────────────────────
    /**
     * Returns a config value as a String with a fallback default.
     * Use this when the key might not exist in all environments.
     *
     * Example:
     *   String browser = ConfigReader.get("browser.default", "headless");
     */
    public static String get(String key, String defaultValue) {
        // 1. Try environment-specific node first
        JsonNode node = traversePath(envNode, key);

        // 2. Fall back to root node if not found in env block
        if (node == null || node.isNull()) {
            node = traversePath(rootNode, key);
        }

        // 3. Return default if still not found
        if (node == null || node.isNull()) {
            log.debug("Config key [{}] not found → using default: [{}]", key, defaultValue);
            return defaultValue;
        }

        String value = node.asText();

        // 4. Resolve CI environment variable references (e.g. "$DB_URL")
        return resolveEnvVar(value);
    }


    // ── getInt(key, defaultValue) ─────────────────────────────
    /**
     * Returns a config value as an int.
     * Falls back to defaultValue if the key is missing or not numeric.
     *
     * Example:
     *   int timeout = ConfigReader.getInt("timeouts.explicitWait", 10);
     *   int threads = ConfigReader.getInt("parallel.threads", 1);
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Config key [{}] value [{}] is not a valid integer → using default: {}",
                    key, value, defaultValue);
            return defaultValue;
        }
    }


    // ── getBoolean(key, defaultValue) ─────────────────────────
    /**
     * Returns a config value as a boolean.
     * Accepts "true" / "false" (case-insensitive).
     * Falls back to defaultValue if key is missing.
     *
     * Example:
     *   boolean screenshots = ConfigReader.getBoolean("reporting.screenshotOnFailure", true);
     *   boolean headless    = ConfigReader.getBoolean("browser.options.headless", true);
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }


    // ── getLong(key, defaultValue) ────────────────────────────
    /**
     * Returns a config value as a long.
     * Useful for timeout values stored as milliseconds.
     *
     * Example:
     *   long connTimeout = ConfigReader.getLong("api.connectionTimeout", 10000L);
     */
    public static long getLong(String key, long defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Config key [{}] value [{}] is not a valid long → using default: {}",
                    key, value, defaultValue);
            return defaultValue;
        }
    }


    // ── getActiveEnv() ────────────────────────────────────────
    /**
     * Returns the currently active environment name.
     * Useful for logging and conditional test logic.
     *
     * Example:
     *   String env = ConfigReader.getActiveEnv(); // "dev", "staging", "prod"
     */
    public static String getActiveEnv() {
        return activeEnv;
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── loadConfig() ─────────────────────────────────────────
    /**
     * Loads and parses config.json from the classpath.
     * Jackson ObjectMapper handles all JSON parsing.
     * Throws RuntimeException on failure — config is mandatory.
     */
    private static JsonNode loadConfig() {
        try {
            // Load from classpath (src/test/resources/config.json)
            InputStream inputStream = ConfigReader.class
                    .getClassLoader()
                    .getResourceAsStream(CONFIG_FILE);

            if (inputStream == null) {
                throw new RuntimeException(
                        "config.json not found on classpath. " +
                                "Ensure it exists at src/test/resources/config.json");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(inputStream);
            log.debug("config.json loaded successfully");
            return node;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.json: " + e.getMessage(), e);
        }
    }


    // ── resolveActiveEnv() ───────────────────────────────────
    /**
     * Determines the active environment in this priority order:
     *   1. -Denv=xxx Maven CLI argument
     *   2. ACTIVE_ENV system environment variable (CI-friendly)
     *   3. "activeEnv" field in config.json
     *   4. Hardcoded fallback: "dev"
     */
    private static String resolveActiveEnv(JsonNode root) {

        // 1. Maven CLI: mvn test -Denv=staging
        String envFromProp = System.getProperty("env");
        if (envFromProp != null && !envFromProp.isBlank()) {
            log.debug("Environment resolved from system property: [{}]", envFromProp);
            return envFromProp.trim().toLowerCase();
        }

        // 2. CI environment variable: export ACTIVE_ENV=staging
        String envFromVar = System.getenv("ACTIVE_ENV");
        if (envFromVar != null && !envFromVar.isBlank()) {
            log.debug("Environment resolved from ACTIVE_ENV env var: [{}]", envFromVar);
            return envFromVar.trim().toLowerCase();
        }

        // 3. config.json "activeEnv" field
        JsonNode activeEnvNode = root.get("activeEnv");
        if (activeEnvNode != null && !activeEnvNode.isNull()) {
            log.debug("Environment resolved from config.json activeEnv: [{}]",
                    activeEnvNode.asText());
            return activeEnvNode.asText().trim().toLowerCase();
        }

        // 4. Safe fallback
        log.warn("No environment specified anywhere → defaulting to [dev]");
        return "dev";
    }


    // ── resolveEnvNode() ─────────────────────────────────────
    /**
     * Navigates to environments.<activeEnv> in the config tree.
     * Returns an empty ObjectNode if the environment block is missing,
     * so callers always get a valid (possibly empty) node to traverse.
     */
    private static JsonNode resolveEnvNode(JsonNode root, String env) {
        JsonNode environments = root.get("environments");
        if (environments == null || environments.isNull()) {
            log.warn("No 'environments' block found in config.json");
            return new ObjectMapper().createObjectNode();
        }

        JsonNode envBlock = environments.get(env);
        if (envBlock == null || envBlock.isNull()) {
            log.warn("Environment [{}] not found in config.json → " +
                    "all values will fall back to root config", env);
            return new ObjectMapper().createObjectNode();
        }

        return envBlock;
    }


    // ── traversePath() ───────────────────────────────────────
    /**
     * Navigates a dot-notation key path through a JsonNode tree.
     * "browser.default" → node.get("browser").get("default")
     * Returns null if any segment of the path is missing.
     *
     * Example paths:
     *   "baseUrl"                        → single key
     *   "browser.default"                → nested key
     *   "timeouts.explicitWait"          → nested key
     *   "reporting.screenshotOnFailure"  → nested key
     */
    private static JsonNode traversePath(JsonNode startNode, String dotPath) {
        if (startNode == null || dotPath == null || dotPath.isBlank()) {
            return null;
        }

        // Split the dot-notation path into individual segments
        String[] segments = dotPath.split("\\.");
        JsonNode current  = startNode;

        for (String segment : segments) {
            if (current == null || current.isNull() || !current.isObject()) {
                return null;
            }
            current = current.get(segment);
        }

        return current;
    }


    // ── resolveEnvVar() ──────────────────────────────────────
    /**
     * If a config value starts with "$", treat it as a reference
     * to a system environment variable and resolve it.
     *
     * This allows prod credentials to be injected by CI without
     * ever being stored in config.json.
     *
     * Example in config.json:
     *   "password": "$DB_PASS"
     *
     * At runtime ConfigReader reads System.getenv("DB_PASS")
     *
     * TODO-3: Extend this if your CI uses a different pattern,
     *         e.g. {{SECRET_NAME}} or %ENV_VAR%
     */
    private static String resolveEnvVar(String value) {
        if (value != null && value.startsWith("$")) {
            String varName    = value.substring(1); // Strip the "$"
            String envValue   = System.getenv(varName);

            if (envValue != null && !envValue.isBlank()) {
                log.debug("Resolved env var [{}] from system environment", varName);
                return envValue;
            } else {
                log.warn("Config references env var [{}] but it is not set " +
                        "in the system environment", varName);
                // Return the original "$VAR_NAME" so callers can detect it
                return value;
            }
        }
        return value;
    }

}