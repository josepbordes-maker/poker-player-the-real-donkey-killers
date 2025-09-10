package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

class GameStateManager {
    private val seenCardsByGame = mutableMapOf<String, MutableSet<String>>()
    private val handEvaluator = HandEvaluator()

    fun addSeenCards(gameId: String, cards: JSONArray) {
        val set = seenCardsByGame.getOrPut(gameId) { mutableSetOf() }
        val newCards = mutableListOf<String>()
        
        for (i in 0 until cards.length()) {
            val c = cards.getJSONObject(i)
            val normalizedCard = CardUtils.normalizeCard(c)
            if (set.add(normalizedCard)) {  // add() returns true if the element was added (wasn't already present)
                newCards.add(normalizedCard)
            }
        }
        
        if (newCards.isNotEmpty()) {
            println("      GameStateManager.addSeenCards() - Game $gameId: Added ${newCards.size} new cards: ${newCards.joinToString(", ")}")
            println("      Total seen cards for game $gameId: ${set.size}")
        }
    }
    
    fun clearGameMemory(gameId: String) {
        if (gameId.isNotEmpty()) {
            val removed = seenCardsByGame.remove(gameId)
            if (removed != null) {
                println("      GameStateManager.clearGameMemory() - Cleared ${removed.size} seen cards for game $gameId")
            }
        }
    }
    
    fun getSeenCards(gameId: String): Set<String> {
        return seenCardsByGame[gameId] ?: emptySet()
    }
    
    fun processShowdown(game_state: JSONObject) {
        println("      GameStateManager.processShowdown() - Processing showdown analysis")
        
        // Capture revealed cards and analyze final hand strength
        val gameId = game_state.optString("game_id", "")
        val players = game_state.optJSONArray("players")
        val communityCards = game_state.optJSONArray("community_cards") ?: JSONArray()
        
        println("      Game ID: $gameId")
        println("      Players: ${players?.length() ?: 0}")
        println("      Community cards: ${communityCards.length()}")
        
        if (players != null) {
            analyzeShowdownHands(players, communityCards, gameId)
            
            // Add all revealed cards to seen cards
            var totalRevealedCards = 0
            for (i in 0 until players.length()) {
                val p = players.getJSONObject(i)
                val hc = p.optJSONArray("hole_cards")
                if (hc != null) {
                    addSeenCards(gameId, hc)
                    totalRevealedCards += hc.length()
                }
            }
            println("      Total revealed hole cards: $totalRevealedCards")
        }
        
        clearGameMemory(gameId)
        println("      GameStateManager.processShowdown() - Complete")
    }
    
    private fun analyzeShowdownHands(players: JSONArray, communityCards: JSONArray, gameId: String) {
        val handAnalysis = mutableListOf<String>()
        
        // Find our player and analyze all revealed hands
        for (i in 0 until players.length()) {
            val player = players.getJSONObject(i)
            val holeCards = player.optJSONArray("hole_cards")
            val playerName = player.optString("name", "Player $i")
            val stack = player.optInt("stack", 0)
            
            if (holeCards != null && holeCards.length() == 2) {
                val handStrength = handEvaluator.evaluateBestHand(holeCards, communityCards)
                val holeCardsStr = formatCards(holeCards)
                
                handAnalysis.add(
                    "$playerName: $holeCardsStr -> ${handStrength.description} (${handStrength.rank.name}), Stack: $stack"
                )
            }
        }
        
        // Log the showdown analysis
        if (handAnalysis.isNotEmpty()) {
            println("=== SHOWDOWN ANALYSIS for Game $gameId ===")
            println("Community Cards: ${formatCards(communityCards)}")
            handAnalysis.forEach { println(it) }
            
            // Determine winner(s) based on hand strength
            val bestRank = handAnalysis.maxOfOrNull { line ->
                val rankName = line.substringAfter("(").substringBefore(")")
                HandEvaluator.HandRank.valueOf(rankName).value
            } ?: 0
            
            val winners = handAnalysis.filter { line ->
                val rankName = line.substringAfter("(").substringBefore(")")
                HandEvaluator.HandRank.valueOf(rankName).value == bestRank
            }
            
            println("Winner(s): ${winners.joinToString("; ")}")
            println("=========================================")
        }
    }
    
    private fun formatCards(cards: JSONArray): String {
        if (cards.length() == 0) return "None"
        
        val cardStrings = mutableListOf<String>()
        for (i in 0 until cards.length()) {
            val card = cards.getJSONObject(i)
            val rank = card.getString("rank")
            val suit = card.getString("suit")
            val suitSymbol = when (suit) {
                "spades" -> "♠"
                "hearts" -> "♥"
                "diamonds" -> "♦"
                "clubs" -> "♣"
                else -> suit.first().uppercaseChar()
            }
            cardStrings.add("$rank$suitSymbol")
        }
        return cardStrings.joinToString(" ")
    }
}
