package stepdefinitions;

/*
  ============================================================
  FILE: DbSteps.java
  LOCATION: src/test/java/stepdefinitions/DbSteps.java

  PURPOSE:
    Step definitions for all database-related Gherkin steps.
    Maps Gherkin steps in db.feature to SampleRepository
    and DBUtils calls.

  TODO (customise per project):
    - TODO-1 : Update step text to match your Gherkin wording
    - TODO-2 : Inject your own repository classes alongside
               or instead of SampleRepository
    - TODO-3 : Add steps for tables specific to your schema
  ============================================================
*/

import context.TestContext;
import io.cucumber.java.en.*;
import repository.SampleRepository;
import utils.*;

import java.util.List;
import java.util.Map;

public class DbSteps {

    private final TestContext      ctx;
    private final SampleRepository userRepo;

    public DbSteps(TestContext ctx) {
        this.ctx      = ctx;
        this.userRepo = new SampleRepository();
    }


    // ── GIVEN steps ──────────────────────────────────────────

    @Given("a database connection is established")
    public void aDatabaseConnectionIsEstablished() {
        // Connection is opened in Hooks.java @Before for @db scenarios
        // This step confirms it is open and valid
        CustomAssert.assertTrue(
                DBUtils.isConnected(),
                "Database connection should be established");
        LogUtil.getLogger(DbSteps.class)
                .info("DB connection confirmed for scenario: [{}]", ctx.getScenarioName());
    }

    @Given("the database contains user {string}")
    public void theDatabaseContainsUser(String email) {
        // Precondition check — verify the user exists before the test
        CustomAssert.assertTrue(
                userRepo.userExistsByEmail(email),
                "Pre-condition: user [" + email + "] should exist in database");
    }


    // ── WHEN steps ────────────────────────────────────────────

    @When("I query the users table for {string}")
    public void iQueryTheUsersTableFor(String email) {
        LogUtil.getLogger(DbSteps.class)
                .info("Querying users table for email: [{}]", email);

        // Execute the named query from dbdata.json
        String sql = TestDataLoader.getDbQuery("findUserByEmail");
        List<Map<String, Object>> results = DBUtils.executeQuery(sql, email);

        // Store results in context for Then steps
        ctx.store("dbQueryResults", results);
        ctx.store("dbQueryEmail",   email);
    }

    @When("I count all users in the database")
    public void iCountAllUsersInTheDatabase() {
        int count = userRepo.countAllUsers();
        ctx.store("dbUserCount", count);
        LogUtil.getLogger(DbSteps.class).info("Total user count: [{}]", count);
    }

    @When("I count users with role {string}")
    public void iCountUsersWithRole(String role) {
        int count = userRepo.countUsersByRole(role);
        ctx.store("dbRoleCount", count);
        LogUtil.getLogger(DbSteps.class)
                .info("User count for role [{}]: [{}]", role, count);
    }


    // ── THEN steps ────────────────────────────────────────────

    @Then("the record should exist")
    public void theRecordShouldExist() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) ctx.retrieve("dbQueryResults");

        CustomAssert.assertNotNull(results, "DB query results should not be null");
        CustomAssert.assertTrue(!results.isEmpty(), "DB query should return at least one record");
    }

    @Then("the record should not exist")
    public void theRecordShouldNotExist() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) ctx.retrieve("dbQueryResults");

        CustomAssert.assertTrue(
                results == null || results.isEmpty(),
                "DB query should return no records");
    }

    @Then("the user {string} should exist in the database")
    public void theUserShouldExistInTheDatabase(String email) {
        CustomAssert.assertTrue(
                userRepo.userExistsByEmail(email),
                "User [" + email + "] should exist in database");
    }

    @Then("the user {string} should not exist in the database")
    public void theUserShouldNotExistInTheDatabase(String email) {
        CustomAssert.assertFalse(
                userRepo.userExistsByEmail(email),
                "User [" + email + "] should NOT exist in database");
    }

    @Then("the user {string} should have role {string}")
    public void theUserShouldHaveRole(String email, String expectedRole) {
        String actualRole = userRepo.getUserRole(email);
        CustomAssert.assertEquals(actualRole, expectedRole,
                "Role for user [" + email + "]");
    }

    @Then("the user {string} should be active")
    public void theUserShouldBeActive(String email) {
        CustomAssert.assertTrue(
                userRepo.isUserActive(email),
                "User [" + email + "] should be active in database");
    }

    @Then("the user count should be greater than {int}")
    public void theUserCountShouldBeGreaterThan(int minimum) {
        int count = (Integer) ctx.retrieve("dbUserCount");
        CustomAssert.assertGreaterThan(count, minimum,
                "Total user count should be greater than " + minimum);
    }

    @Then("the {string} role count should be at least {int}")
    public void theRoleCountShouldBeAtLeast(String role, int minimum) {
        int count = (Integer) ctx.retrieve("dbRoleCount");
        CustomAssert.assertGreaterThan(count, minimum - 1,
                "User count for role [" + role + "] should be at least " + minimum);
    }

    @Then("the admin user data should match expected values")
    public void theAdminUserDataShouldMatchExpectedValues() {
        // Reads expected values from dbdata.json — no hardcoding
        String expectedEmail = TestDataLoader.getDbExpectedValue(
                "expectedValues.users.admin.email");
        String expectedRole  = TestDataLoader.getDbExpectedValue(
                "expectedValues.users.admin.role");

        CustomAssert.assertTrue(
                userRepo.userExistsByEmail(expectedEmail),
                "Admin user should exist with email: " + expectedEmail);

        CustomAssert.assertEquals(
                userRepo.getUserRole(expectedEmail), expectedRole,
                "Admin user role");
    }

}