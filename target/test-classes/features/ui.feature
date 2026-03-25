# ============================================================
# FILE: ui.feature
# LOCATION: src/test/resources/features/ui.feature
#
# PURPOSE:
#   BDD scenarios covering general UI interactions beyond login.
#   Demonstrates table, search, modal, and navigation patterns.
#   Replace example scenarios with your application's real flows.
#
# RUNNERS THAT EXECUTE THIS FILE:
#   SmokeTestRunner      → picks up @smoke scenarios
#   RegressionTestRunner → picks up @regression scenarios
#   FullSuiteRunner      → picks up all scenarios
#
# TODO (customise per project):
#   - TODO-1 : Replace "sample page" with your real page names
#   - TODO-2 : Replace example steps with real user journeys
#   - TODO-3 : Add feature files per major feature area
#              e.g. user-management.feature, orders.feature
# ============================================================

@ui
Feature: Sample Page UI Interactions
  As a logged-in user
  I want to interact with the application pages
  So that I can complete my tasks

  Background:
    Given I am logged in as "admin"
    And I am on the sample page


  # ── Page load scenarios ────────────────────────────────────

  @smoke @regression
  Scenario: Sample page loads successfully
    Then the page should be open
    And the current URL should contain "/sample"

  @regression
  Scenario: Page heading displays correctly
    Then the page heading should be "Sample Page"


  # ── Table scenarios ────────────────────────────────────────
  # TODO-1: Replace with your application's table page name

  @regression
  Scenario: Table displays data rows
    Then the table should have 10 rows

  @regression
  Scenario: Table contains expected record
    Then the table should contain "John Doe"

  @regression
  Scenario: Clicking a table row navigates correctly
    When I click the row containing "John Doe"
    Then the current URL should contain "/detail"


  # ── Search scenarios ───────────────────────────────────────

  @regression
  Scenario: Search returns results for valid term
    When I search for "John"
    Then search results should be displayed

  @regression
  Scenario: Search returns no results for unknown term
    When I search for "xyznotfound123"
    Then no results should be displayed

  @regression
  Scenario: Search results count is correct
    When I search for "John"
    Then there should be 3 search results


  # ── Action and feedback scenarios ─────────────────────────

  @smoke @regression
  Scenario: Primary action button performs expected action
    When I click the primary action button
    Then a success message "Action completed" should be displayed

  @regression
  Scenario: Input field accepts and displays value
    When I enter "test value" in the input field
    Then the result should contain "test value"


  # ── Modal scenarios ────────────────────────────────────────

  @regression
  Scenario: Confirming modal dialog completes action
    When I click the primary action button
    And I confirm the dialog
    Then a success message "Confirmed" should be displayed

  @regression
  Scenario: Cancelling modal dialog dismisses it
    When I click the primary action button
    And I cancel the dialog
    Then the page should be open


  # ── Pagination scenario ────────────────────────────────────

  @regression
  Scenario: Navigating to next page updates table content
    When I go to the next page
    Then the current URL should contain "page=2"
