package poker.player.kotlin

import org.json.JSONObject

object CardUtils {
    fun normalizeCard(card: JSONObject): String {
        val rank = card.getString("rank")
        val suit = card.getString("suit")
        return "$rank-$suit"
    }
    
    fun getRankValue(rank: String): Int {
        return when (rank) {
            "2" -> 2
            "3" -> 3
            "4" -> 4
            "5" -> 5
            "6" -> 6
            "7" -> 7
            "8" -> 8
            "9" -> 9
            "10" -> 10
            "J" -> 11
            "Q" -> 12
            "K" -> 13
            "A" -> 14
            else -> 0
        }
    }
    
    fun isBroadway(rank: String): Boolean {
        return getRankValue(rank) >= 10
    }
}
