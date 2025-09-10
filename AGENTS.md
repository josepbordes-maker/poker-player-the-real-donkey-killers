# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/kotlin/poker/player/kotlin/` (entrypoint: `Application.kt`).
- Core modules (modularized):
  - `Player.kt` — bot facade delegating to strategy classes
  - `BettingStrategy.kt` — preflop bet logic and sizing
  - `HandEvaluator.kt` — hand classification (strong/decent/weak/marginal)
  - `PositionAnalyzer.kt` — position mapping and thresholds/open sizes
  - `GameStateManager.kt` — seen-card memory; showdown processing
  - `CardUtils.kt` — rank helpers and card normalization
- Tests: `src/test/kotlin/poker/player/kotlin/` with unit tests per module:
  - `BettingStrategyTest.kt`, `HandEvaluatorTest.kt`, `PositionAnalyzerTest.kt`,
    `GameStateManagerTest.kt`, `CardUtilsTest.kt`, `PlayerTest.kt`.
- Build config: `build.gradle`, `settings.gradle`. Deployment uses `Procfile` and `PORT` env var.

## Build, Test, and Development Commands
- Run locally: `./gradlew run` (Ktor server on `:8080` or `$PORT`).
- Test all: `./gradlew test`.
- Test one class: `./gradlew test --tests "*.BettingStrategyTest"` (or any `*Test`).
- Build JAR: `./gradlew build` (outputs under `build/libs/`).
- Quick smoke: `curl -X POST localhost:8080 -d "action=version"`.

Requirements: Java 21 toolchain (Gradle config manages this). For tests, ensure JUnit 5 is enabled in `build.gradle`:

- Add dependencies:
  - `testImplementation "org.jetbrains.kotlin:kotlin-test"`
  - `testImplementation "org.jetbrains.kotlin:kotlin-test-junit5"`
  - `testImplementation "org.junit.jupiter:junit-jupiter-api:5.10.2"`
  - `testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.10.2"`
- Configure: `test { useJUnitPlatform() }`

## Coding Style & Naming Conventions
- Kotlin official style; 4‑space indent; 100–120 col soft limit.
- Packages: lower_snake by path; types/files: `PascalCase` (e.g., `HandEvaluator.kt`).
- Functions/props: `camelCase`; boolean predicates start with `is/has/should`.
- Prefer data classes, `when` over if‑else, and small, single‑purpose functions.
- Prefer small helpers over heavy parsing layers; use `CardUtils` and `GameStateManager` for card and state helpers.

## Testing Guidelines
- Frameworks: Kotlin Test + JUnit 5 (`useJUnitPlatform()`).
- Location/naming: mirror package under `src/test/...`, file ends with `Test.kt`.
- Test names: backticked, behavior‑driven (e.g., ``fun `ace king should be premium`()``).
- Aim to cover: hand evaluation, pre‑flop decision matrix, position thresholds, card memory helpers.
- Strategy v1.1 expectations reflected in tests:
  - Strong-hand raise when facing bets: `call + 2 × minimum_raise`.
  - No unconditional small-bet calls; require at least a weak-but-playable hand.
  - Blinds open only strong hands; otherwise check.
- Run locally via `./gradlew test`; target fast, deterministic tests.

## Runtime Strategy Flags
- `STRAT_MODE` = `TIGHT` | `STANDARD` (default) | `LAG` (aliases: `LOOSE`, `AGGRO`)
  - TIGHT lowers risk and calling thresholds; LAG increases both and opens more in late.
- `STRAT_RISK_FREQ` = `0.0–0.5` overrides risk mood probability (default by mode: TIGHT 0.05, STANDARD 0.10, LAG 0.25).
- `STRAT_SMALLBET_MULT` = `0.5–2.0` scales position small-bet call thresholds.
- `STRAT_BLUFF_RAISE` = `off|on` disables/enables bluff-raises in risk mood (default on).

Examples:
- `STRAT_MODE=TIGHT ./gradlew run`
- `STRAT_MODE=LAG STRAT_BLUFF_RAISE=off ./gradlew run`
- `./gradlew test -DSTRAT_MODE=TIGHT -DSTRAT_RISK_FREQ=0.05`

### Config file (commit-based switching)
- File: `src/main/resources/strategy.json` is read at runtime and can set the same fields:
  - `{"mode": "LAG", "riskFreq": 0.12, "smallBetMult": 1.1, "bluffRaise": false}`
- Precedence: JVM property/env var > config file > defaults.
- To try a new strategy with a tiny commit, change only `strategy.json` and deploy.

### Strategy Modes
- TIGHT
  - Risk mood: ~5% (`STRAT_RISK_FREQ` overridable)
  - Small-bet call thresholds: 0.8× position baseline
  - Late opens: only strong/decent hands (no weak‑but‑playable)
- STANDARD (default)
  - Risk mood: ~10%
  - Small-bet call thresholds: 1.0× baseline
  - Late opens: only strong/decent
- LAG
  - Risk mood: ~25%
  - Small-bet call thresholds: 1.2× baseline
  - Late opens: allow weak‑but‑playable hands

Notes
- `Player.version()` includes the active mode (e.g., `v1.2 (STANDARD)`) for quick verification.
- You can globally dial risk/thresholds via `STRAT_RISK_FREQ` and `STRAT_SMALLBET_MULT` without changing the mode.

## Commit & Pull Request Guidelines
- Commits: imperative, concise subject (e.g., "Add betting strategy and tests").
- PRs must include: scope/intent, before/after behavior, linked issues, test evidence (`./gradlew test` output). Update `Player.version()` when changing strategy.
- Keep changes focused; include new/updated tests for logic changes.

Strategy tracking:
- Update `STRATEGY_SUMMARY.md` and bump `Player.version()` when changing strategic behavior.

## Security & Configuration Tips
- Never log sensitive hole cards in production logs.
- Server binds to `$PORT` (Heroku). Validate that the built JAR name in `Procfile` matches the actual artifact under `build/libs/`.
