package poker.player.kotlin

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RainManServiceTest {

    private fun mockClient(responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient {
        return HttpClient(MockEngine) { engine { addHandler(responder) } }
    }

    @Test
    fun `should parse straight flush response from Rain Man API`() {
        val client = mockClient { request ->
            assert(request.url.toString().contains("rainman.leanpoker.org/rank"))
            respond(
                content = """{
                  "rank": 8, 
                  "value": 9, 
                  "second_value": 0, 
                  "kickers": [9,8,7], 
                  "cards_used": [], 
                  "cards": []
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = RainManService(client)

        val cards = JSONArray(listOf(
            JSONObject(mapOf("rank" to "5", "suit" to "diamonds")),
            JSONObject(mapOf("rank" to "6", "suit" to "diamonds")),
            JSONObject(mapOf("rank" to "7", "suit" to "diamonds")),
            JSONObject(mapOf("rank" to "8", "suit" to "diamonds")),
            JSONObject(mapOf("rank" to "9", "suit" to "diamonds"))
        ))

        val result = service.rankHand(cards)
        requireNotNull(result)
        assertEquals(8, result.rank)
        assertEquals(9, result.value)
        service.close()
    }

    @Test
    fun `should return null on non-OK status`() {
        val client = mockClient { respondError(HttpStatusCode.BadRequest) }
        val service = RainManService(client)
        val cards = JSONArray()
        val result = service.rankHand(cards)
        assertNull(result)
        service.close()
    }
}
