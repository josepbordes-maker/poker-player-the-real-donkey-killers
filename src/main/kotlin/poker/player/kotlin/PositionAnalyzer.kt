package poker.player.kotlin

import org.json.JSONArray

class PositionAnalyzer {
    enum class Position {
        EARLY, MIDDLE, LATE, BLINDS
    }

    fun getPosition(players: JSONArray, inAction: Int, dealer: Int): Position {
        val total = players.length()
        val relative = (inAction - dealer + total) % total
        
        println("      PositionAnalyzer.getPosition() - Total players: $total, inAction: $inAction, dealer: $dealer")
        println("      Relative position: $relative")
        
        val position = when {
            relative == 1 || relative == 2 -> {
                println("      Position: BLINDS (relative 1-2)")
                Position.BLINDS
            }
            relative == 0 || relative >= total - 2 -> {
                println("      Position: LATE (relative 0 or >= ${total - 2})")
                Position.LATE
            }
            relative >= total - 5 -> {
                println("      Position: MIDDLE (relative >= ${total - 5})")
                Position.MIDDLE
            }
            else -> {
                println("      Position: EARLY (relative < ${total - 5})")
                Position.EARLY
            }
        }
        
        return position
    }
    
    fun getSmallBetThreshold(position: Position, pot: Int): Int {
        println("      PositionAnalyzer.getSmallBetThreshold() - Position: $position, Pot: $pot")
        
        val threshold = when (position) {
            Position.EARLY -> {
                val result = pot / 4  // More lenient than pot/6
                println("      EARLY position threshold: $result (pot/4)")
                result
            }
            Position.MIDDLE -> {
                val result = pot / 3 // More lenient than pot/5
                println("      MIDDLE position threshold: $result (pot/3)")
                result
            }
            Position.LATE, Position.BLINDS -> {
                val result = pot / 2 // More lenient than pot/4
                println("      LATE/BLINDS position threshold: $result (pot/2)")
                result
            }
        }
        
        return threshold
    }
    
    fun getOpenRaiseSize(position: Position, smallBlind: Int): Int {
        println("      PositionAnalyzer.getOpenRaiseSize() - Position: $position, Small Blind: $smallBlind")
        
        val raiseSize = when (position) {
            Position.EARLY -> {
                val result = smallBlind * 6  // This is only used for strong hands in early position
                println("      EARLY open raise size: $result (${smallBlind}×6)")
                result
            }
            Position.MIDDLE -> {
                val result = smallBlind * 4  // For decent hands
                println("      MIDDLE open raise size: $result (${smallBlind}×4)")
                result
            }
            Position.LATE, Position.BLINDS -> {
                val result = smallBlind * 4  // For decent hands
                println("      LATE/BLINDS open raise size: $result (${smallBlind}×4)")
                result
            }
        }
        
        return raiseSize
    }
    
    fun getStrongHandRaiseSize(smallBlind: Int): Int {
        val raiseSize = smallBlind * 6
        println("      PositionAnalyzer.getStrongHandRaiseSize() - Strong hand raise: $raiseSize (${smallBlind}×6)")
        return raiseSize
    }
}
