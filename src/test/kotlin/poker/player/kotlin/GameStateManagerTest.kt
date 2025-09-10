package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameStateManagerTest {
    
    private val gameStateManager = GameStateManager()
    
    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)
    
    private fun cards(vararg cardPairs: Pair<String, String>): JSONArray {
        val array = JSONArray()
        cardPairs.forEach { (rank, suit) -> array.put(card(rank, suit)) }
        return array
    }
    
    @Test
    fun `addSeenCards stores cards correctly`() {
        val gameId = "game1"
        val cards = cards("A" to "spades", "K" to "hearts")
        
        gameStateManager.addSeenCards(gameId, cards)
        
        val seenCards = gameStateManager.getSeenCards(gameId)
        assertEquals(2, seenCards.size)
        assertTrue(seenCards.contains("A-spades"))
        assertTrue(seenCards.contains("K-hearts"))
    }
    
    @Test
    fun `addSeenCards accumulates cards across multiple calls`() {
        val gameId = "game1"
        val firstCards = cards("A" to "spades", "K" to "hearts")
        val secondCards = cards("Q" to "clubs", "J" to "diamonds")
        
        gameStateManager.addSeenCards(gameId, firstCards)
        gameStateManager.addSeenCards(gameId, secondCards)
        
        val seenCards = gameStateManager.getSeenCards(gameId)
        assertEquals(4, seenCards.size)
        assertTrue(seenCards.contains("A-spades"))
        assertTrue(seenCards.contains("K-hearts"))
        assertTrue(seenCards.contains("Q-clubs"))
        assertTrue(seenCards.contains("J-diamonds"))
    }
    
    @Test
    fun `addSeenCards handles duplicate cards correctly`() {
        val gameId = "game1"
        val cards = cards("A" to "spades", "A" to "spades", "K" to "hearts")
        
        gameStateManager.addSeenCards(gameId, cards)
        
        val seenCards = gameStateManager.getSeenCards(gameId)
        assertEquals(2, seenCards.size) // Set should contain only unique cards
        assertTrue(seenCards.contains("A-spades"))
        assertTrue(seenCards.contains("K-hearts"))
    }
    
    @Test
    fun `addSeenCards handles empty card array`() {
        val gameId = "game1"
        val emptyCards = JSONArray()
        
        gameStateManager.addSeenCards(gameId, emptyCards)
        
        val seenCards = gameStateManager.getSeenCards(gameId)
        assertEquals(0, seenCards.size)
    }
    
    @Test
    fun `getSeenCards returns empty set for unknown game`() {
        val seenCards = gameStateManager.getSeenCards("unknown_game")
        assertTrue(seenCards.isEmpty())
    }
    
    @Test
    fun `different games track cards separately`() {
        val game1 = "game1"
        val game2 = "game2"
        val cards1 = cards("A" to "spades", "K" to "hearts")
        val cards2 = cards("Q" to "clubs", "J" to "diamonds")
        
        gameStateManager.addSeenCards(game1, cards1)
        gameStateManager.addSeenCards(game2, cards2)
        
        val seenCards1 = gameStateManager.getSeenCards(game1)
        val seenCards2 = gameStateManager.getSeenCards(game2)
        
        assertEquals(2, seenCards1.size)
        assertEquals(2, seenCards2.size)
        assertTrue(seenCards1.contains("A-spades"))
        assertTrue(seenCards1.contains("K-hearts"))
        assertFalse(seenCards1.contains("Q-clubs"))
        assertFalse(seenCards1.contains("J-diamonds"))
        
        assertTrue(seenCards2.contains("Q-clubs"))
        assertTrue(seenCards2.contains("J-diamonds"))
        assertFalse(seenCards2.contains("A-spades"))
        assertFalse(seenCards2.contains("K-hearts"))
    }
    
    @Test
    fun `clearGameMemory removes game data`() {
        val gameId = "game1"
        val cards = cards("A" to "spades", "K" to "hearts")
        
        gameStateManager.addSeenCards(gameId, cards)
        assertEquals(2, gameStateManager.getSeenCards(gameId).size)
        
        gameStateManager.clearGameMemory(gameId)
        assertTrue(gameStateManager.getSeenCards(gameId).isEmpty())
    }
    
    @Test
    fun `clearGameMemory handles unknown game safely`() {
        // Should not throw exception
        gameStateManager.clearGameMemory("unknown_game")
        assertTrue(gameStateManager.getSeenCards("unknown_game").isEmpty())
    }
    
    @Test
    fun `clearGameMemory handles empty gameId safely`() {
        // Should not throw exception
        gameStateManager.clearGameMemory("")
        assertTrue(gameStateManager.getSeenCards("").isEmpty())
    }
    
    @Test
    fun `processShowdown captures all player cards and clears memory`() {
        val gameId = "test_game"
        val player1Cards = cards("A" to "spades", "K" to "hearts")
        val player2Cards = cards("Q" to "clubs", "J" to "diamonds")
        
        // First, add some cards to the game state
        gameStateManager.addSeenCards(gameId, cards("10" to "spades", "9" to "hearts"))
        assertEquals(2, gameStateManager.getSeenCards(gameId).size)
        
        // Create showdown game state
        val gameState = JSONObject()
        gameState.put("game_id", gameId)
        
        val players = JSONArray()
        val player1 = JSONObject().put("hole_cards", player1Cards)
        val player2 = JSONObject().put("hole_cards", player2Cards)
        players.put(player1)
        players.put(player2)
        gameState.put("players", players)
        
        gameStateManager.processShowdown(gameState)
        
        // Memory should be cleared after showdown
        assertTrue(gameStateManager.getSeenCards(gameId).isEmpty())
    }
    
    @Test
    fun `processShowdown handles missing game_id`() {
        val gameState = JSONObject()
        val players = JSONArray()
        val player1 = JSONObject().put("hole_cards", cards("A" to "spades", "K" to "hearts"))
        players.put(player1)
        gameState.put("players", players)
        
        // Should not throw exception
        gameStateManager.processShowdown(gameState)
    }
    
    @Test
    fun `processShowdown handles missing players`() {
        val gameState = JSONObject()
        gameState.put("game_id", "test_game")
        
        // Should not throw exception
        gameStateManager.processShowdown(gameState)
    }
    
    @Test
    fun `processShowdown handles players without hole cards`() {
        val gameId = "test_game"
        val gameState = JSONObject()
        gameState.put("game_id", gameId)
        
        val players = JSONArray()
        val player1 = JSONObject() // No hole_cards
        val player2 = JSONObject().put("hole_cards", cards("A" to "spades", "K" to "hearts"))
        players.put(player1)
        players.put(player2)
        gameState.put("players", players)
        
        // Should not throw exception
        gameStateManager.processShowdown(gameState)
        
        // Memory should still be cleared
        assertTrue(gameStateManager.getSeenCards(gameId).isEmpty())
    }
}
