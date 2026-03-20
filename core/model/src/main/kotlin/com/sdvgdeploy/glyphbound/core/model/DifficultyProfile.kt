package com.sdvgdeploy.glyphbound.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SpreadProfile(
    val spreadChance: Double,
    val maxTargets: Int,
    val maxChainDepth: Int
)

data class EnvTuning(
    val ambientRiskChance: Double,
    val oilChance: Double,
    val waterChance: Double,
    val sparkChance: Double,
    val hazardDamageMultiplier: Int,
    val ignitionTurns: Int,
    val ignitionTickDamage: Int,
    val shockTurns: Int,
    val shockTickDamage: Int,
    val fireZoneTtl: Int,
    val shockZoneTtl: Int,
    val fireSpreadProfile: SpreadProfile,
    val shockSpreadProfile: SpreadProfile,
    val configVersion: Int
)

data class ProfileTuning(
    val wallChance: Double,
    val minDisjointPaths: Int,
    val useNodeDisjoint: Boolean,
    val startingHp: Int,
    val env: EnvTuning
)

@Serializable
private data class TuningCatalogFile(
    val configVersion: Int,
    val profiles: Map<String, ProfileTuningConfig>
)

@Serializable
private data class ProfileTuningConfig(
    val wallChance: Double,
    val minDisjointPaths: Int,
    val useNodeDisjoint: Boolean,
    val startingHp: Int,
    val env: EnvTuningConfig
)

@Serializable
private data class EnvTuningConfig(
    val ambientRiskChance: Double,
    val oilChance: Double,
    val waterChance: Double,
    val sparkChance: Double,
    val hazardDamageMultiplier: Int,
    val ignitionTurns: Int,
    val ignitionTickDamage: Int,
    val shockTurns: Int,
    val shockTickDamage: Int,
    val fireZoneTtl: Int,
    val shockZoneTtl: Int,
    val fireSpreadProfile: SpreadProfileConfig,
    val shockSpreadProfile: SpreadProfileConfig
)

@Serializable
private data class SpreadProfileConfig(
    val spreadChance: Double,
    val maxTargets: Int,
    val maxChainDepth: Int
)

private interface ConfigMigrator {
    fun canMigrate(version: Int): Boolean
    fun migrate(raw: String): String
}

private object BaselineV1Migrator : ConfigMigrator {
    override fun canMigrate(version: Int): Boolean = version == 1
    override fun migrate(raw: String): String = raw
}

object DifficultyTuningCatalog {
    const val CONFIG_VERSION: Int = 1
    private const val RESOURCE_PATH = "assets/tuning/profiles.v1.json"

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
    }

    private val migrators: List<ConfigMigrator> = listOf(BaselineV1Migrator)

    private val safeDefaults: Map<String, ProfileTuningConfig> = mapOf(
        "EASY" to ProfileTuningConfig(
            wallChance = 0.24,
            minDisjointPaths = 2,
            useNodeDisjoint = false,
            startingHp = 14,
            env = EnvTuningConfig(
                ambientRiskChance = 0.03,
                oilChance = 0.03,
                waterChance = 0.07,
                sparkChance = 0.03,
                hazardDamageMultiplier = 1,
                ignitionTurns = 2,
                ignitionTickDamage = 1,
                shockTurns = 1,
                shockTickDamage = 1,
                fireZoneTtl = 2,
                shockZoneTtl = 2,
                fireSpreadProfile = SpreadProfileConfig(spreadChance = 0.35, maxTargets = 1, maxChainDepth = 1),
                shockSpreadProfile = SpreadProfileConfig(spreadChance = 0.30, maxTargets = 1, maxChainDepth = 1)
            )
        ),
        "NORMAL" to ProfileTuningConfig(
            wallChance = 0.30,
            minDisjointPaths = 2,
            useNodeDisjoint = false,
            startingHp = 10,
            env = EnvTuningConfig(
                ambientRiskChance = 0.05,
                oilChance = 0.05,
                waterChance = 0.05,
                sparkChance = 0.05,
                hazardDamageMultiplier = 1,
                ignitionTurns = 2,
                ignitionTickDamage = 2,
                shockTurns = 2,
                shockTickDamage = 1,
                fireZoneTtl = 3,
                shockZoneTtl = 2,
                fireSpreadProfile = SpreadProfileConfig(spreadChance = 0.55, maxTargets = 2, maxChainDepth = 2),
                shockSpreadProfile = SpreadProfileConfig(spreadChance = 0.40, maxTargets = 2, maxChainDepth = 1)
            )
        ),
        "HARD" to ProfileTuningConfig(
            wallChance = 0.36,
            minDisjointPaths = 2,
            useNodeDisjoint = true,
            startingHp = 8,
            env = EnvTuningConfig(
                ambientRiskChance = 0.07,
                oilChance = 0.07,
                waterChance = 0.04,
                sparkChance = 0.07,
                hazardDamageMultiplier = 2,
                ignitionTurns = 3,
                ignitionTickDamage = 2,
                shockTurns = 2,
                shockTickDamage = 2,
                fireZoneTtl = 4,
                shockZoneTtl = 3,
                fireSpreadProfile = SpreadProfileConfig(spreadChance = 0.75, maxTargets = 3, maxChainDepth = 2),
                shockSpreadProfile = SpreadProfileConfig(spreadChance = 0.65, maxTargets = 2, maxChainDepth = 2)
            )
        )
    )

    @Volatile
    private var loadedByProfile: Map<String, ProfileTuningConfig>? = null

    fun resolve(profileName: String): ProfileTuning {
        val loaded = loadedByProfile ?: loadCatalog().also { loadedByProfile = it }
        val safeNormal = requireNotNull(safeDefaults["NORMAL"])
        val tuned = loaded[profileName] ?: loaded["NORMAL"] ?: safeNormal
        return toDomain(validateProfile(tuned))
    }

    internal fun resetForTests() {
        loadedByProfile = null
    }

    internal fun loadFromRawForTests(raw: String): Result<Map<String, ProfileTuning>> = runCatching {
        val parsed = parseWithVersionHandling(raw)
        parsed.profiles.mapValues { (_, value) -> toDomain(validateProfile(value)) }
    }

    private fun loadCatalog(): Map<String, ProfileTuningConfig> {
        return runCatching {
            val raw = loadResource(RESOURCE_PATH)
            val parsed = parseWithVersionHandling(raw)
            val validated = parsed.profiles.mapValues { (_, value) -> validateProfile(value) }
            safeDefaults + validated
        }.getOrElse { err ->
            println("[DifficultyTuningCatalog] fallback to safe defaults; reason=${err.message}")
            safeDefaults
        }
    }

    private fun parseWithVersionHandling(raw: String): TuningCatalogFile {
        val lightweight = json.decodeFromString<VersionProbe>(raw)
        if (lightweight.configVersion == CONFIG_VERSION) {
            return json.decodeFromString(raw)
        }
        val migrator = migrators.firstOrNull { it.canMigrate(lightweight.configVersion) }
            ?: throw IllegalArgumentException(
                "Unsupported configVersion=${lightweight.configVersion}; supported=$CONFIG_VERSION"
            )
        println("[DifficultyTuningCatalog] migrating config v${lightweight.configVersion} -> v$CONFIG_VERSION")
        return json.decodeFromString(migrator.migrate(raw))
    }

    @Serializable
    private data class VersionProbe(@SerialName("configVersion") val configVersion: Int)

    private fun loadResource(path: String): String {
        val stream = DifficultyTuningCatalog::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Missing tuning resource: $path")
        return stream.bufferedReader().use { it.readText() }
    }

    private fun validateProfile(config: ProfileTuningConfig): ProfileTuningConfig {
        val env = config.env
        return config.copy(
            wallChance = config.wallChance.coerceIn(0.05, 0.60),
            minDisjointPaths = config.minDisjointPaths.coerceIn(1, 3),
            startingHp = config.startingHp.coerceIn(1, 99),
            env = env.copy(
                ambientRiskChance = env.ambientRiskChance.coerceIn(0.0, 0.30),
                oilChance = env.oilChance.coerceIn(0.0, 0.40),
                waterChance = env.waterChance.coerceIn(0.0, 0.40),
                sparkChance = env.sparkChance.coerceIn(0.0, 0.30),
                hazardDamageMultiplier = env.hazardDamageMultiplier.coerceIn(1, 4),
                ignitionTurns = env.ignitionTurns.coerceIn(1, 6),
                ignitionTickDamage = env.ignitionTickDamage.coerceIn(1, 6),
                shockTurns = env.shockTurns.coerceIn(1, 6),
                shockTickDamage = env.shockTickDamage.coerceIn(1, 6),
                fireZoneTtl = env.fireZoneTtl.coerceIn(1, 8),
                shockZoneTtl = env.shockZoneTtl.coerceIn(1, 8),
                fireSpreadProfile = validateSpread(env.fireSpreadProfile),
                shockSpreadProfile = validateSpread(env.shockSpreadProfile)
            )
        )
    }

    private fun validateSpread(config: SpreadProfileConfig): SpreadProfileConfig = config.copy(
        spreadChance = config.spreadChance.coerceIn(0.0, 1.0),
        maxTargets = config.maxTargets.coerceIn(0, 8),
        maxChainDepth = config.maxChainDepth.coerceIn(0, 6)
    )

    private fun toDomain(config: ProfileTuningConfig): ProfileTuning = ProfileTuning(
        wallChance = config.wallChance,
        minDisjointPaths = config.minDisjointPaths,
        useNodeDisjoint = config.useNodeDisjoint,
        startingHp = config.startingHp,
        env = EnvTuning(
            ambientRiskChance = config.env.ambientRiskChance,
            oilChance = config.env.oilChance,
            waterChance = config.env.waterChance,
            sparkChance = config.env.sparkChance,
            hazardDamageMultiplier = config.env.hazardDamageMultiplier,
            ignitionTurns = config.env.ignitionTurns,
            ignitionTickDamage = config.env.ignitionTickDamage,
            shockTurns = config.env.shockTurns,
            shockTickDamage = config.env.shockTickDamage,
            fireZoneTtl = config.env.fireZoneTtl,
            shockZoneTtl = config.env.shockZoneTtl,
            fireSpreadProfile = SpreadProfile(
                spreadChance = config.env.fireSpreadProfile.spreadChance,
                maxTargets = config.env.fireSpreadProfile.maxTargets,
                maxChainDepth = config.env.fireSpreadProfile.maxChainDepth
            ),
            shockSpreadProfile = SpreadProfile(
                spreadChance = config.env.shockSpreadProfile.spreadChance,
                maxTargets = config.env.shockSpreadProfile.maxTargets,
                maxChainDepth = config.env.shockSpreadProfile.maxChainDepth
            ),
            configVersion = CONFIG_VERSION
        )
    )
}

enum class DifficultyProfile(
    val wallChance: Double,
    val minDisjointPaths: Int,
    val useNodeDisjoint: Boolean,
    val startingHp: Int,
    val env: EnvTuning
) {
    EASY(
        wallChance = DifficultyTuningCatalog.resolve("EASY").wallChance,
        minDisjointPaths = DifficultyTuningCatalog.resolve("EASY").minDisjointPaths,
        useNodeDisjoint = DifficultyTuningCatalog.resolve("EASY").useNodeDisjoint,
        startingHp = DifficultyTuningCatalog.resolve("EASY").startingHp,
        env = DifficultyTuningCatalog.resolve("EASY").env
    ),
    NORMAL(
        wallChance = DifficultyTuningCatalog.resolve("NORMAL").wallChance,
        minDisjointPaths = DifficultyTuningCatalog.resolve("NORMAL").minDisjointPaths,
        useNodeDisjoint = DifficultyTuningCatalog.resolve("NORMAL").useNodeDisjoint,
        startingHp = DifficultyTuningCatalog.resolve("NORMAL").startingHp,
        env = DifficultyTuningCatalog.resolve("NORMAL").env
    ),
    HARD(
        wallChance = DifficultyTuningCatalog.resolve("HARD").wallChance,
        minDisjointPaths = DifficultyTuningCatalog.resolve("HARD").minDisjointPaths,
        useNodeDisjoint = DifficultyTuningCatalog.resolve("HARD").useNodeDisjoint,
        startingHp = DifficultyTuningCatalog.resolve("HARD").startingHp,
        env = DifficultyTuningCatalog.resolve("HARD").env
    );

    companion object {
        fun fromRaw(value: String?): DifficultyProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
    }
}
