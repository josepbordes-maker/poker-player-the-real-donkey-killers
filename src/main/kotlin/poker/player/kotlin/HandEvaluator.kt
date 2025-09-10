package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class HandEvaluator {
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
}
