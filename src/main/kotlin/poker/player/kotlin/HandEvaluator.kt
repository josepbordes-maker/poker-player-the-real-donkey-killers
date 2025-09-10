package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class HandEvaluator {
    private val rainManService = RainManService()
    
    // Poker hand rankings from highest to lowest
    // Updated to match Rain Man API ranking system (0-8)
    enum class HandRank(val value: Int, val rainManRank: Int) {
        HIGH_CARD(1, 0),
        ONE_PAIR(2, 1),
        TWO_PAIR(3, 2),
        THREE_OF_A_KIND(4, 3),
        STRAIGHT(5, 4),
        FLUSH(6, 5),
        FULL_HOUSE(7, 6),
        FOUR_OF_A_KIND(8, 7),
        STRAIGHT_FLUSH(9, 8),
        ROYAL_FLUSH(10, 8) // Royal flush is a special case of straight flush
    }
    
    companion object {
        fun fromRainManRank(rainManRank: Int): HandRank {
            return when (rainManRank) {
                0 -> HandRank.HIGH_CARD
                1 -> HandRank.ONE_PAIR
                2 -> HandRank.TWO_PAIR
                3 -> HandRank.THREE_OF_A_KIND
                4 -> HandRank.STRAIGHT
                5 -> HandRank.FLUSH
                6 -> HandRank.FULL_HOUSE
                7 -> HandRank.FOUR_OF_A_KIND
                8 -> HandRank.STRAIGHT_FLUSH // Rain Man doesn't distinguish royal flush
                else -> HandRank.HIGH_CARD
            }
        }
    }
    
    data class HandStrength(
        val rank: HandRank,
        val description: String,
        val kickerValues: List<Int> = emptyList(),
        val value: Int = 0,
        val secondValue: Int = 0,
        val rainManData: RainManService.RainManResponse? = null
    )
    
    /**
     * Evaluates the best 5-card hand from hole cards + community cards
     * Uses Rain Man API for accurate ranking when possible, falls back to local evaluation
     */
    fun evaluateBestHand(holeCards: JSONArray, communityCards: JSONArray): HandStrength {
        if (holeCards.length() != 2) {
            return HandStrength(HandRank.HIGH_CARD, "Invalid hole cards")
        }
        
        // Combine all available cards
        val allCards = mutableListOf<JSONObject>()
        for (i in 0 until holeCards.length()) {
            allCards.add(holeCards.getJSONObject(i))
        }
        for (i in 0 until communityCards.length()) {
            allCards.add(communityCards.getJSONObject(i))
        }
        
        if (allCards.size < 5) {
            // Pre-flop or early streets - evaluate hole cards only
            return evaluateHoleCards(holeCards)
        }
        
        // Try Rain Man API first for 5+ cards
        return evaluateWithRainMan(allCards) ?: findBestFiveCardHand(allCards)
    }
    
    private fun evaluateHoleCards(holeCards: JSONArray): HandStrength {
        val card1 = holeCards.getJSONObject(0)
        val card2 = holeCards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        val suit1 = card1.getString("suit")
        val suit2 = card2.getString("suit")
        
        val value1 = CardUtils.getRankValue(rank1)
        val value2 = CardUtils.getRankValue(rank2)
        val isPair = rank1 == rank2
        val isSuited = suit1 == suit2
        
        return when {
            isPair -> HandStrength(
                HandRank.ONE_PAIR,
                "Pocket ${rank1}s",
                listOf(value1, value2)
            )
            isSuited -> HandStrength(
                HandRank.HIGH_CARD,
                "Suited ${rank1}${rank2}",
                listOf(maxOf(value1, value2), minOf(value1, value2))
            )
            else -> HandStrength(
                HandRank.HIGH_CARD,
                "Offsuit ${rank1}${rank2}",
                listOf(maxOf(value1, value2), minOf(value1, value2))
            )
        }
    }
    
    /**
     * Use Rain Man API to evaluate hand if possible
     */
    private fun evaluateWithRainMan(allCards: List<JSONObject>): HandStrength? {
        if (allCards.size < 5) return null
        
        // Convert to JSONArray for Rain Man API
        val cardsArray = JSONArray()
        allCards.forEach { cardsArray.put(it) }
        
        val rainManResult = rainManService.rankHand(cardsArray)
        
        return rainManResult?.let { result ->
            var handRank = fromRainManRank(result.rank)
            
            // Post-process: Rain Man doesn't distinguish royal flush from straight flush
            // If we get a straight flush, check if it's actually a royal flush
            if (handRank == HandRank.STRAIGHT_FLUSH && result.value == 14) {
                // A-high straight flush is a royal flush
                val ranks = allCards.map { CardUtils.getRankValue(it.getString("rank")) }
                if (ranks.contains(14) && ranks.contains(13) && ranks.contains(12) && 
                    ranks.contains(11) && ranks.contains(10)) {
                    handRank = HandRank.ROYAL_FLUSH
                }
            }
            
            val description = getHandDescription(handRank, result.value, result.secondValue)
            
            HandStrength(
                rank = handRank,
                description = description,
                kickerValues = result.kickers,
                value = result.value,
                secondValue = result.secondValue,
                rainManData = result
            )
        }
    }
    
    /**
     * Generate description based on Rain Man data
     */
    private fun getHandDescription(rank: HandRank, value: Int, secondValue: Int): String {
        return when (rank) {
            HandRank.ROYAL_FLUSH -> "Royal Flush"
            HandRank.STRAIGHT_FLUSH -> "Straight Flush"
            HandRank.FOUR_OF_A_KIND -> "Four of a Kind"
            HandRank.FULL_HOUSE -> "Full House"
            HandRank.FLUSH -> "Flush"
            HandRank.STRAIGHT -> "Straight"
            HandRank.THREE_OF_A_KIND -> "Three of a Kind"
            HandRank.TWO_PAIR -> "Two Pair"
            HandRank.ONE_PAIR -> "One Pair"
            HandRank.HIGH_CARD -> "High Card"
        }
    }
    
    /**
     * Convert numeric rank to name
     */
    private fun getRankName(value: Int): String {
        return when (value) {
            14 -> "Ace"
            13 -> "King"
            12 -> "Queen"
            11 -> "Jack"
            else -> value.toString()
        }
    }
    
    private fun findBestFiveCardHand(allCards: List<JSONObject>): HandStrength {
        // For simplicity, we'll implement basic hand detection
        // In a full implementation, you'd generate all 5-card combinations
        
        val ranks = allCards.map { CardUtils.getRankValue(it.getString("rank")) }.sorted().reversed()
        val suits = allCards.map { it.getString("suit") }
        val rankCounts = ranks.groupingBy { it }.eachCount()
        
        // Check for flush
        val flushSuit = suits.groupingBy { it }.eachCount().entries.find { it.value >= 5 }?.key
        val isFlush = flushSuit != null
        
        // Check for straight
        val isStraight = checkStraight(ranks.distinct())
        
        // Determine best hand (fallback logic when Rain Man API unavailable)
        return when {
            isFlush && isStraight && ranks.contains(14) && ranks.contains(13) && 
            ranks.contains(12) && ranks.contains(11) && ranks.contains(10) -> 
                HandStrength(HandRank.ROYAL_FLUSH, "Royal Flush", ranks.take(5))
            isFlush && isStraight -> 
                HandStrength(HandRank.STRAIGHT_FLUSH, "Straight Flush", ranks.take(5))
            rankCounts.containsValue(4) -> 
                HandStrength(HandRank.FOUR_OF_A_KIND, "Four of a Kind", ranks.take(5))
            rankCounts.containsValue(3) && rankCounts.containsValue(2) -> 
                HandStrength(HandRank.FULL_HOUSE, "Full House", ranks.take(5))
            isFlush -> 
                HandStrength(HandRank.FLUSH, "Flush", ranks.take(5))
            isStraight -> 
                HandStrength(HandRank.STRAIGHT, "Straight", ranks.take(5))
            rankCounts.containsValue(3) -> 
                HandStrength(HandRank.THREE_OF_A_KIND, "Three of a Kind", ranks.take(5))
            rankCounts.values.count { it == 2 } >= 2 -> 
                HandStrength(HandRank.TWO_PAIR, "Two Pair", ranks.take(5))
            rankCounts.containsValue(2) -> 
                HandStrength(HandRank.ONE_PAIR, "One Pair", ranks.take(5))
            else -> 
                HandStrength(HandRank.HIGH_CARD, "High Card", ranks.take(5))
        }
    }
    
    private fun checkStraight(distinctRanks: List<Int>): Boolean {
        val sorted = distinctRanks.sorted()
        if (sorted.size < 5) return false
        
        // Check for regular straight
        for (i in 0..sorted.size - 5) {
            if (sorted[i + 4] - sorted[i] == 4) return true
        }
        
        // Check for A-2-3-4-5 straight (wheel)
        if (sorted.contains(14) && sorted.contains(2) && sorted.contains(3) && 
            sorted.contains(4) && sorted.contains(5)) {
            return true
        }
        
        return false
    }

    fun hasStrongHand(cards: JSONArray): Boolean {
        if (cards.length() != 2) return false
        
        val card1 = cards.getJSONObject(0)
        val card2 = cards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        
        // Strong hands: pairs of 10 or higher, AK, AQ
        return when {
            rank1 == rank2 && CardUtils.getRankValue(rank1) >= 10 -> true // High pairs
            (rank1 == "A" && rank2 == "K") || (rank1 == "K" && rank2 == "A") -> true // AK
            (rank1 == "A" && rank2 == "Q") || (rank1 == "Q" && rank2 == "A") -> true // AQ
            else -> false
        }
    }
    
    fun hasDecentHand(cards: JSONArray): Boolean {
        if (cards.length() != 2) return false
        
        val card1 = cards.getJSONObject(0)
        val card2 = cards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        val suit1 = card1.getString("suit")
        val suit2 = card2.getString("suit")
        
        // Expanded decent hands: any pair, high cards, suited connectors, broadway cards
        return when {
            rank1 == rank2 -> true // Any pair
            CardUtils.getRankValue(rank1) >= 9 || CardUtils.getRankValue(rank2) >= 9 -> true // 9+ cards (was 10+)
            suit1 == suit2 && abs(CardUtils.getRankValue(rank1) - CardUtils.getRankValue(rank2)) <= 2 -> true // Suited connectors/gappers
            (rank1 == "A" || rank2 == "A") -> true // Any ace
            (rank1 == "K" || rank2 == "K") -> true // Any king
            CardUtils.isBroadway(rank1) && CardUtils.isBroadway(rank2) -> true // Two broadway cards (10, J, Q, K, A)
            else -> false
        }
    }
    
    fun hasWeakButPlayableHand(cards: JSONArray): Boolean {
        if (cards.length() != 2) return false
        
        val card1 = cards.getJSONObject(0)
        val card2 = cards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        val suit1 = card1.getString("suit")
        val suit2 = card2.getString("suit")
        
        // Weak but playable: suited cards, connected cards, any face card
        return when {
            suit1 == suit2 -> true // Any suited cards
            abs(CardUtils.getRankValue(rank1) - CardUtils.getRankValue(rank2)) <= 1 -> true // Connected cards
            CardUtils.getRankValue(rank1) >= 11 || CardUtils.getRankValue(rank2) >= 11 -> true // Any jack or higher
            (CardUtils.getRankValue(rank1) >= 8 && CardUtils.getRankValue(rank2) >= 8) -> true // Both cards 8 or higher
            else -> false
        }
    }
    
    fun hasMarginalHand(cards: JSONArray): Boolean {
        if (cards.length() != 2) return false
        
        val card1 = cards.getJSONObject(0)
        val card2 = cards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        val suit1 = card1.getString("suit")
        val suit2 = card2.getString("suit")
        
        // Marginal hands for bluffing: one high card, suited gaps, etc.
        return when {
            CardUtils.getRankValue(rank1) >= 10 || CardUtils.getRankValue(rank2) >= 10 -> true // At least one high card
            suit1 == suit2 && abs(CardUtils.getRankValue(rank1) - CardUtils.getRankValue(rank2)) <= 3 -> true // Suited with small gap
            abs(CardUtils.getRankValue(rank1) - CardUtils.getRankValue(rank2)) <= 2 -> true // Small gap connectors
            else -> false
        }
    }
    
    /**
     * Enhanced version that takes community cards for more accurate evaluation
     */
    fun hasStrongHandWithCommunity(holeCards: JSONArray, communityCards: JSONArray): Boolean {
        if (holeCards.length() != 2) return false
        
        // If we have community cards, use full hand evaluation
        if (communityCards.length() >= 3) {
            val handStrength = evaluateBestHand(holeCards, communityCards)
            // Strong hands are pairs or better with Rain Man analysis
            return when (handStrength.rank) {
                HandRank.ONE_PAIR -> handStrength.value >= 10 // Pair of tens or better
                HandRank.TWO_PAIR, HandRank.THREE_OF_A_KIND, HandRank.STRAIGHT,
                HandRank.FLUSH, HandRank.FULL_HOUSE, HandRank.FOUR_OF_A_KIND,
                HandRank.STRAIGHT_FLUSH, HandRank.ROYAL_FLUSH -> true
                else -> false
            }
        }
        
        // Fall back to pre-flop evaluation
        return hasStrongHand(holeCards)
    }
    
    /**
     * Close resources when done
     */
    fun close() {
        rainManService.close()
    }
}
