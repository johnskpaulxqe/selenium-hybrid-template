package utils;

/*
  ============================================================
  FILE: Locators.java
  LOCATION: src/test/java/utils/Locators.java

  PURPOSE:
    Central store for all Selenium By locators used across
    every Page Object in the framework. Organises locators
    into static inner classes grouped by page or feature area.

    Responsibilities:
      1. Single source of truth for every UI locator
      2. Grouped by page/component so they are easy to find
      3. If a locator changes in the application, update it
         here once — all page objects using it update automatically
      4. Supports CSS selectors, XPath, ID, name, class, tag

  WHY A CENTRAL LOCATOR FILE?
    Without this, locators are scattered across every page
    object. When the application changes a single element ID,
    you have to hunt through 20 files to find every usage.
    With Locators.java, you change one line and everything works.

  HOW TO USE:
    // In a Page Object (extends BasePage):
    click(Locators.Login.LOGIN_BUTTON);
    type(Locators.Login.USERNAME, "admin@yourapp.com");

    // In a Step Definition:
    WaitUtils.waitForVisible(driver, Locators.Dashboard.WELCOME_MSG);

  LOCATOR STRATEGY GUIDELINES:
    Priority order (most stable → least stable):
      1. By.id()            ← most stable, use whenever available
      2. By.name()          ← stable for form inputs
      3. By.cssSelector()   ← fast, readable, widely supported
      4. By.xpath()         ← use only when CSS is not enough
    Avoid:
      - Absolute XPath (/html/body/div[3]/...)  ← extremely fragile
      - Index-based CSS (div:nth-child(3))       ← breaks on reorder
      - Text-based XPath (//button[text()='OK']) ← breaks on i18n

  TODO (customise per project):
    - TODO-1 : Replace ALL locator values with your application's
               actual element IDs, CSS selectors, or XPaths
    - TODO-2 : Add a new static inner class for each page/component
               in your application
    - TODO-3 : Delete inner classes for pages that don't exist
               in your application (e.g. remove Api if not UI-tested)
    - TODO-4 : Use descriptive constant names that match the
               element's purpose, not its type
               GOOD: LOGIN_BUTTON   BAD: BUTTON_1
  ============================================================
*/

import org.openqa.selenium.By;

public class Locators {

    // Private constructor — constants-only class, never instantiate
    private Locators() {}


    // ══════════════════════════════════════════════════════════
    // COMMON — elements shared across multiple pages
    // TODO-1: Replace with your application's shared element locators
    // ══════════════════════════════════════════════════════════
    public static class Common {

        // Loading spinner shown during page transitions
        public static final By LOADING_SPINNER =
                By.cssSelector("[data-testid='loading-spinner']");

        // Generic success toast / notification
        public static final By SUCCESS_TOAST =
                By.cssSelector("[data-testid='toast-success']");

        // Generic error toast / notification
        public static final By ERROR_TOAST =
                By.cssSelector("[data-testid='toast-error']");

        // Modal dialog container
        public static final By MODAL_DIALOG =
                By.cssSelector("[role='dialog']");

        // Modal confirm button
        public static final By MODAL_CONFIRM_BUTTON =
                By.cssSelector("[data-testid='modal-confirm']");

        // Modal cancel button
        public static final By MODAL_CANCEL_BUTTON =
                By.cssSelector("[data-testid='modal-cancel']");

        // Generic page heading (h1)
        public static final By PAGE_HEADING =
                By.cssSelector("h1");

        // Breadcrumb navigation
        public static final By BREADCRUMB =
                By.cssSelector("[aria-label='breadcrumb']");

        // TODO-1: Replace all above with your real shared element locators
    }


    // ══════════════════════════════════════════════════════════
    // LOGIN PAGE
    // TODO-1: Replace with your login page element locators
    // ══════════════════════════════════════════════════════════
    public static class Login {

        // Username / email input field
        public static final By USERNAME =
                By.id("username");

        // Password input field
        public static final By PASSWORD =
                By.id("password");

        // Login / Sign in submit button
        public static final By LOGIN_BUTTON =
                By.cssSelector("[data-testid='login-btn']");

        // Error message shown on invalid credentials
        public static final By ERROR_MESSAGE =
                By.cssSelector("[data-testid='login-error']");

        // "Forgot password" link
        public static final By FORGOT_PASSWORD_LINK =
                By.cssSelector("a[href*='forgot-password']");

        // "Remember me" checkbox
        public static final By REMEMBER_ME_CHECKBOX =
                By.id("rememberMe");

        // TODO-1: Replace all above with your login page's real locators
    }


    // ══════════════════════════════════════════════════════════
    // DASHBOARD / HOME PAGE
    // TODO-1: Replace with your dashboard element locators
    // ══════════════════════════════════════════════════════════
    public static class Dashboard {

        // Welcome / greeting message after login
        public static final By WELCOME_MESSAGE =
                By.cssSelector("[data-testid='welcome-msg']");

        // User display name in header/navbar
        public static final By USER_DISPLAY_NAME =
                By.cssSelector("[data-testid='user-name']");

        // Logout button or link
        public static final By LOGOUT_BUTTON =
                By.cssSelector("[data-testid='logout-btn']");

        // Navigation menu container
        public static final By NAV_MENU =
                By.cssSelector("nav[role='navigation']");

        // TODO-1: Replace with your dashboard's real locators
    }


    // ══════════════════════════════════════════════════════════
    // NAVIGATION / SIDEBAR
    // TODO-1: Replace with your navigation element locators
    // ══════════════════════════════════════════════════════════
    public static class Navigation {

        // Top-level navigation links — use with findElements()
        public static final By NAV_LINKS =
                By.cssSelector("nav a");

        // Sidebar toggle button (mobile/collapsed view)
        public static final By SIDEBAR_TOGGLE =
                By.cssSelector("[data-testid='sidebar-toggle']");

        // User profile menu in header
        public static final By USER_PROFILE_MENU =
                By.cssSelector("[data-testid='user-menu']");

        // Settings link
        public static final By SETTINGS_LINK =
                By.cssSelector("[data-testid='nav-settings']");

        // TODO-1: Replace with your navigation's real locators
    }


    // ══════════════════════════════════════════════════════════
    // DATA TABLE — reusable table locators
    // TODO-1: Replace with your table element locators
    // ══════════════════════════════════════════════════════════
    public static class Table {

        // All data rows in a table body
        public static final By DATA_ROWS =
                By.cssSelector("table tbody tr");

        // Table header cells
        public static final By HEADER_CELLS =
                By.cssSelector("table thead th");

        // Pagination next button
        public static final By PAGINATION_NEXT =
                By.cssSelector("[data-testid='pagination-next']");

        // Pagination previous button
        public static final By PAGINATION_PREV =
                By.cssSelector("[data-testid='pagination-prev']");

        // Current page indicator
        public static final By PAGINATION_CURRENT_PAGE =
                By.cssSelector("[data-testid='pagination-current']");

        // "No results" empty state message
        public static final By EMPTY_STATE_MESSAGE =
                By.cssSelector("[data-testid='empty-state']");

        // Row action buttons (use with findElements() per row)
        public static final By ROW_ACTION_BUTTON =
                By.cssSelector("[data-testid='row-action']");

        // TODO-1: Replace with your data table's real locators
    }


    // ══════════════════════════════════════════════════════════
    // FORM — generic form element locators
    // TODO-1: Replace with your form element locators
    // ══════════════════════════════════════════════════════════
    public static class Form {

        // Generic submit / save button
        public static final By SUBMIT_BUTTON =
                By.cssSelector("[data-testid='form-submit']");

        // Generic cancel button
        public static final By CANCEL_BUTTON =
                By.cssSelector("[data-testid='form-cancel']");

        // Generic form validation error messages
        public static final By VALIDATION_ERRORS =
                By.cssSelector("[data-testid='field-error']");

        // Generic required field error indicator
        public static final By REQUIRED_FIELD_ERROR =
                By.cssSelector(".field-error--required");

        // TODO-1: Replace with your form's real locators
    }


    // ══════════════════════════════════════════════════════════
    // SEARCH
    // TODO-1: Replace with your search element locators
    // ══════════════════════════════════════════════════════════
    public static class Search {

        // Global / page-level search input
        public static final By SEARCH_INPUT =
                By.cssSelector("[data-testid='search-input']");

        // Search submit button
        public static final By SEARCH_BUTTON =
                By.cssSelector("[data-testid='search-btn']");

        // Search results container
        public static final By SEARCH_RESULTS =
                By.cssSelector("[data-testid='search-results']");

        // Individual search result items
        public static final By SEARCH_RESULT_ITEMS =
                By.cssSelector("[data-testid='search-result-item']");

        // "No results found" message
        public static final By NO_RESULTS_MESSAGE =
                By.cssSelector("[data-testid='no-results']");

        // TODO-1: Replace with your search component's real locators
    }


    // ══════════════════════════════════════════════════════════
    // SAMPLE PAGE — used by SamplePage.java and SampleSteps.java
    // TODO-2: Add a class for each additional page in your app
    // TODO-3: Delete this class if not needed in your project
    // ══════════════════════════════════════════════════════════
    public static class SamplePage {

        // Primary action button on the sample page
        public static final By PRIMARY_BUTTON =
                By.cssSelector("[data-testid='primary-action']");

        // Sample page heading
        public static final By PAGE_HEADING =
                By.cssSelector("[data-testid='sample-heading']");

        // Sample page content body
        public static final By PAGE_CONTENT =
                By.cssSelector("[data-testid='sample-content']");

        // Sample input field
        public static final By SAMPLE_INPUT =
                By.id("sampleInput");

        // Sample result display area
        public static final By RESULT_AREA =
                By.cssSelector("[data-testid='result-area']");

        // TODO-1: Replace with your sample page's real locators
    }


    // ══════════════════════════════════════════════════════════
    // HELPER METHOD — build a dynamic locator at runtime
    // Use when the locator value changes per test (e.g. row by name)
    // ══════════════════════════════════════════════════════════

    /**
     * Builds a CSS selector for a row containing specific text.
     * Use to find a table row that contains a known value.
     *
     * Example:
     *   By rowLocator = Locators.rowContainingText("John Doe");
     *   click(rowLocator);
     *
     * TODO-2: Adjust the selector to match your table's row structure
     */
    public static By rowContainingText(String text) {
        return By.xpath("//tr[contains(.,'" + text + "')]");
    }


    /**
     * Builds a locator for a button with specific visible text.
     *
     * Example:
     *   By editBtn = Locators.buttonWithText("Edit");
     *   click(editBtn);
     */
    public static By buttonWithText(String text) {
        return By.xpath("//button[normalize-space()='" + text + "']");
    }


    /**
     * Builds a locator for any element with a specific data-testid.
     * Use when the testid is constructed dynamically (e.g. per row).
     *
     * Example:
     *   By deleteBtn = Locators.byTestId("delete-user-42");
     *   click(deleteBtn);
     */
    public static By byTestId(String testId) {
        return By.cssSelector("[data-testid='" + testId + "']");
    }


    /**
     * Builds a locator for a link with specific visible text.
     *
     * Example:
     *   By usersLink = Locators.linkWithText("Users");
     *   click(usersLink);
     */
    public static By linkWithText(String text) {
        return By.linkText(text);
    }

}