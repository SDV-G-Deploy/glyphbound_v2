package com.sdvgdeploy.glyphbound.core.model

data class HazardLegendItem(
    val type: HazardType,
    val count: Int,
    val maxTtl: Int
)

data class HazardVisualization(
    val overlays: Map<Pos, Char>,
    val legend: List<HazardLegendItem>
) {
    fun legendSummary(): String {
        if (legend.isEmpty()) return "HZ none"
        return legend.joinToString(prefix = "HZ ") {
            val typeLabel = when (it.type) {
                HazardType.FIRE_ZONE -> "fire"
                HazardType.SHOCK_ZONE -> "shock"
            }
            "$typeLabel x${it.count} ttl:${it.maxTtl}"
        }
    }
}

object HazardVisualizationMapper {
    fun fromState(state: GameState): HazardVisualization {
        val overlays = state.hazardZones
            .groupBy { it.pos }
            .mapValues { (_, zones) ->
                when {
                    zones.any { it.type == HazardType.FIRE_ZONE } -> '^'
                    zones.any { it.type == HazardType.SHOCK_ZONE } -> '!'
                    else -> '?'
                }
            }

        val legend = state.hazardZones
            .groupBy { it.type }
            .map { (type, zones) ->
                HazardLegendItem(type = type, count = zones.size, maxTtl = zones.maxOf { it.ttl })
            }
            .sortedBy { it.type.name }

        return HazardVisualization(overlays = overlays, legend = legend)
    }
}
