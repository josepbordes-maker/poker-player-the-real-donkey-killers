package poker.player.kotlin

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CardUtilsTest {
    
    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)
    
    @Test
    fun `normalizeCard creates correct format`() {
        val card = card("A", "spades")
        val normalized = CardUtils.normalizeCard(card)
        assertEquals("A-spades", normalized)
    }
    
    @Test
    fun `normalizeCard handles all suits`() {
        assertEquals("K-hearts", CardUtils.normalizeCard(card("K", "hearts")))
        assertEquals("Q-diamonds", CardUtils.normalizeCard(card("Q", "diamonds")))
        assertEquals("J-clubs", CardUtils.normalizeCard(card("J", "clubs")))
        assertEquals("10-spades", CardUtils.normalizeCard(card("10", "spades")))
    }
    
    @Test
    fun `getRankValue returns correct values for number cards`() {
        assertEquals(2, CardUtils.getRankValue("2"))
        assertEquals(3, CardUtils.getRankValue("3"))
        assertEquals(4, CardUtils.getRankValue("4"))
        assertEquals(5, CardUtils.getRankValue("5"))
        assertEquals(6, CardUtils.getRankValue("6"))
        assertEquals(7, CardUtils.getRankValue("7"))
        assertEquals(8, CardUtils.getRankValue("8"))
        assertEquals(9, CardUtils.getRankValue("9"))
        assertEquals(10, CardUtils.getRankValue("10"))
    }
    
    @Test
    fun `getRankValue returns correct values for face cards`() {
        assertEquals(11, CardUtils.getRankValue("J"))
        assertEquals(12, CardUtils.getRankValue("Q"))
        assertEquals(13, CardUtils.getRankValue("K"))
        assertEquals(14, CardUtils.getRankValue("A"))
    }
    
    @Test
    fun `getRankValue returns 0 for invalid rank`() {
        assertEquals(0, CardUtils.getRankValue("invalid"))
        assertEquals(0, CardUtils.getRankValue(""))
        assertEquals(0, CardUtils.getRankValue("1"))
    }
    
    @Test
    fun `isBroadway identifies broadway cards correctly`() {
        assertTrue(CardUtils.isBroadway("10"))
        assertTrue(CardUtils.isBroadway("J"))
        assertTrue(CardUtils.isBroadway("Q"))
        assertTrue(CardUtils.isBroadway("K"))
        assertTrue(CardUtils.isBroadway("A"))
    }
    
    @Test
    fun `isBroadway identifies non-broadway cards correctly`() {
        assertFalse(CardUtils.isBroadway("2"))
        assertFalse(CardUtils.isBroadway("3"))
        assertFalse(CardUtils.isBroadway("4"))
        assertFalse(CardUtils.isBroadway("5"))
        assertFalse(CardUtils.isBroadway("6"))
        assertFalse(CardUtils.isBroadway("7"))
        assertFalse(CardUtils.isBroadway("8"))
        assertFalse(CardUtils.isBroadway("9"))
        assertFalse(CardUtils.isBroadway("invalid"))
    }
}
