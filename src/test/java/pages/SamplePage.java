package pages;

/*
  ============================================================
  FILE: SamplePage.java
  LOCATION: src/test/java/pages/SamplePage.java

  PURPOSE:
    Example Page Object for a generic application page.
    Demonstrates how to build page objects for pages beyond
    login — including table interactions, form submissions,
    search, and modal handling.

    Use this as a template when adding your own page objects.

  HOW TO USE:
    SamplePage samplePage = new SamplePage();
    samplePage.navigateToPage();
    samplePage.clickPrimaryAction();
    samplePage.verifyHeading("Expected Heading");

  TODO (customise per project):
    - TODO-1 : Rename this class to match your actual page
               e.g. UserManagementPage, ProductListPage
    - TODO-2 : Update all navigateTo paths and locators
    - TODO-3 : Replace example methods with methods that
               represent actual interactions on your page
    - TODO-4 : Copy this file as many times as you have pages
               — one class per page or major component
  ============================================================
*/

import utils.CustomAssert;
import utils.Locators;
import utils.PageVerifier;
import utils.WaitUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

// TODO-1: Rename to your page name e.g. public class UserManagementPage extends BasePage
public class SamplePage extends BasePage {

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Navigates to this page and waits for it to load.
     * TODO-2: Replace "/sample" with your page's actual path.
     *
     * Example:
     *   samplePage.navigateToPage();
     */
    public void navigateToPage() {
        log.info("Navigating to sample page");
        // TODO-2: Replace "/sample" with your actual page path
        navigateToPath("/sample");
        waitForPageReady();
    }


    // ═══════════════════════════════════════════════════════════
    // PAGE READINESS
    // ═══════════════════════════════════════════════════════════

    /**
     * Waits for the page to be fully ready:
     *   1. Page load complete
     *   2. Loading spinner gone
     *   3. Main heading visible
     *
     * Call at the start of any step that needs the page loaded.
     * TODO-3: Uncomment verifyElementDisplayed once your heading
     *         locator is configured in Locators.SamplePage
     */
    public void waitForPageReady() {
        log.debug("Waiting for sample page to be ready");
        WaitUtils.waitForPageLoad(driver());
        waitForInvisible(Locators.Common.LOADING_SPINNER);
        // TODO-3: Uncomment once heading locator is configured
        // waitForVisible(Locators.SamplePage.PAGE_HEADING);
    }


    // ═══════════════════════════════════════════════════════════
    // PAGE HEADING
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns the page heading text.
     *
     * Example:
     *   String heading = samplePage.getHeading();
     */
    public String getHeading() {
        return getText(Locators.SamplePage.PAGE_HEADING);
    }


    /**
     * Returns the page content body text.
     *
     * Example:
     *   String content = samplePage.getContentText();
     */
    public String getContentText() {
        return getText(Locators.SamplePage.PAGE_CONTENT);
    }


    // ═══════════════════════════════════════════════════════════
    // ACTIONS — PRIMARY BUTTON
    // ═══════════════════════════════════════════════════════════

    /**
     * Clicks the primary action button on the page.
     * TODO-3: Rename to match your button's purpose
     *         e.g. clickAddUserButton(), clickCreateOrderButton()
     *
     * Example:
     *   samplePage.clickPrimaryAction();
     */
    public void clickPrimaryAction() {
        log.info("Clicking primary action button");
        click(Locators.SamplePage.PRIMARY_BUTTON);
    }


    // ═══════════════════════════════════════════════════════════
    // ACTIONS — FORM INPUT
    // ═══════════════════════════════════════════════════════════

    /**
     * Enters text into the sample input field.
     * TODO-3: Rename and update to match your form's fields
     *
     * Example:
     *   samplePage.enterInputValue("search term");
     */
    public void enterInputValue(String value) {
        log.debug("Entering input value: [{}]", value);
        type(Locators.SamplePage.SAMPLE_INPUT, value);
    }


    /**
     * Returns the current value of the sample input field.
     *
     * Example:
     *   String value = samplePage.getInputValue();
     */
    public String getInputValue() {
        return getInputValue(Locators.SamplePage.SAMPLE_INPUT);
    }


    /**
     * Returns the text shown in the result area.
     * TODO-3: Rename to match your result element
     *
     * Example:
     *   String result = samplePage.getResultText();
     */
    public String getResultText() {
        return getText(Locators.SamplePage.RESULT_AREA);
    }


    // ═══════════════════════════════════════════════════════════
    // ACTIONS — TABLE INTERACTIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns the number of data rows currently in the table.
     *
     * Example:
     *   int count = samplePage.getTableRowCount();
     *   CustomAssert.assertEquals(count, 10, "Table shows 10 rows");
     */
    public int getTableRowCount() {
        int count = getElementCount(Locators.Table.DATA_ROWS);
        log.debug("Table row count: [{}]", count);
        return count;
    }


    /**
     * Returns all table row WebElements.
     * Use to iterate rows and extract cell values.
     *
     * Example:
     *   List<WebElement> rows = samplePage.getTableRows();
     *   for (WebElement row : rows) {
     *       String text = row.getText();
     *   }
     */
    public List<WebElement> getTableRows() {
        return findElements(Locators.Table.DATA_ROWS);
    }


    /**
     * Clicks the row in the table that contains the given text.
     * Useful for selecting a specific record by name or ID.
     *
     * Example:
     *   samplePage.clickRowContaining("John Doe");
     *   samplePage.clickRowContaining("ORD-001234");
     */
    public void clickRowContaining(String text) {
        log.info("Clicking table row containing: [{}]", text);
        By rowLocator = Locators.rowContainingText(text);
        click(rowLocator);
    }


    /**
     * Returns true if the table contains a row with the given text.
     *
     * Example:
     *   boolean found = samplePage.tableContainsRow("John Doe");
     *   CustomAssert.assertTrue(found, "User appears in table");
     */
    public boolean tableContainsRow(String text) {
        By rowLocator = Locators.rowContainingText(text);
        boolean found = isPresent(rowLocator);
        log.debug("Table contains row [{}]: {}", text, found);
        return found;
    }


    /**
     * Clicks the next page button in pagination.
     * Waits for table to reload before returning.
     *
     * Example:
     *   samplePage.goToNextPage();
     */
    public void goToNextPage() {
        log.debug("Clicking pagination next");
        click(Locators.Table.PAGINATION_NEXT);
        WaitUtils.waitForPageLoad(driver());
    }


    // ═══════════════════════════════════════════════════════════
    // ACTIONS — SEARCH
    // ═══════════════════════════════════════════════════════════

    /**
     * Performs a search using the page search field.
     *
     * Example:
     *   samplePage.search("John Doe");
     */
    public void search(String searchTerm) {
        log.info("Searching for: [{}]", searchTerm);
        type(Locators.Search.SEARCH_INPUT, searchTerm);
        click(Locators.Search.SEARCH_BUTTON);
        WaitUtils.waitForInvisible(driver(), Locators.Common.LOADING_SPINNER);
    }


    /**
     * Returns the number of search results shown.
     *
     * Example:
     *   int resultCount = samplePage.getSearchResultCount();
     */
    public int getSearchResultCount() {
        return getElementCount(Locators.Search.SEARCH_RESULT_ITEMS);
    }


    /**
     * Returns true if the "no results" message is displayed.
     *
     * Example:
     *   boolean empty = samplePage.isNoResultsDisplayed();
     */
    public boolean isNoResultsDisplayed() {
        return isDisplayed(Locators.Search.NO_RESULTS_MESSAGE);
    }


    // ═══════════════════════════════════════════════════════════
    // ACTIONS — MODAL
    // ═══════════════════════════════════════════════════════════

    /**
     * Confirms the open modal dialog by clicking the confirm button.
     * Waits for the modal to disappear after clicking.
     *
     * Example:
     *   samplePage.confirmModal();
     */
    public void confirmModal() {
        log.info("Confirming modal dialog");
        click(Locators.Common.MODAL_CONFIRM_BUTTON);
        waitForInvisible(Locators.Common.MODAL_DIALOG);
    }


    /**
     * Cancels/dismisses the open modal dialog.
     *
     * Example:
     *   samplePage.cancelModal();
     */
    public void cancelModal() {
        log.info("Cancelling modal dialog");
        click(Locators.Common.MODAL_CANCEL_BUTTON);
        waitForInvisible(Locators.Common.MODAL_DIALOG);
    }


    // ═══════════════════════════════════════════════════════════
    // VERIFICATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Verifies the page heading matches expected text.
     *
     * Example:
     *   samplePage.verifyHeading("User Management");
     */
    public void verifyHeading(String expectedHeading) {
        PageVerifier.verifyElementText(driver(),
                Locators.SamplePage.PAGE_HEADING, expectedHeading);
    }


    /**
     * Verifies the result area contains the expected text.
     *
     * Example:
     *   samplePage.verifyResultContains("Action completed");
     */
    public void verifyResultContains(String expectedText) {
        PageVerifier.verifyElementTextContains(driver(),
                Locators.SamplePage.RESULT_AREA, expectedText);
    }


    /**
     * Verifies the table has the expected number of rows.
     *
     * Example:
     *   samplePage.verifyTableRowCount(5);
     */
    public void verifyTableRowCount(int expectedCount) {
        PageVerifier.verifyElementCount(driver(),
                Locators.Table.DATA_ROWS, expectedCount);
    }


    /**
     * Verifies at least one search result is shown.
     *
     * Example:
     *   samplePage.verifySearchResultsDisplayed();
     */
    public void verifySearchResultsDisplayed() {
        PageVerifier.verifyElementCountGreaterThan(driver(),
                Locators.Search.SEARCH_RESULT_ITEMS, 0);
    }


    /**
     * Verifies the success toast is shown with the expected message.
     *
     * Example:
     *   samplePage.verifySuccessMessage("Record saved successfully");
     */
    public void verifySuccessMessage(String expectedMessage) {
        PageVerifier.verifySuccessToast(driver(), expectedMessage);
    }


    /**
     * Verifies the page is loaded and the primary button is visible.
     * Use as an "is this page open?" guard in step definitions.
     *
     * Example:
     *   samplePage.verifyPageIsOpen();
     */
    public void verifyPageIsOpen() {
        PageVerifier.verifyElementDisplayed(driver(),
                Locators.SamplePage.PRIMARY_BUTTON);
    }

}