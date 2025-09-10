package poker.player.kotlin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class OpponentModelingTest {
    
    private val opponentModeling = OpponentModeling()
    
    @Test
    fun `should classify tight aggressive player correctly`() {
        val playerId = "tight_ag_player"
        
        // Simulate tight-aggressive player: few hands, high aggression when playing
        // Need VPIP < 0.2 for tight classification and high aggression
        repeat(22) { 
            opponentModeling.recordAction(playerId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        repeat(3) { 
            opponentModeling.recordAction(playerId, "raise", 40, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        // Total: 25 hands, 3 played (VPIP = 12%), 3 raises out of 3 actions (aggression = 100%)
        
        val playerType = opponentModeling.getPlayerType(playerId)
        assertEquals(OpponentModeling.PlayerType.TIGHT_AGGRESSIVE, playerType)
        
        val adjustments = opponentModeling.getStrategicAdjustments(playerId)
        assertTrue(adjustments.bluffFrequency < 1.0) // Should bluff less against TAG
        assertTrue(adjustments.callThreshold > 1.0) // Should call more (respect their bets)
    }
    
    @Test
    fun `should classify loose passive calling station correctly`() {
        val playerId = "calling_station"
        
        // Simulate calling station: plays many hands, low aggression
        repeat(10) { 
            opponentModeling.recordAction(playerId, "call", 20, 100, PositionAnalyzer.Position.MIDDLE, "preflop", 1000)
        }
        repeat(8) { 
            opponentModeling.recordAction(playerId, "call", 30, 120, PositionAnalyzer.Position.LATE, "flop", 1000)
        }
        repeat(2) { 
            opponentModeling.recordAction(playerId, "raise", 50, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        
        val playerType = opponentModeling.getPlayerType(playerId)
        assertEquals(OpponentModeling.PlayerType.LOOSE_PASSIVE, playerType)
        
        val adjustments = opponentModeling.getStrategicAdjustments(playerId)
        assertTrue(adjustments.bluffFrequency < 1.0) // Should rarely bluff calling stations
        assertTrue(adjustments.valueBetSizing > 1.0) // Should bet bigger for value
        assertTrue(adjustments.isolationTendency > 1.0) // Should isolate them more
    }
    
    @Test
    fun `should classify loose aggressive maniac correctly`() {
        val playerId = "maniac"
        
        // Simulate LAG player: plays many hands with high aggression
        repeat(8) { 
            opponentModeling.recordAction(playerId, "raise", 60, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        repeat(6) { 
            opponentModeling.recordAction(playerId, "call", 30, 120, PositionAnalyzer.Position.MIDDLE, "preflop", 1000)
        }
        repeat(4) { 
            opponentModeling.recordAction(playerId, "raise", 80, 200, PositionAnalyzer.Position.LATE, "flop", 1000)
        }
        
        val playerType = opponentModeling.getPlayerType(playerId)
        assertEquals(OpponentModeling.PlayerType.LOOSE_AGGRESSIVE, playerType)
        
        val adjustments = opponentModeling.getStrategicAdjustments(playerId)
        assertTrue(adjustments.callThreshold > 1.0) // Should call more (they bluff often)
        assertTrue(adjustments.bluffFrequency < 1.0) // Should bluff less against aggro players
    }
    
    @Test
    fun `should detect tilt behavior`() {
        val playerId = "tilted_player"
        
        // Build normal baseline stats
        repeat(15) { 
            opponentModeling.recordAction(playerId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        repeat(3) { 
            opponentModeling.recordAction(playerId, "call", 20, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        repeat(2) { 
            opponentModeling.recordAction(playerId, "raise", 40, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        
        // Simulate recent aggressive behavior (tilt)
        repeat(8) { 
            opponentModeling.recordAction(playerId, "raise", 80, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        
        assertTrue(opponentModeling.isPlayerOnTilt(playerId))
    }
    
    @Test
    fun `should categorize stack sizes correctly`() {
        val shortStack = opponentModeling.getStackSizeStrategy(280, 20) // 14 BB (< 15)
        val mediumStack = opponentModeling.getStackSizeStrategy(800, 20) // 20 BB (15-50)  
        val deepStack = opponentModeling.getStackSizeStrategy(2000, 20) // 50 BB (> 50)
        
        assertEquals(OpponentModeling.StackStrategy.SHORT_STACK, shortStack)
        assertEquals(OpponentModeling.StackStrategy.MEDIUM_STACK, mediumStack)
        assertEquals(OpponentModeling.StackStrategy.DEEP_STACK, deepStack)
    }
    
    @Test
    fun `should provide unknown classification for insufficient data`() {
        val playerId = "new_player"
        
        // Record only a few actions
        repeat(3) { 
            opponentModeling.recordAction(playerId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        
        val playerType = opponentModeling.getPlayerType(playerId)
        assertEquals(OpponentModeling.PlayerType.UNKNOWN, playerType)
    }
    
    @Test
    fun `should track bet sizing patterns`() {
        val playerId = "bet_sizing_player"
        
        // Record various bet sizes relative to pot
        opponentModeling.recordAction(playerId, "raise", 50, 100, PositionAnalyzer.Position.LATE, "preflop", 1000) // 0.5x pot
        opponentModeling.recordAction(playerId, "raise", 100, 100, PositionAnalyzer.Position.LATE, "flop", 1000) // 1.0x pot
        opponentModeling.recordAction(playerId, "raise", 75, 100, PositionAnalyzer.Position.LATE, "turn", 1000) // 0.75x pot
        
        val avgBetSizing = opponentModeling.getAverageBetSizing(playerId)
        assertTrue(avgBetSizing > 0.7) // Should be around 0.75
        assertTrue(avgBetSizing < 0.8)
    }
    
    @Test
    fun `should handle edge cases gracefully`() {
        val playerId = "edge_case_player"
        
        // Test with no data
        val emptyType = opponentModeling.getPlayerType(playerId)
        assertEquals(OpponentModeling.PlayerType.UNKNOWN, emptyType)
        
        val emptyAnalysis = opponentModeling.getPlayerAnalysis(playerId)
        assertTrue(emptyAnalysis.contains("No data available"))
        
        // Test tilt detection with insufficient data
        assertFalse(opponentModeling.isPlayerOnTilt(playerId))
    }
    
    @Test
    fun `should clear old data to prevent memory leaks`() {
        val playerId = "temp_player"
        
        // Add minimal data (below threshold)
        repeat(3) { 
            opponentModeling.recordAction(playerId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        
        // Clear old data
        opponentModeling.clearOldData()
        
        // Player should be removed due to insufficient hands
        val playerType = opponentModeling.getPlayerType(playerId)
        assertEquals(OpponentModeling.PlayerType.UNKNOWN, playerType)
    }
}
