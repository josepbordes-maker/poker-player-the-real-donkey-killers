package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*
import kotlin.random.Random

/**
 * Adaptive Betting System - Real-time strategy optimization
 * 
 * This system continuously adapts betting strategy based on:
 * - Win/loss patterns
 * - Opponent behavior changes
 * - Stack size evolution
 * - Tournament dynamics
 * - Meta-game considerations
 */
class AdaptiveBettingSystem {
    private val performanceMetrics = PerformanceMetrics()
    private val adaptiveParameters = AdaptiveParameters()
    private val exploitationEngine = ExploitationEngine()
    private val stackOptimizer = StackOptimizer()
    private val random = Random
    
    /**
     * Calculate bet with real-time adaptation
     */
    fun calculateAdaptiveBet(
        baseDecision: DynamicStrategyManager.DynamicBetDecision,
        myCards: JSONArray,
        communityCards: JSONArray,
        myStack: Int,
        pot: Int,
        position: PositionAnalyzer.Position,
        opponentProfile: OpponentProfile,
        gameContext: GameContext
    ): AdaptiveBetDecision {
        
        // Analyze current performance
        val performance = performanceMetrics.getCurrentPerformance()
        
        // Adjust based on recent results
        val adaptedDecision = when (performance.trend) {
            PerformanceTrend.WINNING -> optimizeForWinningStreak(baseDecision, gameContext)
            PerformanceTrend.LOSING -> adjustForLosingStreak(baseDecision, gameContext, opponentProfile)
            PerformanceTrend.BREAKEVEN -> maintainBalance(baseDecision, gameContext)
        }
        
        // Apply exploitation adjustments
        val exploitedDecision = exploitationEngine.applyExploitation(
            adaptedDecision, opponentProfile, position, gameContext
        )
        
        // Stack size optimization
        val optimizedDecision = stackOptimizer.optimizeForStack(
            exploitedDecision, myStack, pot, gameContext
        )
        
        // Record decision for learning
        performanceMetrics.recordDecision(optimizedDecision, myStack, pot)
        
        return optimizedDecision
    }
    
    /**
     * Adjust strategy when winning to maximize profit
     */
    private fun optimizeForWinningStreak(
        baseDecision: DynamicStrategyManager.DynamicBetDecision,
        gameContext: GameContext
    ): AdaptiveBetDecision {
        val multiplier = when (gameContext.tournamentPhase) {
            DynamicStrategyManager.TournamentPhase.EARLY -> 1.2 // Build big stack early
            DynamicStrategyManager.TournamentPhase.BUBBLE -> 0.9 // Protect lead
            DynamicStrategyManager.TournamentPhase.FINAL_TABLE -> 1.1 // Press advantage
            else -> 1.1
        }
        
        return AdaptiveBetDecision(
            action = baseDecision.action,
            amount = (baseDecision.amount * multiplier).toInt(),
            reasoning = "${baseDecision.reasoning} + Winning streak optimization",
            confidence = min(1.0, baseDecision.confidence * 1.1),
            adaptationType = AdaptationType.WINNING_STREAK
        )
    }
    
    /**
     * Adjust strategy when losing to minimize damage and recover
     */
    private fun adjustForLosingStreak(
        baseDecision: DynamicStrategyManager.DynamicBetDecision,
        gameContext: GameContext,
        opponentProfile: OpponentProfile
    ): AdaptiveBetDecision {
        // Tighten up when losing
        val adjustment = when {
            gameContext.stackToBlindRatio < 15 -> 0.8 // Very tight when short
            opponentProfile.totalOpponents > 6 -> 0.7 // Extra tight in big fields
            else -> 0.85
        }
        
        // Switch to more conservative actions
        val adjustedAction = when (baseDecision.action) {
            "bet" -> if (random.nextFloat() < 0.3) "check" else "bet"
            "raise" -> if (random.nextFloat() < 0.4) "call" else "raise"
            else -> baseDecision.action
        }
        
        return AdaptiveBetDecision(
            action = adjustedAction,
            amount = (baseDecision.amount * adjustment).toInt(),
            reasoning = "${baseDecision.reasoning} + Losing streak protection",
            confidence = baseDecision.confidence * 0.9,
            adaptationType = AdaptationType.LOSING_STREAK
        )
    }
    
    /**
     * Maintain balanced approach when breaking even
     */
    private fun maintainBalance(
        baseDecision: DynamicStrategyManager.DynamicBetDecision,
        gameContext: GameContext
    ): AdaptiveBetDecision {
        // Small random variations to avoid predictability
        val variance = 0.95 + random.nextFloat() * 0.1 // 0.95 to 1.05
        
        return AdaptiveBetDecision(
            action = baseDecision.action,
            amount = (baseDecision.amount * variance).toInt(),
            reasoning = "${baseDecision.reasoning} + Balanced approach",
            confidence = baseDecision.confidence,
            adaptationType = AdaptationType.BALANCED
        )
    }
    
    data class AdaptiveBetDecision(
        val action: String,
        val amount: Int,
        val reasoning: String,
        val confidence: Double,
        val adaptationType: AdaptationType
    )
    
    enum class AdaptationType {
        WINNING_STREAK,
        LOSING_STREAK,
        BALANCED,
        EXPLOITATION,
        STACK_OPTIMIZATION
    }
    
    /**
     * Track performance metrics for adaptation
     */
    class PerformanceMetrics {
        private val recentHands = mutableListOf<HandOutcome>()
        private val maxHistory = 30
        private var totalHands = 0
        private var totalProfit = 0
        
        fun recordDecision(decision: AdaptiveBetDecision, stackBefore: Int, pot: Int) {
            totalHands++
            // This would be updated with actual results after hand completion
        }
        
        fun recordHandOutcome(profit: Int, stackChange: Int) {
            recentHands.add(HandOutcome(profit, stackChange))
            totalProfit += profit
            
            if (recentHands.size > maxHistory) {
                recentHands.removeFirst()
            }
        }
        
        fun getCurrentPerformance(): PerformanceAnalysis {
            if (recentHands.size < 5) {
                return PerformanceAnalysis(PerformanceTrend.BREAKEVEN, 0.0, 0)
            }
            
            val recentProfit = recentHands.takeLast(10).sumOf { it.profit }
            val winRate = recentHands.takeLast(10).count { it.profit > 0 }.toDouble() / 10
            
            val trend = when {
                recentProfit > 0 && winRate > 0.6 -> PerformanceTrend.WINNING
                recentProfit < 0 || winRate < 0.4 -> PerformanceTrend.LOSING
                else -> PerformanceTrend.BREAKEVEN
            }
            
            return PerformanceAnalysis(trend, winRate, recentProfit)
        }
        
        data class HandOutcome(val profit: Int, val stackChange: Int)
        data class PerformanceAnalysis(val trend: PerformanceTrend, val winRate: Double, val recentProfit: Int)
    }
    
    enum class PerformanceTrend {
        WINNING, LOSING, BREAKEVEN
    }
    
    /**
     * Adaptive parameters that change based on performance
     */
    class AdaptiveParameters {
        var aggressionLevel = 1.0
            private set
        var bluffFrequency = 0.2
            private set
        var valueBetSizing = 1.0
            private set
        var foldThreshold = 0.4
            private set
        
        fun updateParameters(performance: PerformanceMetrics.PerformanceAnalysis) {
            when (performance.trend) {
                PerformanceTrend.WINNING -> {
                    aggressionLevel = min(1.5, aggressionLevel * 1.05)
                    bluffFrequency = min(0.35, bluffFrequency * 1.1)
                    valueBetSizing = min(1.3, valueBetSizing * 1.02)
                }
                PerformanceTrend.LOSING -> {
                    aggressionLevel = max(0.7, aggressionLevel * 0.95)
                    bluffFrequency = max(0.1, bluffFrequency * 0.9)
                    foldThreshold = min(0.6, foldThreshold * 1.05)
                }
                PerformanceTrend.BREAKEVEN -> {
                    // Slowly return to defaults
                    aggressionLevel = aggressionLevel * 0.99 + 1.0 * 0.01
                    bluffFrequency = bluffFrequency * 0.99 + 0.2 * 0.01
                    valueBetSizing = valueBetSizing * 0.99 + 1.0 * 0.01
                    foldThreshold = foldThreshold * 0.99 + 0.4 * 0.01
                }
            }
        }
    }
    
    /**
     * Engine for exploiting specific opponent weaknesses
     */
    class ExploitationEngine {
        fun applyExploitation(
            decision: AdaptiveBetDecision,
            opponentProfile: OpponentProfile,
            position: PositionAnalyzer.Position,
            gameContext: GameContext
        ): AdaptiveBetDecision {
            
            // Exploit based on opponent profile
            val exploitationMultiplier = when (opponentProfile.tableType) {
                "LOOSE" -> when (decision.action) {
                    "bet", "raise" -> 1.3 // Bet bigger for value
                    "call" -> 1.1 // Call more liberally
                    else -> 1.0
                }
                "TIGHT" -> when (decision.action) {
                    "bet", "raise" -> if (decision.confidence < 0.7) 1.4 else 0.9 // Bluff more, value bet less
                    "fold" -> 0.8 // Fold less against tight players
                    else -> 1.0
                }
                "AGGRESSIVE" -> when (decision.action) {
                    "call" -> 1.2 // Call down more often
                    "raise" -> 0.9 // Raise less frequently
                    else -> 1.0
                }
                else -> 1.0
            }
            
            return AdaptiveBetDecision(
                action = decision.action,
                amount = (decision.amount * exploitationMultiplier).toInt(),
                reasoning = "${decision.reasoning} + ${opponentProfile.tableType} exploitation",
                confidence = decision.confidence,
                adaptationType = AdaptationType.EXPLOITATION
            )
        }
    }
    
    /**
     * Optimize betting based on stack sizes
     */
    class StackOptimizer {
        fun optimizeForStack(
            decision: AdaptiveBetDecision,
            myStack: Int,
            pot: Int,
            gameContext: GameContext
        ): AdaptiveBetDecision {
            val spr = myStack.toDouble() / max(1, pot)
            
            val optimization = when {
                spr < 3 -> {
                    // Short stack - simplify decisions
                    when (decision.action) {
                        "bet", "raise" -> "allin"
                        "call" -> if (decision.amount >= myStack * 0.3) "allin" else "call"
                        else -> decision.action
                    }
                }
                spr > 10 -> {
                    // Deep stack - more complex play
                    when (decision.action) {
                        "bet" -> if (decision.confidence > 0.8) "bet" else "check"
                        "raise" -> if (decision.confidence > 0.7) "raise" else "call"
                        else -> decision.action
                    }
                }
                else -> decision.action
            }
            
            val optimizedAmount = when (optimization) {
                "allin" -> myStack
                else -> min(myStack, decision.amount)
            }
            
            return AdaptiveBetDecision(
                action = optimization,
                amount = optimizedAmount,
                reasoning = "${decision.reasoning} + SPR optimization (SPR: ${spr.toInt()})",
                confidence = decision.confidence,
                adaptationType = AdaptationType.STACK_OPTIMIZATION
            )
        }
    }
    
    // Data classes for opponent profiling
    data class OpponentProfile(
        val totalOpponents: Int,
        val tableType: String,
        val aggressiveCount: Int,
        val passiveCount: Int,
        val tiltedCount: Int
    )
    
    data class GameContext(
        val tournamentPhase: DynamicStrategyManager.TournamentPhase,
        val stackToBlindRatio: Double,
        val averageStack: Double,
        val potSize: Int
    )
}
