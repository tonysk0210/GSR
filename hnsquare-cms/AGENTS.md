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

       Clear conventions keep each module maintainable and predictable. Review the sections below before opening a pull
       request.
## Project Structure & Module Organization

- `hnsquare-app-api`: REST controllers, configuration, deployable WAR (`src/main/java`, resources under
  `src/main/resources`).
- `hnsquare-base`, `hnsquare-cms`, `hnsquare-report`, `hnsquare-evaluation`, `hnsquare-utility`: domain logic, business
  flows, reporting, evaluation, and shared helpers; tests mirror packages under `src/test/java`.
- `hnsquare-config`: shared Spring configuration.
- Static assets and SQL scripts live beside the owning module inside `src/main/resources`.

## Build, Test, and Development Commands

- `./gradlew clean build`: compile all modules and execute every unit/integration test.
- `./gradlew test`: run the suite without rebuilding artifacts; add `--info` for flaky diagnostics.
- `./gradlew hnsquare-app-api:bootRun`: launch the API locally with the default profile.
- `./gradlew hnsquare-app-api:dev` (or `:uat`, `:prod`): produce `api.war` stamped with the target profile.

## Coding Style & Naming Conventions

- Target Java 11 with four-space indentation; Lombok is preferred for boilerplate.
- Package names reflect module intent (for example `com.hn2.cms.*`); use suffixes like `...Controller`, `...Service`,
  `...Payload`.
- REST layers rely on constructor injection and Spring stereotypes; profile-specific configs live in
  `application-<profile>.properties`.

## Testing Guidelines

- JUnit 5 via `spring-boot-starter-test`; mirror production packages under `src/test/java`.
- Name tests `*Test`, focusing on validation paths, SQL adapters, Jasper reports, and cross-module flows.
- Run `./gradlew test --info` when diagnosing environment-sensitive failures; ensure new business logic has
  corresponding coverage.

## Commit & Pull Request Guidelines

- Follow concise, present-tense commit messages referencing impacted modules when helpful (e.g.,
  `update cms validation flow`).
- Pull requests must describe scope, risks, rollout, and link the tracking ticket; attach payload samples or screenshots
  for API changes.
- Confirm tests pass and configuration updates are aligned with the targeted environment before requesting review.

## Security & Configuration Tips

- Store environment secrets encrypted with `jasypt-spring-boot`; never commit plaintext credentials.
- Replicate placeholder keys when introducing new profiles and document activation steps alongside the change.
