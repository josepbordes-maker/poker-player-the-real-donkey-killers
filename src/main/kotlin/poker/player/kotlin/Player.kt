package poker.player.kotlin

import org.json.JSONObject

class Player {
    fun betRequest(game_state: JSONObject): Int {
        // Parse game state
        val players = game_state.getJSONArray("players")
        val inAction = game_state.getInt("in_action")
        val me = players.getJSONObject(inAction)
        
        // Get my information
        val myStack = me.getInt("stack")
        val myBet = me.getInt("bet")
        val myCards = me.getJSONArray("hole_cards")
        
        // Get game information
        val currentBuyIn = game_state.getInt("current_buy_in")
        val pot = game_state.getInt("pot")
        val smallBlind = game_state.getInt("small_blind")
        
        // Calculate call amount
        val callAmount = currentBuyIn - myBet
        
        // Safety check - can't bet more than we have
        if (callAmount >= myStack) {
            return myStack // All-in if we must
        }
        
        // If no bet to call, make a small bet with decent hands
        if (callAmount <= 0) {
            return if (hasDecentHand(myCards)) smallBlind else 0
        }
        
        // Simple strategy: call small bets with decent hands, fold large bets
        return when {
            hasStrongHand(myCards) -> callAmount + smallBlind // Raise with strong hands
            hasDecentHand(myCards) && callAmount <= pot / 4 -> callAmount // Call with decent hands if bet is small
            callAmount <= smallBlind -> callAmount // Call very small bets
            else -> 0 // Fold otherwise
        }
    }
    
    private fun hasStrongHand(cards: JSONArray): Boolean {
        if (cards.length() != 2) return false
        
        val card1 = cards.getJSONObject(0)
        val card2 = cards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        val suit1 = card1.getString("suit")
        val suit2 = card2.getString("suit")
        
        // Strong hands: pairs of 10 or higher, AK, AQ
        return when {
            rank1 == rank2 && getRankValue(rank1) >= 10 -> true // High pairs
            (rank1 == "A" && rank2 == "K") || (rank1 == "K" && rank2 == "A") -> true // AK
            (rank1 == "A" && rank2 == "Q") || (rank1 == "Q" && rank2 == "A") -> true // AQ
            else -> false
        }
    }
    
    private fun hasDecentHand(cards: JSONArray): Boolean {
        if (cards.length() != 2) return false
        
        val card1 = cards.getJSONObject(0)
        val card2 = cards.getJSONObject(1)
        val rank1 = card1.getString("rank")
        val rank2 = card2.getString("rank")
        val suit1 = card1.getString("suit")
        val suit2 = card2.getString("suit")
        
        // Decent hands: any pair, high cards, suited connectors
        return when {
            rank1 == rank2 -> true // Any pair
            getRankValue(rank1) >= 10 || getRankValue(rank2) >= 10 -> true // High cards
            suit1 == suit2 && Math.abs(getRankValue(rank1) - getRankValue(rank2)) <= 1 -> true // Suited connectors
            (rank1 == "A" || rank2 == "A") -> true // Any ace
            else -> false
        }
    }
    
    private fun getRankValue(rank: String): Int {
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

    fun showdown() {
    }

    fun version(): String {
        return "Real Donkey Killer"
    }
}
