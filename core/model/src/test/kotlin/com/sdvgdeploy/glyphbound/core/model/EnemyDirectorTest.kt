package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnemyDirectorTest {
    @Test
    fun spawnInitial_isDeterministicForSameEncounterContext() {
        val level = openLevel(seed = 777L)

        val first = EnemyDirector.spawnInitial(
            level = level,
            profile = DifficultyProfile.NORMAL,
            nodeType = DungeonNodeType.COMBAT,
            theme = DungeonTheme.FLOODED
        )
        val second = EnemyDirector.spawnInitial(
            level = level,
            profile = DifficultyProfile.NORMAL,
            nodeType = DungeonNodeType.COMBAT,
            theme = DungeonTheme.FLOODED
        )

        assertEquals(first, second)
    }

    @Test
    fun branchContext_changesCompositionAndPressurePattern() {
        val level = openLevel(seed = 1337L)

        val combatBranch = EnemyDirector.spawnInitial(
            level = level,
            profile = DifficultyProfile.NORMAL,
            nodeType = DungeonNodeType.COMBAT,
            theme = DungeonTheme.FLOODED
        )
        val treasureBranch = EnemyDirector.spawnInitial(
            level = level,
            profile = DifficultyProfile.NORMAL,
            nodeType = DungeonNodeType.TREASURE,
            theme = DungeonTheme.EMBER
        )

        assertEquals(EnemyArchetype.SPITTER, combatBranch.first().archetype)
        assertEquals(EnemyArchetype.STALKER, treasureBranch.first().archetype)

        val combatDistance = manhattan(combatBranch.first().pos, level.entry)
        val treasureDistance = manhattan(treasureBranch.first().pos, level.entry)
        assertTrue(combatDistance < treasureDistance, "combat branch should start with closer pressure than treasure detour")
    }

    @Test
    fun hardCombat_addsThemeSupportEnemy() {
        val level = openLevel(seed = 42L)

        val hardEmberCombat = EnemyDirector.spawnInitial(
            level = level,
            profile = DifficultyProfile.HARD,
            nodeType = DungeonNodeType.COMBAT,
            theme = DungeonTheme.EMBER
        )

        assertEquals(2, hardEmberCombat.size)
        assertEquals(listOf(EnemyArchetype.BRUTE, EnemyArchetype.STALKER), hardEmberCombat.map { it.archetype })
    }

    private fun openLevel(seed: Long, width: Int = 12, height: Int = 8): Level {
        val tiles = MutableList(height) { y ->
            MutableList(width) { x ->
                val border = x == 0 || y == 0 || x == width - 1 || y == height - 1
                if (border) Tile.WALL else Tile.FLOOR
            }
        }

        val entry = Pos(1, 1)
        val exit = Pos(width - 2, height - 2)
        tiles[entry.y][entry.x] = Tile.ENTRY
        tiles[exit.y][exit.x] = Tile.EXIT

        return Level(
            width = width,
            height = height,
            seed = seed,
            tiles = tiles,
            entry = entry,
            exit = exit
        )
    }

    private fun manhattan(a: Pos, b: Pos): Int = kotlin.math.abs(a.x - b.x) + kotlin.math.abs(a.y - b.y)
}
