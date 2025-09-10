package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import kotlin.math.min
import kotlin.math.roundToInt

class BettingStrategy(
    private val handEvaluator: HandEvaluator,
    private val positionAnalyzer: PositionAnalyzer
) {
    private val random = Random

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
        players: JSONArray? = null
    ): Int {
        println("  --- BettingStrategy.calculateBet() ---")
        
        // Detect heads-up situation
        val isHeadsUp = players?.let { countActivePlayers(it) == 2 } ?: false
        println("  Heads-up detection: $isHeadsUp")
        
        // Calculate call amount
        val callAmount = currentBuyIn - myBet
        println("  Call amount: $callAmount (currentBuyIn=$currentBuyIn - myBet=$myBet)")
        
        // Safety check - can't bet more than we have
        if (callAmount >= myStack) {
            println("  FORCED ALL-IN: Call amount ($callAmount) >= stack ($myStack)")
            return myStack // All-in if we must
        }
        
        // If no bet to call
        if (callAmount <= 0) {
            // Post-flop continuation/value bet sizing using hand rank and position
            if (communityCards.length() >= 3) {
                val strength = handEvaluator.evaluateBestHand(myCards, communityCards)
                val basePot = if (pot > 0) pot else smallBlind * 4 // minimal pot fallback
                
                // Analyze board texture for c-betting strategy
                val boardTexture = analyzeBoardTexture(communityCards)
                
                // Position-based c-betting strategy with increased aggression
                val positionMultiplier = when (position) {
                    PositionAnalyzer.Position.EARLY -> 0.85  // Slightly more aggressive
                    PositionAnalyzer.Position.MIDDLE -> 1.0   // Standard
                    PositionAnalyzer.Position.LATE -> 1.2    // More aggressive on favorable boards
                    PositionAnalyzer.Position.BLINDS -> 0.9
                }
                
                val betSize = when (strength.rank) {
                    HandEvaluator.HandRank.ROYAL_FLUSH,
                    HandEvaluator.HandRank.STRAIGHT_FLUSH,
                    HandEvaluator.HandRank.FOUR_OF_A_KIND,
                    HandEvaluator.HandRank.FULL_HOUSE -> (basePot * StrategyConfig.postflopBig * positionMultiplier).roundToInt()
                    HandEvaluator.HandRank.FLUSH,
                    HandEvaluator.HandRank.STRAIGHT,
                    HandEvaluator.HandRank.THREE_OF_A_KIND -> (basePot * StrategyConfig.postflopMed * positionMultiplier).roundToInt()
                    HandEvaluator.HandRank.TWO_PAIR -> {
                        // Two pair is strong but be careful on wet boards
                        val baseBet = (basePot * StrategyConfig.postflopMed * positionMultiplier).roundToInt()
                        if (strength.value >= 10) baseBet else (baseBet * 0.8).roundToInt()
                    }
                    HandEvaluator.HandRank.ONE_PAIR -> {
                        // More aggressive with pairs, especially on dry boards
                        if (strength.value >= 11) {
                            (basePot * StrategyConfig.postflopSmall * positionMultiplier).roundToInt()
                        } else if (strength.value >= 8 && boardTexture.dry) {
                            // C-bet weak pairs on dry boards for protection
                            (basePot * StrategyConfig.postflopSmall * 0.7 * positionMultiplier).roundToInt()
                        } else {
                            0 // Check weak pairs on wet boards
                        }
                    }
                    HandEvaluator.HandRank.HIGH_CARD -> {
                        // C-bet high cards on favorable boards (A-high, K-high on dry boards)
                        if (boardTexture.dry && boardTexture.highCard && position == PositionAnalyzer.Position.LATE) {
                            (basePot * StrategyConfig.postflopSmall * 0.6).roundToInt()
                        } else {
                            0
                        }
                    }
                }
                val finalBet = min(myStack, maxOf(0, betSize))
                println("  Post-flop bet sizing (pos mult: $positionMultiplier, board: ${if (boardTexture.dry) "dry" else "wet"}) -> $finalBet")
                return finalBet
            }
            println("  No bet to call, calculating open raise...")
            val openRaise = calculateOpenRaise(myCards, communityCards, myStack, smallBlind, position, isHeadsUp)
            println("  Open raise decision: $openRaise")
            return openRaise
        }
        
        // Evaluate hand strength
        val hasStrong = handEvaluator.hasStrongHandWithCommunity(myCards, communityCards)
        val hasDecent = handEvaluator.hasDecentHand(myCards, isHeadsUp)
        val hasWeakPlayable = handEvaluator.hasWeakButPlayableHand(myCards, isHeadsUp)
        val hasMarginal = handEvaluator.hasMarginalHand(myCards)
        
        println("  Hand Evaluation:")
        println("    Strong: $hasStrong")
        println("    Decent: $hasDecent") 
        println("    Weak but Playable: $hasWeakPlayable")
        println("    Marginal: $hasMarginal")
        
        // Position-aware continuations when facing a bet
        var smallBetThreshold = positionAnalyzer.getSmallBetThreshold(position, pot)
        println("  Base small bet threshold (${position.name}): $smallBetThreshold")
        
        // Adjust threshold by strategy mode (tight/lag)
        val multiplier = StrategyConfig.smallBetThresholdMultiplier
        smallBetThreshold = Math.max(1, (smallBetThreshold * multiplier).toInt())
        println("  Adjusted threshold (×$multiplier): $smallBetThreshold")

        // Random risk-taking
        val riskProbability = StrategyConfig.riskMoodProbability
        val isRiskMood = random.nextFloat() < riskProbability
        println("  Risk mood check: $isRiskMood (probability: $riskProbability)")
        
        // Calculate pot odds for better decision making
        val potOdds = if (pot > 0) callAmount.toDouble() / (pot + callAmount).toDouble() else 1.0
        val impliedOdds = if (myStack > callAmount) (callAmount.toDouble() / (pot + callAmount + (myStack - callAmount) * 0.3)) else potOdds
        
        println("  Pot odds: ${(potOdds * 100).toInt()}%, Implied odds: ${(impliedOdds * 100).toInt()}%")
        
        val decision = when {
            hasStrong -> {
                val raiseAmount = min(myStack, callAmount + minimumRaise * 2)
                println("  STRONG HAND -> Raise to $raiseAmount")
                raiseAmount
            }
            hasDecent -> {
                // Better pot odds consideration for decent hands
                val shouldCall = callAmount <= smallBetThreshold || 
                                (potOdds <= 0.25 && position != PositionAnalyzer.Position.EARLY) ||
                                (potOdds <= 0.20 && position == PositionAnalyzer.Position.EARLY)
                if (shouldCall) {
                    println("  DECENT HAND with good odds -> CALL $callAmount")
                    callAmount
                } else {
                    println("  DECENT HAND but poor odds -> FOLD")
                    0
                }
            }
            hasWeakPlayable -> {
                // More conservative with weak playable hands
                val maxCall = when (position) {
                    PositionAnalyzer.Position.EARLY -> smallBlind  // Very conservative
                    PositionAnalyzer.Position.MIDDLE -> smallBlind * 2  // 2x small blind 
                    else -> smallBlind * 2  // Original threshold for late/blinds
                }
                if (callAmount <= maxCall && potOdds <= 0.2) {
                    println("  WEAK BUT PLAYABLE within conservative limit -> CALL $callAmount")
                    callAmount
                } else {
                    println("  WEAK BUT PLAYABLE but too expensive -> FOLD")
                    0
                }
            }
            // Risky play only for marginal non-playable hands with very good odds
            isRiskMood && hasMarginal && !hasWeakPlayable && callAmount <= pot / 5 && potOdds <= 0.15 -> {
                println("  RISK MOOD + MARGINAL with excellent odds -> Consider bluff raise/call")
                calculateRiskyPlay(callAmount, minimumRaise, myStack)
            }
            else -> {
                println("  -> FOLD (0)")
                0
            }
        }
        
        println("  BettingStrategy decision: $decision")
        println("  --- End BettingStrategy.calculateBet() ---")
        return decision
    }
    
    private fun calculateOpenRaise(
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        smallBlind: Int,
        position: PositionAnalyzer.Position,
        isHeadsUp: Boolean = false
    ): Int {
        println("    --- calculateOpenRaise() for ${position.name} (HU: $isHeadsUp) ---")
        
        val hasStrong = handEvaluator.hasStrongHandWithCommunity(myCards, communityCards)
        val hasDecent = handEvaluator.hasDecentHand(myCards, isHeadsUp)
        val hasWeakPlayable = handEvaluator.hasWeakButPlayableHand(myCards, isHeadsUp)
        
        println("    Hand strength: Strong=$hasStrong, Decent=$hasDecent, WeakPlayable=$hasWeakPlayable")
        
        // Check for isolation opportunity against limpers
        // This would be enhanced with actual game state analysis
        val canIsolate = hasDecent && position in listOf(PositionAnalyzer.Position.LATE, PositionAnalyzer.Position.MIDDLE)
        println("    Isolation opportunity: $canIsolate")
        
        val decision = when (position) {
            PositionAnalyzer.Position.EARLY -> when {
                hasStrong -> {
                    val raiseSize = min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                    println("    EARLY + STRONG -> Raise to $raiseSize")
                    raiseSize
                }
                else -> {
                    println("    EARLY + Not strong -> Check (0)")
                    0
                }
            }
            PositionAnalyzer.Position.MIDDLE -> when {
                hasStrong -> {
                    val raiseSize = min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                    println("    MIDDLE + STRONG -> Raise to $raiseSize")
                    raiseSize
                }
                hasDecent -> {
                    val raiseSize = min(myStack, positionAnalyzer.getOpenRaiseSize(position, smallBlind))
                    println("    MIDDLE + DECENT -> Raise to $raiseSize")
                    raiseSize
                }
                else -> {
                    println("    MIDDLE + Weak -> Check (0)")
                    0
                }
            }
            PositionAnalyzer.Position.LATE -> when {
                hasStrong -> {
                    val raiseSize = min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                    println("    LATE + STRONG -> Raise to $raiseSize")
                    raiseSize
                }
                hasDecent -> {
                    val raiseSize = min(myStack, positionAnalyzer.getOpenRaiseSize(position, smallBlind))
                    println("    LATE + DECENT -> Raise to $raiseSize")
                    raiseSize
                }
                // Only open very strong weak-playable hands in late position
                hasWeakPlayable && StrategyConfig.allowLateOpenWithWeakPlayable -> {
                    // Be selective - only suited aces, suited connectors 8+, pocket pairs
                    val card1 = myCards.getJSONObject(0)
                    val card2 = myCards.getJSONObject(1)
                    val rank1 = card1.getString("rank")
                    val rank2 = card2.getString("rank")
                    val suit1 = card1.getString("suit")
                    val suit2 = card2.getString("suit")
                    val isSuited = suit1 == suit2
                    val isPair = rank1 == rank2
                    val value1 = CardUtils.getRankValue(rank1)
                    val value2 = CardUtils.getRankValue(rank2)
                    
                    val shouldOpen = (isPair && minOf(value1, value2) >= 6) || // Pocket pairs 66+
                                   (isSuited && (rank1 == "A" || rank2 == "A") && maxOf(value1, value2) >= 9) || // Suited aces A9s+
                                   (isSuited && minOf(value1, value2) >= 9 && kotlin.math.abs(value1 - value2) <= 1) // Strong suited connectors 9+
                    
                    if (shouldOpen) {
                        val raiseSize = min(myStack, smallBlind * 3) // Smaller sizing for marginal hands
                        println("    LATE + SELECT WEAK PLAYABLE -> Small raise to $raiseSize")
                        raiseSize
                    } else {
                        println("    LATE + WEAK PLAYABLE (not selective) -> Check (0)")
                        0
                    }
                }
                else -> {
                    println("    LATE + Weak -> Check (0)")
                    0
                }
            }
            PositionAnalyzer.Position.BLINDS -> when {
                // More defensive from blinds: raise only strong hands
                hasStrong -> {
                    val raiseSize = min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                    println("    BLINDS + STRONG -> Raise to $raiseSize")
                    raiseSize
                }
                else -> {
                    println("    BLINDS + Not strong -> Check (0)")
                    0
                }
            }
        }
        
        println("    --- End calculateOpenRaise() -> $decision ---")
        return decision
    }
    
    private fun calculateRiskyPlay(callAmount: Int, minimumRaise: Int, myStack: Int): Int {
        println("    --- calculateRiskyPlay() ---")
        
        // Random aggressive play - sometimes raise with marginal hands
        val bluffRaiseEnabled = StrategyConfig.bluffRaiseEnabled
        println("    Bluff raise enabled: $bluffRaiseEnabled")
        
        if (!bluffRaiseEnabled) {
            println("    Bluff raise disabled -> Just call $callAmount")
            return callAmount
        }
        
        val bluffChance = random.nextFloat()
        // OPTIMIZED: More profitable bluffing - better threshold calculation
        val baseBluffThreshold = 0.18f // Slightly increased for LAG mode profitability
        val sizePenalty = callAmount.toFloat() / myStack.toFloat() // Penalty for larger bets
        val bluffThreshold = maxOf(0.08f, baseBluffThreshold - sizePenalty * 0.15f) // Less penalty
        val raiseAmount = callAmount + minimumRaise
        val affordableThreshold = myStack / 5 // Even more conservative - was /4
        val canAffordRaise = raiseAmount <= affordableThreshold
        
        println("    Bluff chance: $bluffChance (threshold: $bluffThreshold)")
        println("    Raise would be: $raiseAmount (call $callAmount + minRaise $minimumRaise)")
        println("    Can afford raise: $canAffordRaise ($raiseAmount <= $affordableThreshold)")
        
        return if (bluffChance < bluffThreshold && canAffordRaise) {
            println("    -> BLUFF RAISE to $raiseAmount")
            raiseAmount // Bluff raise
        } else {
            println("    -> Just CALL $callAmount")
            callAmount // Just call
        }
    }

    /**
     * Simple board texture analysis for c-betting decisions
     */
    private fun analyzeBoardTexture(communityCards: JSONArray): BoardTexture {
        if (communityCards.length() < 3) {
            return BoardTexture(dry = true, highCard = false)
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
        val sortedRanks = ranks.sorted()
        
        // Determine if board is dry (good for c-betting)
        val hasPair = rankCounts.values.any { it >= 2 }
        val flushDraw = suitCounts.values.any { it >= 2 }
        val connected = sortedRanks.zipWithNext().any { (a, b) -> b - a <= 2 }
        val highCard = ranks.any { it >= 10 } // Any 10+ on board
        
        val dry = !hasPair && !flushDraw && !connected
        
        return BoardTexture(dry = dry, highCard = highCard)
    }
    
    data class BoardTexture(
        val dry: Boolean,
        val highCard: Boolean
    )

    /**
     * Count active players in the hand
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
        return count
    }
}

