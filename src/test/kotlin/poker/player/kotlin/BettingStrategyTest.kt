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
    
    // === DEFENSIVE BLINDS TESTS ===
    
    @Test
    fun `calculateBet blinds only open-raise with strong hands`() {
        val strongHand = cards("Q" to "spades", "Q" to "hearts") // Pocket queens - strong
        val decentHand = cards("J" to "hearts", "10" to "hearts") // Suited jack-ten - decent but not strong
        
        val strongBet = bettingStrategy.calculateBet(
            myCards = strongHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.BLINDS
        )
        
        val decentBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // No bet to call
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.BLINDS
        )
        
        assertEquals(60, strongBet) // Open-raise strong hand from blinds
        assertEquals(0, decentBet) // Check decent hand from blinds (defensive)
    }
    
    @Test
    fun `calculateBet blinds vs late position open-raise behavior differs`() {
        val decentHand = cards("A" to "hearts", "9" to "spades") // A9o - decent hand
        
        val blindsBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0,
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.BLINDS
        )
        
        val lateBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0,
            pot = 0,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(0, blindsBet) // Don't open decent hands from blinds
        assertEquals(40, lateBet) // Do open decent hands from late position
    }
    
    // === STRONG HAND RAISE SIZING TESTS ===
    
    @Test
    fun `calculateBet strong hands use 2x minimum raise sizing`() {
        val strongHands = listOf(
            cards("A" to "spades", "A" to "hearts"), // Pocket aces
            cards("K" to "diamonds", "K" to "clubs"), // Pocket kings
            cards("A" to "spades", "K" to "hearts"), // AK offsuit
            cards("Q" to "spades", "Q" to "hearts")  // Pocket queens
        )
        
        strongHands.forEach { hand ->
            val bet = bettingStrategy.calculateBet(
                myCards = hand,
                myStack = 1000,
                myBet = 0,
                currentBuyIn = 30, // Facing a bet
                pot = 100,
                smallBlind = 10,
                minimumRaise = 15,
                position = PositionAnalyzer.Position.MIDDLE
            )
            
            assertEquals(60, bet) // call (30) + 2 * minimum raise (30) = 60
        }
    }
    
    @Test
    fun `calculateBet strong hand raise sizing respects stack limits`() {
        val strongHand = cards("A" to "spades", "A" to "hearts")
        val smallStack = 50
        
        val bet = bettingStrategy.calculateBet(
            myCards = strongHand,
            myStack = smallStack,
            myBet = 0,
            currentBuyIn = 20, // Facing a bet
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        assertEquals(smallStack, bet) // All-in since 20 + 2*20 = 60 > 50
    }
    
    // === SMALL-BET CALLING REQUIREMENTS TESTS ===
    
    @Test
    fun `calculateBet requires playable hands for small-bet calls`() {
        val trashHand = cards("2" to "spades", "7" to "hearts") // 2-7 offsuit - truly trash (gap > 2)
        val marginalHand = cards("6" to "hearts", "7" to "spades") // 6-7 offsuit - marginal  
        val playableHand = cards("8" to "spades", "9" to "spades") // 8-9 suited - playable
        
        val smallBet = 15 // 1.5x small blind
        
        val trashBet = bettingStrategy.calculateBet(
            myCards = trashHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = smallBet,
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        val playableBet = bettingStrategy.calculateBet(
            myCards = playableHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = smallBet,
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(0, trashBet) // Fold trash hand even to small bet
        assertEquals(smallBet, playableBet) // Call with playable hand
    }
    
    @Test
    fun `calculateBet weak but playable hands call bets up to 2x small blind`() {
        val playableHand = cards("7" to "diamonds", "6" to "clubs") // Offsuit connectors - weak but playable
        
        val smallBet = bettingStrategy.calculateBet(
            myCards = playableHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 20, // Exactly 2x small blind
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        val largeBet = bettingStrategy.calculateBet(
            myCards = playableHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 25, // More than 2x small blind
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        assertEquals(20, smallBet) // Call at threshold
        assertEquals(0, largeBet) // Fold above threshold
    }
    
    // === POSITION-AWARE THRESHOLD TESTS ===
    
    @Test
    fun `calculateBet uses different small bet thresholds by position`() {
        val decentHand = cards("K" to "spades", "J" to "hearts") // KJ offsuit
        val pot = 120
        val callAmount = 30
        
        // Early: pot/4 = 30, so exactly at threshold
        val earlyBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.EARLY
        )
        
        // Middle: pot/3 = 40, so under threshold
        val middleBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        // Late: pot/2 = 60, so well under threshold
        val lateBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(callAmount, earlyBet) // Call at exact threshold
        assertEquals(callAmount, middleBet) // Call under threshold
        assertEquals(callAmount, lateBet) // Call well under threshold
    }
    
    @Test
    fun `calculateBet blinds use same threshold as late position`() {
        val decentHand = cards("10" to "hearts", "9" to "hearts") // T9 suited
        val pot = 100
        val callAmount = 55 // Above late threshold (pot/2 = 50) but would call
        
        val blindsBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.BLINDS
        )
        
        val lateBet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = pot,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        assertEquals(lateBet, blindsBet) // Same calling behavior when facing bets
    }
    
    // === EDGE CASES AND STACK MANAGEMENT ===
    
    @Test
    fun `calculateBet handles edge case where call equals stack`() {
        val decentHand = cards("Q" to "spades", "Q" to "hearts")
        val stack = 100
        val callAmount = 100
        
        val bet = bettingStrategy.calculateBet(
            myCards = decentHand,
            myStack = stack,
            myBet = 0,
            currentBuyIn = callAmount,
            pot = 200,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.MIDDLE
        )
        
        assertEquals(stack, bet) // All-in when call equals stack
    }
    
    @Test
    fun `calculateBet handles partial bet situations correctly`() {
        val strongHand = cards("A" to "spades", "K" to "spades")
        val myBet = 15 // Already bet 15
        val currentBuyIn = 40 // Need to call 25 more
        
        val bet = bettingStrategy.calculateBet(
            myCards = strongHand,
            myStack = 1000,
            myBet = myBet,
            currentBuyIn = currentBuyIn,
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        
        // Call amount = 40 - 15 = 25, raise = 25 + 2*20 = 65
        assertEquals(65, bet)
    }
    
    @Test
    fun `calculateBet preserves tight-aggressive profile across positions`() {
        val marginalHand = cards("A" to "spades", "2" to "hearts") // A2 offsuit - marginal
        val largeBet = 50
        
        listOf(
            PositionAnalyzer.Position.EARLY,
            PositionAnalyzer.Position.MIDDLE, 
            PositionAnalyzer.Position.LATE,
            PositionAnalyzer.Position.BLINDS
        ).forEach { position ->
            val bet = bettingStrategy.calculateBet(
                myCards = marginalHand,
                myStack = 1000,
                myBet = 0,
                currentBuyIn = largeBet,
                pot = 100,
                smallBlind = 10,
                minimumRaise = 20,
                position = position
            )
            
            // Should fold marginal hands to large bets regardless of position
            // (unless random risk-taking kicks in, but that's 15% chance)
            assertTrue(bet == 0 || bet == largeBet) // Either fold or call (risky play)
        }
    }
}
