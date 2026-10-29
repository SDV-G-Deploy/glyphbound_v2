package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DungeonNodeType
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.RewardType
import java.util.PriorityQueue

/**
 * Deterministic test bot: hazard-aware shortest path to E, attacks enemies blocking the path,
 * drinks draughts when low, prefers Field Mend, and picks branches by a fixed preference.
 */
object RunBot {
    fun play(
        start: RunSession,
        maxActions: Int = 2_000,
        branchPreference: List<DungeonNodeType> = listOf(DungeonNodeType.REST, DungeonNodeType.TREASURE, DungeonNodeType.COMBAT, DungeonNodeType.ELITE)
    ): RunSession {
        var session = start
        repeat(maxActions) {
            if (session.isOver) return session
            session = when (session.phase) {
                RunPhase.CHOOSING_REWARD -> {
                    val pick = session.rewardChoices.firstOrNull { it.type == RewardType.MEND } ?: session.rewardChoices.first()
                    RunDirector.chooseReward(session, pick.type)
                }
                RunPhase.CHOOSING_BRANCH -> {
                    val pick = session.branchChoices.minBy { node ->
                        branchPreference.indexOf(node.type).let { if (it < 0) Int.MAX_VALUE else it }
                    }
                    RunDirector.chooseBranch(session, pick.id)
                }
                RunPhase.EXPLORING -> {
                    if (session.game.hp <= 3 && session.draughts() > 0 && session.game.hp < session.maxHp) {
                        RunDirector.useDraught(session)
                    } else {
                        RunDirector.move(session, nextDirection(session))
                    }
                }
                else -> session
            }
        }
        return session
    }

    private fun nextDirection(session: RunSession): Direction {
        val game = session.game
        val level = game.level
        // Fight back: kiting enemies otherwise mirror the bot forever.
        game.enemies
            .filter { e -> Direction.entries.any { game.player.offset(it) == e.pos } }
            .minWithOrNull(compareBy({ it.hp }, { it.id }))
            ?.let { target -> return Direction.entries.first { game.player.offset(it) == target.pos } }
        val costs = HashMap<Pos, Int>()
        val parent = HashMap<Pos, Pos>()
        val queue = PriorityQueue<Pair<Int, Pos>>(compareBy<Pair<Int, Pos>> { it.first }.thenBy { it.second.y }.thenBy { it.second.x })
        costs[game.player] = 0
        queue.add(0 to game.player)
        val enemyAt = game.enemies.associateBy { it.pos }
        while (queue.isNotEmpty()) {
            val (cost, pos) = queue.poll()
            if (cost != costs[pos]) continue
            if (pos == level.exit) break
            for (d in Direction.entries) {
                val n = pos.offset(d)
                if (!level.isWalkable(n)) continue
                val tile = level.tileAt(n)
                val hazard = game.hazardZones.count { it.pos == n }
                val enemyCost = enemyAt[n]?.hp?.times(2) ?: 0
                val c = cost + 1 + tile.risk * 6 + hazard * 6 + enemyCost
                if (c < (costs[n] ?: Int.MAX_VALUE)) {
                    costs[n] = c
                    parent[n] = pos
                    queue.add(c to n)
                }
            }
        }
        var cursor = level.exit
        if (cursor !in parent) return Direction.RIGHT
        while (parent[cursor] != game.player) cursor = parent.getValue(cursor)
        return Direction.entries.first { game.player.offset(it) == cursor }
    }

    private fun Pos.offset(d: Direction): Pos = when (d) {
        Direction.UP -> Pos(x, y - 1)
        Direction.DOWN -> Pos(x, y + 1)
        Direction.LEFT -> Pos(x - 1, y)
        Direction.RIGHT -> Pos(x + 1, y)
    }
}
