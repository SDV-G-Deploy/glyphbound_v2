package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcgenValidationTest {
    private val seedsPerProfile = 220

    @Test
    fun reproducibility_sameSeed_sameMap() {
        val seed = 424242L
        val a = LevelGenerator.generate(seed)
        val b = LevelGenerator.generate(seed)
        assertEquals(a.tiles.map { row -> row.map { it.glyph } }, b.tiles.map { row -> row.map { it.glyph } })
    }

    @Test
    fun edgeMode_policyPassAndFail() {
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

        val edge = PathValidationConfig(minDisjointPaths = 2, mode = DisjointMode.EDGE)
        assertFalse(PathValidator.validate(fail, edge).isValid)
        assertTrue(PathValidator.validate(pass, edge).isValid)
    }

    @Test
    fun property_connectivity_and_policy_onDeterministicSeedSpace() {
        val elapsed = measureTimeMillis {
            DifficultyProfile.entries.forEach { profile ->
                fixedSeeds(profile, seedsPerProfile).forEach { seed ->
                    val level = LevelGenerator.generate(seed, profile)
                    val result = PathValidator.validate(level, LevelGenerator.configFor(profile).validator)
                    assertTrue(result.connected, "expected connectivity for $profile/$seed")
                    assertTrue(result.isValid, "expected policy-valid map for $profile/$seed")
                }
            }
        }
        assertTrue(elapsed < 15_000, "seed sweep is too slow: ${elapsed}ms")
    }

    @Test
    fun property_determinism_onDeterministicSeedSpace() {
        DifficultyProfile.entries.forEach { profile ->
            fixedSeeds(profile, seedsPerProfile).forEach { seed ->
                val a = LevelGenerator.generate(seed, profile)
                val b = LevelGenerator.generate(seed, profile)
                assertEquals(a.seed, b.seed)
                assertEquals(
                    a.tiles.map { row -> row.map { it.glyph } },
                    b.tiles.map { row -> row.map { it.glyph } },
                    "map mismatch for $profile/$seed"
                )
            }
        }
    }

    private fun fixedSeeds(profile: DifficultyProfile, count: Int): List<Long> {
        val profileOffset = (profile.ordinal + 1L) * 100_000L
        return List(count) { i -> profileOffset + i * 37L + 11L }
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
                    'o' -> Tile.OIL
                    'w' -> Tile.WATER
                    '*' -> Tile.SPARK
                    'f' -> Tile.FIRE
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
