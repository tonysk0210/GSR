# Repository Guidelines

## Project Structure & Modules
- Multi-module Gradle (Java 11, Spring Boot 2.7):
  - `hnsquare-app-api` — API entrypoint, WAR packaging, templates/resources.
  - `hnsquare-base` — core models, payloads, DTOs, common controllers/handlers.
  - `hnsquare-config` — Spring config (Web, Datasource, Swagger, CORS).
  - `hnsquare-cms` — business services, repositories, payload/DTO classes.
  - `hnsquare-report` — JasperReports integration and resources.
  - `hnsquare-utility` — utilities (cache, dates, SQL helpers), unit tests.
  - `hnsquare-evaluation` — assessment/evaluation module.
- Standard layout per module: `src/main/java`, `src/main/resources`, `src/test/java`.

## Build, Test, and Run
- Build all + run tests: `./gradlew clean build`
- Run API locally (dev): `./gradlew :hnsquare-app-api:bootRun --args='--spring.profiles.active=dev'`
- Build WAR with profile: `./gradlew :hnsquare-app-api:dev` | `:uat` | `:prod`
- Test a module: `./gradlew :hnsquare-utility:test`

## Coding Style & Naming
- Java 11, UTF-8, 4-space indent. Package root: `com.hn2`.
- Classes `PascalCase`; methods/fields `camelCase`.
- Suffix conventions: `*Controller`, `*Service`, `*Repository`, `*Dto`, `*Payload` (see `hnsquare-cms`).
- Validation via `javax.validation`; API docs via Springfox Swagger annotations.
- Lombok is used; avoid manually generating boilerplate it provides.

## Testing Guidelines
- Framework: JUnit 5 (use `@Test`).
- Location mirrors source packages under `src/test/java`.
- Naming: end with `Test` (e.g., `SqlStringHelperTest`).
- Run all tests: `./gradlew test`. Prefer module-scoped tests for business logic.

## Commit & Pull Requests
- Commits: short imperative subject (≤72 chars), optional scope (module), meaningful body for context and breaking changes. Mixed EN/ZH OK, be consistent.
- PRs must include: clear description, linked issue, what/why, profile impacts (dev/uat/prod), test evidence (logs/screenshots) when behavior changes.

## Security & Configuration
- Profiles in `hnsquare-app-api/src/main/resources/application-*.properties`; set with `spring.profiles.active`.
- Secrets: use Jasypt encryption; avoid committing plaintext credentials. DB is MSSQL; SQL logging via log4jdbc.
- Swagger enabled; restrict exposure in non-dev profiles. Cache via Ehcache.

## Agent-Specific Notes
- Preserve module boundaries and dependency versions. Prefer targeted changes within the affected module.
- Follow naming patterns above and keep changes minimal; add or update tests alongside logic changes.

