package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

class GameStateManager {
    private val seenCardsByGame = mutableMapOf<String, MutableSet<String>>()

    fun addSeenCards(gameId: String, cards: JSONArray) {
        val set = seenCardsByGame.getOrPut(gameId) { mutableSetOf() }
        for (i in 0 until cards.length()) {
            val c = cards.getJSONObject(i)
            set.add(CardUtils.normalizeCard(c))
        }
    }
    
    fun clearGameMemory(gameId: String) {
        if (gameId.isNotEmpty()) {
            seenCardsByGame.remove(gameId)
        }
    }
    
    fun getSeenCards(gameId: String): Set<String> {
        return seenCardsByGame[gameId] ?: emptySet()
    }
    
    fun processShowdown(game_state: JSONObject) {
        // Capture revealed cards and clear memory for this game
        val gameId = game_state.optString("game_id", "")
        val players = game_state.optJSONArray("players")
        if (players != null) {
            for (i in 0 until players.length()) {
                val p = players.getJSONObject(i)
                val hc = p.optJSONArray("hole_cards")
                if (hc != null) addSeenCards(gameId, hc)
            }
        }
        clearGameMemory(gameId)
    }
}
