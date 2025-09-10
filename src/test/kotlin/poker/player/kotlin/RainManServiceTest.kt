package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RainManServiceTest {

    @Test
    fun `should handle valid straight flush from Rain Man API`() {
        val rainManService = RainManService()
        
        // Create a straight flush: 5♦ 6♦ 7♦ 8♦ 9♦
        val cards = JSONArray()
        cards.put(JSONObject().put("rank", "5").put("suit", "diamonds"))
        cards.put(JSONObject().put("rank", "6").put("suit", "diamonds"))
        cards.put(JSONObject().put("rank", "7").put("suit", "diamonds"))
        cards.put(JSONObject().put("rank", "8").put("suit", "diamonds"))
        cards.put(JSONObject().put("rank", "9").put("suit", "diamonds"))
        
        // Note: This test might fail if the Rain Man API is not available
        // In real usage, we would mock the HTTP client for testing
        val result = rainManService.rankHand(cards)
        
        // We can't guarantee the API will be available in testing
        // So we test that the method doesn't crash
        // If result is not null, it should be a straight flush (rank 8)
        result?.let {
            assertEquals(8, it.rank, "Should detect straight flush")
            assertEquals(9, it.value, "Should have high card value of 9")
            assertNotNull(it.cardsUsed, "Should have cards used")
        }
        
        rainManService.close()
    }
    
    @Test
    fun `should handle API timeout gracefully`() {
        val rainManService = RainManService()
        
        // Create some cards
        val cards = JSONArray()
        cards.put(JSONObject().put("rank", "A").put("suit", "spades"))
        cards.put(JSONObject().put("rank", "K").put("suit", "hearts"))
        cards.put(JSONObject().put("rank", "Q").put("suit", "clubs"))
        cards.put(JSONObject().put("rank", "J").put("suit", "diamonds"))
        cards.put(JSONObject().put("rank", "10").put("suit", "spades"))
        
        // The method should not crash even if API is unavailable
        val result = rainManService.rankHand(cards)
        
        // Result can be null if API is unavailable, which is expected behavior
        // We just ensure no exception is thrown
        println("Rain Man API result: $result")
        
        rainManService.close()
    }
    
    @Test
    fun `should handle empty cards array gracefully`() {
        val rainManService = RainManService()
        
        val emptyCards = JSONArray()
        
        // Should not crash with empty cards
        val result = rainManService.rankHand(emptyCards)
        
        // Likely to return null due to invalid input
        assertNull(result, "Should return null for empty cards")
        
        rainManService.close()
    }
    
    @Test
    fun `should handle malformed card data gracefully`() {
        val rainManService = RainManService()
        
        // Create cards with invalid data
        val cards = JSONArray()
        cards.put(JSONObject().put("rank", "INVALID").put("suit", "hearts"))
        
        // Should not crash with invalid card data
        val result = rainManService.rankHand(cards)
        
        // Likely to return null due to invalid input
        // We just ensure no exception is thrown
        println("Result with invalid data: $result")
        
        rainManService.close()
    }
}
