package utils;

/*
  ============================================================
  FILE: JsonReader.java
  LOCATION: src/test/java/utils/JsonReader.java

  PURPOSE:
    Generic utility for reading and parsing any JSON file from
    the classpath or filesystem. Used by TestDataLoader to read
    test data files, and by ConfigReader for config.json.
    Responsibilities:
      1. Load JSON files from classpath (src/test/resources)
      2. Load JSON files from absolute or relative file paths
      3. Parse JSON into typed Java objects via Jackson
      4. Read specific fields by dot-notation key paths
      5. Read JSON arrays as Java Lists
      6. Read JSON objects as Java Maps
      7. Pretty-print JSON for logging/debugging

  HOW TO USE:
    // Read entire file as a Java object (POJO)
    User user = JsonReader.readAs("testdata/users.json", User.class);

    // Read a list of objects
    List<User> users = JsonReader.readAsList("testdata/users.json", User.class);

    // Read a specific field by dot-notation
    String username = JsonReader.getField("testdata/users.json", "admin.username");

    // Read raw JsonNode for flexible access
    JsonNode root = JsonReader.readTree("testdata/apidata.json");
    String token  = root.get("authToken").asText();

    // Parse a JSON string directly
    JsonNode node = JsonReader.parseString("{\"key\": \"value\"}");

  TODO (customise per project):
    - TODO-1 : No changes needed for most projects
    - TODO-2 : Add readFromUrl() if you need to fetch JSON from
               an external endpoint during test setup
    - TODO-3 : Add writeJson() if your tests need to generate
               JSON output files (e.g. for data-driven reporting)
  ============================================================
*/

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class JsonReader {

    // ── Logger ────────────────────────────────────────────────
    private static final Logger log = LogUtil.getLogger(JsonReader.class);

    // ── Shared ObjectMapper ───────────────────────────────────
    // ObjectMapper is thread-safe after configuration — share one instance
    private static final ObjectMapper mapper = new ObjectMapper();

    // Pretty-print mapper for logging/debugging output only
    private static final ObjectMapper prettyMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // Private constructor — static utility class
    private JsonReader() {}


    // ═══════════════════════════════════════════════════════════
    // READ FROM CLASSPATH (src/test/resources)
    // ═══════════════════════════════════════════════════════════

    // ── readTree() ────────────────────────────────────────────
    /**
     * Reads a JSON file from the classpath and returns the root
     * JsonNode for flexible field access.
     * Path is relative to src/test/resources.
     *
     * Example:
     *   JsonNode root = JsonReader.readTree("testdata/users.json");
     *   String   name = root.get("admin").get("username").asText();
     */
    public static JsonNode readTree(String classpathPath) {
        log.debug("Reading JSON tree from classpath: [{}]", classpathPath);
        try (InputStream is = getClasspathStream(classpathPath)) {
            return mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read JSON from classpath [" + classpathPath + "]: "
                            + e.getMessage(), e);
        }
    }


    // ── readAs() ──────────────────────────────────────────────
    /**
     * Reads a JSON file from the classpath and deserialises it
     * into an instance of the given class.
     *
     * Example:
     *   UserConfig config = JsonReader.readAs("testdata/users.json", UserConfig.class);
     *   String username   = config.getAdmin().getUsername();
     */
    public static <T> T readAs(String classpathPath, Class<T> type) {
        log.debug("Reading JSON as [{}] from classpath: [{}]", type.getSimpleName(), classpathPath);
        try (InputStream is = getClasspathStream(classpathPath)) {
            return mapper.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to deserialise JSON [" + classpathPath + "] as ["
                            + type.getSimpleName() + "]: " + e.getMessage(), e);
        }
    }


    // ── readAsList() ──────────────────────────────────────────
    /**
     * Reads a JSON array from the classpath and returns it as
     * a typed Java List.
     *
     * Example:
     *   List<User> users = JsonReader.readAsList("testdata/users.json", User.class);
     *   users.forEach(u -> System.out.println(u.getUsername()));
     */
    public static <T> List<T> readAsList(String classpathPath, Class<T> elementType) {
        log.debug("Reading JSON as List<{}> from classpath: [{}]",
                elementType.getSimpleName(), classpathPath);
        try (InputStream is = getClasspathStream(classpathPath)) {
            return mapper.readValue(is,
                    mapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read JSON array [" + classpathPath + "] as List<"
                            + elementType.getSimpleName() + ">: " + e.getMessage(), e);
        }
    }


    // ── readAsMap() ───────────────────────────────────────────
    /**
     * Reads a JSON object from the classpath as a Map<String, Object>.
     * Useful for dynamic/schema-less JSON where you don't have a POJO.
     *
     * Example:
     *   Map<String, Object> data = JsonReader.readAsMap("testdata/apidata.json");
     *   String endpoint = (String) data.get("endpoint");
     */
    public static Map<String, Object> readAsMap(String classpathPath) {
        log.debug("Reading JSON as Map from classpath: [{}]", classpathPath);
        try (InputStream is = getClasspathStream(classpathPath)) {
            return mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read JSON as Map [" + classpathPath + "]: "
                            + e.getMessage(), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // READ FROM FILE SYSTEM (absolute or relative path)
    // ═══════════════════════════════════════════════════════════

    // ── readTreeFromFile() ────────────────────────────────────
    /**
     * Reads a JSON file from the filesystem (not classpath).
     * Use when the file is outside src/test/resources,
     * e.g. generated test data in target/ or a mounted volume.
     *
     * Example:
     *   JsonNode root = JsonReader.readTreeFromFile("target/output/results.json");
     */
    public static JsonNode readTreeFromFile(String filePath) {
        log.debug("Reading JSON tree from file: [{}]", filePath);
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("JSON file not found: [" + filePath + "]");
            }
            return mapper.readTree(file);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read JSON from file [" + filePath + "]: "
                            + e.getMessage(), e);
        }
    }


    // ── readAsFromFile() ─────────────────────────────────────
    /**
     * Reads a JSON file from the filesystem and deserialises
     * it into the given type.
     *
     * Example:
     *   ApiConfig config = JsonReader.readAsFromFile("config/api.json", ApiConfig.class);
     */
    public static <T> T readAsFromFile(String filePath, Class<T> type) {
        log.debug("Reading [{}] from file: [{}]", type.getSimpleName(), filePath);
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("JSON file not found: [" + filePath + "]");
            }
            return mapper.readValue(file, type);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to deserialise JSON file [" + filePath + "] as ["
                            + type.getSimpleName() + "]: " + e.getMessage(), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // FIELD ACCESS BY DOT-NOTATION KEY PATH
    // ═══════════════════════════════════════════════════════════

    // ── getField() ────────────────────────────────────────────
    /**
     * Reads a specific field from a JSON file using a
     * dot-notation path. Returns the value as a String.
     * Returns null if the path does not exist.
     *
     * Example:
     *   String user = JsonReader.getField("testdata/users.json", "admin.username");
     *   String url  = JsonReader.getField("testdata/apidata.json", "endpoints.login");
     */
    public static String getField(String classpathPath, String dotPath) {
        log.debug("Getting field [{}] from [{}]", dotPath, classpathPath);
        JsonNode root = readTree(classpathPath);
        JsonNode node = traversePath(root, dotPath);

        if (node == null || node.isNull()) {
            log.warn("Field [{}] not found in [{}]", dotPath, classpathPath);
            return null;
        }
        return node.asText();
    }


    // ── getFieldAsInt() ───────────────────────────────────────
    /**
     * Reads a specific numeric field from a JSON file.
     * Returns defaultValue if the path does not exist.
     *
     * Example:
     *   int timeout = JsonReader.getFieldAsInt("testdata/apidata.json", "timeout", 30);
     */
    public static int getFieldAsInt(String classpathPath, String dotPath, int defaultValue) {
        String value = getField(classpathPath, dotPath);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Field [{}] value [{}] is not an integer → using default: {}",
                    dotPath, value, defaultValue);
            return defaultValue;
        }
    }


    // ── getFieldAsBoolean() ───────────────────────────────────
    /**
     * Reads a specific boolean field from a JSON file.
     * Returns defaultValue if the path does not exist.
     *
     * Example:
     *   boolean active = JsonReader.getFieldAsBoolean("testdata/users.json",
     *                                                  "admin.active", true);
     */
    public static boolean getFieldAsBoolean(String classpathPath, String dotPath,
                                            boolean defaultValue) {
        String value = getField(classpathPath, dotPath);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }


    // ── getNode() ─────────────────────────────────────────────
    /**
     * Returns a specific JsonNode from a file using dot-notation.
     * Use when you need the raw JsonNode for further traversal.
     *
     * Example:
     *   JsonNode adminNode = JsonReader.getNode("testdata/users.json", "admin");
     *   String   username  = adminNode.get("username").asText();
     *   String   password  = adminNode.get("password").asText();
     */
    public static JsonNode getNode(String classpathPath, String dotPath) {
        log.debug("Getting node [{}] from [{}]", dotPath, classpathPath);
        JsonNode root = readTree(classpathPath);
        return traversePath(root, dotPath);
    }


    // ═══════════════════════════════════════════════════════════
    // PARSE RAW JSON STRINGS
    // ═══════════════════════════════════════════════════════════

    // ── parseString() ─────────────────────────────────────────
    /**
     * Parses a raw JSON string into a JsonNode.
     * Useful for parsing API response bodies.
     *
     * Example:
     *   JsonNode response = JsonReader.parseString(apiResponse.getBody().asString());
     *   String   userId   = response.get("id").asText();
     */
    public static JsonNode parseString(String jsonString) {
        log.debug("Parsing JSON string ({} chars)", jsonString != null ? jsonString.length() : 0);
        try {
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to parse JSON string: " + e.getMessage()
                            + "\nInput: " + jsonString, e);
        }
    }


    // ── parseStringAs() ──────────────────────────────────────
    /**
     * Parses a raw JSON string into a typed Java object.
     *
     * Example:
     *   User user = JsonReader.parseStringAs(responseBody, User.class);
     */
    public static <T> T parseStringAs(String jsonString, Class<T> type) {
        log.debug("Parsing JSON string as [{}]", type.getSimpleName());
        try {
            return mapper.readValue(jsonString, type);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to parse JSON string as [" + type.getSimpleName() + "]: "
                            + e.getMessage(), e);
        }
    }


    // ═══════════════════════════════════════════════════════════
    // UTILITY — SERIALISATION AND PRETTY PRINT
    // ═══════════════════════════════════════════════════════════

    // ── toJson() ──────────────────────────────────────────────
    /**
     * Serialises a Java object to a compact JSON string.
     * Useful for building request bodies from POJOs.
     *
     * Example:
     *   String json = JsonReader.toJson(requestObject);
     *   // Produces: {"username":"admin","password":"pass123"}
     */
    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to serialise object to JSON: " + e.getMessage(), e);
        }
    }


    // ── toPrettyJson() ────────────────────────────────────────
    /**
     * Serialises a Java object to a pretty-printed JSON string.
     * Use for logging request/response bodies in a readable format.
     *
     * Example:
     *   log.debug("Request body:\n{}", JsonReader.toPrettyJson(requestBody));
     */
    public static String toPrettyJson(Object object) {
        try {
            return prettyMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to pretty-print object to JSON: " + e.getMessage(), e);
        }
    }


    // ── prettyPrint() ─────────────────────────────────────────
    /**
     * Pretty-prints a raw JSON string (re-formats with indentation).
     * Use for logging API responses in a readable format.
     *
     * Example:
     *   log.debug("Response:\n{}", JsonReader.prettyPrint(response.getBody().asString()));
     */
    public static String prettyPrint(String jsonString) {
        try {
            JsonNode node = mapper.readTree(jsonString);
            return prettyMapper.writeValueAsString(node);
        } catch (IOException e) {
            // If parsing fails, return the original string unchanged
            log.warn("Could not pretty-print JSON string — returning as-is");
            return jsonString;
        }
    }


    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    // ── getClasspathStream() ─────────────────────────────────
    /**
     * Opens an InputStream for a classpath resource.
     * Throws RuntimeException if the resource is not found.
     */
    private static InputStream getClasspathStream(String classpathPath) {
        InputStream is = JsonReader.class
                .getClassLoader()
                .getResourceAsStream(classpathPath);

        if (is == null) {
            throw new RuntimeException(
                    "JSON file not found on classpath: [" + classpathPath + "]. " +
                            "Ensure it exists under src/test/resources/");
        }
        return is;
    }


    // ── traversePath() ───────────────────────────────────────
    /**
     * Navigates a dot-notation path through a JsonNode tree.
     * "admin.credentials.username" → node → "admin" → "credentials" → "username"
     * Returns null if any segment of the path is missing.
     */
    private static JsonNode traversePath(JsonNode startNode, String dotPath) {
        if (startNode == null || dotPath == null || dotPath.isBlank()) {
            return null;
        }

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

}