package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/**
 * Advanced Opponent Profiler - Deep learning opponent models
 * 
 * Creates sophisticated profiles for each opponent including:
 * - Playing style classification
 * - Betting patterns and sizing tells
 * - Position-specific tendencies
 * - Tilt detection and exploitation
 * - Adaptation and counter-strategies
 */
class AdvancedOpponentProfiler {
    private val playerProfiles = mutableMapOf<String, PlayerProfile>()
    private val gameHistory = mutableMapOf<String, MutableList<ActionSequence>>()
    
    /**
     * Comprehensive player profile
     */
    data class PlayerProfile(
        val playerId: String,
        var handsObserved: Int = 0,
        var vpip: Double = 0.0,           // Voluntarily Put money In Pot
        var pfr: Double = 0.0,            // Pre-Flop Raise
        var aggression: Double = 0.0,     // Overall aggression factor
        var foldToBet: Double = 0.0,      // Fold to bet frequency
        var foldToRaise: Double = 0.0,    // Fold to raise frequency
        var cBetFreq: Double = 0.0,       // Continuation bet frequency
        var callCBet: Double = 0.0,       // Call continuation bet frequency
        var threeBetFreq: Double = 0.0,   // 3-bet frequency
        var fourBetFreq: Double = 0.0,    // 4-bet frequency
        
        // Position-specific stats
        val positionStats: MutableMap<PositionAnalyzer.Position, PositionStats> = mutableMapOf(),
        
        // Betting patterns
        val betSizingPatterns: BettingPatterns = BettingPatterns(),
        
        // Emotional state tracking
        var currentTiltLevel: Double = 0.0,
        var lastBadBeat: Int = 0,
        var consecutiveLosses: Int = 0,
        
        // Advanced metrics
        var showdownWinRate: Double = 0.0,
        var bluffFrequency: Double = 0.0,
        var valueFrequency: Double = 0.0,
        var stackGrowthRate: Double = 0.0,
        
        // Meta-game adaptation
        var adaptationLevel: Double = 0.0, // How much they adjust to opponents
        var exploitability: Double = 1.0,  // How exploitable they are (1.0 = very exploitable)
        
        // Playing style classification
        var playingStyle: PlayingStyle = PlayingStyle.UNKNOWN,
        var styleConfidence: Double = 0.0
    )
    
    data class PositionStats(
        var handsPlayed: Int = 0,
        var vpip: Double = 0.0,
        var pfr: Double = 0.0,
        var aggression: Double = 0.0,
        var openRaiseSize: Double = 2.5, // In big blinds
        var stealAttempts: Int = 0,
        var stealSuccess: Int = 0
    )
    
    data class BettingPatterns(
        var smallBetThreshold: Double = 0.3,  // % of pot
        var mediumBetThreshold: Double = 0.7, // % of pot
        var largeBetThreshold: Double = 1.2,  // % of pot
        var overbetThreshold: Double = 2.0,   // % of pot
        
        var smallBetFreq: Double = 0.0,
        var mediumBetFreq: Double = 0.0,
        var largeBetFreq: Double = 0.0,
        var overbetFreq: Double = 0.0,
        
        // Tells and patterns
        var quickActionTell: Double = 0.0,    // Fast action usually means...
        var slowActionTell: Double = 0.0,     // Slow action usually means...
        var sizingTells: MutableMap<String, Double> = mutableMapOf()
    )
    
    enum class PlayingStyle {
        TIGHT_PASSIVE,      // Nit
        TIGHT_AGGRESSIVE,   // TAG
        LOOSE_PASSIVE,      // Calling station
        LOOSE_AGGRESSIVE,   // LAG
        ULTRA_TIGHT,        // Rock
        MANIAC,             // Very loose aggressive
        UNKNOWN
    }
    
    data class ActionSequence(
        val position: PositionAnalyzer.Position,
        val action: String,
        val amount: Int,
        val potSize: Int,
        val betSize: Double, // As fraction of pot
        val street: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Update player profile with new action
     */
    fun updateProfile(
        playerId: String,
        action: String,
        amount: Int,
        potSize: Int,
        position: PositionAnalyzer.Position,
        street: String,
        wasAggressor: Boolean = false,
        handResult: String? = null
    ) {
        val profile = playerProfiles.getOrPut(playerId) { PlayerProfile(playerId) }
        profile.handsObserved++
        
        // Record action sequence
        val sequence = ActionSequence(position, action, amount, potSize, amount.toDouble() / max(1, potSize), street)
        val history = gameHistory.getOrPut(playerId) { mutableListOf() }
        history.add(sequence)
        
        // Keep only recent history
        if (history.size > 100) {
            history.removeFirst()
        }
        
        // Update basic stats
        updateBasicStats(profile, action, street, position)
        
        // Update position-specific stats
        updatePositionStats(profile, action, amount, potSize, position, street)
        
        // Update betting patterns
        updateBettingPatterns(profile, action, amount, potSize)
        
        // Check for tilt indicators
        updateTiltDetection(profile, action, amount, handResult)
        
        // Classify playing style
        classifyPlayingStyle(profile)
        
        // Update exploitability score
        updateExploitability(profile)
    }
    
    /**
     * Get strategic adjustments against this player
     */
    fun getAdvancedAdjustments(playerId: String): AdvancedAdjustments {
        val profile = playerProfiles[playerId] ?: return AdvancedAdjustments.default()
        
        if (profile.handsObserved < 10) return AdvancedAdjustments.default()
        
        return when (profile.playingStyle) {
            PlayingStyle.TIGHT_PASSIVE -> AdvancedAdjustments(
                bluffFrequency = 2.0,      // Bluff much more
                valueBetSizing = 1.5,      // Bet bigger for value
                isolationTendency = 1.8,   // Isolate their limps
                stealFrequency = 1.6,      // Steal their blinds more
                cBetFrequency = 1.4,       // C-bet more often
                barreling = 0.7,           // Don't barrel as much
                thinValueBets = 1.3        // More thin value bets
            )
            
            PlayingStyle.TIGHT_AGGRESSIVE -> AdvancedAdjustments(
                bluffFrequency = 0.4,      // Bluff much less
                valueBetSizing = 0.9,      // Smaller value bets
                isolationTendency = 0.6,   // Don't try to isolate
                stealFrequency = 0.7,      // Steal less
                cBetFrequency = 0.8,       // C-bet less
                barreling = 0.6,           // Don't barrel
                callDownFrequency = 1.3    // Call down more often
            )
            
            PlayingStyle.LOOSE_PASSIVE -> AdvancedAdjustments(
                bluffFrequency = 0.2,      // Almost never bluff
                valueBetSizing = 2.0,      // Bet huge for value
                isolationTendency = 2.5,   // Always isolate
                thinValueBets = 1.8,       // Many thin value bets
                cBetFrequency = 0.7,       // C-bet less (they call everything)
                barreling = 1.5,           // Barrel for value
                protection = 1.6           // Protect more often
            )
            
            PlayingStyle.LOOSE_AGGRESSIVE -> AdvancedAdjustments(
                bluffFrequency = 0.3,      // Bluff less
                valueBetSizing = 1.2,      // Slightly bigger value bets
                callDownFrequency = 2.0,   // Call down much more
                trapFrequency = 1.5,       // Trap more with strong hands
                slowPlayFrequency = 1.3,   // Slow play more
                barreling = 0.8,           // Less barreling
                protection = 0.7           // Less protection betting
            )
            
            PlayingStyle.MANIAC -> AdvancedAdjustments(
                bluffFrequency = 0.1,      // Never bluff
                valueBetSizing = 1.8,      // Big value bets
                callDownFrequency = 3.0,   // Call down very often
                trapFrequency = 2.0,       // Trap frequently
                slowPlayFrequency = 1.8,   // Slow play strong hands
                thinValueBets = 2.0,       // Lots of thin value
                protection = 2.0           // Protect against draws
            )
            
            PlayingStyle.ULTRA_TIGHT -> AdvancedAdjustments(
                bluffFrequency = 3.0,      // Bluff very often
                valueBetSizing = 1.2,      // Slightly bigger value
                isolationTendency = 2.0,   // Isolate constantly
                stealFrequency = 2.5,      // Steal very often
                cBetFrequency = 1.8,       // C-bet much more
                barreling = 1.2,           // Barrel more
                foldEquity = 1.8           // More fold equity plays
            )
            
            else -> AdvancedAdjustments.default()
        }
    }
    
    data class AdvancedAdjustments(
        val bluffFrequency: Double = 1.0,
        val valueBetSizing: Double = 1.0,
        val isolationTendency: Double = 1.0,
        val stealFrequency: Double = 1.0,
        val cBetFrequency: Double = 1.0,
        val barreling: Double = 1.0,
        val callDownFrequency: Double = 1.0,
        val trapFrequency: Double = 1.0,
        val slowPlayFrequency: Double = 1.0,
        val thinValueBets: Double = 1.0,
        val protection: Double = 1.0,
        val foldEquity: Double = 1.0
    ) {
        companion object {
            fun default() = AdvancedAdjustments()
        }
    }
    
    /**
     * Check if player is currently on tilt
     */
    fun isTilted(playerId: String): Boolean {
        val profile = playerProfiles[playerId] ?: return false
        return profile.currentTiltLevel > 0.6
    }
    
    /**
     * Get tilt exploitation adjustments
     */
    fun getTiltAdjustments(playerId: String): TiltAdjustments? {
        val profile = playerProfiles[playerId] ?: return null
        if (profile.currentTiltLevel < 0.3) return null
        
        return TiltAdjustments(
            aggressionMultiplier = 1.0 + profile.currentTiltLevel,
            bluffResistance = 1.0 - profile.currentTiltLevel * 0.5,
            callDownMore = profile.currentTiltLevel > 0.5,
            isolateMore = true,
            trapLess = true
        )
    }
    
    data class TiltAdjustments(
        val aggressionMultiplier: Double,
        val bluffResistance: Double,
        val callDownMore: Boolean,
        val isolateMore: Boolean,
        val trapLess: Boolean
    )
    
    /**
     * Predict opponent's likely action
     */
    fun predictAction(
        playerId: String,
        situation: ActionSituation
    ): ActionPrediction {
        val profile = playerProfiles[playerId] ?: return ActionPrediction.default()
        
        if (profile.handsObserved < 20) return ActionPrediction.default()
        
        val posStats = profile.positionStats[situation.position]
        val baselineVPIP = posStats?.vpip ?: profile.vpip
        val baselineAggression = posStats?.aggression ?: profile.aggression
        
        // Adjust for tilt
        val tiltAdjustment = profile.currentTiltLevel
        val adjustedAggression = baselineAggression * (1.0 + tiltAdjustment * 0.5)
        
        return when (situation.actionType) {
            "preflop_open" -> ActionPrediction(
                foldProbability = 1.0 - baselineVPIP,
                callProbability = baselineVPIP * (1.0 - (posStats?.pfr ?: profile.pfr)),
                raiseProbability = posStats?.pfr ?: profile.pfr,
                confidence = min(1.0, profile.handsObserved / 50.0)
            )
            
            "facing_cbet" -> ActionPrediction(
                foldProbability = 1.0 - profile.callCBet,
                callProbability = profile.callCBet * 0.8,
                raiseProbability = profile.callCBet * 0.2,
                confidence = min(1.0, profile.handsObserved / 30.0)
            )
            
            "facing_bet" -> ActionPrediction(
                foldProbability = profile.foldToBet,
                callProbability = (1.0 - profile.foldToBet) * 0.7,
                raiseProbability = (1.0 - profile.foldToBet) * 0.3 * adjustedAggression,
                confidence = min(1.0, profile.handsObserved / 40.0)
            )
            
            else -> ActionPrediction.default()
        }
    }
    
    data class ActionSituation(
        val position: PositionAnalyzer.Position,
        val actionType: String,
        val betSize: Double,
        val potSize: Int,
        val street: String
    )
    
    data class ActionPrediction(
        val foldProbability: Double,
        val callProbability: Double,
        val raiseProbability: Double,
        val confidence: Double
    ) {
        companion object {
            fun default() = ActionPrediction(0.5, 0.3, 0.2, 0.1)
        }
    }
    
    // Private helper methods
    private fun updateBasicStats(profile: PlayerProfile, action: String, street: String, position: PositionAnalyzer.Position) {
        when (action.lowercase()) {
            "fold" -> {
                // Update fold frequencies
            }
            "call" -> {
                if (street == "preflop") {
                    profile.vpip = updateMovingAverage(profile.vpip, 1.0, profile.handsObserved)
                }
            }
            "bet", "raise" -> {
                if (street == "preflop") {
                    profile.vpip = updateMovingAverage(profile.vpip, 1.0, profile.handsObserved)
                    profile.pfr = updateMovingAverage(profile.pfr, 1.0, profile.handsObserved)
                }
                profile.aggression = updateMovingAverage(profile.aggression, 1.0, profile.handsObserved)
            }
        }
    }
    
    private fun updatePositionStats(
        profile: PlayerProfile,
        action: String,
        amount: Int,
        potSize: Int,
        position: PositionAnalyzer.Position,
        street: String
    ) {
        val posStats = profile.positionStats.getOrPut(position) { PositionStats() }
        posStats.handsPlayed++
        
        if (street == "preflop") {
            when (action.lowercase()) {
                "call" -> posStats.vpip = updateMovingAverage(posStats.vpip, 1.0, posStats.handsPlayed)
                "bet", "raise" -> {
                    posStats.vpip = updateMovingAverage(posStats.vpip, 1.0, posStats.handsPlayed)
                    posStats.pfr = updateMovingAverage(posStats.pfr, 1.0, posStats.handsPlayed)
                    if (potSize > 0) {
                        val raiseSize = amount.toDouble() / potSize
                        posStats.openRaiseSize = updateMovingAverage(posStats.openRaiseSize, raiseSize, posStats.handsPlayed)
                    }
                }
            }
        }
    }
    
    private fun updateBettingPatterns(profile: PlayerProfile, action: String, amount: Int, potSize: Int) {
        if (action in listOf("bet", "raise") && potSize > 0) {
            val betSizeRatio = amount.toDouble() / potSize
            val patterns = profile.betSizingPatterns
            
            when {
                betSizeRatio <= patterns.smallBetThreshold -> 
                    patterns.smallBetFreq = updateMovingAverage(patterns.smallBetFreq, 1.0, profile.handsObserved)
                betSizeRatio <= patterns.mediumBetThreshold -> 
                    patterns.mediumBetFreq = updateMovingAverage(patterns.mediumBetFreq, 1.0, profile.handsObserved)
                betSizeRatio <= patterns.largeBetThreshold -> 
                    patterns.largeBetFreq = updateMovingAverage(patterns.largeBetFreq, 1.0, profile.handsObserved)
                else -> 
                    patterns.overbetFreq = updateMovingAverage(patterns.overbetFreq, 1.0, profile.handsObserved)
            }
        }
    }
    
    private fun updateTiltDetection(profile: PlayerProfile, action: String, amount: Int, handResult: String?) {
        // Detect tilt indicators
        var tiltIncrease = 0.0
        
        if (handResult == "loss" || handResult == "bad_beat") {
            profile.consecutiveLosses++
            tiltIncrease = 0.2 * profile.consecutiveLosses
            if (handResult == "bad_beat") {
                tiltIncrease *= 1.5
                profile.lastBadBeat = profile.handsObserved
            }
        } else if (handResult == "win") {
            profile.consecutiveLosses = 0
            tiltIncrease = -0.1 // Tilt decreases when winning
        }
        
        // Aggressive actions when unusual for player
        if (action in listOf("bet", "raise") && profile.aggression < 0.3 && amount > 0) {
            tiltIncrease += 0.1 // Uncharacteristic aggression
        }
        
        profile.currentTiltLevel = (profile.currentTiltLevel + tiltIncrease).coerceIn(0.0, 1.0)
        
        // Tilt naturally decreases over time
        if (profile.handsObserved - profile.lastBadBeat > 10) {
            profile.currentTiltLevel *= 0.95
        }
    }
    
    private fun classifyPlayingStyle(profile: PlayerProfile) {
        if (profile.handsObserved < 20) return
        
        val vpipThreshold = 0.25
        val aggressionThreshold = 0.5
        
        profile.playingStyle = when {
            profile.vpip < 0.15 -> PlayingStyle.ULTRA_TIGHT
            profile.vpip < vpipThreshold && profile.aggression > aggressionThreshold -> PlayingStyle.TIGHT_AGGRESSIVE
            profile.vpip < vpipThreshold && profile.aggression <= aggressionThreshold -> PlayingStyle.TIGHT_PASSIVE
            profile.vpip >= 0.5 && profile.aggression > 0.7 -> PlayingStyle.MANIAC
            profile.vpip >= vpipThreshold && profile.aggression > aggressionThreshold -> PlayingStyle.LOOSE_AGGRESSIVE
            profile.vpip >= vpipThreshold && profile.aggression <= aggressionThreshold -> PlayingStyle.LOOSE_PASSIVE
            else -> PlayingStyle.UNKNOWN
        }
        
        profile.styleConfidence = min(1.0, profile.handsObserved / 50.0)
    }
    
    private fun updateExploitability(profile: PlayerProfile) {
        // Calculate how exploitable the player is
        var exploitability = 1.0
        
        // Less exploitable if they're balanced
        val vpipPfrGap = abs(profile.vpip - profile.pfr)
        exploitability -= vpipPfrGap * 0.5
        
        // Less exploitable if they adapt
        exploitability -= profile.adaptationLevel * 0.3
        
        // More exploitable if they have clear patterns
        if (profile.betSizingPatterns.smallBetFreq > 0.8 || profile.betSizingPatterns.largeBetFreq > 0.8) {
            exploitability += 0.3
        }
        
        // More exploitable if tilted
        exploitability += profile.currentTiltLevel * 0.4
        
        profile.exploitability = exploitability.coerceIn(0.2, 1.0)
    }
    
    private fun updateMovingAverage(current: Double, newValue: Double, count: Int): Double {
        val weight = min(0.2, 1.0 / count)
        return current * (1.0 - weight) + newValue * weight
    }
    
    /**
     * Get comprehensive player analysis
     */
    fun getPlayerAnalysis(playerId: String): String {
        val profile = playerProfiles[playerId] ?: return "No data available for $playerId"
        
        return """
            === ADVANCED PLAYER ANALYSIS: $playerId ===
            Hands Observed: ${profile.handsObserved}
            Playing Style: ${profile.playingStyle} (${(profile.styleConfidence * 100).toInt()}% confidence)
            
            Basic Stats:
            - VPIP: ${(profile.vpip * 100).toInt()}%
            - PFR: ${(profile.pfr * 100).toInt()}%
            - Aggression: ${(profile.aggression * 100).toInt()}%
            - Fold to Bet: ${(profile.foldToBet * 100).toInt()}%
            
            Advanced Metrics:
            - Current Tilt Level: ${(profile.currentTiltLevel * 100).toInt()}%
            - Exploitability: ${(profile.exploitability * 100).toInt()}%
            - Consecutive Losses: ${profile.consecutiveLosses}
            
            Betting Patterns:
            - Small Bets: ${(profile.betSizingPatterns.smallBetFreq * 100).toInt()}%
            - Large Bets: ${(profile.betSizingPatterns.largeBetFreq * 100).toInt()}%
            - Overbets: ${(profile.betSizingPatterns.overbetFreq * 100).toInt()}%
            
            Recommendations: ${getExploitationRecommendations(profile)}
        """.trimIndent()
    }
    
    private fun getExploitationRecommendations(profile: PlayerProfile): String {
        val recs = mutableListOf<String>()
        
        if (profile.currentTiltLevel > 0.5) recs.add("EXPLOIT TILT - call down wider, isolate more")
        if (profile.foldToBet > 0.7) recs.add("BLUFF MORE - high fold frequency")
        if (profile.vpip > 0.5) recs.add("VALUE BET WIDER - calling station tendencies")
        if (profile.aggression < 0.3) recs.add("STEAL BLINDS - passive player")
        if (profile.exploitability > 0.8) recs.add("HIGH PRIORITY TARGET - very exploitable")
        
        return if (recs.isEmpty()) "Balanced opponent - play GTO" else recs.joinToString(", ")
    }
}
