package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import kotlin.math.min

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
        // Calculate call amount
        val callAmount = currentBuyIn - myBet
        
        // Safety check - can't bet more than we have
        if (callAmount >= myStack) {
            return myStack // All-in if we must
        }
        
        // If no bet to call, open-raise based on position
        if (callAmount <= 0) {
            return calculateOpenRaise(myCards, communityCards, myStack, smallBlind, position)
        }
        
        // Position-aware continuations when facing a bet
        var smallBetThreshold = positionAnalyzer.getSmallBetThreshold(position, pot)
        // Adjust threshold by strategy mode (tight/lag)
        smallBetThreshold = Math.max(1, (smallBetThreshold * StrategyConfig.smallBetThresholdMultiplier).toInt())

        // Random risk-taking: 15% chance to take a risk with marginal hands
        val isRiskMood = random.nextFloat() < StrategyConfig.riskMoodProbability
        
        return when {
            // Use enhanced evaluation with community cards when available
            handEvaluator.hasStrongHandWithCommunity(myCards, communityCards) -> 
                min(myStack, callAmount + minimumRaise * 2)
            handEvaluator.hasDecentHand(myCards) && callAmount <= smallBetThreshold -> callAmount
            handEvaluator.hasWeakButPlayableHand(myCards) && callAmount <= smallBlind * 2 -> callAmount
            // Remove unconditional small-bet calls; require at least a playable hand
            // Risky play only for marginal non-playable hands
            isRiskMood && handEvaluator.hasMarginalHand(myCards) && !handEvaluator.hasWeakButPlayableHand(myCards) && callAmount <= pot / 3 -> {
                calculateRiskyPlay(callAmount, minimumRaise, myStack)
            }
            else -> 0
        }
    }
    
    private fun calculateOpenRaise(
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        smallBlind: Int,
        position: PositionAnalyzer.Position
    ): Int {
        return when (position) {
            PositionAnalyzer.Position.EARLY -> when {
                handEvaluator.hasStrongHandWithCommunity(myCards, communityCards) -> 
                    min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                else -> 0
            }
            PositionAnalyzer.Position.MIDDLE -> when {
                handEvaluator.hasStrongHandWithCommunity(myCards, communityCards) -> 
                    min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                handEvaluator.hasDecentHand(myCards) -> 
                    min(myStack, positionAnalyzer.getOpenRaiseSize(position, smallBlind))
                else -> 0
            }
            PositionAnalyzer.Position.LATE -> when {
                handEvaluator.hasStrongHandWithCommunity(myCards, communityCards) -> 
                    min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                handEvaluator.hasDecentHand(myCards) -> 
                    min(myStack, positionAnalyzer.getOpenRaiseSize(position, smallBlind))
                else -> 0
            }
            PositionAnalyzer.Position.BLINDS -> when {
                // More defensive from blinds: raise only strong hands
                handEvaluator.hasStrongHandWithCommunity(myCards, communityCards) -> 
                    min(myStack, positionAnalyzer.getStrongHandRaiseSize(smallBlind))
                else -> 0
            }
        }
    }
    
    private fun calculateRiskyPlay(callAmount: Int, minimumRaise: Int, myStack: Int): Int {
        // Random aggressive play - sometimes raise with marginal hands
        if (!StrategyConfig.bluffRaiseEnabled) return callAmount
        return if (random.nextFloat() < 0.3f && callAmount + minimumRaise <= myStack / 4) {
            callAmount + minimumRaise // Bluff raise
        } else {
            callAmount // Just call
        }
    }
}
