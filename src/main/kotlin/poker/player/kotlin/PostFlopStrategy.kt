package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import kotlin.math.min
import kotlin.math.max

/**
 * Advanced post-flop strategy with board texture analysis and continuation betting
 * Professional-level implementation for flop, turn, and river play
 */
class PostFlopStrategy(
    private val handEvaluator: HandEvaluator,
    private val positionAnalyzer: PositionAnalyzer,
    private val opponentModeling: OpponentModeling
) {
    private val random = Random
    
    /**
     * Calculate post-flop betting decision
     */
    fun calculatePostFlopBet(
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        myBet: Int,
        currentBuyIn: Int,
        pot: Int,
        smallBlind: Int,
        minimumRaise: Int,
        position: PositionAnalyzer.Position,
        wasPreFlopAggressor: Boolean,
        opponentCount: Int,
        aggressorId: String? = null
    ): Int {
        val callAmount = currentBuyIn - myBet
        if (callAmount >= myStack) return myStack
        
        val boardTexture = analyzeBoardTexture(communityCards)
        val handStrength = evaluateHandStrength(myCards, communityCards, boardTexture)
        val street = getStreet(communityCards)
        
        // If no bet to call, determine if we should bet/check
        if (callAmount <= 0) {
            return if (wasPreFlopAggressor) {
                calculateContinuationBet(
                    handStrength, boardTexture, pot, position, opponentCount, myStack, smallBlind
                )
            } else {
                calculateDonkBet(handStrength, boardTexture, pot, position, myStack, smallBlind)
            }
        }
        
        // Facing a bet - calculate defensive strategy
        val adjustment = aggressorId?.let { 
            opponentModeling.getStrategicAdjustments(it) 
        } ?: OpponentModeling.StrategicAdjustment(1.0, 1.0, 1.0, 1.0, 1.0)
        
        return calculatePostFlopDefense(
            handStrength, boardTexture, callAmount, pot, minimumRaise, 
            position, myStack, adjustment, street
        )
    }
    
    /**
     * Comprehensive board texture analysis
     */
    private fun analyzeBoardTexture(communityCards: JSONArray): BoardTexture {
        if (communityCards.length() < 3) {
            return BoardTexture()
        }
        
        val ranks = mutableListOf<Int>()
        val suits = mutableListOf<String>()
        
        for (i in 0 until communityCards.length()) {
            val card = communityCards.getJSONObject(i)
            ranks.add(CardUtils.getRankValue(card.getString("rank")))
            suits.add(card.getString("suit"))
        }
        
        val sortedRanks = ranks.sorted()
        val suitCounts = suits.groupingBy { it }.eachCount()
        val rankCounts = ranks.groupingBy { it }.eachCount()
        
        return BoardTexture(
            // Basic texture
            flushDraw = suitCounts.values.any { it >= 3 },
            flushPossible = suitCounts.values.any { it >= 3 },
            straightDraw = hasOpenEndedStraightDraw(sortedRanks),
            straightPossible = hasStraightPossibility(sortedRanks),
            pairOnBoard = rankCounts.values.any { it >= 2 },
            
            // Advanced analysis
            highCard = sortedRanks.maxOrNull() ?: 0,
            lowCard = sortedRanks.minOrNull() ?: 0,
            connected = isConnectedBoard(sortedRanks),
            rainbow = suitCounts.size == communityCards.length(),
            monotone = suitCounts.values.any { it == communityCards.length() },
            
            // Dynamic properties
            dynamic = isDynamicBoard(sortedRanks, suitCounts),
            wetness = calculateWetness(sortedRanks, suitCounts),
            bluffFriendly = isBluffFriendly(sortedRanks, suitCounts)
        )
    }
    
    data class BoardTexture(
        val flushDraw: Boolean = false,
        val flushPossible: Boolean = false,
        val straightDraw: Boolean = false,
        val straightPossible: Boolean = false,
        val pairOnBoard: Boolean = false,
        val highCard: Int = 0,
        val lowCard: Int = 0,
        val connected: Boolean = false,
        val rainbow: Boolean = false,
        val monotone: Boolean = false,
        val dynamic: Boolean = false,
        val wetness: Double = 0.0, // 0.0 = dry, 1.0 = very wet
        val bluffFriendly: Boolean = false
    )
    
    /**
     * Evaluate hand strength considering board texture
     */
    private fun evaluateHandStrength(
        myCards: JSONArray,
        communityCards: JSONArray,
        boardTexture: BoardTexture
    ): PostFlopHandStrength {
        val handResult = handEvaluator.evaluateBestHand(myCards, communityCards)
        val drawStrength = evaluateDrawStrength(myCards, communityCards, boardTexture)
        
        val madeHandStrength = when (handResult.rank) {
            HandEvaluator.HandRank.ROYAL_FLUSH -> MadeHandStrength.NUTS
            HandEvaluator.HandRank.STRAIGHT_FLUSH -> MadeHandStrength.NUTS
            HandEvaluator.HandRank.FOUR_OF_A_KIND -> MadeHandStrength.VERY_STRONG
            HandEvaluator.HandRank.FULL_HOUSE -> MadeHandStrength.VERY_STRONG
            HandEvaluator.HandRank.FLUSH -> {
                if (handResult.value >= 13) MadeHandStrength.VERY_STRONG 
                else MadeHandStrength.STRONG
            }
            HandEvaluator.HandRank.STRAIGHT -> {
                if (boardTexture.flushPossible) MadeHandStrength.STRONG
                else MadeHandStrength.VERY_STRONG
            }
            HandEvaluator.HandRank.THREE_OF_A_KIND -> MadeHandStrength.STRONG
            HandEvaluator.HandRank.TWO_PAIR -> {
                if (handResult.value >= 11) MadeHandStrength.STRONG
                else MadeHandStrength.MEDIUM
            }
            HandEvaluator.HandRank.ONE_PAIR -> {
                when {
                    handResult.value >= 13 && !boardTexture.pairOnBoard -> MadeHandStrength.STRONG // Top pair, good kicker
                    handResult.value >= 10 -> MadeHandStrength.MEDIUM // Top/middle pair
                    boardTexture.dynamic -> MadeHandStrength.WEAK // Weak pair on dangerous board
                    else -> MadeHandStrength.MEDIUM
                }
            }
            HandEvaluator.HandRank.HIGH_CARD -> {
                if (hasOvercards(myCards, communityCards)) MadeHandStrength.WEAK
                else MadeHandStrength.VERY_WEAK
            }
        }
        
        return PostFlopHandStrength(madeHandStrength, drawStrength)
    }
    
    data class PostFlopHandStrength(
        val madeHand: MadeHandStrength,
        val draws: DrawStrength
    )
    
    enum class MadeHandStrength {
        NUTS, VERY_STRONG, STRONG, MEDIUM, WEAK, VERY_WEAK
    }
    
    data class DrawStrength(
        val nutFlushDraw: Boolean = false,
        val flushDraw: Boolean = false,
        val openEndedStraightDraw: Boolean = false,
        val gutShotStraightDraw: Boolean = false,
        val overcards: Int = 0,
        val backdoorFlush: Boolean = false,
        val backdoorStraight: Boolean = false,
        val outs: Int = 0
    ) {
        fun hasStrongDraw(): Boolean = nutFlushDraw || openEndedStraightDraw || 
                                      (flushDraw && overcards >= 1) || outs >= 12
        
        fun hasDecentDraw(): Boolean = flushDraw || openEndedStraightDraw || 
                                      gutShotStraightDraw || overcards >= 2 || outs >= 8
    }
    
    /**
     * Calculate continuation betting strategy
     */
    private fun calculateContinuationBet(
        handStrength: PostFlopHandStrength,
        boardTexture: BoardTexture,
        pot: Int,
        position: PositionAnalyzer.Position,
        opponentCount: Int,
        myStack: Int,
        smallBlind: Int
    ): Int {
        val cBetFrequency = calculateCBetFrequency(handStrength, boardTexture, position, opponentCount)
        
        if (random.nextFloat() > cBetFrequency) return 0 // Check
        
        // Calculate bet sizing based on hand strength and board texture
        val betSizing = when (handStrength.madeHand) {
            MadeHandStrength.NUTS, MadeHandStrength.VERY_STRONG -> {
                if (boardTexture.dynamic) 0.8 // Bet big on dangerous boards
                else 0.6 // Value bet on safe boards
            }
            MadeHandStrength.STRONG -> {
                if (boardTexture.wetness > 0.7) 0.75 // Protect against draws
                else 0.65 // Standard value bet
            }
            MadeHandStrength.MEDIUM -> {
                if (boardTexture.bluffFriendly) 0.5 // Thin value/protection
                else 0.4 // Small bet for value/info
            }
            MadeHandStrength.WEAK, MadeHandStrength.VERY_WEAK -> {
                if (handStrength.draws.hasStrongDraw()) 0.6 // Semi-bluff
                else if (boardTexture.bluffFriendly && opponentCount <= 2) 0.7 // Pure bluff
                else 0.0 // Check
            }
        }
        
        if (betSizing == 0.0) return 0
        
        val betAmount = (pot * betSizing).toInt()
        return min(myStack, max(smallBlind * 2, betAmount))
    }
    
    /**
     * Calculate c-bet frequency based on multiple factors
     */
    private fun calculateCBetFrequency(
        handStrength: PostFlopHandStrength,
        boardTexture: BoardTexture,
        position: PositionAnalyzer.Position,
        opponentCount: Int
    ): Float {
        var frequency = when (handStrength.madeHand) {
            MadeHandStrength.NUTS, MadeHandStrength.VERY_STRONG -> 0.95f
            MadeHandStrength.STRONG -> 0.85f
            MadeHandStrength.MEDIUM -> 0.65f
            MadeHandStrength.WEAK -> 0.35f
            MadeHandStrength.VERY_WEAK -> 0.25f
        }
        
        // Adjust for draws
        if (handStrength.draws.hasStrongDraw()) frequency += 0.3f
        else if (handStrength.draws.hasDecentDraw()) frequency += 0.15f
        
        // Adjust for board texture
        if (boardTexture.bluffFriendly) frequency += 0.2f
        if (boardTexture.dynamic) frequency += 0.1f // More c-betting on dangerous boards
        if (boardTexture.wetness > 0.8) frequency -= 0.15f // Less bluffing on very wet boards
        
        // Adjust for position
        when (position) {
            PositionAnalyzer.Position.LATE -> frequency += 0.1f
            PositionAnalyzer.Position.EARLY -> frequency -= 0.1f
            else -> {} // No adjustment
        }
        
        // Adjust for opponent count
        frequency -= (opponentCount - 1) * 0.15f // Reduce with more opponents
        
        return frequency.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Calculate post-flop defense against bets
     */
    private fun calculatePostFlopDefense(
        handStrength: PostFlopHandStrength,
        boardTexture: BoardTexture,
        callAmount: Int,
        pot: Int,
        minimumRaise: Int,
        position: PositionAnalyzer.Position,
        myStack: Int,
        adjustment: OpponentModeling.StrategicAdjustment,
        street: String
    ): Int {
        val potOdds = callAmount.toDouble() / (pot + callAmount)
        val requiredEquity = potOdds
        
        return when (handStrength.madeHand) {
            MadeHandStrength.NUTS -> {
                // Always raise nuts
                val raiseSize = (callAmount + minimumRaise * 2.5 * adjustment.valueBetSizing).toInt()
                min(myStack, raiseSize)
            }
            
            MadeHandStrength.VERY_STRONG -> {
                // Raise for value and protection
                val raiseSize = (callAmount + minimumRaise * 2 * adjustment.valueBetSizing).toInt()
                min(myStack, raiseSize)
            }
            
            MadeHandStrength.STRONG -> {
                if (boardTexture.dynamic && street == "turn") {
                    // Raise to protect against draws
                    val raiseSize = (callAmount + minimumRaise * 1.5).toInt()
                    min(myStack, raiseSize)
                } else {
                    callAmount // Call with strong hands
                }
            }
            
            MadeHandStrength.MEDIUM -> {
                val adjustedPotOdds = requiredEquity / adjustment.callThreshold
                if (callAmount <= pot * 0.6 && potOdds <= adjustedPotOdds) {
                    callAmount
                } else 0
            }
            
            MadeHandStrength.WEAK -> {
                if (handStrength.draws.hasStrongDraw()) {
                    // Semi-bluff raise or call
                    if (position == PositionAnalyzer.Position.LATE && random.nextFloat() < 0.3) {
                        val raiseSize = (callAmount + minimumRaise).toInt()
                        min(myStack, raiseSize)
                    } else if (handStrength.draws.outs >= 12) {
                        callAmount
                    } else 0
                } else if (callAmount <= pot * 0.3 * adjustment.callThreshold) {
                    callAmount // Cheap call with some equity
                } else 0
            }
            
            MadeHandStrength.VERY_WEAK -> {
                if (handStrength.draws.hasStrongDraw() && callAmount <= pot * 0.4) {
                    callAmount
                } else 0
            }
        }
    }
    
    /**
     * Calculate donk betting (betting into the pre-flop aggressor)
     */
    private fun calculateDonkBet(
        handStrength: PostFlopHandStrength,
        boardTexture: BoardTexture,
        pot: Int,
        position: PositionAnalyzer.Position,
        myStack: Int,
        smallBlind: Int
    ): Int {
        // Donk betting is generally not recommended except in specific spots
        val shouldDonkBet = when {
            handStrength.madeHand == MadeHandStrength.NUTS && boardTexture.dynamic -> true
            handStrength.madeHand == MadeHandStrength.VERY_STRONG && position == PositionAnalyzer.Position.BLINDS -> true
            handStrength.draws.hasStrongDraw() && boardTexture.bluffFriendly && random.nextFloat() < 0.2f -> true
            else -> false
        }
        
        if (!shouldDonkBet) return 0
        
        val betSizing = when (handStrength.madeHand) {
            MadeHandStrength.NUTS, MadeHandStrength.VERY_STRONG -> 0.7
            MadeHandStrength.STRONG -> 0.6
            else -> 0.5 // Bluff sizing
        }
        
        val betAmount = (pot * betSizing).toInt()
        return min(myStack, max(smallBlind * 2, betAmount))
    }
    
    // Helper functions for board analysis
    private fun getStreet(communityCards: JSONArray): String = when (communityCards.length()) {
        3 -> "flop"
        4 -> "turn"
        5 -> "river"
        else -> "preflop"
    }
    
    private fun hasOpenEndedStraightDraw(ranks: List<Int>): Boolean {
        val sorted = ranks.distinct().sorted()
        for (i in 0..sorted.size - 3) {
            if (sorted[i + 2] - sorted[i] == 2) return true
        }
        return false
    }
    
    private fun hasStraightPossibility(ranks: List<Int>): Boolean {
        val sorted = ranks.distinct().sorted()
        if (sorted.size < 3) return false
        
        for (i in 0..sorted.size - 3) {
            if (sorted[i + 2] - sorted[i] <= 4) return true
        }
        
        // Check for wheel possibility
        return sorted.contains(14) && sorted.contains(2) && sorted.size >= 3
    }
    
    private fun isConnectedBoard(ranks: List<Int>): Boolean {
        val sorted = ranks.sorted()
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1] - sorted[i] <= 2) return true
        }
        return false
    }
    
    private fun isDynamicBoard(ranks: List<Int>, suitCounts: Map<String, Int>): Boolean {
        return isConnectedBoard(ranks) || suitCounts.values.any { it >= 3 }
    }
    
    private fun calculateWetness(ranks: List<Int>, suitCounts: Map<String, Int>): Double {
        var wetness = 0.0
        
        // Flush draws increase wetness
        if (suitCounts.values.any { it >= 3 }) wetness += 0.4
        if (suitCounts.values.any { it >= 2 }) wetness += 0.2
        
        // Straight possibilities increase wetness
        if (hasOpenEndedStraightDraw(ranks)) wetness += 0.3
        if (hasStraightPossibility(ranks)) wetness += 0.2
        
        // Connected boards increase wetness
        if (isConnectedBoard(ranks)) wetness += 0.2
        
        return wetness.coerceIn(0.0, 1.0)
    }
    
    private fun isBluffFriendly(ranks: List<Int>, suitCounts: Map<String, Int>): Boolean {
        // Bluff-friendly boards favor the aggressor and are hard to connect with
        val hasHighCards = ranks.any { it >= 10 }
        val isRainbow = suitCounts.size >= 3
        val disconnected = !isConnectedBoard(ranks)
        
        return hasHighCards && isRainbow && disconnected
    }
    
    private fun evaluateDrawStrength(
        myCards: JSONArray,
        communityCards: JSONArray,
        boardTexture: BoardTexture
    ): DrawStrength {
        if (myCards.length() != 2) return DrawStrength()
        
        val myRanks = listOf(
            CardUtils.getRankValue(myCards.getJSONObject(0).getString("rank")),
            CardUtils.getRankValue(myCards.getJSONObject(1).getString("rank"))
        )
        val mySuits = listOf(
            myCards.getJSONObject(0).getString("suit"),
            myCards.getJSONObject(1).getString("suit")
        )
        
        // Count overcards
        val boardHighCard = (0 until communityCards.length())
            .map { CardUtils.getRankValue(communityCards.getJSONObject(it).getString("rank")) }
            .maxOrNull() ?: 0
        val overcards = myRanks.count { it > boardHighCard }
        
        // Check for flush draws
        val boardSuits = (0 until communityCards.length())
            .map { communityCards.getJSONObject(it).getString("suit") }
        val flushDraw = mySuits.any { suit -> 
            boardSuits.count { it == suit } >= 2 && boardSuits.count { it == suit } + 1 >= 4
        }
        val nutFlushDraw = flushDraw && mySuits.any { suit ->
            boardSuits.count { it == suit } >= 2 && myRanks.maxOrNull() == 14
        }
        
        // Estimate total outs (simplified)
        var outs = 0
        if (nutFlushDraw) outs += 9
        else if (flushDraw) outs += 9
        if (hasOpenEndedStraightDraw(myRanks + (0 until communityCards.length()).map { 
            CardUtils.getRankValue(communityCards.getJSONObject(it).getString("rank")) 
        })) outs += 8
        outs += overcards * 3 // Rough estimate for overcard outs
        
        return DrawStrength(
            nutFlushDraw = nutFlushDraw,
            flushDraw = flushDraw,
            openEndedStraightDraw = hasOpenEndedStraightDraw(myRanks + (0 until communityCards.length()).map { 
                CardUtils.getRankValue(communityCards.getJSONObject(it).getString("rank")) 
            }),
            overcards = overcards,
            outs = outs
        )
    }
    
    private fun hasOvercards(myCards: JSONArray, communityCards: JSONArray): Boolean {
        val myRanks = listOf(
            CardUtils.getRankValue(myCards.getJSONObject(0).getString("rank")),
            CardUtils.getRankValue(myCards.getJSONObject(1).getString("rank"))
        )
        val boardHighCard = (0 until communityCards.length())
            .map { CardUtils.getRankValue(communityCards.getJSONObject(it).getString("rank")) }
            .maxOrNull() ?: 0
        
        return myRanks.any { it > boardHighCard }
    }
}
