package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerTest {
    private fun baseState(): JSONObject {
        val me = JSONObject()
            .put("id", 0)
            .put("name", "Me")
            .put("status", "active")
            .put("version", "test")
            .put("stack", 1000)
            .put("bet", 0)
            .put("hole_cards", JSONArray())

        val players = JSONArray().put(me)

        return JSONObject()
            .put("tournament_id", "t1")
            .put("game_id", "g1")
            .put("round", 0)
            .put("bet_index", 0)
            .put("small_blind", 10)
            .put("current_buy_in", 0)
            .put("pot", 0)
            .put("minimum_raise", 20)
            .put("dealer", 0)
            .put("in_action", 0)
            .put("players", players)
            .put("community_cards", JSONArray())
    }

    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)

    @Test
    fun `opens stronger hands preflop`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        me.put("hole_cards", JSONArray().put(card("A", "spades")).put(card("K", "spades")))

        // No bet to call, should open ~3x BB = 30 (we used 6x small blind)
        val bet = player.betRequest(state)
        assertEquals(60, bet)
    }

    @Test
    fun `calls small bet with decent hand`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        me.put("hole_cards", JSONArray().put(card("Q", "hearts")).put(card("J", "hearts")))

        state.put("current_buy_in", 10)
        me.put("bet", 0)
        state.put("pot", 100)

        val bet = player.betRequest(state)
        assertEquals(10, bet) // call small bet
    }

    @Test
    fun `min-raises with strong hand facing bet`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        me.put("hole_cards", JSONArray().put(card("Q", "diamonds")).put(card("Q", "clubs")))

        state.put("current_buy_in", 40)
        state.put("minimum_raise", 20)
        state.put("pot", 100)

        val bet = player.betRequest(state)
        // callAmount = 40, raise stronger -> 40 + 2*20 = 80
        assertEquals(80, bet)
    }

    @Test
    fun `folds trash hand to small bet`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        // 7-2 offsuit: not playable by our heuristics
        me.put("hole_cards", JSONArray().put(card("7", "spades")).put(card("2", "hearts")))

        state.put("current_buy_in", 20) // 2x SB
        me.put("bet", 0)
        state.put("pot", 40)

        val bet = player.betRequest(state)
        assertEquals(0, bet)
    }

    @Test
    fun `blinds do not open-raise with only decent hand`() {
        val player = Player()

        // Create 3-player table and put us in blinds with no bet to call
        val p0 = JSONObject().put("id", 0).put("name", "P0").put("status", "active").put("stack", 1000).put("bet", 0).put("hole_cards", JSONArray())
        val p1 = JSONObject().put("id", 1).put("name", "Me").put("status", "active").put("stack", 1000).put("bet", 0)
        val p2 = JSONObject().put("id", 2).put("name", "P2").put("status", "active").put("stack", 1000).put("bet", 0).put("hole_cards", JSONArray())
        val players = JSONArray().put(p0).put(p1).put(p2)

        val state = JSONObject()
            .put("tournament_id", "t1")
            .put("game_id", "g1")
            .put("round", 0)
            .put("bet_index", 0)
            .put("small_blind", 10)
            .put("current_buy_in", 0)
            .put("pot", 0)
            .put("minimum_raise", 20)
            .put("dealer", 0) // dealer = 0, in_action = 1 -> relative = 1 (blinds)
            .put("in_action", 1)
            .put("players", players)
            .put("community_cards", JSONArray())

        // Give us a decent but non-premium hand (JTs)
        p1.put("hole_cards", JSONArray().put(card("J", "spades")).put(card("10", "spades")))

        val bet = player.betRequest(state)
        assertEquals(0, bet)
    }
    
    // === INTEGRATION TESTS FOR NEW STRATEGY ===
    
    @Test
    fun `blinds open-raise strong hands but check decent hands`() {
        val player = Player()
        
        // Create multi-player table with us in blinds position
        val p0 = JSONObject().put("id", 0).put("name", "P0").put("status", "active").put("stack", 1000).put("bet", 0).put("hole_cards", JSONArray())
        val p1 = JSONObject().put("id", 1).put("name", "Me").put("status", "active").put("stack", 1000).put("bet", 0)
        val p2 = JSONObject().put("id", 2).put("name", "P2").put("status", "active").put("stack", 1000).put("bet", 0).put("hole_cards", JSONArray())
        val players = JSONArray().put(p0).put(p1).put(p2)
        
        val baseState = JSONObject()
            .put("tournament_id", "t1")
            .put("game_id", "g1")
            .put("round", 0)
            .put("bet_index", 0)
            .put("small_blind", 10)
            .put("current_buy_in", 0)
            .put("pot", 0)
            .put("minimum_raise", 20)
            .put("dealer", 0) // dealer = 0, in_action = 1 -> blinds position
            .put("in_action", 1)
            .put("players", players)
            .put("community_cards", JSONArray())
        
        // Test with strong hand (pocket queens)
        val strongState = JSONObject(baseState.toString())
        strongState.getJSONArray("players").getJSONObject(1)
            .put("hole_cards", JSONArray().put(card("Q", "spades")).put(card("Q", "hearts")))
        
        val strongBet = player.betRequest(strongState)
        assertEquals(60, strongBet) // Should open-raise strong hand from blinds
        
        // Test with decent hand (AJ suited)
        val decentState = JSONObject(baseState.toString())
        decentState.getJSONArray("players").getJSONObject(1)
            .put("hole_cards", JSONArray().put(card("A", "hearts")).put(card("J", "hearts")))
        
        val decentBet = player.betRequest(decentState)
        assertEquals(0, decentBet) // Should check decent hand from blinds (defensive)
    }
    
    @Test
    fun `stronger raise sizing with premium hands facing bets`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        
        // Test multiple strong hands
        val strongHands = listOf(
            listOf(card("A", "spades"), card("A", "hearts")), // Pocket aces
            listOf(card("K", "diamonds"), card("K", "clubs")), // Pocket kings  
            listOf(card("A", "spades"), card("K", "hearts")), // AK offsuit
            listOf(card("10", "spades"), card("10", "hearts")) // Pocket tens
        )
        
        strongHands.forEach { handCards ->
            val testState = JSONObject(state.toString())
            testState.put("current_buy_in", 35)
            testState.put("minimum_raise", 15)
            testState.put("pot", 120)
            
            val me = testState.getJSONArray("players").getJSONObject(0)
            me.put("hole_cards", JSONArray().put(handCards[0]).put(handCards[1]))
            
            val bet = player.betRequest(testState)
            assertEquals(65, bet) // call (35) + 2 * minimum raise (30) = 65
        }
    }
    
    @Test
    fun `no unconditional small-bet calls - requires hand quality`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        
        // Test various weak hands that used to call small bets unconditionally
        val weakHands = listOf(
            listOf(card("2", "spades"), card("7", "hearts")), // 2-7 offsuit
            listOf(card("3", "clubs"), card("8", "diamonds")), // 3-8 offsuit
            listOf(card("4", "hearts"), card("6", "spades")), // 4-6 offsuit
            listOf(card("2", "diamonds"), card("5", "clubs"))  // 2-5 offsuit
        )
        
        weakHands.forEach { handCards ->
            val testState = JSONObject(state.toString())
            testState.put("current_buy_in", 20) // 2x small blind - used to be auto-call
            testState.put("pot", 60)
            
            val me = testState.getJSONArray("players").getJSONObject(0)
            me.put("hole_cards", JSONArray().put(handCards[0]).put(handCards[1]))
            
            val bet = player.betRequest(testState)
            assertEquals(0, bet) // Should fold weak hands even to small bets
        }
    }
    
    @Test
    fun `playable hands still call small bets within limits`() {
        val player = Player()
        val state = baseState()
        val me = state.getJSONArray("players").getJSONObject(0)
        
        // Test playable hands that should still call small bets
        val playableHands = listOf(
            listOf(card("8", "spades"), card("9", "spades")), // Suited connectors
            listOf(card("9", "hearts"), card("8", "hearts")), // More suited connectors
            listOf(card("J", "clubs"), card("9", "diamonds")), // Face card
            listOf(card("10", "spades"), card("8", "hearts"))  // Two medium cards
        )
        
        playableHands.forEach { handCards ->
            val testState = JSONObject(state.toString())
            testState.put("current_buy_in", 20) // Exactly 2x small blind
            testState.put("pot", 80)
            
            val me = testState.getJSONArray("players").getJSONObject(0)
            me.put("hole_cards", JSONArray().put(handCards[0]).put(handCards[1]))
            
            val bet = player.betRequest(testState)
            assertEquals(20, bet) // Should call small bet with playable hand
        }
    }
    
    @Test
    fun `position affects calling thresholds appropriately`() {
        val player = Player()
        
        // Test same hand in different positions with same bet size
        val decentHand = listOf(card("K", "spades"), card("Q", "hearts")) // KQ offsuit
        val callAmount = 35
        val pot = 100 // Early threshold: 25, Middle: 33, Late: 50
        
        // Early position - should fold (35 > 25)
        val earlyState = createPositionState(decentHand, callAmount, pot, 0, 3) // dealer=0, in_action=3, relative=3 = early
        val earlyBet = player.betRequest(earlyState)
        assertEquals(0, earlyBet)
        
        // Late position - should call (35 < 50)  
        val lateState = createPositionState(decentHand, callAmount, pot, 0, 0) // dealer=0, in_action=0, relative=0 = late
        val lateBet = player.betRequest(lateState)
        assertEquals(callAmount, lateBet)
    }
    
    @Test
    fun `version reflects strategy improvements`() {
        val player = Player()
        val version = player.version()
        // Mode can vary if other tests tweak STRAT_MODE; assert stable parts
        kotlin.test.assertTrue(version.startsWith("Real Donkey Killer v1.2"))
        kotlin.test.assertTrue(version.contains("Rain Man Enhanced"))
    }
    
    @Test
    fun `integration test - complete betting round scenarios`() {
        val player = Player()
        
        // Scenario 1: Strong hand in early position facing raise
        val scenario1 = baseState()
        scenario1.put("current_buy_in", 45)
        scenario1.put("minimum_raise", 25)
        scenario1.put("pot", 150)
        scenario1.getJSONArray("players").getJSONObject(0)
            .put("hole_cards", JSONArray().put(card("A", "diamonds")).put(card("A", "clubs")))
        
        val bet1 = player.betRequest(scenario1)
        assertEquals(95, bet1) // call (45) + 2*min_raise (50) = 95
        
        // Scenario 2: Marginal hand in blinds facing small bet
        val scenario2 = create3PlayerBlindsState()
        scenario2.put("current_buy_in", 15) // Small bet
        scenario2.put("pot", 45)
        scenario2.getJSONArray("players").getJSONObject(1) // We're player 1 in blinds
            .put("hole_cards", JSONArray().put(card("A", "spades")).put(card("3", "hearts"))) // A3 offsuit - marginal
        
        val bet2 = player.betRequest(scenario2)
        // Marginal hand might call due to risk-taking (15% chance) or fold
        assertTrue(bet2 == 0 || bet2 == 15)
        
        // Scenario 3: Decent hand open-raising from late position
        val scenario3 = createPositionState(
            listOf(card("Q", "hearts"), card("J", "spades")), // QJ offsuit
            0, // No bet to call
            0, // Empty pot
            0, // dealer
            7  // Late position in 9-player game
        )
        
        val bet3 = player.betRequest(scenario3)
        assertEquals(40, bet3) // Open-raise decent hand from late position
    }
    
    // Helper methods for complex state creation
    
    private fun createPositionState(
        holeCards: List<JSONObject>, 
        currentBuyIn: Int, 
        pot: Int, 
        dealer: Int, 
        inAction: Int
    ): JSONObject {
        val players = JSONArray()
        for (i in 0..8) { // 9-player table
            val player = JSONObject()
                .put("id", i)
                .put("name", "Player$i")
                .put("status", "active")
                .put("stack", 1000)
                .put("bet", 0)
                .put("hole_cards", if (i == inAction) 
                    JSONArray().put(holeCards[0]).put(holeCards[1]) 
                    else JSONArray())
            players.put(player)
        }
        
        return JSONObject()
            .put("tournament_id", "t1")
            .put("game_id", "g1")
            .put("round", 0)
            .put("bet_index", 0)
            .put("small_blind", 10)
            .put("current_buy_in", currentBuyIn)
            .put("pot", pot)
            .put("minimum_raise", 20)
            .put("dealer", dealer)
            .put("in_action", inAction)
            .put("players", players)
            .put("community_cards", JSONArray())
    }
    
    private fun create3PlayerBlindsState(): JSONObject {
        val p0 = JSONObject().put("id", 0).put("name", "P0").put("status", "active").put("stack", 1000).put("bet", 0).put("hole_cards", JSONArray())
        val p1 = JSONObject().put("id", 1).put("name", "Me").put("status", "active").put("stack", 1000).put("bet", 0)
        val p2 = JSONObject().put("id", 2).put("name", "P2").put("status", "active").put("stack", 1000).put("bet", 0).put("hole_cards", JSONArray())
        val players = JSONArray().put(p0).put(p1).put(p2)
        
        return JSONObject()
            .put("tournament_id", "t1")
            .put("game_id", "g1")
            .put("round", 0)
            .put("bet_index", 0)
            .put("small_blind", 10)
            .put("current_buy_in", 0)
            .put("pot", 0)
            .put("minimum_raise", 20)
            .put("dealer", 0) // dealer = 0, in_action = 1 -> blinds position
            .put("in_action", 1)
            .put("players", players)
            .put("community_cards", JSONArray())
    }
}
