package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class HudRenderModelSnapshotTest {

    @Test
    fun hudLegend_smallScreen_snapshot() {
        val rendered = HudRenderModel.render(
            HudRenderModel.Input(
                hp = 7,
                seed = 1234L,
                profile = "NORMAL",
                configVersion = 1,
                steps = 18,
                effectSummary = "ignite:1",
                hazardSummary = "HZ F1 S0 ttl:2",
                highContrast = false,
                smallScreen = true
            )
        )

        assertEquals("HP 7 | NORMAL | STD | S18 | HZ F1 S0 ttl:2 | FX ignite:1", rendered)
    }

    @Test
    fun overlayConflicts_mixedFireShock_snapshot() {
        val rendered = HudRenderModel.render(
            HudRenderModel.Input(
                hp = 4,
                seed = 9L,
                profile = "HARD",
                configVersion = 1,
                steps = 33,
                effectSummary = "shock:2,ignite:1",
                hazardSummary = "HZ F2 S2 ttl:4 (^ ! &)",
                highContrast = false,
                smallScreen = true
            )
        )

        assertEquals("HP 4 | HARD | STD | S33 | HZ F2 S2 ttl:4 (^ ! &) | FX shock:2,ignite:1", rendered)
    }

    @Test
    fun highContrast_snapshot() {
        val rendered = HudRenderModel.render(
            HudRenderModel.Input(
                hp = 10,
                seed = 77L,
                profile = "EASY",
                configVersion = 1,
                steps = 2,
                effectSummary = "none",
                hazardSummary = "HZ none",
                highContrast = true,
                smallScreen = true
            )
        )

        assertEquals("HP 10 | EASY | HC | S2 | HZ none | FX none", rendered)
    }
}
