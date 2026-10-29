package com.sdvgdeploy.glyphbound.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON persistence for [SaveGame].
 *
 * Versioning rules (see RELEASING.md):
 * - `schemaVersion` is independent from product `versionName` and tuning `configVersion`.
 * - Saves with a newer schema than [SAVE_SCHEMA_VERSION] are rejected (never partially loaded).
 * - Older schemas must be migrated in [migrate] before decoding; v1 is the first persisted schema.
 * - The run graph is stored explicitly (not regenerated from the seed) so future graph-layout
 *   changes do not silently corrupt in-progress runs.
 */
object SaveCodec {
    sealed interface DecodeResult {
        data class Ok(val save: SaveGame) : DecodeResult
        data class UnsupportedVersion(val found: Int) : DecodeResult
        data class Corrupt(val reason: String) : DecodeResult
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(save: SaveGame): String = json.encodeToString(SaveGameDto.serializer(), save.toDto())

    fun decode(raw: String): DecodeResult {
        val version = try {
            json.parseToJsonElement(raw).jsonObject["schemaVersion"]?.jsonPrimitive?.int
        } catch (e: Exception) {
            return DecodeResult.Corrupt("not a JSON object: ${e.message}")
        } ?: return DecodeResult.Corrupt("missing schemaVersion")

        if (version > SAVE_SCHEMA_VERSION || version < 1) return DecodeResult.UnsupportedVersion(version)

        return try {
            val dto = json.decodeFromString(SaveGameDto.serializer(), migrate(raw, version))
            DecodeResult.Ok(dto.toModel())
        } catch (e: Exception) {
            DecodeResult.Corrupt(e.message ?: e::class.simpleName.orEmpty())
        }
    }

    /** Placeholder for future schema migrations (v1 is current, so this is the identity). */
    private fun migrate(raw: String, fromVersion: Int): String {
        check(fromVersion == SAVE_SCHEMA_VERSION) { "no migration from $fromVersion" }
        return raw
    }
}

@Serializable
private data class SaveGameDto(
    val schemaVersion: Int,
    val activeRun: RunCheckpointDto? = null,
    val meta: MetaDto = MetaDto()
)

@Serializable
private data class RunCheckpointDto(
    val profile: String,
    val carryHp: Int,
    val totalMoves: Int = 0,
    val run: RunDto
)

@Serializable
private data class RunDto(
    val runId: String,
    val baseSeed: Long,
    val currentNodeId: String,
    val nodes: List<NodeDto>,
    val edges: Map<String, List<String>>,
    val visitedNodeIds: List<String>,
    val completedNodeIds: List<String>,
    val pendingEffects: List<PendingEffectDto> = emptyList(),
    val progression: ProgressionDto = ProgressionDto(),
    val inventory: InventoryDto = InventoryDto()
)

@Serializable
private data class NodeDto(
    val id: String,
    val depth: Int,
    val type: String,
    val theme: String,
    val floorSeed: Long,
    val discovered: Boolean = false,
    val completed: Boolean = false
)

@Serializable
private data class PendingEffectDto(val type: String, val amount: Int, val source: String)

@Serializable
private data class ProgressionDto(
    val level: Int = 1,
    val experience: Int = 0,
    val salvage: Int = 0,
    val perkChoices: List<String> = emptyList(),
    val unlockedTraits: List<String> = emptyList()
)

@Serializable
private data class ItemStackDto(val itemId: String, val charges: Int = 1)

@Serializable
private data class InventoryDto(
    val equipped: List<ItemStackDto> = emptyList(),
    val backpack: List<ItemStackDto> = emptyList(),
    val quickSlots: List<ItemStackDto> = emptyList()
)

@Serializable
private data class MetaDto(
    val unlockedArchetypes: List<String> = listOf("SCAVENGER"),
    val unlockedThemes: List<String> = listOf(DungeonTheme.NEUTRAL.name),
    val completedRuns: Int = 0,
    val highestDepth: Int = 0,
    val totalRuns: Int = 0
)

private fun SaveGame.toDto() = SaveGameDto(
    schemaVersion = schemaVersion,
    activeRun = activeRun?.let { cp ->
        RunCheckpointDto(
            profile = cp.profile.name,
            carryHp = cp.carryHp,
            totalMoves = cp.totalMoves,
            run = cp.run.toDto()
        )
    },
    meta = MetaDto(
        unlockedArchetypes = meta.unlockedArchetypes.sorted(),
        unlockedThemes = meta.unlockedThemes.map { it.name }.sorted(),
        completedRuns = meta.completedRuns,
        highestDepth = meta.highestDepth,
        totalRuns = meta.totalRuns
    )
)

private fun RunState.toDto() = RunDto(
    runId = runId,
    baseSeed = baseSeed,
    currentNodeId = graph.currentNodeId,
    nodes = graph.nodes.map { NodeDto(it.id, it.depth, it.type.name, it.theme.name, it.floorSeed, it.discovered, it.completed) },
    edges = graph.edges,
    visitedNodeIds = visitedNodeIds.sorted(),
    completedNodeIds = completedNodeIds.sorted(),
    pendingEffects = pendingNodeRewardEffects.map { PendingEffectDto(it.type.name, it.amount, it.source.name) },
    progression = ProgressionDto(
        level = progression.level,
        experience = progression.experience,
        salvage = progression.salvage,
        perkChoices = progression.perkChoices,
        unlockedTraits = progression.unlockedTraits.sorted()
    ),
    inventory = InventoryDto(
        equipped = inventory.equipped.map { ItemStackDto(it.itemId, it.charges) },
        backpack = inventory.backpack.map { ItemStackDto(it.itemId, it.charges) },
        quickSlots = inventory.quickSlots.map { ItemStackDto(it.itemId, it.charges) }
    )
)

private fun SaveGameDto.toModel(): SaveGame = SaveGame(
    schemaVersion = schemaVersion,
    activeRun = activeRun?.let { cp ->
        val profile = DifficultyProfile.entries.firstOrNull { it.name == cp.profile }
            ?: error("unknown profile ${cp.profile}")
        require(cp.carryHp > 0) { "carryHp must be positive" }
        RunCheckpoint(run = cp.run.toModel(), profile = profile, carryHp = cp.carryHp, totalMoves = cp.totalMoves)
    },
    meta = MetaProgressionState(
        unlockedArchetypes = meta.unlockedArchetypes.toSet(),
        unlockedThemes = meta.unlockedThemes.map { DungeonTheme.valueOf(it) }.toSet(),
        completedRuns = meta.completedRuns,
        highestDepth = meta.highestDepth,
        totalRuns = meta.totalRuns
    )
)

private fun RunDto.toModel(): RunState {
    val nodes = nodes.map {
        DungeonNode(
            id = it.id,
            depth = it.depth,
            type = DungeonNodeType.valueOf(it.type),
            theme = DungeonTheme.valueOf(it.theme),
            floorSeed = it.floorSeed,
            discovered = it.discovered,
            completed = it.completed
        )
    }
    val ids = nodes.map { it.id }.toSet()
    require(currentNodeId in ids) { "currentNodeId $currentNodeId not in graph" }
    require(edges.all { (from, to) -> from in ids && to.all { it in ids } }) { "edges reference unknown nodes" }

    return RunState(
        runId = runId,
        baseSeed = baseSeed,
        graph = DungeonGraph(nodes = nodes, edges = edges, currentNodeId = currentNodeId),
        inventory = InventoryState(
            equipped = inventory.equipped.map { ItemStack(it.itemId, it.charges) },
            backpack = inventory.backpack.map { ItemStack(it.itemId, it.charges) },
            quickSlots = inventory.quickSlots.map { ItemStack(it.itemId, it.charges) }
        ),
        progression = PlayerProgressionState(
            level = progression.level,
            experience = progression.experience,
            salvage = progression.salvage,
            perkChoices = progression.perkChoices,
            unlockedTraits = progression.unlockedTraits.toSet()
        ),
        pendingNodeRewardEffects = pendingEffects.map {
            PendingNodeRewardEffect(
                type = PendingNodeRewardEffectType.valueOf(it.type),
                amount = it.amount,
                source = RewardType.valueOf(it.source)
            )
        },
        visitedNodeIds = visitedNodeIds.toSet(),
        completedNodeIds = completedNodeIds.toSet()
    )
}
