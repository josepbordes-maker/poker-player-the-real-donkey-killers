package poker.player.kotlin

/**
 * Test utilities for isolating tests from external configuration
 */
object TestUtils {
    
    /**
     * Executes a test block with a specific strategy mode override.
     * Restores the original configuration after the test.
     */
    fun withStrategyMode(mode: StrategyConfig.Mode, block: () -> Unit) {
        val prev = System.getProperty("STRAT_MODE")
        try {
            System.setProperty("STRAT_MODE", mode.name)
            block()
        } finally {
            if (prev == null) System.clearProperty("STRAT_MODE") else System.setProperty("STRAT_MODE", prev)
        }
    }
    
    /**
     * Executes a test block with strategy configuration overrides.
     * Restores the original configuration after the test.
     */
    fun withStrategyConfig(
        mode: StrategyConfig.Mode? = null,
        riskFreq: Float? = null,
        smallBetMult: Double? = null,
        bluffRaise: Boolean? = null,
        rainMan: Boolean? = null,
        block: () -> Unit
    ) {
        val prevMode = System.getProperty("STRAT_MODE")
        val prevRiskFreq = System.getProperty("STRAT_RISK_FREQ")
        val prevSmallBetMult = System.getProperty("STRAT_SMALLBET_MULT")
        val prevBluffRaise = System.getProperty("STRAT_BLUFF_RAISE")
        val prevRainMan = System.getProperty("STRAT_RAINMAN")
        
        try {
            mode?.let { System.setProperty("STRAT_MODE", it.name) }
            riskFreq?.let { System.setProperty("STRAT_RISK_FREQ", it.toString()) }
            smallBetMult?.let { System.setProperty("STRAT_SMALLBET_MULT", it.toString()) }
            bluffRaise?.let { System.setProperty("STRAT_BLUFF_RAISE", it.toString()) }
            rainMan?.let { System.setProperty("STRAT_RAINMAN", it.toString()) }
            
            block()
        } finally {
            restoreProperty("STRAT_MODE", prevMode)
            restoreProperty("STRAT_RISK_FREQ", prevRiskFreq)
            restoreProperty("STRAT_SMALLBET_MULT", prevSmallBetMult)
            restoreProperty("STRAT_BLUFF_RAISE", prevBluffRaise)
            restoreProperty("STRAT_RAINMAN", prevRainMan)
        }
    }
    
    private fun restoreProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
