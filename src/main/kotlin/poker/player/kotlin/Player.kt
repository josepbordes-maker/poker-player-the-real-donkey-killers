package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

class Player {
    private val gameStateManager = GameStateManager()
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val opponentModeling = OpponentModeling()
    private val bettingStrategy = BettingStrategy(handEvaluator, positionAnalyzer)
    private val enhancedBettingStrategy = EnhancedBettingStrategy(handEvaluator, positionAnalyzer, opponentModeling)
    private val postFlopStrategy = PostFlopStrategy(handEvaluator, positionAnalyzer, opponentModeling)
    private val dynamicStrategyManager = DynamicStrategyManager(handEvaluator, positionAnalyzer, opponentModeling)
    
    // Track game state for enhanced decision making
    private var lastAction: String = ""
    private var wasPreFlopAggressor: Boolean = false
    private var lastBetAmount: Int = 0

    fun betRequest(game_state: JSONObject): Int {
        // Parse game state
        val players = game_state.getJSONArray("players")
        val inAction = game_state.getInt("in_action")
        val me = players.getJSONObject(inAction)
        
        // Get my information
        val myStack = me.getInt("stack")
        val myBet = me.getInt("bet")
        val myCards = me.getJSONArray("hole_cards")
        
        // Get game information
        val gameId = game_state.getString("game_id")
        val currentBuyIn = game_state.getInt("current_buy_in")
        val pot = game_state.getInt("pot")
        val smallBlind = game_state.getInt("small_blind")
        val minimumRaise = game_state.getInt("minimum_raise")
        val dealer = game_state.getInt("dealer")
        val round = game_state.optInt("round", 0)
        
        println("\n=== BETTING DECISION ANALYSIS ===")
        println("Game ID: $gameId, Round: $round")
        println("My Position: Player $inAction (dealer: $dealer)")
        println("My Stack: $myStack, My Current Bet: $myBet")
        println("Current Buy-in: $currentBuyIn, Pot: $pot")
        println("Small Blind: $smallBlind, Min Raise: $minimumRaise")
        
        // Format and log my cards
        val myCardsStr = formatCards(myCards)
        println("My Cards: $myCardsStr")
        
        // Remember seen cards (hole + community) for this game
        gameStateManager.addSeenCards(gameId, myCards)
        val community = game_state.optJSONArray("community_cards") ?: JSONArray()
        if (community.length() > 0) {
            gameStateManager.addSeenCards(gameId, community)
            val communityStr = formatCards(community)
            println("Community Cards: $communityStr")
        } else {
            println("Community Cards: None (Pre-flop)")
        }

        // Get position for betting decision
        val position = positionAnalyzer.getPosition(players, inAction, dealer)
        println("My Position: ${position.name}")
        
        // Log active players and their stacks
        logPlayerStates(players, inAction)
        
        // Calculate call amount
        val callAmount = currentBuyIn - myBet
        println("Call Amount: $callAmount")
        
        // Record opponent actions for modeling
        println("Recording opponent actions for modeling...")
        recordOpponentActions(players, currentBuyIn, pot, position, community)
        
        // Determine if this is post-flop play
        val isPostFlop = community.length() >= 3
        val street = when (community.length()) {
            0 -> "PRE-FLOP"
            3 -> "FLOP"
            4 -> "TURN"
            5 -> "RIVER"
            else -> "UNKNOWN"
        }
        println("Street: $street (isPostFlop: $isPostFlop)")
        
        // Find who was the aggressor (last to raise)
        val aggressorId = findLastAggressor(players, currentBuyIn)
        val opponentCount = countActivePlayers(players) - 1
        println("Aggressor: ${aggressorId ?: "None"}, Opponent Count: $opponentCount")
        println("Was Pre-flop Aggressor: $wasPreFlopAggressor")
        
        println("Using DYNAMIC WORLD CHAMPION strategy...")
        val dynamicDecision = dynamicStrategyManager.calculateOptimalBet(
            myCards = myCards,
            communityCards = community,
            myStack = myStack,
            myBet = myBet,
            currentBuyIn = currentBuyIn,
            pot = pot,
            smallBlind = smallBlind,
            minimumRaise = minimumRaise,
            position = position,
            players = players,
            inAction = inAction,
            dealer = dealer,
            round = round,
            aggressorId = aggressorId
        )
        
        val betAmount = dynamicDecision.amount
        
        // Update our action tracking
        updateActionTracking(betAmount, currentBuyIn, myBet)
        
        // Log final decision
        val actionDescription = when {
            betAmount == 0 -> "FOLD"
            betAmount == callAmount -> "CALL ($callAmount)"
            betAmount > callAmount -> "RAISE to $betAmount (raise by ${betAmount - callAmount})"
            betAmount < callAmount && callAmount > 0 -> "ALL-IN $betAmount (short of call)"
            else -> "BET $betAmount"
        }
        println("FINAL DECISION: $actionDescription")
        println("Action Tracking Updated: lastAction=$lastAction, lastBetAmount=$lastBetAmount")
        println("=== END BETTING ANALYSIS ===\n")
        
        return betAmount
    }
    fun showdown(game_state: JSONObject) {
        println("\n=== SHOWDOWN CALLED ===")
        gameStateManager.processShowdown(game_state)
        println("=== SHOWDOWN COMPLETE ===\n")
    }
    
    private fun formatCards(cards: JSONArray): String {
        if (cards.length() == 0) return "None"
        
        val cardStrings = mutableListOf<String>()
        for (i in 0 until cards.length()) {
            val card = cards.getJSONObject(i)
            val rank = card.getString("rank")
            val suit = card.getString("suit")
            val suitSymbol = when (suit) {
                "spades" -> "♠"
                "hearts" -> "♥"
                "diamonds" -> "♦"
                "clubs" -> "♣"
                else -> suit.first().uppercaseChar()
            }
            cardStrings.add("$rank$suitSymbol")
        }
        return cardStrings.joinToString(" ")
    }
    
    private fun logPlayerStates(players: JSONArray, myIndex: Int) {
        println("--- Player States ---")
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val name = player.optString("name", "Player $i")
            val stack = player.getInt("stack")
            val bet = player.getInt("bet")
            val status = player.optString("status", "active")
            val indicator = if (i == myIndex) " (ME)" else ""
            
            println("Player $i: $name$indicator - Stack: $stack, Bet: $bet, Status: $status")
        }
        println("--- End Player States ---")
    }

    fun version(): String {
        val mode = StrategyConfig.mode().name
        return "Real Donkey Killer v2.1 - Dynamic World Champion ($mode)"
    }
    
    /**
     * Record opponent actions for learning and adaptation
     */
    private fun recordOpponentActions(
        players: JSONArray,
        currentBuyIn: Int,
        pot: Int,
        position: PositionAnalyzer.Position,
        community: JSONArray
    ) {
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val playerId = player.optString("name", "Player_$i")
            val playerBet = player.getInt("bet")
            val playerStack = player.getInt("stack")
            
            // Determine action based on bet amount
            val action = when {
                playerBet == 0 -> "fold"
                playerBet == currentBuyIn -> "call"
                playerBet > currentBuyIn -> "raise"
                else -> "unknown"
            }
            
            // Record the action for opponent modeling
            if (action != "unknown") {
                opponentModeling.recordAction(
                    playerId = playerId,
                    action = action,
                    amount = playerBet,
                    potSize = pot,
                    position = position,
                    street = when (community.length()) {
                        0 -> "preflop"
                        3 -> "flop"
                        4 -> "turn"
                        5 -> "river"
                        else -> "unknown"
                    },
                    stackSize = playerStack
                )
            }
        }
    }
    
    /**
     * Find the last player to raise (aggressor)
     */
    private fun findLastAggressor(players: JSONArray, currentBuyIn: Int): String? {
        var lastAggressor: String? = null
        var highestBet = 0
        
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val playerBet = player.getInt("bet")
            
            if (playerBet > highestBet && playerBet == currentBuyIn) {
                lastAggressor = player.optString("name", "Player_$i")
                highestBet = playerBet
            }
        }
        
        return lastAggressor
    }
    
    /**
     * Count active players (those with chips)
     */
    private fun countActivePlayers(players: JSONArray): Int {
        var count = 0
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            if (player.getInt("stack") > 0) count++
        }
        return count
    }
    
    /**
     * Update our action tracking for strategy analysis
     */
    private fun updateActionTracking(betAmount: Int, currentBuyIn: Int, myBet: Int) {
        val callAmount = currentBuyIn - myBet
        
        lastAction = when {
            betAmount == 0 -> "fold"
            betAmount == callAmount -> "call"
            betAmount > callAmount -> "raise"
            else -> "check"
        }
        
        lastBetAmount = betAmount
        
        // Track if we were the pre-flop aggressor
        if (currentBuyIn == 0 && betAmount > 0) {
            wasPreFlopAggressor = true
        }
    }
}
