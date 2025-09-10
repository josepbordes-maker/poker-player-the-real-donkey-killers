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
    
    @Test
    fun `processShowdown analyzes hands with community cards`() {
        val gameId = "showdown_test"
        val gameState = JSONObject()
        gameState.put("game_id", gameId)
        
        // Create a showdown scenario with community cards
        val communityCards = cards("A" to "spades", "K" to "hearts", "Q" to "clubs", "J" to "diamonds", "10" to "spades")
        gameState.put("community_cards", communityCards)
        
        val players = JSONArray()
        
        // Player with royal flush
        val player1 = JSONObject()
        player1.put("name", "RoyalFlushPlayer")
        player1.put("stack", 2000)
        player1.put("hole_cards", cards("A" to "hearts", "K" to "spades")) // Will make royal flush with community
        players.put(player1)
        
        // Player with pair
        val player2 = JSONObject()
        player2.put("name", "PairPlayer")
        player2.put("stack", 1500)
        player2.put("hole_cards", cards("2" to "hearts", "2" to "spades"))
        players.put(player2)
        
        gameState.put("players", players)
        
        // This should analyze and print the showdown without throwing exceptions
        gameStateManager.processShowdown(gameState)
        
        // Memory should be cleared after analysis
        assertTrue(gameStateManager.getSeenCards(gameId).isEmpty())
    }
    
    @Test
    fun `processShowdown handles pre-flop showdown`() {
        val gameId = "preflop_test"
        val gameState = JSONObject()
        gameState.put("game_id", gameId)
        
        // No community cards (pre-flop all-in scenario)
        val players = JSONArray()
        
        val player1 = JSONObject()
        player1.put("name", "PocketAces")
        player1.put("stack", 0)
        player1.put("hole_cards", cards("A" to "spades", "A" to "hearts"))
        players.put(player1)
        
        val player2 = JSONObject()
        player2.put("name", "PocketKings")
        player2.put("stack", 0)
        player2.put("hole_cards", cards("K" to "spades", "K" to "hearts"))
        players.put(player2)
        
        gameState.put("players", players)
        
        // Should handle pre-flop analysis
        gameStateManager.processShowdown(gameState)
        
        assertTrue(gameStateManager.getSeenCards(gameId).isEmpty())
    }
    
    @Test
    fun `processShowdown handles mixed scenarios`() {
        val gameId = "mixed_test"
        val gameState = JSONObject()
        gameState.put("game_id", gameId)
        
        val communityCards = cards("7" to "spades", "8" to "hearts", "9" to "clubs")
        gameState.put("community_cards", communityCards)
        
        val players = JSONArray()
        
        // Player with completed hand
        val player1 = JSONObject()
        player1.put("name", "Player1")
        player1.put("stack", 1000)
        player1.put("hole_cards", cards("6" to "diamonds", "10" to "spades"))
        players.put(player1)
        
        // Player without hole cards revealed (folded)
        val player2 = JSONObject()
        player2.put("name", "FoldedPlayer")
        player2.put("stack", 1200)
        // No hole_cards
        players.put(player2)
        
        // Player with hole cards but no name
        val player3 = JSONObject()
        player3.put("stack", 800)
        player3.put("hole_cards", cards("A" to "spades", "A" to "hearts"))
        players.put(player3)
        
        gameState.put("players", players)
        
        // Should handle mixed scenario gracefully
        gameStateManager.processShowdown(gameState)
        
        assertTrue(gameStateManager.getSeenCards(gameId).isEmpty())
    }
}
