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
    private var lastGameId: String = ""
    private var prevCurrentBuyIn: Int = 0

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
        
        // Create a unique identifier for this decision for log tracking  
        val shortGameId = gameId.takeLast(6)
        val community = game_state.optJSONArray("community_cards") ?: JSONArray()
        val street = when (community.length()) {
            0 -> "PRE"
            3 -> "FLP"
            4 -> "TRN"
            5 -> "RIV"
            else -> "UNK"
        }
        val decisionId = "[$shortGameId/$street/P$inAction]"
        
        println("\n=== BETTING DECISION ANALYSIS $decisionId ===")
        println("$decisionId Game ID: $gameId, Round: $round")
        println("$decisionId My Position: Player $inAction (dealer: $dealer)")
        println("$decisionId My Stack: $myStack, My Current Bet: $myBet")
        println("$decisionId Current Buy-in: $currentBuyIn, Pot: $pot")
        println("$decisionId Small Blind: $smallBlind, Min Raise: $minimumRaise")   
        
        // Format and log my cards
        val myCardsStr = formatCards(myCards)
        println("$decisionId My Cards: $myCardsStr")
        
        // Remember seen cards (hole + community) for this game
        gameStateManager.addSeenCards(gameId, myCards)
        val communityStr = if (community.length() > 0) {
            gameStateManager.addSeenCards(gameId, community)
            formatCards(community)
        } else {
            "None (Pre-flop)"
        }
        println("$decisionId Community Cards: $communityStr")

        // Get position for betting decision
        val position = positionAnalyzer.getPosition(players, inAction, dealer)
        println("$decisionId My Position: ${position.name}")
        
        // Log active players and their stacks
        logPlayerStates(players, inAction, decisionId)
        
        // Calculate call amount
        val callAmount = currentBuyIn - myBet
        println("$decisionId Call Amount: $callAmount")
        
        // Record opponent actions for modeling
        println("$decisionId Recording opponent actions for modeling...")
        recordOpponentActions(players, currentBuyIn, pot, position, community, decisionId)
        
        // Determine if this is post-flop play
        val isPostFlop = community.length() >= 3
        val streetFull = when (community.length()) {
            0 -> "PRE-FLOP"
            3 -> "FLOP"
            4 -> "TURN"
            5 -> "RIVER"
            else -> "UNKNOWN"
        }
        println("$decisionId Street: $streetFull (isPostFlop: $isPostFlop)")
        
        // Find who was the aggressor (last to raise)
        val aggressorId = findLastAggressor(players, currentBuyIn, prevCurrentBuyIn)
        val opponentCount = countActivePlayers(players) - 1
        println("$decisionId Aggressor: ${aggressorId ?: "None"}, Opponent Count: $opponentCount")
        println("$decisionId Was Pre-flop Aggressor: $wasPreFlopAggressor")
        
        println("$decisionId Using DYNAMIC WORLD CHAMPION strategy...")
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
        updateActionTracking(betAmount, currentBuyIn, myBet, gameId, community)
        
        // Log final decision
        val actionDescription = when {
            callAmount == 0 && betAmount == 0 -> "CHECK"
            betAmount == 0 -> "FOLD"
            betAmount == callAmount -> "CALL ($callAmount)"
            betAmount > callAmount -> "RAISE to $betAmount (raise by ${betAmount - callAmount})"
            betAmount < callAmount && callAmount > 0 -> "ALL-IN $betAmount (short of call)"
            else -> "BET $betAmount"
        }
        if (!StrategyConfig.quietLogs) println("$decisionId FINAL DECISION: $actionDescription")
        if (StrategyConfig.structuredLogs) {
            val handClass = when {
                handEvaluator.hasStrongHandWithCommunity(myCards, community) -> "STRONG"
                handEvaluator.hasDecentHand(myCards) -> "DECENT"
                handEvaluator.hasWeakButPlayableHand(myCards) -> "WEAK"
                handEvaluator.hasMarginalHand(myCards) -> "MARGINAL"
                else -> "TRASH"
            }
            val evalRank = if (community.length() >= 3) {
                handEvaluator.evaluateBestHand(myCards, community).rank.name
            } else {
                "NA"
            }
            val json = org.json.JSONObject()
                .put("t", "dec")
                .put("game", gameId)
                .put("rnd", round)
                .put("st", street)
                .put("pos", position.name)
                .put("opp", countActivePlayers(players) - 1)
                .put("mode", StrategyConfig.mode().name)
                .put("stack", myStack)
                .put("pot", pot)
                .put("call", callAmount)
                .put("minR", minimumRaise)
                .put("hand", handClass)
                .put("eval", evalRank)
                .put("cfg", org.json.JSONObject()
                    .put("risk", StrategyConfig.riskMoodProbability)
                    .put("sbm", StrategyConfig.smallBetThresholdMultiplier)
                    .put("ps", StrategyConfig.postflopSmall)
                    .put("pm", StrategyConfig.postflopMed)
                    .put("pb", StrategyConfig.postflopBig)
                    .put("al", StrategyConfig.enableAntiLimp)
                    .put("sq", StrategyConfig.enableSqueeze)
                )
                .put("act", when {
                    callAmount == 0 && betAmount == 0 -> "CHECK"
                    betAmount == 0 -> "FOLD"
                    betAmount == callAmount -> "CALL"
                    betAmount > callAmount && callAmount > 0 -> "RAISE"
                    callAmount == 0 && betAmount > 0 -> "BET"
                    else -> "UNK"
                })
                .put("amt", betAmount)
            Logger.logStructured(json)
        }
        if (!StrategyConfig.quietLogs) println("$decisionId Action Tracking Updated: lastAction=$lastAction, lastBetAmount=$lastBetAmount")
        if (!StrategyConfig.quietLogs) println("=== END BETTING ANALYSIS $decisionId ===\n")
        
        return betAmount
    }
    fun showdown(game_state: JSONObject) {
        if (!StrategyConfig.quietLogs) println("\n=== SHOWDOWN CALLED ===")
        // Structured showdown log
        if (StrategyConfig.structuredLogs) {
            val gameId = game_state.optString("game_id", "")
            val json = org.json.JSONObject()
                .put("t", "sd")
                .put("game", gameId)
                .put("info", "showdown")
            Logger.logStructured(json)
        }
        gameStateManager.processShowdown(game_state)
        if (!StrategyConfig.quietLogs) println("=== SHOWDOWN COMPLETE ===\n")
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
    
    private fun logPlayerStates(players: JSONArray, myIndex: Int, decisionId: String) {
        println("$decisionId --- Player States ---")
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val name = player.optString("name", "Player $i")
            val stack = player.getInt("stack")
            val bet = player.getInt("bet")
            val status = player.optString("status", "active")
            val indicator = if (i == myIndex) " (ME)" else ""
            
            println("$decisionId Player $i: $name$indicator - Stack: $stack, Bet: $bet, Status: $status")
        }
        println("$decisionId --- End Player States ---")
    }

    fun version(): String {
        val mode = StrategyConfig.mode().name
        return "Real Donkey Killer v3.2 - Optimized Quick Wins ($mode)"
    }
    
    /**
     * Record opponent actions for learning and adaptation
     */
    private fun recordOpponentActions(
        players: JSONArray,
        currentBuyIn: Int,
        pot: Int,
        position: PositionAnalyzer.Position,
        community: JSONArray,
        decisionId: String
    ) {
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val playerId = player.optString("name", "Player_$i")
            val playerBet = player.getInt("bet")
            val playerStack = player.getInt("stack")
            
            // Determine action based on bet amount
            val action = when {
                playerBet == 0 && currentBuyIn == 0 -> "check"
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
    private fun findLastAggressor(players: JSONArray, currentBuyIn: Int, prevCurrentBuyIn: Int): String? {
        var lastAggressor: String? = null
        
        // If current buy-in increased from previous, someone raised
        if (currentBuyIn > prevCurrentBuyIn) {
            for (i in 0 until players.length()) {
                val player = players.getJSONObject(i)
                val playerBet = player.getInt("bet")
                
                // The player with bet equal to current buy-in is the aggressor
                if (playerBet == currentBuyIn && currentBuyIn > 0) {
                    lastAggressor = player.optString("name", "Player_$i")
                    break
                }
            }
        }
        
        println("Aggressor Detection: prevBuyIn=$prevCurrentBuyIn, currentBuyIn=$currentBuyIn, aggressor=$lastAggressor")
        return lastAggressor
    }
    
    /**
     * Count active players (those still in the hand)
     */
    private fun countActivePlayers(players: JSONArray): Int {
        var count = 0
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val status = player.optString("status", "active")
            // Count players who are still active in the hand
            if (status == "active" || (status.isEmpty() && player.getInt("stack") > 0)) {
                count++
            }
        }
        println("Active Player Count: $count (status-based counting)")
        return count
    }
    
    /**
     * Update our action tracking for strategy analysis
     */
    private fun updateActionTracking(betAmount: Int, currentBuyIn: Int, myBet: Int, gameId: String, community: JSONArray) {
        val callAmount = currentBuyIn - myBet
        
        // Reset aggressor tracking for new games or returning to preflop
        if (gameId != lastGameId || (community.length() == 0 && lastGameId.isNotEmpty())) {
            wasPreFlopAggressor = false
            lastGameId = gameId
            prevCurrentBuyIn = 0
            println("Action Tracking: Reset for new game/preflop (gameId: $gameId)")
        }
        
        lastAction = when {
            betAmount == 0 -> "fold"
            betAmount == callAmount -> "call"
            betAmount > callAmount -> "raise"
            else -> "check"
        }
        
        lastBetAmount = betAmount
        
        // Track if we were the pre-flop aggressor
        val isPreflop = community.length() == 0
        if (isPreflop && betAmount > callAmount) {
            wasPreFlopAggressor = true
            println("Action Tracking: Set wasPreFlopAggressor = true (betAmount=$betAmount > callAmount=$callAmount)")
        }
        
        // Update previous buy-in for aggressor detection
        prevCurrentBuyIn = currentBuyIn
    }
}
