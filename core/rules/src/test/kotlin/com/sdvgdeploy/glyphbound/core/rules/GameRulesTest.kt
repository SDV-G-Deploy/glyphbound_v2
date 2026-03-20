package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRulesTest {
    @Test
    fun steppingOnRiskTile_reducesHp() {
        val tiles = mutableListOf(
            mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL),
            mutableListOf(Tile.ENTRY, Tile.RISK, Tile.EXIT),
            mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL)
        )
        val level = Level(3, 3, 1L, tiles, Pos(0, 1), Pos(2, 1))
        val initial = GameState(level = level, player = level.entry, hp = 10)

        val next = step(initial, Direction.RIGHT)

        assertEquals(9, next.hp)
    }

    @Test
    fun oilPlusSpark_triggersIgnitionEffect() {
        val tiles = mutableListOf(
            mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL),
            mutableListOf(Tile.WALL, Tile.ENTRY, Tile.OIL, Tile.WALL),
            mutableListOf(Tile.WALL, Tile.FLOOR, Tile.SPARK, Tile.WALL),
            mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL)
        )
        val level = Level(4, 4, 2L, tiles, Pos(1, 1), Pos(2, 2))
        val initial = GameState(level = level, player = level.entry, profile = DifficultyProfile.NORMAL, hp = 10)

        val next = step(initial, Direction.RIGHT)

        assertTrue(next.envEffects.isNotEmpty())
        assertTrue(next.message.contains("Ignition"))
    }

    @Test
    fun waterPlusSpark_triggersShockEffect() {
        val tiles = mutableListOf(
            mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL),
            mutableListOf(Tile.WALL, Tile.ENTRY, Tile.WATER, Tile.WALL),
            mutableListOf(Tile.WALL, Tile.FLOOR, Tile.SPARK, Tile.WALL),
            mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL)
        )
        val level = Level(4, 4, 3L, tiles, Pos(1, 1), Pos(2, 2))
        val initial = GameState(level = level, player = level.entry, profile = DifficultyProfile.NORMAL, hp = 10)

        val next = step(initial, Direction.RIGHT)

        assertTrue(next.envEffects.isNotEmpty())
        assertTrue(next.message.contains("Shock"))
    }
}
