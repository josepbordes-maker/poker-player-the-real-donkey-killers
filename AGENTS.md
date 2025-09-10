# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/kotlin/poker/player/kotlin/` (entrypoint: `Application.kt`).
- Core modules: `GameState.kt` (models), `HandEvaluator.kt` (hand strength/position), `BettingStrategy.kt` (bet sizing), `Player.kt` (bot facade).
- Tests: `src/test/kotlin/poker/player/kotlin/` with `*Test.kt` classes.
- Build config: `build.gradle`, `settings.gradle`. Deployment uses `Procfile` and `PORT` env var.

## Build, Test, and Development Commands
- Run locally: `./gradlew run` (Ktor server on `:8080` or `$PORT`).
- Test all: `./gradlew test`.
- Test one class: `./gradlew test --tests "*.HandEvaluatorTest"`.
- Build JAR: `./gradlew build` (outputs under `build/libs/`).
- Quick smoke: `curl -X POST localhost:8080 -d "action=version"`.

Requirements: Java 21 toolchain (Gradle config manages this).

## Coding Style & Naming Conventions
- Kotlin official style; 4‑space indent; 100–120 col soft limit.
- Packages: lower_snake by path; types/files: `PascalCase` (e.g., `HandEvaluator.kt`).
- Functions/props: `camelCase`; boolean predicates start with `is/has/should`.
- Prefer data classes, `when` over if‑else, and small, single‑purpose functions.
- Use extension functions for JSON parsing (see `JSONObject.toGameState()`).

## Testing Guidelines
- Frameworks: Kotlin Test + JUnit 5 (`useJUnitPlatform()`).
- Location/naming: mirror package under `src/test/...`, file ends with `Test.kt`.
- Test names: backticked, behavior‑driven (e.g., ``fun `ace king should be premium`()``).
- Aim to cover: hand evaluation, pre‑flop decision matrix, JSON parsing helpers.
- Run locally via `./gradlew test`; target fast, deterministic tests.

## Commit & Pull Request Guidelines
- Commits: imperative, concise subject (e.g., "Add betting strategy and tests").
- PRs must include: scope/intent, before/after behavior, linked issues, test evidence (`./gradlew test` output). Update `PokerPlayer.version()` when changing strategy.
- Keep changes focused; include new/updated tests for logic changes.

## Security & Configuration Tips
- Never log sensitive hole cards in production logs.
- Server binds to `$PORT` (Heroku). Validate that the built JAR name in `Procfile` matches the actual artifact under `build/libs/`.
