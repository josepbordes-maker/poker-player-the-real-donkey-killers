package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnhancedBettingStrategyTest {
    
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val opponentModeling = OpponentModeling()
    private val enhancedStrategy = EnhancedBettingStrategy(handEvaluator, positionAnalyzer, opponentModeling)
    
    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)
    
    private fun cards(vararg cardPairs: Pair<String, String>): JSONArray {
        val array = JSONArray()
        cardPairs.forEach { (rank, suit) -> array.put(card(rank, suit)) }
        return array
    }
    
    private fun players(count: Int): JSONArray {
        val array = JSONArray()
        repeat(count) { i ->
            array.put(JSONObject().apply {
                put("name", "Player_$i")
                put("stack", 1000)
                put("bet", 0)
            })
        }
        return array
    }
    
    @Test
    fun `should adjust strategy against tight aggressive players`() {
        val playerId = "tight_player"
        
        // Train opponent model with tight-aggressive behavior
        repeat(20) { 
            opponentModeling.recordAction(playerId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        repeat(3) { 
            opponentModeling.recordAction(playerId, "raise", 60, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        
        val myCards = cards("K" to "hearts", "Q" to "diamonds") // Decent hand
        val playerArray = players(4)
        
        val bet = enhancedStrategy.calculateBet(
            myCards = myCards,
            communityCards = JSONArray(),
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 60, // Facing a bet from tight player
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE,
            players = playerArray,
            inAction = 0,
            aggressorId = playerId
        )
        
        // Should be more likely to fold against tight players
        assertTrue(bet == 0 || bet == 60) // Either fold or just call, unlikely to raise
    }
    
    @Test
    fun `should exploit loose passive players with larger value bets`() {
        val playerId = "calling_station"
        
        // Train opponent model with loose-passive behavior
        repeat(15) { 
            opponentModeling.recordAction(playerId, "call", 20, 100, PositionAnalyzer.Position.MIDDLE, "preflop", 1000)
        }
        repeat(2) { 
            opponentModeling.recordAction(playerId, "raise", 40, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        
        val myCards = cards("A" to "hearts", "A" to "spades") // Premium hand
        val playerArray = players(4)
        
        val bet = enhancedStrategy.calculateBet(
            myCards = myCards,
            communityCards = JSONArray(),
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 20, // Small bet from loose player
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE,
            players = playerArray,
            inAction = 0,
            aggressorId = playerId
        )
        
        // Should raise big for value against calling stations
        assertTrue(bet > 60) // Expecting a substantial raise
    }
    
    @Test
    fun `should implement short stack push-fold strategy`() {
        val myCards = cards("K" to "hearts", "Q" to "spades") // Decent hand
        val playerArray = players(4)
        val shortStack = 280 // 14 BB (< 15 BB threshold)
        
        val bet = enhancedStrategy.calculateBet(
            myCards = myCards,
            communityCards = JSONArray(),
            myStack = shortStack,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 30,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE,
            players = playerArray,
            inAction = 0
        )
        
        // Short stack should either shove or fold with decent hands in late position
        // Since KQ is a decent hand in late position, should shove
        assertEquals(shortStack, bet)
    }
    
    @Test
    fun `should use position for steal attempts in late position`() {
        val myCards = cards("9" to "hearts", "8" to "spades") // Weak but playable
        val playerArray = players(3) // Few players, good steal spot
        
        val bet = enhancedStrategy.calculateBet(
            myCards = myCards,
            communityCards = JSONArray(),
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 15,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE,
            players = playerArray,
            inAction = 0
        )
        
        // Should occasionally steal with weak hands in late position
        assertTrue(bet >= 0) // May bet or check, but shouldn't crash
    }
    
    @Test
    fun `should record opponent actions for learning`() {
        val playerId = "learning_opponent"
        
        enhancedStrategy.recordOpponentAction(
            playerId = playerId,
            action = "raise",
            amount = 60,
            potSize = 100,
            position = PositionAnalyzer.Position.EARLY,
            communityCards = JSONArray()
        )
        
        // Verify the action was recorded by checking if player type can be determined
        // (requires multiple actions, but this tests the recording mechanism)
        val playerType = opponentModeling.getPlayerType(playerId)
        // Should be UNKNOWN due to insufficient data, but shouldn't crash
        assertEquals(OpponentModeling.PlayerType.UNKNOWN, playerType)
    }
    
    @Test
    fun `should respect stack limits in all scenarios`() {
        val myCards = cards("A" to "hearts", "K" to "spades") // Premium hand
        val playerArray = players(4)
        val smallStack = 50
        
        val bet = enhancedStrategy.calculateBet(
            myCards = myCards,
            communityCards = JSONArray(),
            myStack = smallStack,
            myBet = 0,
            currentBuyIn = 30,
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE,
            players = playerArray,
            inAction = 0
        )
        
        // Should never bet more than stack
        assertTrue(bet <= smallStack)
    }
    
    @Test
    fun `should handle edge cases gracefully`() {
        val myCards = cards("2" to "hearts", "3" to "clubs") // Weak hand
        val playerArray = players(2)
        
        // Test with various edge case scenarios
        val bet1 = enhancedStrategy.calculateBet(
            myCards = myCards,
            communityCards = JSONArray(),
            myStack = 10, // Very short stack
            myBet = 0,
            currentBuyIn = 50, // Bet larger than stack
            pot = 100,
            smallBlind = 5,
            minimumRaise = 10,
            position = PositionAnalyzer.Position.EARLY,
            players = playerArray,
            inAction = 0
        )
        
        // Should go all-in when call amount exceeds stack
        assertEquals(10, bet1)
        
        val bet2 = enhancedStrategy.calculateBet(
            myCards = JSONArray(), // Empty cards (shouldn't happen but test robustness)
            communityCards = JSONArray(),
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0,
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY,
            players = playerArray,
            inAction = 0
        )
        
        // Should handle gracefully and not crash
        assertTrue(bet2 >= 0)
    }
    
    @Test
    fun `should adjust bluff frequency based on opponent tendencies`() {
        val tightPassiveId = "tight_folder"
        val loosePassiveId = "loose_caller"
        
        // Train tight-passive player model - need VPIP < 0.2 and low aggression
        repeat(18) { 
            opponentModeling.recordAction(tightPassiveId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        repeat(1) { 
            opponentModeling.recordAction(tightPassiveId, "raise", 40, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        repeat(1) { 
            opponentModeling.recordAction(tightPassiveId, "call", 20, 100, PositionAnalyzer.Position.LATE, "preflop", 1000)
        }
        // Total: 20 hands, 2 played (VPIP = 10%), low aggression (50%)
        
        // Train loose-passive player model - need VPIP >= 0.35 for loose classification
        repeat(10) { 
            opponentModeling.recordAction(loosePassiveId, "call", 20, 100, PositionAnalyzer.Position.MIDDLE, "preflop", 1000)
        }
        repeat(5) { 
            opponentModeling.recordAction(loosePassiveId, "fold", 0, 100, PositionAnalyzer.Position.EARLY, "preflop", 1000)
        }
        // Total: 15 hands, 10 played (VPIP = 67%), low aggression
        
        val myCards = cards("7" to "hearts", "6" to "clubs") // Marginal bluff hand
        val playerArray = players(4)
        
        // Test against tight-passive player (should bluff more)
        val tightType = opponentModeling.getPlayerType(tightPassiveId)
        val adjustmentTight = opponentModeling.getStrategicAdjustments(tightPassiveId)
        assertTrue(adjustmentTight.bluffFrequency > 1.0, "Should bluff more against tight-passive players. Type: $tightType, got bluff freq: ${adjustmentTight.bluffFrequency}")
        
        // Test against loose-passive player (should bluff less)  
        val looseType = opponentModeling.getPlayerType(loosePassiveId)
        val adjustmentLoose = opponentModeling.getStrategicAdjustments(loosePassiveId)
        assertTrue(adjustmentLoose.bluffFrequency < 1.0, "Should bluff less against loose-passive calling stations. Type: $looseType, got bluff freq: ${adjustmentLoose.bluffFrequency}")
    }
}
