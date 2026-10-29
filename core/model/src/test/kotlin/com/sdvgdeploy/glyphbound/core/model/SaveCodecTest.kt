package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveCodecTest {
    private fun midRunSave(): SaveGame {
        val run = RunState.initial(4242L)
            .completeCurrent().advanceTo("path_b")
            .completeCurrent()
            .applyReward(RewardType.SALVAGE)
            .applyReward(RewardType.SPARK_CORE)
            .let { it.copy(inventory = it.inventory.addQuickItem(ItemType.DRAUGHT, 2)) }
            .advanceTo("elite")
        return SaveGame(
            activeRun = RunCheckpoint(run = run, profile = DifficultyProfile.HARD, carryHp = 5, totalMoves = 61),
            meta = MetaProgressionState(completedRuns = 2, highestDepth = 4, totalRuns = 5, unlockedThemes = setOf(DungeonTheme.NEUTRAL, DungeonTheme.EMBER))
        )
    }

    @Test
    fun roundTrip_preservesEverything() {
        val save = midRunSave()

        val decoded = SaveCodec.decode(SaveCodec.encode(save))

        assertIs<SaveCodec.DecodeResult.Ok>(decoded)
        assertEquals(save, decoded.save)
    }

    @Test
    fun encode_isStableForSameInput() {
        assertEquals(SaveCodec.encode(midRunSave()), SaveCodec.encode(midRunSave()))
    }

    @Test
    fun metaOnlySave_roundTrips() {
        val save = SaveGame(meta = MetaProgressionState(completedRuns = 1, totalRuns = 1))
        val decoded = SaveCodec.decode(SaveCodec.encode(save))
        assertIs<SaveCodec.DecodeResult.Ok>(decoded)
        assertEquals(null, decoded.save.activeRun)
        assertEquals(save.meta, decoded.save.meta)
    }

    @Test
    fun encodedSave_carriesSchemaVersion() {
        assertTrue(SaveCodec.encode(SaveGame()).contains("\"schemaVersion\":$SAVE_SCHEMA_VERSION"))
    }

    @Test
    fun newerSchema_isRejected() {
        val future = SaveCodec.encode(midRunSave()).replace("\"schemaVersion\":$SAVE_SCHEMA_VERSION", "\"schemaVersion\":${SAVE_SCHEMA_VERSION + 1}")
        assertEquals(SaveCodec.DecodeResult.UnsupportedVersion(SAVE_SCHEMA_VERSION + 1), SaveCodec.decode(future))
    }

    @Test
    fun corruptOrInconsistentInput_isReportedNotThrown() {
        assertIs<SaveCodec.DecodeResult.Corrupt>(SaveCodec.decode("not json"))
        assertIs<SaveCodec.DecodeResult.Corrupt>(SaveCodec.decode("{}"))
        val badNode = SaveCodec.encode(midRunSave()).replace("\"currentNodeId\":\"elite\"", "\"currentNodeId\":\"nowhere\"")
        assertIs<SaveCodec.DecodeResult.Corrupt>(SaveCodec.decode(badNode))
        val badProfile = SaveCodec.encode(midRunSave()).replace("\"profile\":\"HARD\"", "\"profile\":\"NIGHTMARE\"")
        assertIs<SaveCodec.DecodeResult.Corrupt>(SaveCodec.decode(badProfile))
    }
}
