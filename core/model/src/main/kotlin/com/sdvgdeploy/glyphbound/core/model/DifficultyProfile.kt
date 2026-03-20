package com.sdvgdeploy.glyphbound.core.model

data class EnvTuning(
    val ambientRiskChance: Double,
    val oilChance: Double,
    val waterChance: Double,
    val sparkChance: Double,
    val hazardDamageMultiplier: Int,
    val ignitionTurns: Int,
    val ignitionTickDamage: Int,
    val shockTurns: Int,
    val shockTickDamage: Int
)

enum class DifficultyProfile(
    val wallChance: Double,
    val minDisjointPaths: Int,
    val useNodeDisjoint: Boolean,
    val startingHp: Int,
    val env: EnvTuning
) {
    EASY(
        wallChance = 0.24,
        minDisjointPaths = 2,
        useNodeDisjoint = false,
        startingHp = 14,
        env = EnvTuning(
            ambientRiskChance = 0.03,
            oilChance = 0.03,
            waterChance = 0.07,
            sparkChance = 0.03,
            hazardDamageMultiplier = 1,
            ignitionTurns = 2,
            ignitionTickDamage = 1,
            shockTurns = 1,
            shockTickDamage = 1
        )
    ),
    NORMAL(
        wallChance = 0.30,
        minDisjointPaths = 2,
        useNodeDisjoint = false,
        startingHp = 10,
        env = EnvTuning(
            ambientRiskChance = 0.05,
            oilChance = 0.05,
            waterChance = 0.05,
            sparkChance = 0.05,
            hazardDamageMultiplier = 1,
            ignitionTurns = 2,
            ignitionTickDamage = 2,
            shockTurns = 2,
            shockTickDamage = 1
        )
    ),
    HARD(
        wallChance = 0.36,
        minDisjointPaths = 2,
        useNodeDisjoint = true,
        startingHp = 8,
        env = EnvTuning(
            ambientRiskChance = 0.07,
            oilChance = 0.07,
            waterChance = 0.04,
            sparkChance = 0.07,
            hazardDamageMultiplier = 2,
            ignitionTurns = 3,
            ignitionTickDamage = 2,
            shockTurns = 2,
            shockTickDamage = 2
        )
    );

    companion object {
        fun fromRaw(value: String?): DifficultyProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
    }
}
