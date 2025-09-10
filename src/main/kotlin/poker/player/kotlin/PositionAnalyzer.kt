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
                val result = pot / 5  // More conservative for early position
                println("      EARLY position threshold: $result (pot/5)")
                result
            }
            Position.MIDDLE -> {
                val result = pot / 3 // Moderate for middle position
                println("      MIDDLE position threshold: $result (pot/3)")
                result
            }
            Position.LATE, Position.BLINDS -> {
                // Changed from pot/2 to pot/3 for better defense against small opens
                val result = pot / 3 // More reasonable for late/blinds
                println("      LATE/BLINDS position threshold: $result (pot/3, improved from pot/2)")
                result
            }
        }
        
        return kotlin.math.max(1, threshold) // Ensure minimum threshold of 1
    }
    
    fun getOpenRaiseSize(position: Position, smallBlind: Int): Int {
        println("      PositionAnalyzer.getOpenRaiseSize() - Position: $position, Small Blind: $smallBlind")
        
        // MAJOR FIX: Proper preflop sizing - 2.2-2.5x instead of pot-sized opens
        val bigBlind = smallBlind * 2
        val raiseSize = when (position) {
            Position.EARLY -> {
                val result = (bigBlind * 2.5).toInt()  // 2.5x BB from early
                println("      EARLY open raise size: $result (${bigBlind}×2.5 = ${smallBlind}×5)")
                result
            }
            Position.MIDDLE -> {
                val result = (bigBlind * 2.3).toInt()  // 2.3x BB from middle
                println("      MIDDLE open raise size: $result (${bigBlind}×2.3 ≈ ${smallBlind}×4.6)")
                result
            }
            Position.LATE, Position.BLINDS -> {
                val result = (bigBlind * 2.2).toInt()  // 2.2x BB from late/blinds
                println("      LATE/BLINDS open raise size: $result (${bigBlind}×2.2 = ${smallBlind}×4.4)")
                result
            }
        }
        
        return raiseSize
    }
    
    fun getStrongHandRaiseSize(smallBlind: Int): Int {
        // MAJOR FIX: Proper sizing for strong hands - still 2.5x BB, not pot-sized
        val bigBlind = smallBlind * 2
        val raiseSize = (bigBlind * 2.5).toInt()  // Same as early position sizing
        println("      PositionAnalyzer.getStrongHandRaiseSize() - Strong hand raise: $raiseSize (${bigBlind}×2.5 = ${smallBlind}×5)")
        return raiseSize
    }
}
