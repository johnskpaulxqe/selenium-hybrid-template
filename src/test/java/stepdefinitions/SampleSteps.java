package stepdefinitions;

/*
  ============================================================
  FILE: SampleSteps.java
  LOCATION: src/test/java/stepdefinitions/SampleSteps.java

  PURPOSE:
    Step definitions for generic UI scenarios in ui.feature.
    Demonstrates common UI step patterns using SamplePage,
    PageVerifier, and CustomAssert.

  TODO (customise per project):
    - TODO-1 : Rename to match your page e.g. UserManagementSteps
    - TODO-2 : Update step text to match your Gherkin wording
    - TODO-3 : Replace SamplePage with your own page objects
  ============================================================
*/

import context.TestContext;
import io.cucumber.java.en.*;
import pages.SamplePage;
import utils.CustomAssert;
import utils.PageVerifier;

public class SampleSteps {

    private final TestContext ctx;
    private final SamplePage  samplePage;

    public SampleSteps(TestContext ctx) {
        this.ctx        = ctx;
        this.samplePage = ctx.getSamplePage();
    }


    // ── GIVEN steps ──────────────────────────────────────────

    @Given("I am on the sample page")
    public void iAmOnTheSamplePage() {
        samplePage.navigateToPage();
    }


    // ── WHEN steps ────────────────────────────────────────────

    @When("I click the primary action button")
    public void iClickThePrimaryActionButton() {
        samplePage.clickPrimaryAction();
    }

    @When("I enter {string} in the input field")
    public void iEnterInTheInputField(String value) {
        samplePage.enterInputValue(value);
    }

    @When("I search for {string}")
    public void iSearchFor(String searchTerm) {
        samplePage.search(searchTerm);
    }

    @When("I click the row containing {string}")
    public void iClickTheRowContaining(String text) {
        samplePage.clickRowContaining(text);
    }

    @When("I confirm the dialog")
    public void iConfirmTheDialog() {
        samplePage.confirmModal();
    }

    @When("I cancel the dialog")
    public void iCancelTheDialog() {
        samplePage.cancelModal();
    }

    @When("I go to the next page")
    public void iGoToTheNextPage() {
        samplePage.goToNextPage();
    }


    // ── THEN steps ────────────────────────────────────────────

    @Then("the page heading should be {string}")
    public void thePageHeadingShouldBe(String expectedHeading) {
        samplePage.verifyHeading(expectedHeading);
    }

    @Then("the result should contain {string}")
    public void theResultShouldContain(String expectedText) {
        samplePage.verifyResultContains(expectedText);
    }

    @Then("the table should have {int} rows")
    public void theTableShouldHaveRows(int expectedCount) {
        samplePage.verifyTableRowCount(expectedCount);
    }

    @Then("the table should contain {string}")
    public void theTableShouldContain(String text) {
        CustomAssert.assertTrue(
                samplePage.tableContainsRow(text),
                "Table should contain row with text: [" + text + "]");
    }

    @Then("the table should not contain {string}")
    public void theTableShouldNotContain(String text) {
        CustomAssert.assertFalse(
                samplePage.tableContainsRow(text),
                "Table should NOT contain row with text: [" + text + "]");
    }

    @Then("search results should be displayed")
    public void searchResultsShouldBeDisplayed() {
        samplePage.verifySearchResultsDisplayed();
    }

    @Then("no results should be displayed")
    public void noResultsShouldBeDisplayed() {
        CustomAssert.assertTrue(
                samplePage.isNoResultsDisplayed(),
                "No results message should be displayed");
    }

    @Then("there should be {int} search results")
    public void thereShouldBeSearchResults(int expectedCount) {
        CustomAssert.assertEquals(
                samplePage.getSearchResultCount(), expectedCount,
                "Search result count");
    }

    @Then("a success message {string} should be displayed")
    public void aSuccessMessageShouldBeDisplayed(String expectedMessage) {
        samplePage.verifySuccessMessage(expectedMessage);
    }

    @Then("the current URL should contain {string}")
    public void theCurrentUrlShouldContain(String urlFragment) {
        PageVerifier.verifyUrlContains(samplePage.driver(), urlFragment);
    }

    @Then("the page should be open")
    public void thePageShouldBeOpen() {
        samplePage.verifyPageIsOpen();
    }

}