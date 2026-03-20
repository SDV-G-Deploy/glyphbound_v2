package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GlyphRenderSnapshotTest {
    @Test
    fun fixedBuffer_matchesGolden() {
        val level = Level(
            width = 5,
            height = 4,
            seed = 42L,
            tiles = mutableListOf(
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.ENTRY, Tile.FLOOR, Tile.RISK, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.FLOOR, Tile.EXIT, Tile.FLOOR, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL)
            ),
            entry = Pos(1, 1),
            exit = Pos(2, 2)
        )

        val rendered = GlyphRender.buildBuffer(level, player = Pos(2, 1)).joinToString("\n")
        val golden = """
            #####
            #S@~#
            #.E.#
            #####
        """.trimIndent()

        assertEquals(golden, rendered)
    }

    @Test
    fun highContrastPalette_differsFromDefault() {
        val glyph = '~'
        val normalColor = GlyphRender.defaultPalette.colorFor(glyph)
        val highContrastColor = GlyphRender.highContrastPalette.colorFor(glyph)

        assertNotEquals(normalColor, highContrastColor)
        assertEquals(0xFFFF0000.toInt(), highContrastColor)
    }

    @Test
    fun hazardOverlay_isRenderedIntoBuffer() {
        val level = Level(
            width = 4,
            height = 3,
            seed = 2L,
            tiles = mutableListOf(
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.FLOOR, Tile.FLOOR, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL)
            ),
            entry = Pos(1, 1),
            exit = Pos(2, 1)
        )

        val rendered = GlyphRender.buildBuffer(level, player = Pos(1, 1), hazardOverlays = mapOf(Pos(2, 1) to '^')).joinToString("\n")
        assertEquals("####\n#@^#\n####", rendered)
    }

    @Test
    fun enemyGlyph_isRenderedIntoBuffer() {
        val level = Level(
            width = 4,
            height = 3,
            seed = 3L,
            tiles = mutableListOf(
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.FLOOR, Tile.FLOOR, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL)
            ),
            entry = Pos(1, 1),
            exit = Pos(2, 1)
        )

        val rendered = GlyphRender.buildBuffer(
            level = level,
            player = Pos(1, 1),
            enemies = listOf(Enemy(id = "e0", pos = Pos(2, 1), archetype = EnemyArchetype.STALKER))
        ).joinToString("\n")

        assertEquals("####\n#@g#\n####", rendered)
    }

    @Test
    fun spitterGlyph_usesDistinctPaletteEntry() {
        assertEquals(0xFFA5D6A7.toInt(), GlyphRender.defaultPalette.colorFor('s'))
        assertEquals(0xFF69F0AE.toInt(), GlyphRender.highContrastPalette.colorFor('s'))
    }
}
