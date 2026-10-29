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
        assertEquals(7, run.graph.nodes.size)
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
        val save = SaveGame(activeRun = RunCheckpoint(RunState.initial(99L), DifficultyProfile.NORMAL, carryHp = 10))

        assertEquals(SAVE_SCHEMA_VERSION, save.schemaVersion)
        assertEquals("run-99", save.activeRun?.run?.runId)
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
        assertEquals("-1 foe", salvaged.pendingNodeRewardSummary())
        assertEquals("+1 HP", sparked.pendingNodeRewardSummary())
        assertEquals(7, run.applyRewardToHp(RewardType.MEND, 5))
    }

    @Test
    fun pendingNodeRewardEffects_areConsumedOnNodeEntry() {
        val run = RunState.initial(77L)
            .completeCurrent()
            .advanceTo("path_a")
            .applyReward(RewardType.SALVAGE)
            .applyReward(RewardType.SPARK_CORE)

        val consumed = run.consumePendingNodeRewardEffects()

        assertEquals(1, consumed.enemyReduction)
        assertEquals(1, consumed.hpBonus)
        assertTrue(consumed.updatedRun.pendingNodeRewardEffects.isEmpty())
        assertEquals("+1 HP, -1 foe", consumed.summary())
    }

    @Test
    fun depthOne_nodesBothLeadToEliteOrRest_andConvergeOnBoss() {
        val run = RunState.initial(77L)

        assertEquals(setOf("elite", "rest"), run.graph.neighbors("path_a").map { it.id }.toSet())
        assertEquals(setOf("elite", "rest"), run.graph.neighbors("path_b").map { it.id }.toSet())
        assertEquals(listOf("boss"), run.graph.neighbors("elite").map { it.id })
        assertEquals(listOf("boss"), run.graph.neighbors("rest").map { it.id })
        assertEquals(DungeonNodeType.ELITE, run.graph.nodes.first { it.id == "elite" }.type)
    }

    @Test
    fun rewardChoices_eliteOffersAllThree_bossAndRestOfferNone() {
        val base = RunState.initial(77L).completeCurrent()

        assertEquals(3, base.advanceTo("elite").rewardChoicesForCurrent().map { it.type }.toSet().size)
        assertTrue(base.advanceTo("boss").rewardChoicesForCurrent().isEmpty())
        assertTrue(base.advanceTo("rest").rewardChoicesForCurrent().isEmpty())
    }

    @Test
    fun quickItems_stackWithCapAndConsume() {
        var inv = InventoryState()
        assertEquals(null, inv.consumeQuickItem(ItemType.DRAUGHT))

        repeat(5) { inv = inv.addQuickItem(ItemType.DRAUGHT) }
        assertEquals(MAX_QUICK_ITEM_CHARGES, inv.charges(ItemType.DRAUGHT))

        val used = inv.consumeQuickItem(ItemType.DRAUGHT)!!
        assertEquals(MAX_QUICK_ITEM_CHARGES - 1, used.charges(ItemType.DRAUGHT))
        assertEquals(1, used.quickSlots.size)
    }

    @Test
    fun meta_recordsVictoryDefeatDepthAndThemes() {
        val run = RunState.initial(77L)
            .completeCurrent().advanceTo("path_a")
            .completeCurrent().advanceTo("rest")
            .completeCurrent().advanceTo("boss")

        val afterLoss = MetaProgressionState().recordRun(run, RunOutcome.DEFEAT)
        assertEquals(1, afterLoss.totalRuns)
        assertEquals(0, afterLoss.completedRuns)
        assertEquals(3, afterLoss.highestDepth)

        val afterWin = afterLoss.recordRun(run.completeCurrent(), RunOutcome.VICTORY)
        assertEquals(2, afterWin.totalRuns)
        assertEquals(1, afterWin.completedRuns)
        assertTrue(run.graph.nodes.first { it.id == "path_a" }.theme in afterWin.unlockedThemes)
    }
}
