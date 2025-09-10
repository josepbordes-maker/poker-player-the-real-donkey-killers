# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/kotlin/poker/player/kotlin/` (entrypoint: `Application.kt`).
- Core modules (modularized):
  - `Player.kt` — bot facade delegating to strategy classes
  - `BettingStrategy.kt` — basic preflop bet logic and sizing
  - `EnhancedBettingStrategy.kt` — advanced betting with opponent modeling and exploitative adjustments
  - `HandEvaluator.kt` — hand classification (strong/decent/weak/marginal) and strength evaluation
  - `PositionAnalyzer.kt` — position mapping and thresholds/open sizes
  - `GameStateManager.kt` — seen-card memory; showdown processing
  - `CardUtils.kt` — rank helpers and card normalization
  - `OpponentModeling.kt` — advanced opponent profiling and player type classification
  - `PostFlopStrategy.kt` — advanced post-flop strategy with board texture analysis
  - `DynamicStrategyManager.kt` — real-time strategy adaptation based on game conditions
  - `AdaptiveBettingSystem.kt` — performance-based betting adjustments
  - `AdvancedOpponentProfiler.kt` — sophisticated opponent analysis and exploitation
  - `RainManService.kt` — external API integration for enhanced decision-making
  - `StrategyConfig.kt` — runtime configuration management and strategy flags
- Tests: `src/test/kotlin/poker/player/kotlin/` with unit tests per module:
  - `BettingStrategyTest.kt`, `BettingStrategyPostflopTest.kt`, `BettingStrategyPostflopConfigTest.kt`,
    `EnhancedBettingStrategyTest.kt`, `HandEvaluatorTest.kt`, `PositionAnalyzerTest.kt`,
    `GameStateManagerTest.kt`, `CardUtilsTest.kt`, `PlayerTest.kt`, `OpponentModelingTest.kt`,
    `RainManServiceTest.kt`, `StrategyConfigTest.kt`, `TestUtils.kt`.
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
- Strategy v3.2 expectations reflected in tests:
  - Dynamic betting with opponent modeling and exploitative adjustments
  - Advanced post-flop strategy with board texture analysis
  - Real-time strategy adaptation based on game conditions
  - Strong-hand raise when facing bets: `call + 2 × minimum_raise`.
  - No unconditional small-bet calls; require at least a weak-but-playable hand.
  - Blinds open only strong hands; otherwise check.
  - Enhanced opponent profiling and player type classification
- Run locally via `./gradlew test`; target fast, deterministic tests.

## Runtime Strategy Flags
- `STRAT_MODE` = `TIGHT` | `STANDARD` (default) | `LAG` (aliases: `LOOSE`, `AGGRO`)
  - TIGHT lowers risk and calling thresholds; LAG increases both and opens more in late.
- `STRAT_RISK_FREQ` = `0.0–0.5` overrides risk mood probability (default by mode: TIGHT 0.05, STANDARD 0.10, LAG 0.25).
- `STRAT_SMALLBET_MULT` = `0.5–2.0` scales position small-bet call thresholds.
- `STRAT_BLUFF_RAISE` = `off|on` disables/enables bluff-raises in risk mood (default on).
- `STRAT_RAINMAN` = `off|on` disables/enables Rain Man API usage (default on). For CI/offline, set to `off`.
 - `STRAT_ANTI_LIMP` = `off|on` enable/disable anti-limp isolation raises (default on).
 - `STRAT_SQUEEZE` = `off|on` enable/disable squeeze plays over raise+callers (default on).
- Post-flop bet sizing (fractions of pot when no bet to call on flop/turn/river):
  - `STRAT_POSTFLOP_SMALL` (default 0.33)
  - `STRAT_POSTFLOP_MED` (default 0.5)
  - `STRAT_POSTFLOP_BIG` (default 0.66)
  - Also configurable via `src/main/resources/strategy.json` as `postflopSmall|postflopMed|postflopBig`.

Examples:
- `STRAT_MODE=TIGHT ./gradlew run`
- `STRAT_MODE=LAG STRAT_BLUFF_RAISE=off ./gradlew run`
- `./gradlew test -DSTRAT_MODE=TIGHT -DSTRAT_RISK_FREQ=0.05`

### Config file (commit-based switching)
- File: `src/main/resources/strategy.json` is read at runtime and can set the same fields:
  - Example: `{"mode": "STANDARD", "riskFreq": 0.06, "smallBetMult": 0.9, "bluffRaise": true, "rainMan": true, "postflopSmall": 0.35, "postflopMed": 0.55, "postflopBig": 0.8, "antiLimp": true, "squeeze": true}`
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
- `Player.version()` includes the active mode (e.g., `Real Donkey Killer v3.2 - Optimized Quick Wins (STANDARD)`) for quick verification.
- You can globally dial risk/thresholds via `STRAT_RISK_FREQ` and `STRAT_SMALLBET_MULT` without changing the mode.

#### Quick Reference (defaults)
- TIGHT: risk 5%, small-bet threshold 0.8×, late opens strong/decent only
- STANDARD: risk 10%, small-bet threshold 1.0×, late opens strong/decent only
- LAG: risk 25%, small-bet threshold 1.2×, late opens allow weak‑but‑playable

## Advanced Strategy Features (v3.2)

### Dynamic Strategy System
- **Real-Time Adaptation**: Strategy automatically adapts based on game flow and performance
- **Meta-Game Awareness**: Adjusts to table image and opponent reactions
- **Performance Tracking**: Monitors win/loss patterns and adjusts aggression accordingly
- **Tournament Phase Recognition**: Optimizes for early accumulation, bubble survival, and final table play

### Opponent Modeling & Profiling
- **Player Type Classification**: Identifies 7 distinct player types (tight/loose, aggressive/passive, etc.)
- **Behavioral Pattern Recognition**: Tracks VPIP, PFR, aggression, bet sizing patterns
- **Tilt Detection**: Recognizes emotional state changes and exploits them
- **Action Prediction**: Predicts opponent actions with statistical confidence levels
- **Exploitability Scoring**: Rates how exploitable each opponent is

### Advanced Post-Flop Strategy
- **Board Texture Analysis**: Evaluates flop/turn/river textures for strategic decisions
- **Continuation Betting**: Professional-level c-betting strategy based on board and opponents
- **Hand Strength Assessment**: Dynamic evaluation considering board development
- **Pot Control**: Sophisticated pot size management based on hand strength and position

### External Integration
- **Rain Man API**: Optional integration with external poker analysis service
- **Enhanced Decision Making**: Leverages additional data sources when available
- **Configurable**: Can be disabled for offline/CI environments via `STRAT_RAINMAN=off`

## Commit & Pull Request Guidelines
- Commits: imperative, concise subject (e.g., "Add betting strategy and tests").
- PRs must include: scope/intent, before/after behavior, linked issues, test evidence (`./gradlew test` output). Update `Player.version()` when changing strategy.
- Keep changes focused; include new/updated tests for logic changes.

Strategy tracking:
- Update `STRATEGY_SUMMARY.md` and bump `Player.version()` when changing strategic behavior.

Speed of iteration:
- Prefer quick, small commits for each focused change. Land incremental improvements fast and iterate.
- When feasible, run `./gradlew test` before each commit; keep commits atomic so rollbacks are easy.

## Security & Configuration Tips
- Never log sensitive hole cards in production logs.
- Server binds to `$PORT` (Heroku). Validate that the built JAR name in `Procfile` matches the actual artifact under `build/libs/`.
