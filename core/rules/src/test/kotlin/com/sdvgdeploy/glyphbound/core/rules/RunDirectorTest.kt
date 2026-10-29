package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DungeonNodeType
import com.sdvgdeploy.glyphbound.core.model.ItemType
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.RewardType
import com.sdvgdeploy.glyphbound.core.model.RunCheckpoint
import com.sdvgdeploy.glyphbound.core.model.RunOutcome
import com.sdvgdeploy.glyphbound.core.model.RunState
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RunDirectorTest {
    @Test
    fun start_isDeterministicAndBeginsExploringAtEntry() {
        val a = RunDirector.start(1337L, DifficultyProfile.NORMAL)
        val b = RunDirector.start(1337L, DifficultyProfile.NORMAL)

        assertEquals(a, b)
        assertEquals(RunPhase.EXPLORING, a.phase)
        assertEquals("entry", a.run.graph.currentNodeId)
        assertEquals(DifficultyProfile.NORMAL.startingHp, a.game.hp)
        assertNull(a.outcome)
    }

    @Test
    fun clearingEntry_offersBranchBetweenCombatAndTreasure() {
        val cleared = clearCurrentNode(RunDirector.start(77L, DifficultyProfile.NORMAL))

        assertEquals(RunPhase.CHOOSING_BRANCH, cleared.phase)
        assertEquals(setOf(DungeonNodeType.COMBAT, DungeonNodeType.TREASURE), cleared.branchChoices.map { it.type }.toSet())
        assertTrue("entry" in cleared.run.completedNodeIds)
    }

    @Test
    fun movesAreIgnoredWhileADecisionIsPending() {
        val cleared = clearCurrentNode(RunDirector.start(77L, DifficultyProfile.NORMAL))

        assertSame(cleared, RunDirector.move(cleared, Direction.UP))
        assertSame(cleared, RunDirector.chooseBranch(cleared, "not-a-node"))
        assertSame(cleared, RunDirector.chooseReward(cleared, RewardType.MEND))
    }

    @Test
    fun combatNode_offersTwoRewards_eliteOffersThree() {
        val combat = clearCurrentNode(enterAt("path_a"))
        val elite = clearCurrentNode(enterAt("elite", completed = listOf("entry", "path_a")))

        assertEquals(RunPhase.CHOOSING_REWARD, combat.phase)
        assertEquals(2, combat.rewardChoices.size)
        assertEquals(RunPhase.CHOOSING_REWARD, elite.phase)
        assertEquals(3, elite.rewardChoices.size)
    }

    @Test
    fun rewardChoice_thenBranchChoice_leadsIntoDepthTwo() {
        val combat = clearCurrentNode(enterAt("path_a"))
        val afterReward = RunDirector.chooseReward(combat, combat.rewardChoices.first().type)

        assertEquals(RunPhase.CHOOSING_BRANCH, afterReward.phase)
        assertEquals(setOf("elite", "rest"), afterReward.branchChoices.map { it.id }.toSet())

        val atRest = RunDirector.chooseBranch(afterReward, "rest")
        assertEquals(RunPhase.EXPLORING, atRest.phase)
        assertEquals("rest", atRest.run.graph.currentNodeId)
    }

    @Test
    fun restNode_hasNoEnemiesAndHealsUpToMaxHp() {
        val profile = DifficultyProfile.NORMAL
        val wounded = enterAt("rest", completed = listOf("entry", "path_a"), carryHp = 2, profile = profile)
        val healthy = enterAt("rest", completed = listOf("entry", "path_a"), carryHp = profile.startingHp, profile = profile)

        assertTrue(wounded.game.enemies.isEmpty())
        assertEquals(2 + RunDirector.REST_HEAL, wounded.game.hp)
        assertEquals(profile.startingHp, healthy.game.hp, "rest never overheals above max HP")
        assertTrue(wounded.game.messageLog.any { it.startsWith("Campfire") })
    }

    @Test
    fun treasureClear_grantsDraught_andDraughtHealsCappedAtMax() {
        val cleared = clearCurrentNode(enterAt("path_b"))
        assertEquals(1, cleared.draughts())

        val next = RunDirector.chooseBranch(cleared, "elite")
        val wounded = next.copy(game = next.game.copy(hp = 2))
        val drank = RunDirector.useDraught(wounded)

        assertEquals(2 + 3, drank.game.hp)
        assertEquals(0, drank.draughts())
        assertEquals(wounded.game.moves, drank.game.moves, "drinking is a free action")
        assertSame(drank, RunDirector.useDraught(drank), "no draught left -> no-op")

        val nearlyFull = next.copy(game = next.game.copy(hp = next.maxHp - 1))
        assertEquals(next.maxHp, RunDirector.useDraught(nearlyFull).game.hp)
    }

    @Test
    fun bossClear_endsRunInVictory() {
        val boss = enterAt("boss", completed = listOf("entry", "path_a", "rest"))
        assertTrue(boss.game.enemies.size >= 2, "boss floor spawns a pack")

        val won = clearCurrentNode(boss)

        assertEquals(RunPhase.VICTORY, won.phase)
        assertEquals(RunOutcome.VICTORY, won.outcome)
        assertTrue(won.isOver)
        assertTrue(won.game.message.startsWith("Run cleared"))
        assertSame(won, RunDirector.move(won, Direction.LEFT))
    }

    @Test
    fun dyingEndsRunInDefeat() {
        val start = RunDirector.start(5L, DifficultyProfile.NORMAL)
        val level = levelFrom(listOf("#####", "#S~E#", "#####"))
        val doomed = start.copy(game = start.game.copy(level = level, player = level.entry, hp = 1, enemies = emptyList()))

        val dead = RunDirector.move(doomed, Direction.RIGHT)

        assertEquals(RunPhase.DEFEAT, dead.phase)
        assertEquals(RunOutcome.DEFEAT, dead.outcome)
        assertTrue(dead.game.message.startsWith("Run lost"))
        assertSame(dead, RunDirector.move(dead, Direction.RIGHT))
    }

    @Test
    fun resumeFromCheckpoint_rebuildsTheSameNode() {
        val combat = clearCurrentNode(enterAt("path_a"))
        val afterReward = RunDirector.chooseReward(combat, RewardType.SALVAGE)
        val entered = RunDirector.chooseBranch(afterReward, "elite")

        val resumed = RunDirector.resume(entered.checkpoint)

        assertEquals(entered.run, resumed.run)
        assertEquals(entered.game.level, resumed.game.level)
        assertEquals(entered.game.enemies, resumed.game.enemies)
        assertEquals(entered.game.hp, resumed.game.hp)
        assertEquals(entered.totalMoves, resumed.totalMoves)
        assertEquals("Resumed run", resumed.game.messageLog.first { it == "Resumed run" })
    }

    @Test
    fun botPlaythrough_isDeterministic() {
        val a = RunBot.play(RunDirector.start(2024L, DifficultyProfile.NORMAL))
        val b = RunBot.play(RunDirector.start(2024L, DifficultyProfile.NORMAL))

        assertTrue(a.isOver)
        assertEquals(a, b)
    }

    @Test
    fun demoSeeds_areClearableByTheBot() {
        // Seeds referenced by docs/DEMO_SCRIPT.md: keep them winnable when tuning changes.
        for ((seed, profile) in listOf(DEMO_SEED to DifficultyProfile.EASY, 1L to DifficultyProfile.NORMAL)) {
            val result = RunBot.play(RunDirector.start(seed, profile))
            assertEquals(RunPhase.VICTORY, result.phase, "demo seed $seed should be clearable on $profile: ${result.summaryLine()}")
            assertTrue("boss" in result.run.completedNodeIds)
        }
    }

    @Test
    fun seedSweep_runsAlwaysTerminateAndManyAreWinnable() {
        val results = (1L..40L).map { seed -> RunBot.play(RunDirector.start(seed, DifficultyProfile.NORMAL)) }

        assertTrue(results.all { it.isOver }, "every run must reach victory or defeat")
        val wins = results.count { it.phase == RunPhase.VICTORY }
        assertTrue(wins >= 15, "expected the bot to clear a healthy share of NORMAL seeds, got $wins/40")
        assertTrue(wins < 40, "NORMAL should still be able to kill a naive bot")
    }

    // --- helpers ---

    private fun enterAt(
        nodeId: String,
        completed: List<String> = listOf("entry"),
        carryHp: Int? = null,
        profile: DifficultyProfile = DifficultyProfile.NORMAL,
        seed: Long = 77L
    ): RunSession {
        var run = RunState.initial(seed)
        completed.forEach { id -> run = run.advanceTo(id).completeCurrent() }
        run = run.advanceTo(nodeId)
        return RunDirector.resume(RunCheckpoint(run = run, profile = profile, carryHp = carryHp ?: profile.startingHp))
    }

    /** Teleports next to E with no enemies, then steps onto E (E itself deals no damage). */
    private fun clearCurrentNode(session: RunSession): RunSession {
        val level = session.game.level
        val left = Pos(level.exit.x - 1, level.exit.y)
        val up = Pos(level.exit.x, level.exit.y - 1)
        val (from, dir) = if (level.isWalkable(left)) left to Direction.RIGHT else up to Direction.DOWN
        val staged = session.copy(game = session.game.copy(player = from, enemies = emptyList(), hazardZones = emptyList()))
        return RunDirector.move(staged, dir)
    }

    private fun levelFrom(rows: List<String>): Level {
        val tiles = rows.map { row -> row.map { c -> Tile.entries.first { it.glyph == c } }.toMutableList() }
        fun find(c: Char): Pos {
            rows.forEachIndexed { y, row -> val x = row.indexOf(c); if (x >= 0) return Pos(x, y) }
            error("missing $c")
        }
        return Level(rows.first().length, rows.size, 9L, tiles, find('S'), find('E'))
    }

    companion object {
        const val DEMO_SEED: Long = 1337L
    }
}
