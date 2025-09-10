package poker.player.kotlin

object StrategyConfig {
    enum class Mode { TIGHT, STANDARD, LAG }

    private fun rawMode(): String? {
        return System.getenv("STRAT_MODE") ?: System.getProperty("STRAT_MODE")
    }

    private fun envOrProp(name: String): String? {
        return System.getProperty(name) ?: System.getenv(name)
    }

    fun mode(): Mode {
        return when (rawMode()?.trim()?.uppercase()) {
            "TIGHT" -> Mode.TIGHT
            "LAG", "LOOSE", "AGGRO" -> Mode.LAG
            else -> Mode.STANDARD
        }
    }

    val riskMoodProbability: Float
        get() {
            val override = envOrProp("STRAT_RISK_FREQ")?.toFloatOrNull()
            if (override != null) return override.coerceIn(0f, 0.5f)
            return when (mode()) {
                Mode.TIGHT -> 0.05f
                Mode.STANDARD -> 0.10f
                Mode.LAG -> 0.25f
            }
        }

    val smallBetThresholdMultiplier: Double
        get() {
            val override = envOrProp("STRAT_SMALLBET_MULT")?.toDoubleOrNull()
            if (override != null) return override.coerceIn(0.5, 2.0)
            return when (mode()) {
                Mode.TIGHT -> 0.8
                Mode.STANDARD -> 1.0
                Mode.LAG -> 1.2
            }
        }

    val allowLateOpenWithWeakPlayable: Boolean
        get() = mode() == Mode.LAG

    val bluffRaiseEnabled: Boolean
        get() = when (envOrProp("STRAT_BLUFF_RAISE")?.trim()?.lowercase()) {
            "0", "false", "off", "no" -> false
            else -> true
        }
}
