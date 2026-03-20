package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcgenValidationTest {
    @Test
    fun reproducibility_sameSeed_sameMap() {
        val seed = 424242L
        val a = LevelGenerator.generate(seed)
        val b = LevelGenerator.generate(seed)

        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)
        assertEquals(a.entry, b.entry)
        assertEquals(a.exit, b.exit)
        assertEquals(
            a.tiles.map { row -> row.map { it.glyph } },
            b.tiles.map { row -> row.map { it.glyph } }
        )
    }

    @Test
    fun connectivity_entryAlwaysReachesExit() {
        val level = LevelGenerator.generate(123456L)
        val result = PathValidator.validate(level)
        assertTrue(result.connected)
    }

    @Test
    fun disjointValidator_failAndPassCases() {
        val fail = fromAscii(
            "#####",
            "#S..#",
            "###.#",
            "#..E#",
            "#####"
        )
        val pass = fromAscii(
            "#####",
            "#S..#",
            "#.#.#",
            "#..E#",
            "#####"
        )

        assertFalse(PathValidator.validate(fail).isValid)
        assertTrue(PathValidator.validate(pass).isValid)
    }

    private fun fromAscii(vararg rows: String): Level {
        val height = rows.size
        val width = rows.first().length
        val tiles = MutableList(height) { y ->
            MutableList(width) { x ->
                when (rows[y][x]) {
                    '#' -> Tile.WALL
                    '.' -> Tile.FLOOR
                    'S' -> Tile.ENTRY
                    'E' -> Tile.EXIT
                    '~' -> Tile.RISK
                    else -> Tile.FLOOR
                }
            }
        }

        var entry = Pos(0, 0)
        var exit = Pos(0, 0)
        for (y in rows.indices) {
            for (x in rows[y].indices) {
                if (rows[y][x] == 'S') entry = Pos(x, y)
                if (rows[y][x] == 'E') exit = Pos(x, y)
            }
        }

        return Level(width, height, 0L, tiles, entry, exit)
    }
}
