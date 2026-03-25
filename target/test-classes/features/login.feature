# ============================================================
# FILE: login.feature
# LOCATION: src/test/resources/features/login.feature
#
# PURPOSE:
#   BDD scenarios covering login page functionality.
#   Tagged with @smoke for critical path and @regression
#   for the full suite. Negative scenarios tagged separately.
#
# RUNNERS THAT EXECUTE THIS FILE:
#   SmokeTestRunner      → picks up @smoke scenarios
#   RegressionTestRunner → picks up @regression scenarios
#   FullSuiteRunner      → picks up all scenarios
#
# TODO (customise per project):
#   - TODO-1 : Update scenario text to match your application
#   - TODO-2 : Add scenarios for your specific login flows
#              e.g. SSO, MFA, account lockout after N attempts
#   - TODO-3 : Update Examples tables with your real test data
# ============================================================

@ui
Feature: User Login
  As a registered user
  I want to log in to the application
  So that I can access my account

  Background:
    Given I am on the login page


  # ── Positive scenarios ─────────────────────────────────────

  @smoke @regression
  Scenario: Successful login with valid admin credentials
    When I login as "admin"
    Then I should be on the dashboard

  @smoke @regression
  Scenario: Successful login with valid standard user credentials
    When I login as "standard"
    Then I should be on the dashboard

  @regression
  Scenario: Login button is enabled when page loads
    Then the login button should be enabled


  # ── Negative scenarios ─────────────────────────────────────

  @regression
  Scenario: Failed login with invalid credentials
    When I enter invalid credentials
    And I click the login button
    Then I should see a login error message
    And I should remain on the login page

  @regression
  Scenario: Failed login with wrong password
    When I enter username "admin@yourapp.com" and password "WrongPassword123"
    And I click the login button
    Then I should see a login error message
    And the login error message should contain "Invalid"

  @regression
  Scenario: Failed login with empty credentials
    When I enter username "" and password ""
    And I click the login button
    Then I should see a login error message
    And I should remain on the login page


  # ── Data-driven login scenarios ───────────────────────────
  # TODO-3: Update the Examples table with your user roles

  @regression
  Scenario Outline: Successful login for multiple user roles
    When I login as "<role>"
    Then I should be on the dashboard

    Examples:
      | role     |
      | admin    |
      | standard |
