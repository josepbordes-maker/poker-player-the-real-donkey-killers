package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class HandEvaluatorTest {
    
    private val handEvaluator = HandEvaluator()
    
    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)
    
    private fun cards(vararg cardPairs: Pair<String, String>): JSONArray {
        val array = JSONArray()
        cardPairs.forEach { (rank, suit) -> array.put(card(rank, suit)) }
        return array
    }
    
    @Test
    fun `hasStrongHand identifies high pairs correctly`() {
        assertTrue(handEvaluator.hasStrongHand(cards("A" to "spades", "A" to "hearts")))
        assertTrue(handEvaluator.hasStrongHand(cards("K" to "spades", "K" to "hearts")))
        assertTrue(handEvaluator.hasStrongHand(cards("Q" to "spades", "Q" to "hearts")))
        assertTrue(handEvaluator.hasStrongHand(cards("J" to "spades", "J" to "hearts")))
        assertTrue(handEvaluator.hasStrongHand(cards("10" to "spades", "10" to "hearts")))
    }
    
    @Test
    fun `hasStrongHand rejects low pairs`() {
        assertFalse(handEvaluator.hasStrongHand(cards("9" to "spades", "9" to "hearts")))
        assertFalse(handEvaluator.hasStrongHand(cards("8" to "spades", "8" to "hearts")))
        assertFalse(handEvaluator.hasStrongHand(cards("2" to "spades", "2" to "hearts")))
    }
    
    @Test
    fun `hasStrongHand identifies AK correctly`() {
        assertTrue(handEvaluator.hasStrongHand(cards("A" to "spades", "K" to "hearts")))
        assertTrue(handEvaluator.hasStrongHand(cards("K" to "spades", "A" to "hearts")))
    }
    
    @Test
    fun `hasStrongHand identifies AQ correctly`() {
        assertTrue(handEvaluator.hasStrongHand(cards("A" to "spades", "Q" to "hearts")))
        assertTrue(handEvaluator.hasStrongHand(cards("Q" to "spades", "A" to "hearts")))
    }
    
    @Test
    fun `hasStrongHand rejects other ace combinations`() {
        assertFalse(handEvaluator.hasStrongHand(cards("A" to "spades", "J" to "hearts")))
        assertFalse(handEvaluator.hasStrongHand(cards("A" to "spades", "10" to "hearts")))
        assertFalse(handEvaluator.hasStrongHand(cards("A" to "spades", "9" to "hearts")))
    }
    
    @Test
    fun `hasStrongHand handles invalid input`() {
        assertFalse(handEvaluator.hasStrongHand(JSONArray()))
        assertFalse(handEvaluator.hasStrongHand(cards("A" to "spades")))
    }
    
    @Test
    fun `hasDecentHand identifies any pair`() {
        assertTrue(handEvaluator.hasDecentHand(cards("2" to "spades", "2" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("9" to "spades", "9" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("A" to "spades", "A" to "hearts")))
    }
    
    @Test
    fun `hasDecentHand identifies high cards`() {
        assertFalse(handEvaluator.hasDecentHand(cards("9" to "spades", "2" to "hearts"))) // 9 no longer decent
        assertTrue(handEvaluator.hasDecentHand(cards("10" to "spades", "3" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("J" to "spades", "4" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("Q" to "spades", "5" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("K" to "spades", "6" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("A" to "spades", "7" to "hearts")))
    }
    
    @Test
    fun `hasDecentHand identifies suited connectors`() {
        assertTrue(handEvaluator.hasDecentHand(cards("7" to "spades", "8" to "spades")))
        assertTrue(handEvaluator.hasDecentHand(cards("5" to "hearts", "7" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("10" to "clubs", "8" to "clubs")))
    }
    
    @Test
    fun `hasDecentHand identifies any ace or king`() {
        assertTrue(handEvaluator.hasDecentHand(cards("A" to "spades", "2" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("K" to "spades", "3" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("7" to "spades", "A" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("8" to "spades", "K" to "hearts")))
    }
    
    @Test
    fun `hasDecentHand identifies broadway combinations`() {
        assertTrue(handEvaluator.hasDecentHand(cards("10" to "spades", "J" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("Q" to "spades", "K" to "hearts")))
        assertTrue(handEvaluator.hasDecentHand(cards("J" to "spades", "10" to "hearts")))
    }
    
    @Test
    fun `hasDecentHand rejects low offsuit cards`() {
        assertFalse(handEvaluator.hasDecentHand(cards("2" to "spades", "7" to "hearts")))
        assertFalse(handEvaluator.hasDecentHand(cards("3" to "spades", "8" to "hearts")))
        assertFalse(handEvaluator.hasDecentHand(cards("4" to "spades", "6" to "hearts")))
    }
    
    @Test
    fun `hasWeakButPlayableHand identifies suited cards`() {
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("7" to "spades", "8" to "spades"))) // Suited 7+ 
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("9" to "hearts", "J" to "hearts"))) // Suited with face
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("A" to "clubs", "3" to "clubs"))) // Suited ace
        // These should fail under tightened rules
        assertFalse(handEvaluator.hasWeakButPlayableHand(cards("2" to "spades", "5" to "spades"))) // Too low suited
    }
    
    @Test
    fun `hasWeakButPlayableHand identifies connected cards`() {
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("6" to "spades", "6" to "hearts"))) // Connected value 6+
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("8" to "clubs", "9" to "diamonds"))) // Connected 8+
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("7" to "spades", "7" to "hearts"))) // Connected 7+ 
        // Low connectors should fail under tightened rules
        assertFalse(handEvaluator.hasWeakButPlayableHand(cards("4" to "spades", "5" to "hearts"))) // Too low connected
    }
    
    @Test
    fun `hasWeakButPlayableHand identifies face cards`() {
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("J" to "spades", "7" to "hearts"))) // Jack with 7+
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("Q" to "spades", "8" to "hearts"))) // Queen with 7+
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("K" to "spades", "9" to "hearts"))) // King with 7+
        // Face cards with very low cards should fail under tightened rules
        assertFalse(handEvaluator.hasWeakButPlayableHand(cards("J" to "spades", "2" to "hearts"))) // J2 too weak
    }
    
    @Test
    fun `hasWeakButPlayableHand identifies medium pairs and high cards`() {
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("9" to "spades", "9" to "hearts"))) // Pair of 9s
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("10" to "spades", "J" to "hearts"))) // Both high cards
        assertTrue(handEvaluator.hasWeakButPlayableHand(cards("9" to "spades", "10" to "hearts"))) // Both 9+
    }
    
    @Test
    fun `hasWeakButPlayableHand rejects very weak hands`() {
        assertFalse(handEvaluator.hasWeakButPlayableHand(cards("2" to "spades", "5" to "hearts")))
        assertFalse(handEvaluator.hasWeakButPlayableHand(cards("3" to "spades", "6" to "hearts")))
        assertFalse(handEvaluator.hasWeakButPlayableHand(cards("4" to "spades", "7" to "hearts")))
    }
    
    @Test
    fun `hasMarginalHand identifies hands with high cards`() {
        assertTrue(handEvaluator.hasMarginalHand(cards("10" to "spades", "2" to "hearts")))
        assertTrue(handEvaluator.hasMarginalHand(cards("J" to "spades", "3" to "hearts")))
        assertTrue(handEvaluator.hasMarginalHand(cards("Q" to "spades", "4" to "hearts")))
        assertTrue(handEvaluator.hasMarginalHand(cards("K" to "spades", "5" to "hearts")))
        assertTrue(handEvaluator.hasMarginalHand(cards("A" to "spades", "6" to "hearts")))
    }
    
    @Test
    fun `hasMarginalHand identifies suited gaps`() {
        assertTrue(handEvaluator.hasMarginalHand(cards("5" to "spades", "8" to "spades")))
        assertTrue(handEvaluator.hasMarginalHand(cards("6" to "hearts", "9" to "hearts")))
        assertTrue(handEvaluator.hasMarginalHand(cards("7" to "clubs", "10" to "clubs")))
    }
    
    @Test
    fun `hasMarginalHand identifies small gap connectors`() {
        assertTrue(handEvaluator.hasMarginalHand(cards("5" to "spades", "7" to "hearts")))
        assertTrue(handEvaluator.hasMarginalHand(cards("8" to "clubs", "10" to "diamonds")))
        assertTrue(handEvaluator.hasMarginalHand(cards("6" to "spades", "8" to "hearts")))
    }
    
    @Test
    fun `hasMarginalHand rejects truly bad hands`() {
        assertFalse(handEvaluator.hasMarginalHand(cards("2" to "spades", "6" to "hearts")))
        assertFalse(handEvaluator.hasMarginalHand(cards("3" to "spades", "7" to "hearts")))
        assertFalse(handEvaluator.hasMarginalHand(cards("4" to "spades", "8" to "hearts")))
    }
    
    @Test
    fun `all hand evaluation methods handle invalid input`() {
        val emptyCards = JSONArray()
        val singleCard = cards("A" to "spades")
        
        assertFalse(handEvaluator.hasStrongHand(emptyCards))
        assertFalse(handEvaluator.hasDecentHand(emptyCards))
        assertFalse(handEvaluator.hasWeakButPlayableHand(emptyCards))
        assertFalse(handEvaluator.hasMarginalHand(emptyCards))
        
        assertFalse(handEvaluator.hasStrongHand(singleCard))
        assertFalse(handEvaluator.hasDecentHand(singleCard))
        assertFalse(handEvaluator.hasWeakButPlayableHand(singleCard))
        assertFalse(handEvaluator.hasMarginalHand(singleCard))
    }
    
    // ===== Tests for new evaluateBestHand functionality =====
    
    @Test
    fun `evaluateBestHand identifies pocket pairs correctly`() {
        val holeCards = cards("A" to "spades", "A" to "hearts")
        val communityCards = JSONArray() // Pre-flop
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.ONE_PAIR, result.rank)
        assertEquals("Pocket As", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies suited hole cards`() {
        val holeCards = cards("K" to "spades", "Q" to "spades")
        val communityCards = JSONArray() // Pre-flop
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.HIGH_CARD, result.rank)
        assertEquals("Suited KQ", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies offsuit hole cards`() {
        val holeCards = cards("A" to "spades", "K" to "hearts")
        val communityCards = JSONArray() // Pre-flop
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.HIGH_CARD, result.rank)
        assertEquals("Offsuit AK", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies royal flush`() {
        val holeCards = cards("A" to "spades", "K" to "spades")
        val communityCards = cards("Q" to "spades", "J" to "spades", "10" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.ROYAL_FLUSH, result.rank)
        assertEquals("Royal Flush", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies straight flush`() {
        val holeCards = cards("9" to "hearts", "8" to "hearts")
        val communityCards = cards("7" to "hearts", "6" to "hearts", "5" to "hearts")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.STRAIGHT_FLUSH, result.rank)
        assertEquals("Straight Flush", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies four of a kind`() {
        val holeCards = cards("A" to "spades", "A" to "hearts")
        val communityCards = cards("A" to "clubs", "A" to "diamonds", "K" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.FOUR_OF_A_KIND, result.rank)
        assertEquals("Four of a Kind", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies full house`() {
        val holeCards = cards("K" to "spades", "K" to "hearts")
        val communityCards = cards("K" to "clubs", "Q" to "diamonds", "Q" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.FULL_HOUSE, result.rank)
        assertEquals("Full House", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies flush`() {
        val holeCards = cards("A" to "spades", "K" to "spades")
        val communityCards = cards("Q" to "spades", "J" to "spades", "9" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.FLUSH, result.rank)
        assertEquals("Flush", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies straight`() {
        val holeCards = cards("A" to "spades", "K" to "hearts")
        val communityCards = cards("Q" to "clubs", "J" to "diamonds", "10" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.STRAIGHT, result.rank)
        assertEquals("Straight", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies wheel straight`() {
        val holeCards = cards("A" to "spades", "2" to "hearts")
        val communityCards = cards("3" to "clubs", "4" to "diamonds", "5" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.STRAIGHT, result.rank)
        assertEquals("Straight", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies three of a kind`() {
        val holeCards = cards("Q" to "spades", "Q" to "hearts")
        val communityCards = cards("Q" to "clubs", "J" to "diamonds", "9" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.THREE_OF_A_KIND, result.rank)
        assertEquals("Three of a Kind", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies two pair`() {
        val holeCards = cards("A" to "spades", "A" to "hearts")
        val communityCards = cards("K" to "clubs", "K" to "diamonds", "Q" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.TWO_PAIR, result.rank)
        assertEquals("Two Pair", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies one pair`() {
        val holeCards = cards("A" to "spades", "K" to "hearts")
        val communityCards = cards("Q" to "clubs", "Q" to "diamonds", "J" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.ONE_PAIR, result.rank)
        assertEquals("One Pair", result.description)
    }
    
    @Test
    fun `evaluateBestHand identifies high card`() {
        val holeCards = cards("A" to "spades", "K" to "hearts")
        val communityCards = cards("Q" to "clubs", "J" to "diamonds", "9" to "spades")
        
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        assertEquals(HandEvaluator.HandRank.HIGH_CARD, result.rank)
        assertEquals("High Card", result.description)
    }
    
    @Test
    fun `evaluateBestHand handles invalid hole cards`() {
        val invalidHoleCards = cards("A" to "spades") // Only one card
        val communityCards = cards("K" to "hearts", "Q" to "clubs", "J" to "diamonds", "10" to "spades", "9" to "hearts")
        
        val result = handEvaluator.evaluateBestHand(invalidHoleCards, communityCards)
        assertEquals(HandEvaluator.HandRank.HIGH_CARD, result.rank)
        assertEquals("Invalid hole cards", result.description)
    }
    
    // Tests for Rain Man enhanced functionality
    @Test
    fun `hasStrongHandWithCommunity should evaluate post-flop hands`() {
        val holeCards = cards("A" to "spades", "K" to "hearts")
        val communityCards = cards("A" to "clubs", "K" to "diamonds", "2" to "spades")
        
        // Two pair Aces and Kings should be strong
        assertTrue(handEvaluator.hasStrongHandWithCommunity(holeCards, communityCards))
    }
    
    @Test
    fun `hasStrongHandWithCommunity should fall back to preflop evaluation`() {
        val holeCards = cards("A" to "spades", "K" to "hearts")
        val noCommunityCards = JSONArray()
        
        // Should use pre-flop evaluation for AK (strong hand)
        assertTrue(handEvaluator.hasStrongHandWithCommunity(holeCards, noCommunityCards))
    }
    
    @Test
    fun `hasStrongHandWithCommunity should reject weak post-flop hands`() {
        val holeCards = cards("7" to "spades", "2" to "hearts")
        val communityCards = cards("K" to "clubs", "Q" to "diamonds", "J" to "spades")
        
        // Seven high should not be strong even with community cards
        assertFalse(handEvaluator.hasStrongHandWithCommunity(holeCards, communityCards))
    }
    
    @Test
    fun `evaluateBestHand should use Rain Man when community cards available`() {
        val holeCards = cards("A" to "spades", "K" to "spades")
        val communityCards = cards("Q" to "spades", "J" to "spades", "10" to "spades")
        
        // This should be a royal flush - the evaluation should detect it
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        
        // If Rain Man API works, it should detect royal flush or straight flush
        // If not, our fallback should still detect straight flush
        assertTrue(
            result.rank == HandEvaluator.HandRank.ROYAL_FLUSH || 
            result.rank == HandEvaluator.HandRank.STRAIGHT_FLUSH,
            "Should detect royal flush or straight flush, got: ${result.description}"
        )
    }
    
    @Test
    fun `evaluateBestHand should fall back gracefully when Rain Man unavailable`() {
        val holeCards = cards("A" to "hearts", "A" to "clubs")
        val communityCards = cards("A" to "spades", "K" to "diamonds", "Q" to "hearts")
        
        // Three Aces should be detected regardless of Rain Man availability
        val result = handEvaluator.evaluateBestHand(holeCards, communityCards)
        
        assertTrue(
            result.rank == HandEvaluator.HandRank.THREE_OF_A_KIND,
            "Should detect three of a kind, got: ${result.description}"
        )
    }
    
    @Test
    fun `HandRank fromRainManRank should map correctly`() {
        assertEquals(HandEvaluator.HandRank.HIGH_CARD, HandEvaluator.fromRainManRank(0))
        assertEquals(HandEvaluator.HandRank.ONE_PAIR, HandEvaluator.fromRainManRank(1))
        assertEquals(HandEvaluator.HandRank.TWO_PAIR, HandEvaluator.fromRainManRank(2))
        assertEquals(HandEvaluator.HandRank.THREE_OF_A_KIND, HandEvaluator.fromRainManRank(3))
        assertEquals(HandEvaluator.HandRank.STRAIGHT, HandEvaluator.fromRainManRank(4))
        assertEquals(HandEvaluator.HandRank.FLUSH, HandEvaluator.fromRainManRank(5))
        assertEquals(HandEvaluator.HandRank.FULL_HOUSE, HandEvaluator.fromRainManRank(6))
        assertEquals(HandEvaluator.HandRank.FOUR_OF_A_KIND, HandEvaluator.fromRainManRank(7))
        assertEquals(HandEvaluator.HandRank.STRAIGHT_FLUSH, HandEvaluator.fromRainManRank(8))
    }
}
