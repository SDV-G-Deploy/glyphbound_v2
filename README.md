# Glyphbound (v0.2.6)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## Local environment prerequisites
- **Java:** use **JDK 17** for Gradle/Android builds in this repo.
- In this environment, the default `java` on `PATH` may point to a newer JDK (for example JDK 21), while this project is configured around Java 17 toolchains.
- If Gradle fails even though JDK 17 is installed, explicitly switch before running checks:
  ```bash
  export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
  export PATH="$JAVA_HOME/bin:$PATH"
  java -version
  ./gradlew -version
  ```
- **Android SDK:** app tasks such as `:app:testDebugUnitTest` and `:app:assembleDebug` also require a valid SDK location.
  - Fast path for Codex/runner environments:
    ```bash
    bash scripts/bootstrap-android-env.sh
    ```
  - Either set `ANDROID_HOME`, or
  - create `local.properties` with:
    ```properties
    sdk.dir=/absolute/path/to/Android/Sdk
    ```

### Quick local check order
1. Point `JAVA_HOME` to JDK 17.
2. Confirm `java -version` prints 17.x.
3. Ensure Android SDK is configured (`ANDROID_HOME` or `local.properties`).
4. Run JVM-only checks first:
   ```bash
   ./gradlew :core:model:test :core:rules:test :core:procgen:test
   ```
5. Then run Android/app checks:
   ```bash
   bash scripts/bootstrap-android-env.sh
   ./gradlew :app:compileDebugKotlin
   ./gradlew test
   ./gradlew :app:assembleDebug
   ```

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

If your runner (e.g. Codex shell) does **not** execute devcontainer `postCreateCommand`, run a one-shot bootstrap first:
```bash
bash scripts/bootstrap-android-env.sh
./gradlew :app:assembleDebug
```

Or as a single command:
```bash
bash scripts/bootstrap-android-env.sh && ./gradlew :app:assembleDebug
```

## Release lane (existing flow, unchanged)
Workflow: `.github/workflows/android-release.yml`

- Tag push `v*` runs tests and uploads one installable APK asset.
- If signing secrets exist → signed release APK.
- If signing secrets are missing → debug-signed fallback APK (`app-debug.apk`, uploaded as `glyphbound-<tag>-debug.apk`).
- Avoid `*-unsigned.apk` for device install: Android blocks unsigned packages.
