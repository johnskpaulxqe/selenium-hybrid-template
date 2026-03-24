# ============================================================
# FILE: db.feature
# LOCATION: src/test/resources/features/db.feature
#
# PURPOSE:
#   BDD scenarios covering database state validation.
#   Verifies data integrity, expected records, and DB state
#   after application operations via JDBC.
#
# RUNNERS THAT EXECUTE THIS FILE:
#   DbTestRunner    → picks up all @db scenarios
#   FullSuiteRunner → picks up all scenarios
#
# TODO (customise per project):
#   - TODO-1 : Replace email addresses with your test data values
#   - TODO-2 : Replace role names with your application's roles
#   - TODO-3 : Add scenarios for tables in your schema
#   - TODO-4 : Update expected counts to match your seed data
# ============================================================

@db
Feature: Database Validation
  As a test engineer
  I want to verify the database contains correct data
  So that I can confirm application operations persist correctly

  Background:
    Given a database connection is established


  # ── User record existence scenarios ───────────────────────

  @db @smoke
  Scenario: Admin user exists in the database
    When I query the users table for "admin@yourapp.com"
    Then the record should exist

  @db @regression
  Scenario: Standard user exists in the database
    Then the user "user@yourapp.com" should exist in the database

  @db @regression
  Scenario: Non-existent user is not in the database
    When I query the users table for "doesnotexist@yourapp.com"
    Then the record should not exist


  # ── User attribute validation scenarios ───────────────────

  @db @regression
  Scenario: Admin user has correct role in database
    Then the user "admin@yourapp.com" should have role "ADMIN"

  @db @regression
  Scenario: Standard user has correct role in database
    Then the user "user@yourapp.com" should have role "USER"

  @db @regression
  Scenario: Admin user is active in the database
    Then the user "admin@yourapp.com" should be active

  @db @smoke
  Scenario: Admin user data matches expected values from test data file
    Then the admin user data should match expected values


  # ── Count and aggregate scenarios ─────────────────────────

  @db @regression
  Scenario: Database contains at least one user
    When I count all users in the database
    Then the user count should be greater than 0

  @db @regression
  Scenario: Database contains at least one admin user
    When I count users with role "ADMIN"
    Then the "ADMIN" role count should be at least 1

  @db @regression
  Scenario: Database contains standard users
    When I count users with role "USER"
    Then the "USER" role count should be at least 1


  # ── Data-driven DB scenarios ──────────────────────────────
  # TODO-3: Add your own user emails and expected roles

  @db @regression
  Scenario Outline: Named users exist in database with correct roles
    Then the user "<email>" should exist in the database
    And the user "<email>" should have role "<expectedRole>"

    Examples:
      | email                    | expectedRole |
      | admin@yourapp.com        | ADMIN        |
      | user@yourapp.com         | USER         |
