package poker.player.kotlin

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

/**
 * Service to interact with the Rain Man API for hand ranking
 * API Documentation: https://www.leanpoker.org/docs/api/rain-man
 */
class RainManService {
    private val logger = LoggerFactory.getLogger(RainManService::class.java)
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 5000 // 5 second timeout
        }
    }
    
    data class RainManResponse(
        val rank: Int,
        val value: Int,
        val secondValue: Int,
        val kickers: List<Int>,
        val cardsUsed: JSONArray,
        val cards: JSONArray
    )
    
    /**
     * Calls the Rain Man API to get professional hand ranking
     * Returns null if API call fails or times out
     */
    fun rankHand(cards: JSONArray): RainManResponse? {
        return try {
            runBlocking {
                withTimeoutOrNull(3000) { // 3 second timeout for the call
                    makeApiCall(cards)
                }
            }
        } catch (e: Exception) {
            logger.warn("Rain Man API call failed: ${e.message}")
            null
        }
    }
    
    private suspend fun makeApiCall(cards: JSONArray): RainManResponse? {
        try {
            // Convert cards to Rain Man format
            val cardsParam = buildString {
                append("[")
                for (i in 0 until cards.length()) {
                    if (i > 0) append(",")
                    val card = cards.getJSONObject(i)
                    append("""{"rank":"${card.getString("rank")}","suit":"${card.getString("suit")}"}""")
                }
                append("]")
            }
            
            val response: HttpResponse = client.get("https://rainman.leanpoker.org/rank") {
                parameter("cards", cardsParam)
                contentType(ContentType.Application.Json)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val responseText = response.bodyAsText()
                val json = JSONObject(responseText)
                
                return RainManResponse(
                    rank = json.getInt("rank"),
                    value = json.getInt("value"),
                    secondValue = json.getInt("second_value"),
                    kickers = json.getJSONArray("kickers").let { kickers ->
                        (0 until kickers.length()).map { kickers.getInt(it) }
                    },
                    cardsUsed = json.getJSONArray("cards_used"),
                    cards = json.getJSONArray("cards")
                )
            } else {
                logger.warn("Rain Man API returned status: ${response.status}")
                return null
            }
        } catch (e: Exception) {
            logger.warn("Error calling Rain Man API: ${e.message}")
            return null
        }
    }
    
    /**
     * Close the HTTP client when done
     */
    fun close() {
        client.close()
    }
}
