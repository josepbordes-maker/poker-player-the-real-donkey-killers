# The Real Donkey Killers - Poker Strategy Summary

## Overview
Our poker bot "Real Donkey Killer" implements a comprehensive position-aware strategy with aggressive tendencies and calculated risk-taking. The strategy balances tight-aggressive play in early positions with more speculative play in late positions.

## Core Strategy Components

### 1. Position Awareness System
The bot classifies positions into four categories:
- **EARLY**: Conservative play, only strong hands
- **MIDDLE**: Moderate aggression with decent hands
- **LATE**: More speculative play, wider hand range
- **BLINDS**: Defensive but opportunistic

Position calculation accounts for dealer position and uses relative positioning for accurate classification.

### 2. Hand Strength Classification

#### Strong Hands (Premium)
- High pocket pairs (10+): TT, JJ, QQ, KK, AA
- Premium suited aces: AK (suited/offsuit), AQ (suited/offsuit)

**Strategy**: Always aggressive - raise/re-raise when possible

#### Decent Hands (Solid)
- Any pocket pair (22-99 included)
- High cards (9+ value)
- Suited connectors and gappers (max 2-gap)
- Any ace or king
- Broadway combinations (T, J, Q, K, A)

**Strategy**: Position-dependent aggression, willing to call reasonable bets

#### Weak but Playable Hands
- Any suited cards
- Connected cards (1-gap max)
- Face cards (J+)
- Medium pairs (88+)

**Strategy**: Opportunistic play, small bet calls only

#### Marginal Hands (Bluff Candidates)
- One high card (T+)
- Suited with small gaps (3-gap max)
- Small gap connectors

**Strategy**: Occasional bluffs and risk-taking moves

### 3. Betting Strategy

#### Pre-Flop Aggression (No Current Bet)
- **Early Position**: 6x small blind with strong hands only
- **Middle Position**: 6x SB (strong), 4x SB (decent)
- **Late/Blinds**: 6x SB (strong), 4x SB (decent)

#### Facing Bets - Position-Dependent Thresholds
- **Early Position**: Call if bet ≤ pot/4
- **Middle Position**: Call if bet ≤ pot/3  
- **Late/Blinds**: Call if bet ≤ pot/2

#### Special Betting Rules
- Always call small bets (≤ 2x small blind) with any playable hand
- Weak playable hands: call only if bet ≤ 2x small blind
- All-in protection: never bet more than current stack

### 4. Randomized Risk-Taking
- **10% chance** (default) to enter "risk mood" each hand
- In risk mood with marginal hands and reasonable pot odds (bet ≤ pot/3):
  - **30% chance** to bluff-raise (call + minimum raise)
  - **70% chance** to call
- Bluff-raise limited to ≤ 25% of stack for bankroll protection

### 5. Bankroll Management
- Stack protection: never risk more than available
- Bluff sizing: limited to 25% of stack maximum
- All-in scenarios: handled gracefully when forced

### 6. Card Memory System
- Tracks seen cards per game for future enhancement
- Clears memory after showdown
- Currently passive but infrastructure ready for advanced features

## Strategy Strengths

1. **Position Awareness**: Tight in early position, aggressive in late position
2. **Balanced Aggression**: Mix of tight-aggressive and controlled speculation
3. **Randomization**: Unpredictable play prevents exploitation
4. **Risk Management**: Multiple safeguards against stack depletion
5. **Adaptable Thresholds**: Position-based bet sizing and calling ranges

## Strategy Philosophy

The "Real Donkey Killer" employs a **tight-aggressive** base strategy enhanced with:
- **Controlled aggression** in favorable positions
- **Random variance** to maintain unpredictability  
- **Calculated risks** with marginal holdings
- **Conservative bankroll** protection

The strategy aims to:
- Maximize value from strong hands
- Steal pots with position and aggression
- Avoid major losses through position discipline
- Maintain balanced, unexploitable play

## Strategy Modes

Three runtime-selectable modes adjust risk and looseness without code changes:

- TIGHT
  - Risk mood: ~5%
  - Call thresholds vs small bets: 0.8× position baseline (more folds)
  - Late opens: do not open weak‑but‑playable hands; stick to decent+ only

- STANDARD (default)
  - Risk mood: ~10%
  - Call thresholds: 1.0× position baseline
  - Late opens: decent+ only (no weak‑but‑playable opens)

- LAG (loose‑aggressive)
  - Risk mood: ~25%
  - Call thresholds: 1.2× position baseline (more calls)
  - Late opens: allow weak‑but‑playable opens (e.g., suited/connected)

How to select: set `STRAT_MODE=TIGHT|STANDARD|LAG` or edit `src/main/resources/strategy.json` (see AGENTS.md for overrides).

### Quick Reference (defaults)

- TIGHT
  - Risk mood probability: 5%
  - Small-bet call threshold multiplier: 0.8×
  - Late opens: only strong/decent hands

- STANDARD
  - Risk mood probability: 10%
  - Small-bet call threshold multiplier: 1.0×
  - Late opens: only strong/decent hands

- LAG
  - Risk mood probability: 25%
  - Small-bet call threshold multiplier: 1.2×
  - Late opens: allow weak‑but‑playable hands

Notes
- You can override risk and thresholds for any mode using `STRAT_RISK_FREQ` and `STRAT_SMALLBET_MULT`.
- You can disable bluff-raises (in risk mood) with `STRAT_BLUFF_RAISE=off`.

## Current Limitations & Future Enhancements

### Limitations
- No post-flop play optimization
- No opponent modeling or history tracking
- No stack-size relative adjustments
- No tournament vs. cash game differentiation

### Ready Infrastructure
- Card tracking system in place
- Position calculation framework
- Randomization engine ready
- Expandable hand classification system

## Technical Implementation
- Built in Kotlin with Ktor web framework
- JSON-based game state parsing
- Modular hand evaluation functions
- Memory-efficient card tracking
- RESTful API compliance with Lean Poker standards

---

---

Defaults can be tuned at runtime using `STRAT_MODE` (TIGHT | STANDARD | LAG) and optional overrides (see AGENTS.md). The above reflects STANDARD defaults.

*Strategy Version: Real Donkey Killer v1.2*  
*Last Updated: Current Implementation Analysis*
