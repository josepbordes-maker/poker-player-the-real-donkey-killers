package poker.player.kotlin

import org.json.JSONArray

class PositionAnalyzer {
    enum class Position {
        EARLY, MIDDLE, LATE, BLINDS
    }

    fun getPosition(players: JSONArray, inAction: Int, dealer: Int): Position {
        val total = players.length()
        val relative = (inAction - dealer + total) % total
        return when {
            relative == 1 || relative == 2 -> Position.BLINDS
            relative == 0 || relative >= total - 2 -> Position.LATE
            relative >= total - 5 -> Position.MIDDLE
            else -> Position.EARLY
        }
    }
    
    fun getSmallBetThreshold(position: Position, pot: Int): Int {
        return when (position) {
            Position.EARLY -> pot / 4  // More lenient than pot/6
            Position.MIDDLE -> pot / 3 // More lenient than pot/5
            Position.LATE, Position.BLINDS -> pot / 2 // More lenient than pot/4
        }
    }
    
    fun getOpenRaiseSize(position: Position, smallBlind: Int): Int {
        return when (position) {
            Position.EARLY -> smallBlind * 6  // This is only used for strong hands in early position
            Position.MIDDLE -> smallBlind * 4  // For decent hands
            Position.LATE, Position.BLINDS -> smallBlind * 4  // For decent hands
        }
    }
    
    fun getStrongHandRaiseSize(smallBlind: Int): Int {
        return smallBlind * 6
    }
}
