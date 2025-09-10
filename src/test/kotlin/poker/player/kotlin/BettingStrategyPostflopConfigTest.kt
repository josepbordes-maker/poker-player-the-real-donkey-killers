package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import kotlin.test.*

@Execution(CONCURRENT)
class BettingStrategyPostflopConfigTest {
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val bettingStrategy = BettingStrategy(handEvaluator, positionAnalyzer)

    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)

    @BeforeTest
    fun setProps() {
        System.setProperty("STRAT_RAINMAN", "off")
        System.setProperty("STRAT_POSTFLOP_SMALL", "0.25")
        System.setProperty("STRAT_POSTFLOP_MED", "0.4")
        System.setProperty("STRAT_POSTFLOP_BIG", "0.75")
    }

    @AfterTest
    fun clearProps() {
        System.clearProperty("STRAT_RAINMAN")
        System.clearProperty("STRAT_POSTFLOP_SMALL")
        System.clearProperty("STRAT_POSTFLOP_MED")
        System.clearProperty("STRAT_POSTFLOP_BIG")
    }

    @Test
    fun `one pair uses configured small fraction`() {
        val myCards = JSONArray().put(card("A", "spades")).put(card("K", "hearts"))
        val community = JSONArray().put(card("A", "diamonds")).put(card("7", "clubs")).put(card("2", "hearts"))
        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            communityCards = community,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0,
            pot = 100,
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )
        val expected = kotlin.math.round(100 * 0.25 * 1.2).toInt() // 100 * postflopSmall * latePositionMultiplier
        assertEquals(expected, bet)
    }
}

