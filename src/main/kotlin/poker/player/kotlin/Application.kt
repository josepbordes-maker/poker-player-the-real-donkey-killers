package poker.player.kotlin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    val player = Player()
    embeddedServer(Netty, getPort()) {
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
            post {
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                val formParameters = call.receiveParameters()
                val action = formParameters["action"].toString()
                
                println("[$timestamp] === INCOMING REQUEST ===")
                println("[$timestamp] Action: $action")
                
                val result = when (action) {
                    "bet_request" -> {
                        val gameState = formParameters["game_state"]

                        if (gameState == null) {
                            println("[$timestamp] ERROR: Missing game_state for bet_request")
                            "Missing game_state!"
                        } else {
                            try {
                                println("[$timestamp] Processing bet_request with game_state...")
                                val json = JSONObject(gameState)
                                val betAmount = player.betRequest(json)
                                println("[$timestamp] BET DECISION: $betAmount")
                                betAmount.toString()
                            } catch (e: Exception) {
                                println("[$timestamp] ERROR processing bet_request: ${e.message}")
                                e.printStackTrace()
                                "0"  // Fold on error
                            }
                        }
                    }

                    "showdown" -> {
                        val gameState = formParameters["game_state"]
                        if (gameState == null) {
                            println("[$timestamp] ERROR: Missing game_state for showdown")
                            "Missing game_state!"
                        } else {
                            try {
                                println("[$timestamp] Processing showdown...")
                                val json = JSONObject(gameState)
                                player.showdown(json)
                                println("[$timestamp] Showdown processed successfully")
                                "OK"
                            } catch (e: Exception) {
                                println("[$timestamp] ERROR processing showdown: ${e.message}")
                                e.printStackTrace()
                                "ERROR"
                            }
                        }
                    }

                    "version" -> {
                        val version = player.version()
                        println("[$timestamp] Version requested: $version")
                        version
                    }
                    
                    "check" -> {
                        println("[$timestamp] Health check requested")
                        "OK"
                    }
                    
                    else -> {
                        println("[$timestamp] ERROR: Unknown action '$action'")
                        "Unknown action '$action'!"
                    }
                }

                println("[$timestamp] Response: $result")
                println("[$timestamp] === REQUEST COMPLETE ===\n")
                call.respondText(result)
            }
        }
    }.start(wait = true)
}

private fun getPort(): Int {
    val port = System.getenv("PORT") ?: "8080"

    return Integer.parseInt(port)
}
