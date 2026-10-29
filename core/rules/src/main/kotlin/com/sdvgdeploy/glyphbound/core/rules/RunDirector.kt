package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.DRAUGHT_HEAL
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DungeonNode
import com.sdvgdeploy.glyphbound.core.model.DungeonNodeType
import com.sdvgdeploy.glyphbound.core.model.DungeonTheme
import com.sdvgdeploy.glyphbound.core.model.Enemy
import com.sdvgdeploy.glyphbound.core.model.EnemyDirector
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.ItemType
import com.sdvgdeploy.glyphbound.core.model.RewardChoice
import com.sdvgdeploy.glyphbound.core.model.RewardType
import com.sdvgdeploy.glyphbound.core.model.RunCheckpoint
import com.sdvgdeploy.glyphbound.core.model.RunOutcome
import com.sdvgdeploy.glyphbound.core.model.RunState
import com.sdvgdeploy.glyphbound.core.procgen.LevelGenerator

enum class RunPhase { EXPLORING, CHOOSING_REWARD, CHOOSING_BRANCH, VICTORY, DEFEAT }

/**
 * Whole-run state: the run graph/progression, the current node's tactical [GameState], and which
 * decision the player is facing. Immutable; every transition goes through [RunDirector].
 */
data class RunSession(
    val run: RunState,
    val game: GameState,
    val phase: RunPhase,
    val checkpoint: RunCheckpoint,
    val rewardChoices: List<RewardChoice> = emptyList(),
    val branchChoices: List<DungeonNode> = emptyList(),
    val totalMoves: Int = 0
) {
    val profile: DifficultyProfile get() = game.profile
    val isOver: Boolean get() = phase == RunPhase.VICTORY || phase == RunPhase.DEFEAT
    val outcome: RunOutcome?
        get() = when (phase) {
            RunPhase.VICTORY -> RunOutcome.VICTORY
            RunPhase.DEFEAT -> RunOutcome.DEFEAT
            else -> null
        }

    /** Max HP used to cap healing from rests and draughts (rewards like Field Mend may overheal). */
    val maxHp: Int get() = profile.startingHp

    fun draughts(): Int = run.inventory.charges(ItemType.DRAUGHT)

    fun summaryLine(): String {
        val node = run.currentNode()
        val depth = node.depth
        val cleared = run.completedNodeIds.size
        return when (phase) {
            RunPhase.VICTORY -> "Run cleared • $totalMoves moves • $cleared nodes • HP ${game.hp} • salvage ${run.progression.salvage}"
            RunPhase.DEFEAT -> "Run lost at depth $depth (${node.type.displayName()}) • $totalMoves moves • $cleared nodes cleared"
            else -> "Depth $depth • ${node.type.displayName()} • $cleared cleared"
        }
    }
}

object RunDirector {
    const val REST_HEAL: Int = 3
    const val LOG_SIZE: Int = 8

    fun start(seed: Long, profile: DifficultyProfile): RunSession =
        enterNode(RunCheckpoint(run = RunState.initial(seed), profile = profile, carryHp = profile.startingHp))

    /** Rebuilds the session from a node-entry checkpoint (e.g. a loaded save). Deterministic. */
    fun resume(checkpoint: RunCheckpoint): RunSession = enterNode(checkpoint, priorLog = listOf("Resumed run"))

    fun move(session: RunSession, direction: Direction): RunSession {
        if (session.phase != RunPhase.EXPLORING) return session
        val stepped = step(session.game, direction)
        val moved = session.copy(game = stepped, totalMoves = session.totalMoves + 1)
        return when {
            stepped.won -> onNodeCleared(moved)
            stepped.finished -> moved.copy(
                phase = RunPhase.DEFEAT,
                game = stepped.withLog(moved.copy(phase = RunPhase.DEFEAT).summaryLine())
            )
            else -> moved
        }
    }

    fun chooseReward(session: RunSession, type: RewardType): RunSession {
        if (session.phase != RunPhase.CHOOSING_REWARD) return session
        val selected = session.rewardChoices.firstOrNull { it.type == type } ?: return session
        val run = session.run.applyReward(type)
        val pending = run.pendingNodeRewardSummary()
        val line = if (pending == "none") "Reward: ${selected.label}" else "Reward: ${selected.label} (next: $pending)"
        val game = session.game.copy(hp = run.applyRewardToHp(type, session.game.hp)).withLog(line)
        return advance(session.copy(run = run, game = game, rewardChoices = emptyList()))
    }

    fun chooseBranch(session: RunSession, nodeId: String): RunSession {
        if (session.phase != RunPhase.CHOOSING_BRANCH) return session
        val selected = session.branchChoices.firstOrNull { it.id == nodeId } ?: return session
        return enterNode(
            RunCheckpoint(
                run = session.run.advanceTo(selected.id),
                profile = session.profile,
                carryHp = session.game.hp.coerceAtLeast(1),
                totalMoves = session.totalMoves
            ),
            priorLog = session.game.messageLog
        )
    }

    /** Drinks a draught: heals up to [RunSession.maxHp]; free action (no enemy turn). */
    fun useDraught(session: RunSession): RunSession {
        if (session.phase != RunPhase.EXPLORING) return session
        val inventory = session.run.inventory.consumeQuickItem(ItemType.DRAUGHT) ?: return session
        val healed = (session.game.hp + DRAUGHT_HEAL).coerceAtMost(maxOf(session.maxHp, session.game.hp))
        val gained = healed - session.game.hp
        return session.copy(
            run = session.run.copy(inventory = inventory),
            game = session.game.copy(hp = healed).withLog("Drank ${ItemType.DRAUGHT.label}: +$gained HP")
        )
    }

    private fun onNodeCleared(session: RunSession): RunSession {
        val node = session.run.currentNode()
        var run = session.run
        var game = session.game
        if (node.type == DungeonNodeType.TREASURE) {
            run = run.copy(inventory = run.inventory.addQuickItem(ItemType.DRAUGHT))
            game = game.withLog("Found ${ItemType.DRAUGHT.label} (${run.inventory.charges(ItemType.DRAUGHT)})")
        }
        val rewards = run.rewardChoicesForCurrent()
        val updated = session.copy(run = run, game = game)
        if (rewards.isNotEmpty()) {
            return updated.copy(
                phase = RunPhase.CHOOSING_REWARD,
                rewardChoices = rewards,
                game = game.withLog("Choose reward")
            )
        }
        return advance(updated)
    }

    private fun advance(session: RunSession): RunSession {
        val completed = session.run.completeCurrent()
        val next = completed.graph.neighbors().filter { it.id !in completed.completedNodeIds }
        if (next.isEmpty() || next.all { it.type == DungeonNodeType.EXIT }) {
            val won = session.copy(run = completed, phase = RunPhase.VICTORY, rewardChoices = emptyList(), branchChoices = emptyList())
            return won.copy(game = session.game.withLog(won.summaryLine()))
        }
        if (next.size > 1) {
            return session.copy(
                run = completed,
                phase = RunPhase.CHOOSING_BRANCH,
                branchChoices = next,
                rewardChoices = emptyList(),
                game = session.game.withLog("Choose next path")
            )
        }
        val target = next.first()
        return enterNode(
            RunCheckpoint(
                run = completed.advanceTo(target.id),
                profile = session.profile,
                carryHp = session.game.hp.coerceAtLeast(1),
                totalMoves = session.totalMoves
            ),
            priorLog = session.game.messageLog
        )
    }

    private fun enterNode(checkpoint: RunCheckpoint, priorLog: List<String> = listOf("Reach E")): RunSession {
        val profile = checkpoint.profile
        val entry = checkpoint.run.consumePendingNodeRewardEffects()
        val run = entry.updatedRun
        val node = run.currentNode()
        val level = LevelGenerator.generate(node.floorSeed, profile)

        var hp = (checkpoint.carryHp + entry.hpBonus).coerceAtLeast(1)
        val restHeal = if (node.type == DungeonNodeType.REST) {
            val healed = (hp + REST_HEAL).coerceAtMost(maxOf(profile.startingHp, hp))
            (healed - hp).also { hp = healed }
        } else 0

        val spawned = EnemyDirector.spawnInitial(level = level, profile = profile, nodeType = node.type, theme = node.theme)
        val reduced = reduceEnemies(spawned, entry.enemyReduction, node.type)

        val intro = "${node.type.displayName()} • ${node.theme.displayName()}"
        val lines = buildList {
            add(intro)
            if (entry.hpBonus > 0) add("Reward surge: +${entry.hpBonus} HP")
            if (reduced.size < spawned.size) add("Reward scouting: -${spawned.size - reduced.size} foe")
            if (restHeal > 0) add("Campfire: +$restHeal HP")
            if (node.type == DungeonNodeType.BOSS) add("Boss floor: defeat or evade the warden pack and reach E")
        }
        val log = (priorLog + lines).takeLast(LOG_SIZE)

        val game = refreshEnemyIntents(
            GameState(
                level = level,
                player = level.entry,
                profile = profile,
                hp = hp,
                message = log.last(),
                messageLog = log,
                enemies = reduced
            )
        )
        return RunSession(
            run = run,
            game = game,
            phase = RunPhase.EXPLORING,
            checkpoint = checkpoint,
            totalMoves = checkpoint.totalMoves
        )
    }

    /** Scouting removes the most dangerous foes first, but combat-type nodes always keep at least one. */
    internal fun reduceEnemies(enemies: List<Enemy>, reduction: Int, nodeType: DungeonNodeType): List<Enemy> {
        if (reduction <= 0 || enemies.isEmpty()) return enemies
        val minimum = if (nodeType in setOf(DungeonNodeType.COMBAT, DungeonNodeType.ELITE, DungeonNodeType.BOSS)) 1 else 0
        val toRemove = reduction.coerceAtMost((enemies.size - minimum).coerceAtLeast(0))
        if (toRemove <= 0) return enemies
        val ids = enemies
            .sortedWith(
                compareByDescending<Enemy> { it.archetype.contactDamage }
                    .thenByDescending { it.archetype.maxHp }
                    .thenBy { it.id }
            )
            .take(toRemove)
            .map { it.id }
            .toSet()
        return enemies.filterNot { it.id in ids }
    }

    private fun GameState.withLog(line: String): GameState =
        copy(message = line, messageLog = (messageLog + line).takeLast(LOG_SIZE))
}

fun DungeonNodeType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun DungeonTheme.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
