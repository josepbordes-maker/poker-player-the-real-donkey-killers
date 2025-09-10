package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import kotlin.math.min
import kotlin.math.max

/**
 * Enhanced betting strategy with opponent modeling and exploitative adjustments
 * World championship-level poker strategy implementation
 */
class EnhancedBettingStrategy(
    private val handEvaluator: HandEvaluator,
    private val positionAnalyzer: PositionAnalyzer,
    private val opponentModeling: OpponentModeling
) {
    private val random = Random
    
    /**
     * Main betting calculation with exploitative adjustments
     */
    fun calculateBet(
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        myBet: Int,
        currentBuyIn: Int,
        pot: Int,
        smallBlind: Int,
        minimumRaise: Int,
        position: PositionAnalyzer.Position,
        players: JSONArray,
        inAction: Int,
        aggressorId: String? = null
    ): Int {
        // Basic safety checks
        val callAmount = currentBuyIn - myBet
        if (callAmount >= myStack) return myStack
        
        // Get stack strategy
        val stackStrategy = opponentModeling.getStackSizeStrategy(myStack, smallBlind)
        
        // If no bet to call, calculate opening strategy
        if (callAmount <= 0) {
            return calculateOpeningStrategy(
                myCards, communityCards, myStack, smallBlind, position, 
                players, stackStrategy
            )
        }
        
        // Facing a bet - get exploitative adjustments
        val adjustment = if (aggressorId != null) {
            opponentModeling.getStrategicAdjustments(aggressorId)
        } else {
            OpponentModeling.StrategicAdjustment(1.0, 1.0, 1.0, 1.0, 1.0)
        }
        
        return calculateDefensiveStrategy(
            myCards, communityCards, myStack, callAmount, pot, smallBlind,
            minimumRaise, position, adjustment, stackStrategy
        )
    }
    
    /**
     * Opening strategy with position and stack considerations
     */
    private fun calculateOpeningStrategy(
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        smallBlind: Int,
        position: PositionAnalyzer.Position,
        players: JSONArray,
        stackStrategy: OpponentModeling.StackStrategy
    ): Int {
        val handStrength = getHandStrength(myCards, communityCards)
        
        // Short stack strategy - push/fold approach
        if (stackStrategy == OpponentModeling.StackStrategy.SHORT_STACK) {
            return when {
                handStrength >= HandStrength.STRONG -> myStack // Shove strong hands
                handStrength >= HandStrength.DECENT && position in listOf(
                    PositionAnalyzer.Position.LATE, PositionAnalyzer.Position.BLINDS
                ) -> myStack // Shove decent hands in late position
                else -> 0
            }
        }
        
        // Handle single-player edge case (for testing)
        val activePlayerCount = countActivePlayers(players)
        if (activePlayerCount <= 1) {
            // Single player scenario - use simplified logic for testing
            return when {
                handStrength >= HandStrength.STRONG -> smallBlind * 6
                handStrength >= HandStrength.DECENT -> smallBlind * 4
                else -> 0
            }
        }
        
        // Standard opening sizing based on position and hand strength
        val baseSize = when (position) {
            PositionAnalyzer.Position.EARLY -> when {
                handStrength >= HandStrength.STRONG -> smallBlind * 6
                else -> 0 // Very tight from early position
            }
            PositionAnalyzer.Position.MIDDLE -> when {
                handStrength >= HandStrength.STRONG -> smallBlind * 6
                handStrength >= HandStrength.DECENT -> smallBlind * 4
                else -> 0
            }
            PositionAnalyzer.Position.LATE -> when {
                handStrength >= HandStrength.STRONG -> smallBlind * 6
                handStrength >= HandStrength.DECENT -> smallBlind * 4
                handStrength >= HandStrength.WEAK_PLAYABLE && isStealSpot(players) -> smallBlind * 3
                else -> 0
            }
            PositionAnalyzer.Position.BLINDS -> when {
                handStrength >= HandStrength.STRONG -> smallBlind * 6
                // Be more conservative from blinds - only open strong hands
                else -> 0
            }
        }
        
        return min(myStack, baseSize)
    }
    
    /**
     * Defensive strategy when facing bets
     */
    private fun calculateDefensiveStrategy(
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        callAmount: Int,
        pot: Int,
        smallBlind: Int,
        minimumRaise: Int,
        position: PositionAnalyzer.Position,
        adjustment: OpponentModeling.StrategicAdjustment,
        stackStrategy: OpponentModeling.StackStrategy
    ): Int {
        val handStrength = getHandStrength(myCards, communityCards)
        val potOdds = pot.toDouble() / (pot + callAmount)
        val adjustedCallThreshold = adjustment.callThreshold
        
        // Enhanced hand evaluation with community cards
        val postFlopStrength = if (communityCards.length() >= 3) {
            getPostFlopStrength(myCards, communityCards)
        } else handStrength
        
        return when (postFlopStrength) {
            HandStrength.PREMIUM -> {
                // Premium hands - always raise for value (call + 2 * minimum raise)
                val raiseSize = (callAmount + minimumRaise * 2 * adjustment.valueBetSizing).toInt()
                min(myStack, raiseSize)
            }
            
            HandStrength.STRONG -> {
                // Strong hands - always raise for value (call + 2 * minimum raise)
                val raiseSize = (callAmount + minimumRaise * 2 * adjustment.valueBetSizing).toInt()
                min(myStack, raiseSize)
            }
            
            HandStrength.DECENT -> {
                // Decent hands - position and pot odds dependent
                val threshold = positionAnalyzer.getSmallBetThreshold(position, pot) * adjustedCallThreshold
                if (callAmount <= threshold) callAmount else 0
            }
            
            HandStrength.WEAK_PLAYABLE -> {
                // Weak playable - only call very small bets
                if (callAmount <= smallBlind * 2 * adjustedCallThreshold) callAmount else 0
            }
            
            HandStrength.MARGINAL -> {
                // Marginal hands - bluff potential or fold
                val isBluffSpot = isGoodBluffSpot(position, pot, callAmount, adjustment)
                if (isBluffSpot && StrategyConfig.bluffRaiseEnabled) {
                    calculateBluffRaise(callAmount, minimumRaise, myStack, adjustment)
                } else if (callAmount <= pot / 4 * adjustedCallThreshold && random.nextFloat() < 0.3) {
                    callAmount
                } else {
                    0
                }
            }
            
            HandStrength.TRASH -> 0 // Always fold trash
        }
    }
    
    /**
     * Enhanced hand strength classification
     */
    private fun getHandStrength(myCards: JSONArray, communityCards: JSONArray): HandStrength {
        return when {
            handEvaluator.hasStrongHandWithCommunity(myCards, communityCards) -> {
                // Further classify strong hands
                if (isPremiumHand(myCards)) HandStrength.PREMIUM else HandStrength.STRONG
            }
            // If not strong but is premium (like AK), still classify as strong  
            isPremiumHand(myCards) -> HandStrength.STRONG
            handEvaluator.hasDecentHand(myCards) -> HandStrength.DECENT
            handEvaluator.hasWeakButPlayableHand(myCards) -> HandStrength.WEAK_PLAYABLE
            handEvaluator.hasMarginalHand(myCards) -> HandStrength.MARGINAL
            else -> HandStrength.TRASH
        }
    }
    
    /**
     * Post-flop hand strength with board texture analysis
     */
    private fun getPostFlopStrength(myCards: JSONArray, communityCards: JSONArray): HandStrength {
        val handResult = handEvaluator.evaluateBestHand(myCards, communityCards)
        val boardTexture = analyzeBoardTexture(communityCards)
        
        return when (handResult.rank) {
            HandEvaluator.HandRank.ROYAL_FLUSH, 
            HandEvaluator.HandRank.STRAIGHT_FLUSH,
            HandEvaluator.HandRank.FOUR_OF_A_KIND -> HandStrength.PREMIUM
            
            HandEvaluator.HandRank.FULL_HOUSE,
            HandEvaluator.HandRank.FLUSH -> HandStrength.PREMIUM
            
            HandEvaluator.HandRank.STRAIGHT -> {
                if (boardTexture.flushPossible || boardTexture.pairOnBoard) {
                    HandStrength.STRONG
                } else {
                    HandStrength.PREMIUM
                }
            }
            
            HandEvaluator.HandRank.THREE_OF_A_KIND -> HandStrength.STRONG
            
            HandEvaluator.HandRank.TWO_PAIR -> {
                if (handResult.value >= 10) HandStrength.STRONG else HandStrength.DECENT
            }
            
            HandEvaluator.HandRank.ONE_PAIR -> {
                when {
                    handResult.value >= 13 -> HandStrength.STRONG // Top pair, ace or king
                    handResult.value >= 10 -> HandStrength.DECENT // Top pair, jack or queen
                    boardTexture.coordinated -> HandStrength.WEAK_PLAYABLE // Weak pair on coordinated board
                    else -> HandStrength.DECENT
                }
            }
            
            HandEvaluator.HandRank.HIGH_CARD -> {
                if (hasDrawingPotential(myCards, communityCards)) {
                    HandStrength.WEAK_PLAYABLE
                } else {
                    HandStrength.MARGINAL
                }
            }
        }
    }
    
    /**
     * Analyze board texture for strategic decisions
     */
    private fun analyzeBoardTexture(communityCards: JSONArray): BoardTexture {
        if (communityCards.length() < 3) {
            return BoardTexture(false, false, false, false, false)
        }
        
        val ranks = mutableListOf<Int>()
        val suits = mutableListOf<String>()
        
        for (i in 0 until communityCards.length()) {
            val card = communityCards.getJSONObject(i)
            ranks.add(CardUtils.getRankValue(card.getString("rank")))
            suits.add(card.getString("suit"))
        }
        
        val suitCounts = suits.groupingBy { it }.eachCount()
        val rankCounts = ranks.groupingBy { it }.eachCount()
        
        return BoardTexture(
            flushPossible = suitCounts.values.any { it >= 3 },
            straightPossible = checkStraightDraw(ranks),
            pairOnBoard = rankCounts.values.any { it >= 2 },
            coordinated = isCoordinatedBoard(ranks),
            wet = isWetBoard(ranks, suits)
        )
    }
    
    data class BoardTexture(
        val flushPossible: Boolean,
        val straightPossible: Boolean,
        val pairOnBoard: Boolean,
        val coordinated: Boolean,
        val wet: Boolean
    )
    
    enum class HandStrength {
        TRASH,        // Unplayable
        MARGINAL,     // Bluff candidates
        WEAK_PLAYABLE, // Marginal but playable
        DECENT,       // Playable hands
        STRONG,       // Very strong hands
        PREMIUM       // Nuts or near-nuts
    }
    
    private fun isPremiumHand(myCards: JSONArray): Boolean {
        if (myCards.length() != 2) return false
        
        val card1 = myCards.getJSONObject(0)
        val card2 = myCards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        
        // Premium starting hands
        return when {
            rank1 == rank2 && CardUtils.getRankValue(rank1) >= 13 -> true // AA, KK
            (rank1 == "A" && rank2 == "K") || (rank1 == "K" && rank2 == "A") -> true // AK
            else -> false
        }
    }
    
    private fun isStealSpot(players: JSONArray): Boolean {
        // Check if this is a good steal spot (few players behind, likely to fold)
        return countActivePlayers(players) <= 4
    }
    
    private fun countActivePlayers(players: JSONArray): Int {
        var count = 0
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            if (player.getInt("stack") > 0) count++
        }
        return count
    }
    
    private fun isGoodBluffSpot(
        position: PositionAnalyzer.Position,
        pot: Int,
        callAmount: Int,
        adjustment: OpponentModeling.StrategicAdjustment
    ): Boolean {
        val riskMoodActive = random.nextFloat() < StrategyConfig.riskMoodProbability * adjustment.bluffFrequency
        return riskMoodActive && 
               position in listOf(PositionAnalyzer.Position.LATE, PositionAnalyzer.Position.MIDDLE) &&
               callAmount <= pot / 3
    }
    
    private fun calculateBluffRaise(
        callAmount: Int,
        minimumRaise: Int,
        myStack: Int,
        adjustment: OpponentModeling.StrategicAdjustment
    ): Int {
        val bluffSize = (callAmount + minimumRaise * adjustment.bluffFrequency).toInt()
        return min(myStack, min(bluffSize, myStack / 4)) // Never risk more than 25% of stack on bluff
    }
    
    private fun hasDrawingPotential(myCards: JSONArray, communityCards: JSONArray): Boolean {
        if (myCards.length() != 2 || communityCards.length() < 3) return false
        
        // Check for flush draws, straight draws, overcards, etc.
        val allCards = mutableListOf<JSONObject>()
        for (i in 0 until myCards.length()) allCards.add(myCards.getJSONObject(i))
        for (i in 0 until communityCards.length()) allCards.add(communityCards.getJSONObject(i))
        
        val suits = allCards.map { it.getString("suit") }
        val ranks = allCards.map { CardUtils.getRankValue(it.getString("rank")) }
        
        // Flush draw potential
        val mySuits = listOf(myCards.getJSONObject(0).getString("suit"), myCards.getJSONObject(1).getString("suit"))
        val flushDrawPotential = mySuits.any { suit -> suits.count { it == suit } >= 3 }
        
        // Straight draw potential
        val straightDrawPotential = checkStraightDraw(ranks)
        
        // Overcard potential
        val myHighCards = listOf(
            CardUtils.getRankValue(myCards.getJSONObject(0).getString("rank")),
            CardUtils.getRankValue(myCards.getJSONObject(1).getString("rank"))
        )
        val boardHighCard = communityCards.let { board ->
            (0 until board.length()).map { CardUtils.getRankValue(board.getJSONObject(it).getString("rank")) }.maxOrNull() ?: 0
        }
        val overcardPotential = myHighCards.any { it > boardHighCard }
        
        return flushDrawPotential || straightDrawPotential || overcardPotential
    }
    
    private fun checkStraightDraw(ranks: List<Int>): Boolean {
        val sortedRanks = ranks.distinct().sorted()
        if (sortedRanks.size < 3) return false
        
        // Check for open-ended straight draws
        for (i in 0..sortedRanks.size - 3) {
            if (sortedRanks[i + 2] - sortedRanks[i] <= 4) return true
        }
        
        // Check for wheel draw (A-2-3-4-5)
        if (sortedRanks.contains(14) && sortedRanks.contains(2) && sortedRanks.contains(3)) return true
        
        return false
    }
    
    private fun isCoordinatedBoard(ranks: List<Int>): Boolean {
        val sortedRanks = ranks.sorted()
        // Check if board has connecting cards
        for (i in 0 until sortedRanks.size - 1) {
            if (sortedRanks[i + 1] - sortedRanks[i] <= 2) return true
        }
        return false
    }
    
    private fun isWetBoard(ranks: List<Int>, suits: List<String>): Boolean {
        return isCoordinatedBoard(ranks) || suits.groupingBy { it }.eachCount().values.any { it >= 2 }
    }
    
    /**
     * Record opponent actions for modeling
     */
    fun recordOpponentAction(
        playerId: String,
        action: String,
        amount: Int,
        potSize: Int,
        position: PositionAnalyzer.Position,
        communityCards: JSONArray
    ) {
        val street = when (communityCards.length()) {
            0 -> "preflop"
            3 -> "flop"
            4 -> "turn"
            5 -> "river"
            else -> "unknown"
        }
        
        opponentModeling.recordAction(playerId, action, amount, potSize, position, street, 0)
    }
}
