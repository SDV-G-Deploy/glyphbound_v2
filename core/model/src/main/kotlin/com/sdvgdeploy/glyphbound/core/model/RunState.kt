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

enum class PendingNodeRewardEffectType {
    HP_BOOST,
    ENEMY_REDUCTION
}

data class PendingNodeRewardEffect(
    val type: PendingNodeRewardEffectType,
    val amount: Int,
    val source: RewardType
)

data class NodeEntryRewardResolution(
    val updatedRun: RunState,
    val hpBonus: Int,
    val enemyReduction: Int
) {
    fun isEmpty(): Boolean = hpBonus == 0 && enemyReduction == 0

    fun summary(): String {
        if (isEmpty()) return "none"
        val parts = buildList {
            if (hpBonus > 0) add("+$hpBonus HP")
            if (enemyReduction > 0) add("-$enemyReduction foe")
        }
        return parts.joinToString(separator = ", ")
    }
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

/** Consumable items that can live in quick slots. */
enum class ItemType(val itemId: String, val label: String, val description: String) {
    DRAUGHT("draught", "Ember Draught", "Drink to recover +3 HP");

    companion object {
        fun fromId(itemId: String): ItemType? = entries.firstOrNull { it.itemId == itemId }
    }
}

const val DRAUGHT_HEAL: Int = 3
const val MAX_QUICK_ITEM_CHARGES: Int = 3

data class InventoryState(
    val equipped: List<ItemStack> = emptyList(),
    val backpack: List<ItemStack> = emptyList(),
    val quickSlots: List<ItemStack> = emptyList()
) {
    fun charges(item: ItemType): Int = quickSlots.filter { it.itemId == item.itemId }.sumOf { it.charges }

    /** Adds charges to the quick slot of [item], capped at [MAX_QUICK_ITEM_CHARGES]. */
    fun addQuickItem(item: ItemType, amount: Int = 1): InventoryState {
        val total = (charges(item) + amount).coerceIn(0, MAX_QUICK_ITEM_CHARGES)
        val others = quickSlots.filterNot { it.itemId == item.itemId }
        return copy(quickSlots = if (total > 0) others + ItemStack(item.itemId, total) else others)
    }

    /** Returns null when no charge of [item] is available. */
    fun consumeQuickItem(item: ItemType): InventoryState? {
        val current = charges(item)
        if (current <= 0) return null
        return addQuickItem(item, -1)
    }
}

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
    val pendingNodeRewardEffects: List<PendingNodeRewardEffect> = emptyList(),
    val visitedNodeIds: Set<String> = setOf(graph.currentNodeId),
    val completedNodeIds: Set<String> = emptySet()
) {
    fun currentNode(): DungeonNode = graph.currentNode()

    fun rewardChoicesForCurrent(): List<RewardChoice> {
        val node = currentNode()
        // Boss clears end the run, so only COMBAT (2 picks) and ELITE (3 picks) offer rewards.
        if (node.type !in setOf(DungeonNodeType.COMBAT, DungeonNodeType.ELITE)) return emptyList()

        val catalog = listOf(
            RewardChoice(
                type = RewardType.MEND,
                label = "Field Mend",
                description = "Recover +2 HP immediately"
            ),
            RewardChoice(
                type = RewardType.SALVAGE,
                label = "Salvage Cache",
                description = "+1 salvage now and scout the next node (-1 enemy)"
            ),
            RewardChoice(
                type = RewardType.SPARK_CORE,
                label = "Spark Core",
                description = "Unlock SPARK_CORE and gain +1 HP at next node entry"
            )
        )

        val offset = ((baseSeed xor currentNode().floorSeed).absoluteValue % catalog.size).toInt()
        val count = if (node.type == DungeonNodeType.COMBAT) 2 else catalog.size
        return List(count) { index -> catalog[(offset + index) % catalog.size] }
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
            progression = progression.copy(salvage = progression.salvage + 1),
            pendingNodeRewardEffects = pendingNodeRewardEffects + PendingNodeRewardEffect(
                type = PendingNodeRewardEffectType.ENEMY_REDUCTION,
                amount = 1,
                source = RewardType.SALVAGE
            )
        )

        RewardType.SPARK_CORE -> copy(
            progression = progression.copy(
                unlockedTraits = progression.unlockedTraits + "SPARK_CORE"
            ),
            pendingNodeRewardEffects = pendingNodeRewardEffects + PendingNodeRewardEffect(
                type = PendingNodeRewardEffectType.HP_BOOST,
                amount = 1,
                source = RewardType.SPARK_CORE
            )
        )
    }

    fun applyRewardToHp(type: RewardType, hp: Int): Int = when (type) {
        RewardType.MEND -> hp + 2
        RewardType.SALVAGE, RewardType.SPARK_CORE -> hp
    }

    fun consumePendingNodeRewardEffects(): NodeEntryRewardResolution {
        if (pendingNodeRewardEffects.isEmpty()) {
            return NodeEntryRewardResolution(updatedRun = this, hpBonus = 0, enemyReduction = 0)
        }

        val hpBonus = pendingNodeRewardEffects
            .filter { it.type == PendingNodeRewardEffectType.HP_BOOST }
            .sumOf { it.amount }

        val enemyReduction = pendingNodeRewardEffects
            .filter { it.type == PendingNodeRewardEffectType.ENEMY_REDUCTION }
            .sumOf { it.amount }

        return NodeEntryRewardResolution(
            updatedRun = copy(pendingNodeRewardEffects = emptyList()),
            hpBonus = hpBonus,
            enemyReduction = enemyReduction
        )
    }

    fun pendingNodeRewardSummary(): String {
        if (pendingNodeRewardEffects.isEmpty()) return "none"
        val hpBonus = pendingNodeRewardEffects
            .filter { it.type == PendingNodeRewardEffectType.HP_BOOST }
            .sumOf { it.amount }
        val enemyReduction = pendingNodeRewardEffects
            .filter { it.type == PendingNodeRewardEffectType.ENEMY_REDUCTION }
            .sumOf { it.amount }

        return buildList {
            if (hpBonus > 0) add("+$hpBonus HP")
            if (enemyReduction > 0) add("-$enemyReduction foe")
        }.joinToString(separator = ", ")
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
                DungeonNode(id = "elite", depth = 2, type = DungeonNodeType.ELITE, theme = sideTheme, floorSeed = floorSeed(baseSeed, 6)),
                DungeonNode(id = "rest", depth = 2, type = DungeonNodeType.REST, theme = DungeonTheme.NEUTRAL, floorSeed = floorSeed(baseSeed, 3)),
                DungeonNode(id = "boss", depth = 3, type = DungeonNodeType.BOSS, theme = midTheme, floorSeed = floorSeed(baseSeed, 4)),
                DungeonNode(id = "exit", depth = 4, type = DungeonNodeType.EXIT, theme = DungeonTheme.NEUTRAL, floorSeed = floorSeed(baseSeed, 5))
            )

            val edges = mapOf(
                "entry" to listOf("path_a", "path_b"),
                "path_a" to listOf("elite", "rest"),
                "path_b" to listOf("elite", "rest"),
                "elite" to listOf("boss"),
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

enum class RunOutcome { VICTORY, DEFEAT }

data class MetaProgressionState(
    val unlockedArchetypes: Set<String> = setOf("SCAVENGER"),
    val unlockedThemes: Set<DungeonTheme> = setOf(DungeonTheme.NEUTRAL),
    val completedRuns: Int = 0,
    val highestDepth: Int = 0,
    val totalRuns: Int = 0
) {
    /** Records a finished run. Themes seen on cleared nodes are unlocked for display/meta purposes. */
    fun recordRun(run: RunState, outcome: RunOutcome): MetaProgressionState {
        val reachedDepth = run.graph.nodes.filter { it.id in run.visitedNodeIds }.maxOfOrNull { it.depth } ?: 0
        val clearedThemes = run.graph.nodes.filter { it.id in run.completedNodeIds }.map { it.theme }
        return copy(
            totalRuns = totalRuns + 1,
            completedRuns = completedRuns + if (outcome == RunOutcome.VICTORY) 1 else 0,
            highestDepth = maxOf(highestDepth, reachedDepth),
            unlockedThemes = unlockedThemes + clearedThemes
        )
    }
}

/**
 * Node-entry checkpoint: the run *before* pending entry effects are consumed, plus the HP carried in.
 * Re-entering the node from this checkpoint is fully deterministic, which is what makes it a safe save point.
 */
data class RunCheckpoint(
    val run: RunState,
    val profile: DifficultyProfile,
    val carryHp: Int,
    val totalMoves: Int = 0
)

data class SaveGame(
    val schemaVersion: Int = SAVE_SCHEMA_VERSION,
    val activeRun: RunCheckpoint? = null,
    val meta: MetaProgressionState = MetaProgressionState()
)
