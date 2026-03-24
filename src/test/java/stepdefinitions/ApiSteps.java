package stepdefinitions;

/*
  ============================================================
  FILE: ApiSteps.java
  LOCATION: src/test/java/stepdefinitions/ApiSteps.java

  PURPOSE:
    Step definitions for all API-related Gherkin steps.
    Maps Gherkin steps in api.feature to ApiUtils,
    RequestBuilder, and ResponseValidator calls.
    Stores API responses in TestContext for Then steps.

  TODO (customise per project):
    - TODO-1 : Update step text to match your Gherkin wording
    - TODO-2 : Add steps for your specific API endpoints
    - TODO-3 : Add authentication steps if your API uses
               a different auth mechanism than Bearer token
  ============================================================
*/

import context.TestContext;
import io.cucumber.java.en.*;
import io.restassured.response.Response;
import utils.*;

import java.util.Map;

public class ApiSteps {

    private final TestContext ctx;

    public ApiSteps(TestContext ctx) {
        this.ctx = ctx;
    }


    // ── GIVEN steps ──────────────────────────────────────────

    @Given("the API is available")
    public void theApiIsAvailable() {
        // ApiUtils is configured in Hooks.java @Before for @api scenarios
        // This step is a no-op confirmation that setup occurred
        LogUtil.getLogger(ApiSteps.class)
                .info("API setup confirmed for scenario: [{}]", ctx.getScenarioName());
    }

    @Given("I am authenticated as {string}")
    public void iAmAuthenticatedAs(String userRole) {
        // Perform login via API to get a Bearer token
        String endpoint = TestDataLoader.getApiEndpoint("login");

        Map<String, String> credentials = Map.of(
                "username", TestDataLoader.getUsername(userRole),
                "password", TestDataLoader.getPassword(userRole)
        );

        Response response = ApiUtils.post(endpoint, credentials);

        // Assert login succeeded
        ResponseValidator.assertStatusCode(response, 200);
        ResponseValidator.assertFieldNotNull(response, "token");

        // Store the token for subsequent steps in this scenario
        String token = ResponseValidator.extractString(response, "token");
        ctx.setAuthToken(token);
        ctx.setLoggedInUsername(TestDataLoader.getUsername(userRole));

        LogUtil.getLogger(ApiSteps.class)
                .info("Authenticated as role: [{}]", userRole);
    }


    // ── WHEN steps — simple requests ─────────────────────────

    @When("I send a GET request to {string}")
    public void iSendAGetRequestTo(String endpoint) {
        LogUtil.getLogger(ApiSteps.class).info("GET {}", endpoint);
        Response response = ApiUtils.get(endpoint);
        ctx.setLastApiResponse(response);
    }

    @When("I send an authenticated GET request to {string}")
    public void iSendAnAuthenticatedGetRequestTo(String endpoint) {
        LogUtil.getLogger(ApiSteps.class).info("GET (auth) {}", endpoint);
        Response response = ApiUtils.getWithAuth(endpoint, ctx.getAuthToken());
        ctx.setLastApiResponse(response);
    }

    @When("I send a POST request to {string} with body:")
    public void iSendAPostRequestToWithBody(String endpoint, String body) {
        LogUtil.getLogger(ApiSteps.class).info("POST {}", endpoint);
        Response response = ApiUtils.post(endpoint, body);
        ctx.setLastApiResponse(response);
    }

    @When("I send an authenticated POST request to {string} with body:")
    public void iSendAnAuthenticatedPostRequestToWithBody(String endpoint, String body) {
        LogUtil.getLogger(ApiSteps.class).info("POST (auth) {}", endpoint);
        Response response = ApiUtils.postWithAuth(endpoint, body, ctx.getAuthToken());
        ctx.setLastApiResponse(response);
    }

    @When("I send a DELETE request to {string}")
    public void iSendADeleteRequestTo(String endpoint) {
        LogUtil.getLogger(ApiSteps.class).info("DELETE (auth) {}", endpoint);
        Response response = ApiUtils.deleteWithAuth(endpoint, ctx.getAuthToken());
        ctx.setLastApiResponse(response);
    }


    // ── WHEN steps — named test data requests ─────────────────

    @When("I send the {string} API request")
    public void iSendTheApiRequest(String requestName) {
        // Looks up endpoint and body from apidata.json by name
        String   endpoint = TestDataLoader.getApiEndpoint(requestName);
        String   body     = JsonReader.toJson(TestDataLoader.getRequestBody(requestName));
        Response response = ApiUtils.postWithAuth(endpoint, body, ctx.getAuthToken());
        ctx.setLastApiResponse(response);
    }

    @When("I request the {string} endpoint")
    public void iRequestTheEndpoint(String endpointName) {
        // Looks up endpoint path from apidata.json
        String   endpoint = TestDataLoader.getApiEndpoint(endpointName);
        Response response = ApiUtils.getWithAuth(endpoint, ctx.getAuthToken());
        ctx.setLastApiResponse(response);
    }


    // ── THEN steps — status code ──────────────────────────────

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        ResponseValidator.assertStatusCode(ctx.getLastApiResponse(), expectedStatus);
    }

    @Then("the response should be successful")
    public void theResponseShouldBeSuccessful() {
        ResponseValidator.assertSuccess(ctx.getLastApiResponse());
    }


    // ── THEN steps — response body ────────────────────────────

    @Then("the response field {string} should equal {string}")
    public void theResponseFieldShouldEqual(String jsonPath, String expectedValue) {
        ResponseValidator.assertFieldEquals(
                ctx.getLastApiResponse(), jsonPath, expectedValue);
    }

    @Then("the response field {string} should not be null")
    public void theResponseFieldShouldNotBeNull(String jsonPath) {
        ResponseValidator.assertFieldNotNull(ctx.getLastApiResponse(), jsonPath);
    }

    @Then("the response body should contain {string}")
    public void theResponseBodyShouldContain(String expectedText) {
        ResponseValidator.assertBodyContains(ctx.getLastApiResponse(), expectedText);
    }

    @Then("the response array {string} should not be empty")
    public void theResponseArrayShouldNotBeEmpty(String jsonPath) {
        ResponseValidator.assertArrayNotEmpty(ctx.getLastApiResponse(), jsonPath);
    }

    @Then("the response array {string} should have {int} items")
    public void theResponseArrayShouldHaveItems(String jsonPath, int expectedCount) {
        ResponseValidator.assertArraySize(
                ctx.getLastApiResponse(), jsonPath, expectedCount);
    }


    // ── THEN steps — performance ──────────────────────────────

    @Then("the response time should be below {int} milliseconds")
    public void theResponseTimeShouldBeBelowMilliseconds(int thresholdMs) {
        ResponseValidator.assertResponseTimeBelow(
                ctx.getLastApiResponse(), thresholdMs);
    }


    // ── THEN steps — store values for later steps ─────────────

    @Then("I store the response field {string} as {string}")
    public void iStoreTheResponseFieldAs(String jsonPath, String contextKey) {
        String value = ResponseValidator.extractString(
                ctx.getLastApiResponse(), jsonPath);
        ctx.store(contextKey, value);
        LogUtil.getLogger(ApiSteps.class)
                .debug("Stored [{}] = [{}]", contextKey, value);
    }

}