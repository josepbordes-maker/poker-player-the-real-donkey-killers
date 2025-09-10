package poker.player.kotlin

import org.json.JSONObject
import java.nio.charset.StandardCharsets

object StrategyConfig {
    enum class Mode { TIGHT, STANDARD, LAG }

    private fun rawMode(): String? {
        return System.getProperty("STRAT_MODE") ?: System.getenv("STRAT_MODE") ?: fileConfig.mode
    }

    private fun envOrProp(name: String, fallback: String? = null): String? {
        return System.getProperty(name) ?: System.getenv(name) ?: fallback
    }

    private data class FileConfig(
        val mode: String? = null,
        val riskFreq: String? = null,
        val smallBetMult: String? = null,
        val bluffRaise: String? = null,
        val rainMan: String? = null
    )

    private val fileConfig: FileConfig by lazy {
        try {
            val stream = StrategyConfig::class.java.getResourceAsStream("/strategy.json")
                ?: return@lazy FileConfig()
            val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            FileConfig(
                mode = json.optString("mode", null),
                riskFreq = json.opt("riskFreq")?.toString(),
                smallBetMult = json.opt("smallBetMult")?.toString(),
                bluffRaise = json.opt("bluffRaise")?.toString(),
                rainMan = json.opt("rainMan")?.toString()
            )
        } catch (_: Exception) {
            FileConfig()
        }
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
            val override = envOrProp("STRAT_RISK_FREQ", fileConfig.riskFreq)?.toFloatOrNull()
            if (override != null) return override.coerceIn(0f, 0.5f)
            return when (mode()) {
                Mode.TIGHT -> 0.05f
                Mode.STANDARD -> 0.10f
                Mode.LAG -> 0.25f
            }
        }

    val smallBetThresholdMultiplier: Double
        get() {
            val override = envOrProp("STRAT_SMALLBET_MULT", fileConfig.smallBetMult)?.toDoubleOrNull()
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
        get() = when (envOrProp("STRAT_BLUFF_RAISE", fileConfig.bluffRaise)?.trim()?.lowercase()) {
            "0", "false", "off", "no" -> false
            else -> true
        }

    val enableRainMan: Boolean
        get() = when (envOrProp("STRAT_RAINMAN", fileConfig.rainMan)?.trim()?.lowercase()) {
            "0", "false", "off", "no" -> false
            else -> true
        }
}
