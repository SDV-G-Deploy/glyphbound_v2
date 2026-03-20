package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DifficultyTuningCatalogTest {
    @Test
    fun unknownProfile_fallsBackToNormal() {
        val unknown = DifficultyTuningCatalog.resolve("NOPE")
        val normal = DifficultyTuningCatalog.resolve("NORMAL")
        assertEquals(normal, unknown)
    }

    @Test
    fun allProfiles_haveBoundedValidatedValues() {
        DifficultyProfile.entries.forEach { profile ->
            val env = profile.env
            assertTrue(profile.wallChance in 0.05..0.60)
            assertTrue(env.hazardDamageMultiplier in 1..4)
            assertTrue(env.fireZoneTtl in 1..8)
            assertTrue(env.shockZoneTtl in 1..8)
            assertTrue(env.fireSpreadProfile.spreadChance in 0.0..1.0)
            assertTrue(env.shockSpreadProfile.spreadChance in 0.0..1.0)
            assertTrue(env.fireSpreadProfile.maxTargets in 0..8)
            assertTrue(env.shockSpreadProfile.maxTargets in 0..8)
            assertEquals(DifficultyTuningCatalog.CONFIG_VERSION, env.configVersion)
        }
    }

    @Test
    fun unsupportedConfigVersion_returnsFailureForLoader() {
        val raw = """
            {
              "configVersion": 99,
              "profiles": {
                "NORMAL": {
                  "wallChance": 0.3,
                  "minDisjointPaths": 2,
                  "useNodeDisjoint": false,
                  "startingHp": 10,
                  "env": {
                    "ambientRiskChance": 0.05,
                    "oilChance": 0.05,
                    "waterChance": 0.05,
                    "sparkChance": 0.05,
                    "hazardDamageMultiplier": 1,
                    "ignitionTurns": 2,
                    "ignitionTickDamage": 2,
                    "shockTurns": 2,
                    "shockTickDamage": 1,
                    "fireZoneTtl": 3,
                    "shockZoneTtl": 2,
                    "fireSpreadProfile": { "spreadChance": 0.5, "maxTargets": 2, "maxChainDepth": 1 },
                    "shockSpreadProfile": { "spreadChance": 0.4, "maxTargets": 2, "maxChainDepth": 1 }
                  }
                }
              }
            }
        """.trimIndent()

        val result = DifficultyTuningCatalog.loadFromRawForTests(raw)
        assertTrue(result.isFailure)
    }

    @Test
    fun configReload_keepsDeterministicResolvedValues() {
        val before = DifficultyTuningCatalog.resolve("HARD")
        DifficultyTuningCatalog.resetForTests()
        val after = DifficultyTuningCatalog.resolve("HARD")

        assertEquals(before, after)
    }
}
