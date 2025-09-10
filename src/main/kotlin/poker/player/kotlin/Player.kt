package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

class Player {
    private val gameStateManager = GameStateManager()
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val bettingStrategy = BettingStrategy(handEvaluator, positionAnalyzer)

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
        gameStateManager.addSeenCards(gameId, myCards)
        val community = game_state.optJSONArray("community_cards") ?: JSONArray()
        if (community.length() > 0) gameStateManager.addSeenCards(gameId, community)

        // Get position for betting decision
        val position = positionAnalyzer.getPosition(players, inAction, dealer)
        
        // Use betting strategy to calculate bet
        return bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = myStack,
            myBet = myBet,
            currentBuyIn = currentBuyIn,
            pot = pot,
            smallBlind = smallBlind,
            minimumRaise = minimumRaise,
            position = position
        )
    }
    fun showdown(game_state: JSONObject) {
        gameStateManager.processShowdown(game_state)
    }

    fun version(): String {
        return "Real Donkey Killer"
    }
}
