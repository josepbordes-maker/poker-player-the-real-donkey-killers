package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced opponent modeling system for exploitative play
 * Tracks player tendencies and suggests strategic adjustments
 */
class OpponentModeling {
    
    data class PlayerStats(
        var handsPlayed: Int = 0,
        var timesRaised: Int = 0,
        var timesCalled: Int = 0,
        var timesFolded: Int = 0,
        var timesAllIn: Int = 0,
        var preflopRaises: Int = 0,
        var preflopCalls: Int = 0,
        var postflopAggression: Int = 0,
        var postflopCalls: Int = 0,
        var showdownHands: Int = 0,
        var strongHandsShown: Int = 0,
        var bluffsShown: Int = 0,
        var stackSize: Int = 0,
        var positionPlays: MutableMap<PositionAnalyzer.Position, Int> = mutableMapOf(),
        var betSizes: MutableList<Double> = mutableListOf() // Relative to pot
    )
    
    enum class PlayerType {
        TIGHT_PASSIVE,    // Low VPIP, low aggression
        TIGHT_AGGRESSIVE, // Low VPIP, high aggression  
        LOOSE_PASSIVE,    // High VPIP, low aggression (calling station)
        LOOSE_AGGRESSIVE, // High VPIP, high aggression (LAG)
        UNKNOWN          // Insufficient data
    }
    
    private val playerStats = mutableMapOf<String, PlayerStats>()
    private val gameHistory = mutableMapOf<String, MutableList<GameAction>>()
    
    data class GameAction(
        val playerId: String,
        val action: String, // "fold", "call", "raise", "all_in"
        val amount: Int,
        val potSize: Int,
        val position: PositionAnalyzer.Position,
        val street: String, // "preflop", "flop", "turn", "river"
        val stackSize: Int
    )
    
    /**
     * Update player stats based on observed action
     */
    fun recordAction(
        playerId: String,
        action: String,
        amount: Int,
        potSize: Int,
        position: PositionAnalyzer.Position,
        street: String,
        stackSize: Int
    ) {
        val stats = playerStats.getOrPut(playerId) { PlayerStats() }
        stats.handsPlayed++
        stats.stackSize = stackSize
        
        // Track position-based plays
        stats.positionPlays[position] = stats.positionPlays.getOrDefault(position, 0) + 1
        
        when (action.lowercase()) {
            "fold" -> stats.timesFolded++
            "call" -> {
                stats.timesCalled++
                if (street == "preflop") stats.preflopCalls++
                else stats.postflopCalls++
            }
            "raise", "bet" -> {
                stats.timesRaised++
                if (street == "preflop") stats.preflopRaises++
                else stats.postflopAggression++
                
                // Track relative bet sizing
                if (potSize > 0) {
                    val relativeBetSize = amount.toDouble() / potSize
                    stats.betSizes.add(relativeBetSize)
                    // Keep only last 20 bet sizes for recent trends
                    if (stats.betSizes.size > 20) {
                        stats.betSizes.removeFirst()
                    }
                }
            }
            "all_in" -> {
                stats.timesAllIn++
                stats.timesRaised++ // All-in is aggressive
            }
        }
        
        // Record action for sequence analysis
        val gameId = "current" // In a real implementation, pass game ID
        val actions = gameHistory.getOrPut(gameId) { mutableListOf() }
        actions.add(GameAction(playerId, action, amount, potSize, position, street, stackSize))
    }
    
    /**
     * Classify player type based on statistical analysis
     */
    fun getPlayerType(playerId: String): PlayerType {
        val stats = playerStats[playerId] ?: return PlayerType.UNKNOWN
        
        if (stats.handsPlayed < 5) return PlayerType.UNKNOWN
        
        // Calculate key metrics
        val vpip = (stats.preflopCalls + stats.preflopRaises).toDouble() / stats.handsPlayed
        val pfr = stats.preflopRaises.toDouble() / stats.handsPlayed
        val totalActions = stats.timesCalled + stats.timesRaised + stats.timesFolded
        val aggression = if (totalActions > 0) {
            stats.timesRaised.toDouble() / (stats.timesRaised + stats.timesCalled)
        } else 0.0
        
        return when {
            vpip < 0.2 && aggression > 0.6 -> PlayerType.TIGHT_AGGRESSIVE
            vpip < 0.2 && aggression <= 0.6 -> PlayerType.TIGHT_PASSIVE
            vpip >= 0.35 && aggression > 0.6 -> PlayerType.LOOSE_AGGRESSIVE
            vpip >= 0.35 && aggression <= 0.6 -> PlayerType.LOOSE_PASSIVE
            else -> PlayerType.UNKNOWN
        }
    }
    
    /**
     * Get strategic adjustments against this player type
     */
    fun getStrategicAdjustments(playerId: String): StrategicAdjustment {
        val playerType = getPlayerType(playerId)
        val stats = playerStats[playerId]
        
        return when (playerType) {
            PlayerType.TIGHT_PASSIVE -> StrategicAdjustment(
                bluffFrequency = 1.5, // Bluff more
                valueBetSizing = 1.2, // Bet bigger for value
                callThreshold = 0.8, // Call less (they don't bluff much)
                isolationTendency = 1.3, // Isolate their weak calls
                continuationBetting = 1.4 // C-bet more often
            )
            
            PlayerType.TIGHT_AGGRESSIVE -> StrategicAdjustment(
                bluffFrequency = 0.6, // Bluff less
                valueBetSizing = 0.9, // Smaller value bets
                callThreshold = 1.2, // Call more (respect their aggression)
                isolationTendency = 0.7, // Don't try to isolate
                continuationBetting = 0.8 // C-bet less
            )
            
            PlayerType.LOOSE_PASSIVE -> StrategicAdjustment(
                bluffFrequency = 0.3, // Rarely bluff calling stations
                valueBetSizing = 1.5, // Bet huge for value
                callThreshold = 0.7, // Call less with marginal hands
                isolationTendency = 1.6, // Isolate constantly
                continuationBetting = 0.9 // C-bet less (they call everything)
            )
            
            PlayerType.LOOSE_AGGRESSIVE -> StrategicAdjustment(
                bluffFrequency = 0.4, // Reduced bluffing
                valueBetSizing = 1.1, // Slightly bigger value bets
                callThreshold = 1.4, // Call more often (they bluff a lot)
                isolationTendency = 0.8, // Be careful isolating
                continuationBetting = 1.2 // C-bet more to stay aggressive
            )
            
            PlayerType.UNKNOWN -> StrategicAdjustment(
                bluffFrequency = 1.0,
                valueBetSizing = 1.0,
                callThreshold = 1.0,
                isolationTendency = 1.0,
                continuationBetting = 1.0
            )
        }
    }
    
    data class StrategicAdjustment(
        val bluffFrequency: Double,     // Multiplier for bluff frequency
        val valueBetSizing: Double,     // Multiplier for value bet sizing
        val callThreshold: Double,      // Multiplier for calling thresholds
        val isolationTendency: Double,  // Multiplier for isolation plays
        val continuationBetting: Double // Multiplier for c-betting frequency
    )
    
    /**
     * Get player's average bet sizing tendency
     */
    fun getAverageBetSizing(playerId: String): Double {
        val stats = playerStats[playerId] ?: return 1.0
        return if (stats.betSizes.isNotEmpty()) {
            stats.betSizes.average()
        } else 1.0
    }
    
    /**
     * Check if player is on tilt or playing differently
     */
    fun isPlayerOnTilt(playerId: String): Boolean {
        val stats = playerStats[playerId] ?: return false
        if (stats.handsPlayed < 20) return false
        
        // Check recent aggression vs historical
        val recentActions = gameHistory["current"]?.takeLast(10) ?: return false
        val recentPlayerActions = recentActions.filter { it.playerId == playerId }
        
        if (recentPlayerActions.size < 5) return false
        
        val recentAggression = recentPlayerActions.count { 
            it.action in listOf("raise", "bet", "all_in") 
        }.toDouble() / recentPlayerActions.size
        
        val overallAggression = stats.timesRaised.toDouble() / 
            (stats.timesRaised + stats.timesCalled + stats.timesFolded)
        
        // If recent aggression is significantly higher, possible tilt
        return recentAggression > overallAggression * 1.8
    }
    
    /**
     * Get stack-size adjusted strategy
     */
    fun getStackSizeStrategy(stackSize: Int, smallBlind: Int): StackStrategy {
        val bigBlind = smallBlind * 2
        val stackInBB = stackSize.toDouble() / bigBlind
        
        return when {
            stackInBB < 15 -> StackStrategy.SHORT_STACK
            stackInBB < 50 -> StackStrategy.MEDIUM_STACK
            else -> StackStrategy.DEEP_STACK
        }
    }
    
    enum class StackStrategy {
        SHORT_STACK,   // Push/fold, high variance
        MEDIUM_STACK,  // Balanced approach
        DEEP_STACK     // More post-flop play
    }
    
    /**
     * Clear old data to prevent memory leaks
     */
    fun clearOldData() {
        playerStats.entries.removeAll { it.value.handsPlayed < 5 }
        gameHistory.clear()
    }
    
    /**
     * Get readable player analysis for debugging
     */
    fun getPlayerAnalysis(playerId: String): String {
        val stats = playerStats[playerId] ?: return "No data available"
        val playerType = getPlayerType(playerId)
        val adjustment = getStrategicAdjustments(playerId)
        
        val vpip = if (stats.handsPlayed > 0) {
            ((stats.preflopCalls + stats.preflopRaises).toDouble() / stats.handsPlayed * 100).toInt()
        } else 0
        
        val aggression = if ((stats.timesCalled + stats.timesRaised) > 0) {
            (stats.timesRaised.toDouble() / (stats.timesRaised + stats.timesCalled) * 100).toInt()
        } else 0
        
        return """
            Player: $playerId
            Type: $playerType
            Hands: ${stats.handsPlayed}
            VPIP: $vpip%
            Aggression: $aggression%
            Adjustments: Bluff ${(adjustment.bluffFrequency * 100).toInt()}%, Value Bet ${(adjustment.valueBetSizing * 100).toInt()}%
            On Tilt: ${isPlayerOnTilt(playerId)}
        """.trimIndent()
    }
}
