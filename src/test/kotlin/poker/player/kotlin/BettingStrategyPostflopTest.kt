package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Execution(CONCURRENT)
class BettingStrategyPostflopTest {
    private val handEvaluator = HandEvaluator()
    private val positionAnalyzer = PositionAnalyzer()
    private val bettingStrategy = BettingStrategy(handEvaluator, positionAnalyzer)

    private fun card(rank: String, suit: String) = JSONObject().put("rank", rank).put("suit", suit)

    @BeforeTest
    fun disableRainMan() { System.setProperty("STRAT_RAINMAN", "off") }

    @AfterTest
    fun cleanup() { System.clearProperty("STRAT_RAINMAN") }

    @Test
    fun `postflop one pair c-bets ~1-3 pot when checked to`() {
        val myCards = JSONArray().put(card("A", "spades")).put(card("K", "hearts"))
        val community = JSONArray().put(card("A", "diamonds")).put(card("7", "clubs")).put(card("2", "hearts"))

        val bet = bettingStrategy.calculateBet(
            myCards = myCards,
            communityCards = community,
            myStack = 1000,
            myBet = 0,
            currentBuyIn = 0, // no bet to call
            pot =  ninety(),
            smallBlind = 10,
            minimumRaise = 20,
            position = PositionAnalyzer.Position.LATE
        )

        val baseExpected = 90 * StrategyConfig.postflopSmall
        val positionMultiplier = 1.2 // Late position multiplier
        val expected = kotlin.math.round(baseExpected * positionMultiplier).toInt()
        // Allow off-by-one due to rounding and integer math
        val ok = bet == expected || bet == expected - 1 || bet == expected + 1
        kotlin.test.assertTrue(ok, "expected ~$expected got $bet")
    }

    private fun ninety() = 90
}
