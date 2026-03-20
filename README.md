# Glyphbound (v0.2.5)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## V2-5 highlights
- **External tuning config (P0):**
  - Source of truth moved to `core/model/src/main/resources/assets/tuning/profiles.v1.json`.
  - Strict JSON parser (`kotlinx.serialization`, unknown keys rejected).
  - Validation clamps to safe bounds (required fields + ranges).
  - Safe fallback policy: if resource is missing/invalid/unsupported version, runtime falls back to built-in safe defaults and logs reason.

- **Config versioning + migration skeleton (P0):**
  - Explicit `configVersion` in JSON and runtime (`EnvTuning.configVersion`).
  - Supported baseline: `v1`.
  - Unknown version path is explicit (`Unsupported configVersion=...`) and triggers safe fallback.
  - Migration interface added (`ConfigMigrator`) with baseline `BaselineV1Migrator` stub for future v2+ migrations.

- **Determinism hardening:**
  - Seed pipeline now mixes `seed + profile + configVersion` (`LevelGenerator.seedWithProfile`).

- **Snapshot/golden coverage for HUD/overlay (P1):**
  - Added stable render-model snapshots in `HudRenderModelSnapshotTest` for:
    - small-screen HUD legend format
    - overlay conflict summary (`fire/shock/mixed`)
    - high-contrast variant badge (`HC`)

- **Property/fuzz expansion (P0):**
  - Deterministic rules fuzz sweep increased to **1500 seeds** (`500 x EASY/NORMAL/HARD`).
  - Added edge-seed suite including `Long.MIN_VALUE`, `Long.MAX_VALUE`, and signed extremes.
  - Invariants checked:
    - bounded hazards/chains
    - HP + TTL bounds
    - deterministic replay consistency
  - Procgen deterministic sweep kept in place (`220 x 3`), plus seed-mix test for profile/version keying.

## Modules
- `app` — Android UI/input/render loop + `GameViewModel`
- `core:model` — domain model, immutable state, hazard/tile definitions, glyph rendering, tuning catalog loader
- `core:rules` — reducer + staged effect pipeline + hazard/environment rules
- `core:procgen` — deterministic generation + path validation API

## External tuning config: edit + validate
1. Edit `core/model/src/main/resources/assets/tuning/profiles.v1.json`.
2. Keep `configVersion` aligned with supported parser version.
3. Run tests:
   ```bash
   ./gradlew test
   ```
4. If parser/validation fails at runtime, safe defaults are used automatically (with log line from `DifficultyTuningCatalog`).

## Run focused test suites
```bash
./gradlew :core:model:test --tests "*HudRenderModelSnapshotTest"
./gradlew :core:rules:test --tests "*deterministicFuzzSweep_seedSpaceInvariants"
./gradlew :core:rules:test --tests "*edgeSeedSuite_extremeProfilesStayBounded"
./gradlew :core:procgen:test --tests "*property_*"
```

## Release lane (existing flow, unchanged)
Workflow: `.github/workflows/android-release.yml`

- Tag push `v*` runs tests and uploads one installable APK asset.
- If signing secrets exist → signed release APK.
- If signing secrets are missing → debug-signed fallback APK (`app-debug.apk`, uploaded as `glyphbound-<tag>-debug.apk`).
- Avoid `*-unsigned.apk` for device install: Android blocks unsigned packages.
