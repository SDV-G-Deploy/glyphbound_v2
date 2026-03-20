package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
