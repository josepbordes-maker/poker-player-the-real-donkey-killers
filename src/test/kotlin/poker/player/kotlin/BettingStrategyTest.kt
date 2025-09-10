package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BettingStrategyTest {
    
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val bettingStrategy = BettingStrategy(handEvaluator, positionAnalyzer)
    
    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)
    
    private fun cards(vararg cardPairs: Pair<String, String>): JSONArray {
        val array = JSONArray()
        cardPairs.forEach { (rank, suit) -> array.put(card(rank, suit)) }
        return array
    }
    
    @Test
    fun `calculateBet goes all-in when call amount exceeds stack`() {
        val myCards = cards("A" to "spades", "K" to "hearts")
        val myStack = 100
        val myBet = 0
        val currentBuyIn = 150
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = myStack,
            myBet = myBet,
            currentBuyIn = currentBuyIn,
            pot = 200,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(myStack, bet)
    }
    
    @Test
    fun `calculateBet open-raises with strong hand in early position`() {
        val myCards = cards("A" to "spades", "A" to "hearts") // Pocket aces
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY
        )
        
        assertEquals(60, bet) // 6x small blind for strong hand
    }
    
    @Test
    fun `calculateBet checks with weak hand in early position when no bet to call`() {
        val myCards = cards("2" to "spades", "7" to "hearts") // Weak hand
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY
        )
        
        assertEquals(0, bet)
    }
    
    @Test
    fun `calculateBet open-raises with decent hand in late position`() {
        val myCards = cards("Q" to "spades", "J" to "hearts") // Decent hand
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(40, bet) // 4x small blind for decent hand in late position
    }
    
    @Test
    fun `calculateBet raises with strong hand facing bet`() {
        val myCards = cards("K" to "spades", "K" to "hearts") // Pocket kings
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 50, // Facing a bet
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        assertEquals(90, bet) // call (50) + 2 * minimum raise (40)
    }
    
    @Test
    fun `calculateBet raises with strong hand facing small bet`() {
        val myCards = cards("10" to "spades", "10" to "hearts") // Pocket tens - strong hand
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 20, // Small bet
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(60, bet) // Raise (call 20 + 2 * minimum raise 40)
    }
    
    @Test
    fun `calculateBet calls with weak but playable hand facing very small bet`() {
        val myCards = cards("8" to "spades", "9" to "spades") // Suited connectors
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 15, // Very small bet (1.5x small blind)
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        assertEquals(15, bet) // Call the small bet
    }
    
    @Test
    fun `calculateBet folds weak hands to small bets`() {
        val myCards = cards("2" to "spades", "5" to "hearts") // Truly weak hand
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 20, // 2x small blind
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(0, bet) // Fold weak hand - no more unconditional small-bet calls
    }
    
    @Test
    fun `calculateBet folds weak hand facing large bet`() {
        val myCards = cards("2" to "spades", "5" to "hearts") // Very weak hand
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 80, // Large bet
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY
        )
        
        assertEquals(0, bet) // Fold
    }
    
    @Test
    fun `calculateBet respects stack size limits`() {
        val myCards = cards("A" to "spades", "A" to "hearts") // Strong hand
        val smallStack = 30
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = smallStack,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY
        )
        
        assertEquals(smallStack, bet) // All-in because 6x SB = 60 > stack
    }
    
    @Test
    fun `calculateBet respects position-based thresholds`() {
        val myCards = cards("9" to "spades", "9" to "hearts") // Decent hand
        val callAmount = 40
        val pot = 100
        
        // Early position - stricter threshold (pot/4 = 25)
        val earlyBet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY
        )
        
        // Late position - more lenient threshold (pot/2 = 50)
        val lateBet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(0, earlyBet) // Fold in early position (40 > 25)
        assertEquals(40, lateBet) // Call in late position (40 <= 50)
    }
    
    @Test
    fun `calculateBet handles minimum bet correctly`() {
        val myCards = cards("A" to "spades", "K" to "hearts")
        val myStack = 1000
        val myBet = 5
        val currentBuyIn = 10
        
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            myStack = myStack,
            myBet = myBet,
            currentBuyIn = currentBuyIn,
            pot = 50,
            smallBlind = 5,
            minimumRaise = 10,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        assertEquals(25, bet) // Call amount (10-5=5) + 2 * minimum raise (20) = 25
    }
}
