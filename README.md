# Glyphbound (v0.2.6)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## V2-6 highlights
- **Persistent hazard damage balancing (P0):**
  - Added per-profile cap `persistentDamageCapPerTurn` to prevent runaway burst damage from stacked zones.
  - Applied in `tickHazards` as a deterministic clamp (no random branches, no extra allocations).

- **Mixed-zone interaction depth (P1):**
  - Added per-profile `mixedZoneBonusDamage` when player stands in simultaneous fire+shock zones.
  - Bonus is still bounded by the same per-turn cap to keep low-end gameplay stable.

- **Data-driven tuning extension (P0):**
  - Extended external config `profiles.v1.json` with both new env fields for EASY/NORMAL/HARD.
  - Validation ranges added in catalog loader with safe default fallback retained.

- **Regression coverage update:**
  - New rules test for mixed hazard cap behavior.
  - Catalog bounds test expanded to include new tuning fields.

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

## Codespaces / devcontainer Android SDK setup
- Devcontainer sets `ANDROID_SDK_ROOT`/`ANDROID_HOME` to `${containerWorkspaceFolder}/.android-sdk`.
- On container create, these scripts run automatically:
  - `.devcontainer/scripts/setup-android-sdk.sh` — installs Android command-line tools + platform/build-tools for API 34.
  - `.devcontainer/scripts/ensure-local-properties.sh` — writes `local.properties` with `sdk.dir=<workspace>/.android-sdk`.
- This keeps `local.properties` out of git while ensuring AGP always sees a valid SDK path in Codespaces.

If you need to re-run manually:
```bash
bash .devcontainer/scripts/setup-android-sdk.sh
bash .devcontainer/scripts/ensure-local-properties.sh
./gradlew :app:compileDebugKotlin
```

## Release lane (existing flow, unchanged)
Workflow: `.github/workflows/android-release.yml`

- Tag push `v*` runs tests and uploads one installable APK asset.
- If signing secrets exist → signed release APK.
- If signing secrets are missing → debug-signed fallback APK (`app-debug.apk`, uploaded as `glyphbound-<tag>-debug.apk`).
- Avoid `*-unsigned.apk` for device install: Android blocks unsigned packages.
