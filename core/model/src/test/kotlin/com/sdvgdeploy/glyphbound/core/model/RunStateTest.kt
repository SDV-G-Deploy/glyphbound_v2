package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunStateTest {
    @Test
    fun initialRun_isDeterministicForSameSeed() {
        val a = RunState.initial(1234L)
        val b = RunState.initial(1234L)

        assertEquals(a, b)
    }

    @Test
    fun initialRun_buildsBranchingGraphSkeleton() {
        val run = RunState.initial(77L)

        assertEquals("entry", run.graph.currentNodeId)
        assertEquals(6, run.graph.nodes.size)
        assertEquals(setOf("path_a", "path_b"), run.graph.neighbors().map { it.id }.toSet())
        assertTrue(run.graph.nodes.any { it.type == DungeonNodeType.BOSS })
        assertTrue(run.graph.nodes.any { it.type == DungeonNodeType.EXIT })
    }

    @Test
    fun completeAndAdvance_updatesVisitedAndCompletedNodes() {
        val run = RunState.initial(77L)
            .completeCurrent()
            .advanceTo("path_a")

        assertTrue("entry" in run.completedNodeIds)
        assertEquals("path_a", run.graph.currentNodeId)
        assertTrue("path_a" in run.visitedNodeIds)
        assertTrue(run.graph.currentNode().discovered)
    }


    @Test
    fun entryNode_exposesTwoExplicitBranchChoices() {
        val run = RunState.initial(77L)

        val choices = run.graph.neighbors()

        assertEquals(2, choices.size)
        assertEquals(setOf(DungeonNodeType.COMBAT, DungeonNodeType.TREASURE), choices.map { it.type }.toSet())
    }

    @Test
    fun saveGame_defaultsToSchemaVersionOne() {
        val save = SaveGame(activeRun = RunState.initial(99L))

        assertEquals(SAVE_SCHEMA_VERSION, save.schemaVersion)
        assertEquals("run-99", save.activeRun?.runId)
        assertTrue("SCAVENGER" in save.meta.unlockedArchetypes)
    }

    @Test
    fun rewardChoices_areDeterministicForCombatNode() {
        val run = RunState.initial(77L).completeCurrent().advanceTo("path_a")

        val first = run.rewardChoicesForCurrent()
        val second = run.rewardChoicesForCurrent()

        assertEquals(first, second)
        assertEquals(2, first.size)
    }

    @Test
    fun applyReward_updatesPersistentProgressionAndHpRules() {
        val run = RunState.initial(77L).completeCurrent().advanceTo("path_a")

        val salvaged = run.applyReward(RewardType.SALVAGE)
        val sparked = run.applyReward(RewardType.SPARK_CORE)

        assertEquals(run.progression.salvage + 1, salvaged.progression.salvage)
        assertTrue("SPARK_CORE" in sparked.progression.unlockedTraits)
        assertEquals(7, run.applyRewardToHp(RewardType.MEND, 5))
    }
}
