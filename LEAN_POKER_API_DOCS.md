# Lean Poker Player API Documentation

**Source:** [https://www.leanpoker.org/docs/api/player](https://www.leanpoker.org/docs/api/player)

## Overview

Players are simple web services that respond to POST requests. Each request includes an `action` parameter that specifies what the player should do. Some actions also include a `game_state` parameter with a JSON document describing the current game state.

## API Actions

### 1. check
- **Purpose:** Health check to verify the player is running
- **Parameters:** None
- **Response:** HTTP 200 OK (response string not used)
- **Example:** `curl -d 'action=check' localhost`

### 2. version
- **Purpose:** Return version identifier for tournament leaderboard
- **Parameters:** None  
- **Response:** Version string (displayed on leaderboard)
- **Implementation:** Define constant in Player class and return its value
- **Example:** `curl -d 'action=version' localhost`

### 3. bet_request
- **Purpose:** Main betting decision when it's player's turn
- **Parameters:** `game_state` (JSON document)
- **Response:** Integer representing chips to add to pot
- **Implementation:** Define `bet_request` method in Player class
- **Default:** Should return 0 initially

#### Betting Rules:
- **0:** Check (if current_buy_in equals player's bet) or Fold (otherwise)
- **Call:** Return `current_buy_in - players[in_action][bet]`
- **Raise:** Return amount > `current_buy_in - players[in_action][bet] + minimum_raise`
- **Invalid bets:** Smaller than call amount are treated as fold

### 4. showdown  
- **Purpose:** Called at round end with revealed opponent cards
- **Parameters:** `game_state` (JSON with opponent hole cards if revealed)
- **Response:** String (not used)
- **Implementation:** Define void `showdown` method for learning algorithms

## Game State Structure

```json
{
  "tournament_id": "550d1d68cd7bd10003000003",
  "game_id": "550da1cb2d909006e90004b1", 
  "round": 0,
  "bet_index": 0,
  "small_blind": 10,
  "current_buy_in": 320,
  "pot": 400,
  "minimum_raise": 240,
  "dealer": 1,
  "orbits": 7,
  "in_action": 1,
  "players": [
    {
      "id": 0,
      "name": "Albert",
      "status": "active", // "active", "folded", "out"
      "version": "Default random player",
      "stack": 1010,
      "bet": 320,
      "hole_cards": [ // Only visible for your player
        {
          "rank": "6", // 2-10, J, Q, K, A
          "suit": "hearts" // clubs, spades, hearts, diamonds
        },
        {
          "rank": "K",
          "suit": "spades" 
        }
      ]
    }
  ],
  "community_cards": [
    {
      "rank": "4",
      "suit": "spades"
    },
    {
      "rank": "A", 
      "suit": "hearts"
    },
    {
      "rank": "6",
      "suit": "clubs"
    }
  ]
}
```

## Key Game State Fields

### Basic Game Info
- `tournament_id`: Unique tournament identifier
- `game_id`: Unique sit'n'go game identifier  
- `round`: Current round index within game
- `bet_index`: Betting opportunity index within round
- `small_blind`: Small blind amount (big blind = 2x small blind)
- `orbits`: Number of completed dealer button rotations

### Betting Information  
- `current_buy_in`: Largest current bet from any player
- `pot`: Total size of pot (sum of all player bets)
- `minimum_raise`: Minimum amount to raise
- `dealer`: Index of dealer button player
- `in_action`: Index of player currently acting

### Players Array
- `id`: Player identifier (same as array index)
- `name`: Player name from tournament config
- `status`: "active", "folded", or "out" 
- `version`: Version string returned by player
- `stack`: Available chips (excluding current round bets)
- `bet`: Chips put into pot this round
- `hole_cards`: Player's cards (only visible for your own player)

### Community Cards
- Array of up to 5 cards revealed on table
- Each card has `rank` (2-10, J, Q, K, A) and `suit` (clubs, spades, hearts, diamonds)

## Betting Calculations

### To Call:
```kotlin
val callAmount = current_buy_in - players[in_action].bet
```

### To Raise (Minimum):
```kotlin  
val minRaise = current_buy_in - players[in_action].bet + minimum_raise
```

### All-in Protection:
```kotlin
val maxBet = players[in_action].stack
```

## Implementation Strategy

1. **Start Simple:** Return 0 from `bet_request` (fold/check only)
2. **Add Basic Logic:** Implement call/fold based on hand strength
3. **Hand Evaluation:** Create function to assess hole cards + community cards
4. **Position Awareness:** Consider dealer position and betting order
5. **Opponent Modeling:** Track betting patterns in `showdown` method
6. **Advanced Strategy:** Implement bluffing, pot odds, expected value calculations

## Example Kotlin Implementation

```kotlin
fun betRequest(game_state: JSONObject): Int {
    val players = game_state.getJSONArray("players")
    val inAction = game_state.getInt("in_action")
    val me = players.getJSONObject(inAction)
    val currentBuyIn = game_state.getInt("current_buy_in")
    val myBet = me.getInt("bet")
    val myStack = me.getInt("stack")
    
    val callAmount = currentBuyIn - myBet
    
    // Simple strategy: call small bets, fold large ones
    return when {
        callAmount <= 0 -> 0 // Check
        callAmount > myStack -> myStack // All-in
        callAmount <= 50 -> callAmount // Call small bets
        else -> 0 // Fold large bets
    }
}
```

## Testing

Use curl to test your player locally:

```bash
# Health check
curl -d 'action=check' localhost:8080

# Version check  
curl -d 'action=version' localhost:8080

# Betting (with sample game state)
curl -d 'action=bet_request&game_state={"players":[...],"pot":100,...}' localhost:8080
```
