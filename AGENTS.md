# Repository Guidelines

## Localization & Language Preference

- 所有 Codex CLI（終端機）回覆應以繁體中文撰寫。
- 若輸入為英文，請自動以繁體中文回覆並保留技術關鍵詞（如 class、method、bean、IoC）。
- 回覆時保持專業、簡潔，除非使用者特別要求，不需翻譯專有名詞。

## Code Explanation & Commenting Standard (Traditional Chinese, Mandatory)

- 對 **每一個新建或修改的程式碼**，請一律提供：
    1) **變更摘要（繁體中文）**：在回覆中以條列清楚說明修改目的、影響範圍、設計取捨與可能的副作用。
    2) **中文註解（內嵌於程式碼）**：在關鍵邏輯、非直覺判斷、錯誤處理、邊界條件與演算法步驟處，加入簡潔的繁體中文註解。
    3) **方法／類別層級註解**：以 JavaDoc 或等價註解（繁體中文）描述用途、參數、回傳與可能例外。
    4) **測試說明**：若有新增/更新測試，說明測試情境與覆蓋的行為（繁體中文）。

- 註解風格要求（以 Java 為例，其他語言比照）：
    - 內嵌註解：使用 `//` 或 `/* ... */`，重點在「為何這麼做」，避免贅述程式表面語意。
    - 保留技術關鍵詞原文（如 IoC、bean、Controller、Factory method），其餘以繁體中文敘述。

- 產出格式（回覆時請同時提供）：
    1) 「變更摘要（繁體中文）」小節
    2) 「修改後的完整檔案」：含繁體中文註解

## Project Structure & Module Organization

This Gradle multi-module Spring Boot project lives under `./hnsquare-*`. `hnsquare-app-api` produces the deployable WAR
and houses controllers and configuration in `src/main/java`. Shared configuration sits in `hnsquare-config`,
foundational domain and data logic in `hnsquare-base`, business flows in `hnsquare-cms`, reporting in `hnsquare-report`,
evaluation logic in `hnsquare-evaluation`, and cross-cutting helpers in `hnsquare-utility`. Static resources and SQL
assets belong alongside each module’s `src/main/resources`. Tests should mirror the same package layout in
`src/test/java`.

## Build, Test, and Development Commands

Run `./gradlew clean build` for a full compile and module test pass. Use `./gradlew test` when iterating on unit tests.
Generate environment-specific deployables with `./gradlew hnsquare-app-api:dev` (or `:uat`, `:prod`) to stamp
`spring.profiles.active` before producing `api.war`. Launch the API locally via `./gradlew hnsquare-app-api:bootRun`.

## Coding Style & Naming Conventions

Target Java 11 with four-space indentation and Lombok for boilerplate reduction. Align package names with module
intent (`com.hn2.cms...`, `com.hn2.config...`) and append feature codes where applicable (for example,
`Mdreg1000Controller`). Payload, service, repository, and DTO classes follow suffix naming (`...Payload`, `...Service`,
etc.). Favor constructor injection, annotate REST layers with Spring stereotypes, and keep configuration in
`application-<profile>.properties`.

## Testing Guidelines

JUnit 5 powers the suite (`spring-boot-starter-test` is preconfigured). Place tests under the matching module
`src/test/java` tree and name files with the `*Test` suffix, mirroring the production package. Aim to cover validation
paths, SQL adapters, and Jasper report builders, adding integration tests with `@SpringBootTest` when touching multiple
modules. Use `./gradlew test --info` to diagnose flaky behavior.

## Commit & Pull Request Guidelines

Recent history favors concise, present-tense summaries (for example, `add logic comments to all methods`). Reference
impacted modules or feature codes when helpful. Each pull request should describe scope, risks, and rollout plan, link
the tracking ticket, and attach screenshots or sample payloads for API-facing changes. Ensure tests pass and
profile-specific configs are updated before requesting review.

## Configuration & Secrets

Environment properties live in `hnsquare-app-api/src/main/resources/application-*.properties`. Store sensitive values
encrypted via `jasypt-spring-boot`; never commit raw credentials. When introducing new profiles, replicate the
placeholder keys and document activation steps in the ticket.
