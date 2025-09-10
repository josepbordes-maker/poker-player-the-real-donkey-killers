package poker.player.kotlin

import org.json.JSONObject

object Logger {
    fun logStructured(event: JSONObject) {
        println(event.toString())
    }
}

