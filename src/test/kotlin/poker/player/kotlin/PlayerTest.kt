package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

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
}

