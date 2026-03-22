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
                nodeLabel = "combat-1-ember",
                progressionSummary = "PG L1 X0 \$1 T0",
                effectSummary = "ignite:1",
                hazardSummary = "HZ F1 S0 ttl:2",
                enemyIntentSummary = "EN M1 R0 A1",
                highContrast = false,
                smallScreen = true
            )
        )

        assertEquals("HP 7 | NORMAL | combat-1-ember | PG L1 X0 \$1 T0 | STD | S18 | HZ F1 S0 ttl:2 | EN M1 R0 A1 | FX ignite:1", rendered)
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
                nodeLabel = "boss-3-ember",
                progressionSummary = "PG L1 X4 \$2 T1",
                effectSummary = "shock:2,ignite:1",
                hazardSummary = "HZ F2 S2 ttl:4 (^ ! &)",
                enemyIntentSummary = "EN M0 R1 A2",
                highContrast = false,
                smallScreen = true
            )
        )

        assertEquals("HP 4 | HARD | boss-3-ember | PG L1 X4 \$2 T1 | STD | S33 | HZ F2 S2 ttl:4 (^ ! &) | EN M0 R1 A2 | FX shock:2,ignite:1", rendered)
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
                nodeLabel = "entry-0-neutral",
                progressionSummary = "PG L1 X0 \$0 T0",
                effectSummary = "none",
                hazardSummary = "HZ none",
                enemyIntentSummary = "EN none",
                highContrast = true,
                smallScreen = true
            )
        )

        assertEquals("HP 10 | EASY | entry-0-neutral | PG L1 X0 \$0 T0 | HC | S2 | HZ none | EN none | FX none", rendered)
    }
}
