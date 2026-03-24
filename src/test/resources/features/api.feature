# ============================================================
# FILE: api.feature
# LOCATION: src/test/resources/features/api.feature
#
# PURPOSE:
#   BDD scenarios covering REST API contract and integration
#   tests. Covers authentication, CRUD operations, error
#   handling, and performance assertions.
#
# RUNNERS THAT EXECUTE THIS FILE:
#   ApiTestRunner   → picks up all @api scenarios
#   FullSuiteRunner → picks up all scenarios
#
# TODO (customise per project):
#   - TODO-1 : Update endpoint paths to match your API
#   - TODO-2 : Update expected field names in Then steps
#   - TODO-3 : Add scenarios for your specific API endpoints
#   - TODO-4 : Update response time thresholds to match
#              your API's performance SLAs
# ============================================================

@api
Feature: REST API
  As an API consumer
  I want the REST API to behave according to its contract
  So that integrations work reliably

  Background:
    Given the API is available


  # ── Authentication scenarios ───────────────────────────────

  @api @smoke
  Scenario: Login API returns token for valid credentials
    When I send a POST request to "/api/v1/auth/login" with body:
      """
      {
        "username": "admin@yourapp.com",
        "password": "Admin@123"
      }
      """
    Then the response status should be 200
    And the response field "token" should not be null
    And the response time should be below 2000 milliseconds

  @api @regression
  Scenario: Login API returns 401 for invalid credentials
    When I send a POST request to "/api/v1/auth/login" with body:
      """
      {
        "username": "invalid@yourapp.com",
        "password": "WrongPassword"
      }
      """
    Then the response status should be 401

  @api @regression
  Scenario: Authenticated request stores token for subsequent steps
    Given I am authenticated as "admin"
    When I send an authenticated GET request to "/api/v1/users"
    Then the response status should be 200
    And I store the response field "data[0].id" as "firstUserId"


  # ── GET scenarios ─────────────────────────────────────────

  @api @smoke
  Scenario: GET users returns 200 with data array
    Given I am authenticated as "admin"
    When I send an authenticated GET request to "/api/v1/users"
    Then the response status should be 200
    And the response should be successful
    And the response array "data" should not be empty
    And the response time should be below 3000 milliseconds

  @api @regression
  Scenario: GET users without auth returns 401
    When I send a GET request to "/api/v1/users"
    Then the response status should be 401

  @api @regression
  Scenario: GET non-existent resource returns 404
    Given I am authenticated as "admin"
    When I send an authenticated GET request to "/api/v1/users/99999999"
    Then the response status should be 404


  # ── POST scenarios ────────────────────────────────────────

  @api @regression
  Scenario: POST creates a new user and returns 201
    Given I am authenticated as "admin"
    When I send an authenticated POST request to "/api/v1/users" with body:
      """
      {
        "username":  "newuser@yourapp.com",
        "password":  "NewUser@123",
        "email":     "newuser@yourapp.com",
        "firstName": "New",
        "lastName":  "User",
        "role":      "USER"
      }
      """
    Then the response status should be 201
    And the response field "email" should equal "newuser@yourapp.com"
    And the response field "id" should not be null
    And I store the response field "id" as "newUserId"

  @api @regression
  Scenario: POST with missing required field returns 400
    Given I am authenticated as "admin"
    When I send an authenticated POST request to "/api/v1/users" with body:
      """
      {
        "firstName": "Incomplete"
      }
      """
    Then the response status should be 400


  # ── DELETE scenarios ──────────────────────────────────────

  @api @regression
  Scenario: DELETE existing resource returns 204
    Given I am authenticated as "admin"
    When I send a DELETE request to "/api/v1/users/1"
    Then the response status should be 204


  # ── Health check scenario ─────────────────────────────────

  @api @smoke
  Scenario: Health check endpoint returns 200
    When I send a GET request to "/api/v1/health"
    Then the response status should be 200
    And the response body should contain "UP"
    And the response time should be below 1000 milliseconds


  # ── Data-driven API scenarios ─────────────────────────────
  # TODO-3: Add your endpoint paths and expected status codes

  @api @regression
  Scenario Outline: Protected endpoints require authentication
    When I send a GET request to "<endpoint>"
    Then the response status should be 401

    Examples:
      | endpoint          |
      | /api/v1/users     |
      | /api/v1/products  |
