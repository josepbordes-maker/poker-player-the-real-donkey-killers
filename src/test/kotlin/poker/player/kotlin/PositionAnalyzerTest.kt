package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PositionAnalyzerTest {
    
    private val positionAnalyzer = PositionAnalyzer()
    
    private fun createPlayers(count: Int): JSONArray {
        val players = JSONArray()
        for (i in 0 until count) {
            players.put(JSONObject().put("id", i))
        }
        return players
    }
    
    @Test
    fun `getPosition identifies blinds correctly in 6-handed game`() {
        val players = createPlayers(6)
        val dealer = 0
        
        // Small blind (position 1 after dealer)
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 1, dealer))
        // Big blind (position 2 after dealer)  
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 2, dealer))
    }
    
    @Test
    fun `getPosition identifies late position correctly in 6-handed game`() {
        val players = createPlayers(6)
        val dealer = 0
        
        // Dealer (button)
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 0, dealer))
        // Cutoff (position before dealer)
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 5, dealer))
    }
    
    @Test
    fun `getPosition identifies middle position correctly in 6-handed game`() {
        val players = createPlayers(6)
        val dealer = 0
        
        // Hijack (2 seats before dealer, but in 6-handed this is late position)
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 4, dealer))
    }
    
    @Test
    fun `getPosition identifies early position correctly in 6-handed game`() {
        val players = createPlayers(6)
        val dealer = 0
        
        // Under the gun (3 seats after dealer) - in 6-handed this is middle
        assertEquals(PositionAnalyzer.Position.MIDDLE, positionAnalyzer.getPosition(players, 3, dealer))
    }
    
    @Test
    fun `getPosition works with different dealer positions`() {
        val players = createPlayers(6)
        val dealer = 3
        
        // Small blind (1 after dealer)
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 4, dealer))
        // Big blind (2 after dealer)
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 5, dealer))
        // Dealer
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 3, dealer))
        // Cutoff
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 2, dealer))
    }
    
    @Test
    fun `getPosition handles heads-up correctly`() {
        val players = createPlayers(2)
        val dealer = 0
        
        // In heads-up, dealer is late position (button)
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 0, dealer))
        // Other player is big blind
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 1, dealer))
    }
    
    @Test
    fun `getPosition handles 9-handed game correctly`() {
        val players = createPlayers(9)
        val dealer = 0
        
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 1, dealer)) // SB
        assertEquals(PositionAnalyzer.Position.BLINDS, positionAnalyzer.getPosition(players, 2, dealer)) // BB
        assertEquals(PositionAnalyzer.Position.EARLY, positionAnalyzer.getPosition(players, 3, dealer)) // UTG
        assertEquals(PositionAnalyzer.Position.MIDDLE, positionAnalyzer.getPosition(players, 4, dealer)) // UTG+1
        assertEquals(PositionAnalyzer.Position.MIDDLE, positionAnalyzer.getPosition(players, 5, dealer)) // MP
        assertEquals(PositionAnalyzer.Position.MIDDLE, positionAnalyzer.getPosition(players, 6, dealer)) // MP+1
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 7, dealer)) // CO
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 8, dealer)) // HJ
        assertEquals(PositionAnalyzer.Position.LATE, positionAnalyzer.getPosition(players, 0, dealer)) // BTN
    }
    
    @Test
    fun `getSmallBetThreshold returns correct values by position`() {
        val pot = 100
        
        assertEquals(25, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.EARLY, pot))
        assertEquals(33, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.MIDDLE, pot))
        assertEquals(50, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.LATE, pot))
        assertEquals(50, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.BLINDS, pot))
    }
    
    @Test
    fun `getSmallBetThreshold scales with pot size`() {
        assertEquals(50, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.EARLY, 200))
        assertEquals(66, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.MIDDLE, 200))
        assertEquals(100, positionAnalyzer.getSmallBetThreshold(PositionAnalyzer.Position.LATE, 200))
    }
    
    @Test
    fun `getOpenRaiseSize returns correct values by position`() {
        val smallBlind = 10
        
        assertEquals(60, positionAnalyzer.getOpenRaiseSize(PositionAnalyzer.Position.EARLY, smallBlind))
        assertEquals(40, positionAnalyzer.getOpenRaiseSize(PositionAnalyzer.Position.MIDDLE, smallBlind))
        assertEquals(40, positionAnalyzer.getOpenRaiseSize(PositionAnalyzer.Position.LATE, smallBlind))
        assertEquals(40, positionAnalyzer.getOpenRaiseSize(PositionAnalyzer.Position.BLINDS, smallBlind))
    }
    
    @Test
    fun `getOpenRaiseSize scales with blind size`() {
        assertEquals(80, positionAnalyzer.getOpenRaiseSize(PositionAnalyzer.Position.MIDDLE, 20))
        assertEquals(80, positionAnalyzer.getOpenRaiseSize(PositionAnalyzer.Position.LATE, 20))
        assertEquals(120, positionAnalyzer.getStrongHandRaiseSize(20))
    }
    
    @Test
    fun `getStrongHandRaiseSize returns consistent 6x small blind`() {
        assertEquals(60, positionAnalyzer.getStrongHandRaiseSize(10))
        assertEquals(120, positionAnalyzer.getStrongHandRaiseSize(20))
        assertEquals(30, positionAnalyzer.getStrongHandRaiseSize(5))
    }
}
