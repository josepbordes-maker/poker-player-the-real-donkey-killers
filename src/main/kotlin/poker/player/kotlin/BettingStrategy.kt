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
        position: PositionAnalyzer.Position
    ): Int {
        println("  --- BettingStrategy.calculateBet() ---")
        
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
            // Post-flop continuation/value bet sizing using hand rank
            if (communityCards.length() >= 3) {
                val strength = handEvaluator.evaluateBestHand(myCards, communityCards)
                val basePot = if (pot > 0) pot else smallBlind * 4 // minimal pot fallback
                val betSize = when (strength.rank) {
                    HandEvaluator.HandRank.ROYAL_FLUSH,
                    HandEvaluator.HandRank.STRAIGHT_FLUSH,
                    HandEvaluator.HandRank.FOUR_OF_A_KIND,
                    HandEvaluator.HandRank.FULL_HOUSE -> (basePot * StrategyConfig.postflopBig).roundToInt()
                    HandEvaluator.HandRank.FLUSH,
                    HandEvaluator.HandRank.STRAIGHT,
                    HandEvaluator.HandRank.THREE_OF_A_KIND,
                    HandEvaluator.HandRank.TWO_PAIR -> (basePot * StrategyConfig.postflopMed).roundToInt()
                    HandEvaluator.HandRank.ONE_PAIR -> (basePot * StrategyConfig.postflopSmall).roundToInt()
                    HandEvaluator.HandRank.HIGH_CARD -> 0
                }
                val finalBet = min(myStack, maxOf(0, betSize))
                println("  Post-flop bet sizing -> $finalBet")
                return finalBet
            }
            println("  No bet to call, calculating open raise...")
            val openRaise = calculateOpenRaise(myCards, communityCards, myStack, smallBlind, position)
            println("  Open raise decision: $openRaise")
            return openRaise
        }
        
        // Evaluate hand strength
        val hasStrong = handEvaluator.hasStrongHandWithCommunity(myCards, communityCards)
        val hasDecent = handEvaluator.hasDecentHand(myCards)
        val hasWeakPlayable = handEvaluator.hasWeakButPlayableHand(myCards)
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
        
        val decision = when {
            hasStrong -> {
                val raiseAmount = min(myStack, callAmount + minimumRaise * 2)
                println("  STRONG HAND -> Raise to $raiseAmount")
                raiseAmount
            }
            hasDecent && callAmount <= smallBetThreshold -> {
                println("  DECENT HAND within threshold -> CALL $callAmount")
                callAmount
            }
            hasWeakPlayable && callAmount <= smallBlind * 2 -> {
                println("  WEAK BUT PLAYABLE within 2xSB -> CALL $callAmount")
                callAmount
            }
            // Risky play only for marginal non-playable hands
            isRiskMood && hasMarginal && !hasWeakPlayable && callAmount <= pot / 4 -> {
                println("  RISK MOOD + MARGINAL -> Consider bluff raise/call")
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
        position: PositionAnalyzer.Position
    ): Int {
        println("    --- calculateOpenRaise() for ${position.name} ---")
        
        val hasStrong = handEvaluator.hasStrongHandWithCommunity(myCards, communityCards)
        val hasDecent = handEvaluator.hasDecentHand(myCards)
        
        println("    Hand strength: Strong=$hasStrong, Decent=$hasDecent")
        
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
        val bluffThreshold = 0.3f
        val raiseAmount = callAmount + minimumRaise
        val affordableThreshold = myStack / 4
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
}
