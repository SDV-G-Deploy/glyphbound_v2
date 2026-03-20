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
}
