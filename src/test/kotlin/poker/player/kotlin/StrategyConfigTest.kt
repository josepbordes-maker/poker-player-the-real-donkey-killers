package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import kotlin.test.Test
import kotlin.test.assertEquals

@Execution(SAME_THREAD)
class StrategyConfigTest {
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val bettingStrategy = BettingStrategy(handEvaluator, positionAnalyzer)

    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)
    private fun cards(vararg pairs: Pair<String, String>): JSONArray {
        val arr = JSONArray()
        pairs.forEach { (r, s) -> arr.put(card(r, s)) }
        return arr
    }

    @Test
    fun `lag mode opens weak playable in late`() {
        val prev = System.getProperty("STRAT_MODE")
        try {
            System.setProperty("STRAT_MODE", "LAG")
            val myCards = cards("J" to "clubs", "9" to "clubs") // weak but playable (suited gap 1)
            val bet = bettingStrategy.calculateBet(
                myCards = myCards,
                communityCards = JSONArray(),
                myStack = 1000,
                myBet = 0,
                currentBuyIn = 0,
                pot = 0,
                smallBlind = 10,
                minimumRaise = 20,
                position = PositionAnalyzer.Position.LATE
            )
            assertEquals(40, bet) // 4x SB open in late
        } finally {
            if (prev == null) System.clearProperty("STRAT_MODE") else System.setProperty("STRAT_MODE", prev)
        }
    }
}
