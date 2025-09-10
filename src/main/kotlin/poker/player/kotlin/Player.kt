package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

class Player {
    private val seenCardsByGame = mutableMapOf<String, MutableSet<String>>()

    enum class Position {
        EARLY, MIDDLE, LATE, BLINDS
    }

    private fun normalizeCard(card: JSONObject): String {
        val rank = card.getString("rank")
        val suit = card.getString("suit")
        return "$rank-$suit"
    }

    private fun addSeenCards(gameId: String, cards: JSONArray) {
        val set = seenCardsByGame.getOrPut(gameId) { mutableSetOf() }
        for (i in 0 until cards.length()) {
            val c = cards.getJSONObject(i)
            set.add(normalizeCard(c))
        }
    }

    private fun getPosition(players: JSONArray, inAction: Int, dealer: Int): Position {
        val total = players.length()
        val relative = (inAction - dealer + total) % total
        return when {
            relative == 1 || relative == 2 -> Position.BLINDS
            relative == 0 || relative >= total - 2 -> Position.LATE
            relative >= total - 5 -> Position.MIDDLE
            else -> Position.EARLY
        }
    }

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
        val gameId = game_state.getString("game_id")
        val currentBuyIn = game_state.getInt("current_buy_in")
        val pot = game_state.getInt("pot")
        val smallBlind = game_state.getInt("small_blind")
        val minimumRaise = game_state.getInt("minimum_raise")
        val dealer = game_state.getInt("dealer")
        
        // Remember seen cards (hole + community) for this game
        addSeenCards(gameId, myCards)
        val community = game_state.optJSONArray("community_cards") ?: JSONArray()
        if (community.length() > 0) addSeenCards(gameId, community)

        // Calculate call amount
        val callAmount = currentBuyIn - myBet
        
        // Safety check - can't bet more than we have
        if (callAmount >= myStack) {
            return myStack // All-in if we must
        }
        
        // If no bet to call, open-raise based on position
        if (callAmount <= 0) {
            val position = getPosition(players, inAction, dealer)
            return when (position) {
                Position.EARLY -> when {
                    hasStrongHand(myCards) -> Math.min(myStack, smallBlind * 6)
                    else -> 0
                }
                Position.MIDDLE -> when {
                    hasStrongHand(myCards) -> Math.min(myStack, smallBlind * 6)
                    hasDecentHand(myCards) -> Math.min(myStack, smallBlind * 4)
                    else -> 0
                }
                Position.LATE, Position.BLINDS -> when {
                    hasStrongHand(myCards) -> Math.min(myStack, smallBlind * 6)
                    hasDecentHand(myCards) -> Math.min(myStack, smallBlind * 4)
                    else -> 0
                }
            }
        }
        
        // Position-aware continuations when facing a bet
        val position = getPosition(players, inAction, dealer)
        val smallBetThreshold = when (position) {
            Position.EARLY -> pot / 6
            Position.MIDDLE -> pot / 5
            Position.LATE, Position.BLINDS -> pot / 4
        }

        return when {
            hasStrongHand(myCards) -> Math.min(myStack, callAmount + minimumRaise)
            hasDecentHand(myCards) && callAmount <= smallBetThreshold -> callAmount
            callAmount <= smallBlind -> callAmount
            else -> 0
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

    fun showdown(game_state: JSONObject) {
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
        if (gameId.isNotEmpty()) {
            seenCardsByGame.remove(gameId)
        }
    }

    fun version(): String {
        return "Real Donkey Killer"
    }
}
