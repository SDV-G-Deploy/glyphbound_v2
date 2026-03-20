package com.sdvgdeploy.glyphbound.core.model

import kotlin.math.absoluteValue

const val SAVE_SCHEMA_VERSION: Int = 1

enum class DungeonNodeType {
    ENTRY,
    COMBAT,
    ELITE,
    TREASURE,
    SHRINE,
    REST,
    BOSS,
    EXIT
}

enum class DungeonTheme {
    NEUTRAL,
    EMBER,
    FLOODED,
    BASTION,
    ROT
}

enum class RewardType {
    MEND,
    SALVAGE,
    SPARK_CORE
}

data class RewardChoice(
    val type: RewardType,
    val label: String,
    val description: String
)

data class DungeonNode(
    val id: String,
    val depth: Int,
    val type: DungeonNodeType,
    val theme: DungeonTheme,
    val floorSeed: Long,
    val discovered: Boolean = false,
    val completed: Boolean = false
) {
    fun title(): String = "${type.name.lowercase()}-${depth}-${theme.name.lowercase()}"
}

data class DungeonGraph(
    val nodes: List<DungeonNode>,
    val edges: Map<String, List<String>>,
    val currentNodeId: String
) {
    fun currentNode(): DungeonNode = nodes.first { it.id == currentNodeId }

    fun neighbors(nodeId: String = currentNodeId): List<DungeonNode> =
        edges[nodeId].orEmpty().map { neighborId -> nodes.first { it.id == neighborId } }

    fun withCurrent(nodeId: String): DungeonGraph = copy(
        currentNodeId = nodeId,
        nodes = nodes.map { node -> if (node.id == nodeId) node.copy(discovered = true) else node }
    )

    fun markCompleted(nodeId: String): DungeonGraph = copy(
        nodes = nodes.map { node -> if (node.id == nodeId) node.copy(completed = true, discovered = true) else node }
    )
}

data class ItemStack(
    val itemId: String,
    val charges: Int = 1
)

data class InventoryState(
    val equipped: List<ItemStack> = emptyList(),
    val backpack: List<ItemStack> = emptyList(),
    val quickSlots: List<ItemStack> = emptyList()
)

data class PlayerProgressionState(
    val level: Int = 1,
    val experience: Int = 0,
    val salvage: Int = 0,
    val perkChoices: List<String> = emptyList(),
    val unlockedTraits: Set<String> = emptySet()
)

data class RunState(
    val runId: String,
    val baseSeed: Long,
    val graph: DungeonGraph,
    val inventory: InventoryState = InventoryState(),
    val progression: PlayerProgressionState = PlayerProgressionState(),
    val visitedNodeIds: Set<String> = setOf(graph.currentNodeId),
    val completedNodeIds: Set<String> = emptySet()
) {
    fun currentNode(): DungeonNode = graph.currentNode()

    fun rewardChoicesForCurrent(): List<RewardChoice> {
        val node = currentNode()
        if (node.type !in setOf(DungeonNodeType.COMBAT, DungeonNodeType.ELITE, DungeonNodeType.BOSS)) return emptyList()

        val catalog = listOf(
            RewardChoice(
                type = RewardType.MEND,
                label = "Field Mend",
                description = "+2 HP carried into the next node"
            ),
            RewardChoice(
                type = RewardType.SALVAGE,
                label = "Salvage Cache",
                description = "+1 salvage for later run progression"
            ),
            RewardChoice(
                type = RewardType.SPARK_CORE,
                label = "Spark Core",
                description = "Unlocks SPARK_CORE trait for this run"
            )
        )

        val offset = ((baseSeed xor currentNode().floorSeed).absoluteValue % catalog.size).toInt()
        return List(2) { index -> catalog[(offset + index) % catalog.size] }
            .distinctBy { it.type }
    }

    fun completeCurrent(): RunState {
        val nodeId = graph.currentNodeId
        return copy(
            graph = graph.markCompleted(nodeId),
            completedNodeIds = completedNodeIds + nodeId,
            visitedNodeIds = visitedNodeIds + nodeId
        )
    }

    fun nextNode(): DungeonNode? = graph.neighbors().firstOrNull { it.id !in completedNodeIds }

    fun advanceTo(nodeId: String): RunState = copy(
        graph = graph.withCurrent(nodeId),
        visitedNodeIds = visitedNodeIds + nodeId
    )

    fun applyReward(type: RewardType): RunState = when (type) {
        RewardType.MEND -> this
        RewardType.SALVAGE -> copy(
            progression = progression.copy(salvage = progression.salvage + 1)
        )
        RewardType.SPARK_CORE -> copy(
            progression = progression.copy(
                unlockedTraits = progression.unlockedTraits + "SPARK_CORE"
            )
        )
    }

    fun applyRewardToHp(type: RewardType, hp: Int): Int = when (type) {
        RewardType.MEND -> hp + 2
        RewardType.SALVAGE, RewardType.SPARK_CORE -> hp
    }

    companion object {
        fun initial(baseSeed: Long): RunState {
            val branchSelector = ((baseSeed xor 0x9E3779B97F4A7C15uL.toLong()).absoluteValue % 3).toInt()
            val midTheme = when (branchSelector) {
                0 -> DungeonTheme.EMBER
                1 -> DungeonTheme.FLOODED
                else -> DungeonTheme.BASTION
            }
            val sideTheme = if (midTheme == DungeonTheme.EMBER) DungeonTheme.ROT else DungeonTheme.EMBER

            val nodes = listOf(
                DungeonNode(id = "entry", depth = 0, type = DungeonNodeType.ENTRY, theme = DungeonTheme.NEUTRAL, floorSeed = floorSeed(baseSeed, 0), discovered = true),
                DungeonNode(id = "path_a", depth = 1, type = DungeonNodeType.COMBAT, theme = midTheme, floorSeed = floorSeed(baseSeed, 1)),
                DungeonNode(id = "path_b", depth = 1, type = DungeonNodeType.TREASURE, theme = sideTheme, floorSeed = floorSeed(baseSeed, 2)),
                DungeonNode(id = "rest", depth = 2, type = DungeonNodeType.REST, theme = DungeonTheme.NEUTRAL, floorSeed = floorSeed(baseSeed, 3)),
                DungeonNode(id = "boss", depth = 3, type = DungeonNodeType.BOSS, theme = midTheme, floorSeed = floorSeed(baseSeed, 4)),
                DungeonNode(id = "exit", depth = 4, type = DungeonNodeType.EXIT, theme = DungeonTheme.NEUTRAL, floorSeed = floorSeed(baseSeed, 5))
            )

            val edges = mapOf(
                "entry" to listOf("path_a", "path_b"),
                "path_a" to listOf("rest"),
                "path_b" to listOf("rest"),
                "rest" to listOf("boss"),
                "boss" to listOf("exit"),
                "exit" to emptyList()
            )

            return RunState(
                runId = "run-$baseSeed",
                baseSeed = baseSeed,
                graph = DungeonGraph(nodes = nodes, edges = edges, currentNodeId = "entry")
            )
        }

        private fun floorSeed(baseSeed: Long, index: Int): Long =
            baseSeed xor ((index.toLong() + 1L) shl 40) xor 0x51ED270BL
    }
}

data class MetaProgressionState(
    val unlockedArchetypes: Set<String> = setOf("SCAVENGER"),
    val unlockedThemes: Set<DungeonTheme> = setOf(DungeonTheme.NEUTRAL),
    val completedRuns: Int = 0,
    val highestDepth: Int = 0
)

data class SaveGame(
    val schemaVersion: Int = SAVE_SCHEMA_VERSION,
    val activeRun: RunState? = null,
    val meta: MetaProgressionState = MetaProgressionState()
)
