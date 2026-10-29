# Glyphbound (v0.3.0)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## Project documentation map
- `PLAN.md` — short operational status: what is done, what is current, what is next.
- `RECOVERY_PLAN.md` — keep/rework/split strategy after the previous oversized PR.
- `DEVELOPMENT.md` — practical working rules for JDK 17, Android SDK bootstrap, and PR/doc hygiene.
- `RELEASING.md` — product versioning, release tags, changelog, and schema-version rules.
- `CHANGELOG.md` — curated, player-facing changes per version.
- `docs/DEMO_SCRIPT.md` — a 5-minute walkthrough for showing the current build.
- `roadmap_raw_test_md_md.md` — long-form design and roadmap archive.

## Local environment prerequisites
- **Java:** use **JDK 17** for Gradle/Android builds in this repo.
- In this environment, the default `java` on `PATH` may point to a newer JDK (for example JDK 21), while this project is configured around Java 17 toolchains.
- On Debian/Ubuntu runners without mise, `apt-get install -y openjdk-17-jdk-headless` and use `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
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

## v0.3.0 highlights — complete demo run
- **Full run loop in core (`RunDirector`, `core:rules`):** Entry → Combat/Treasure → Elite/Rest → Boss → victory,
  or defeat on death. Phases (`EXPLORING`, `CHOOSING_REWARD`, `CHOOSING_BRANCH`, `VICTORY`, `DEFEAT`) are pure and
  unit-tested; `GameViewModel` is now a thin adapter.
- **Encounter variety:** Elite nodes (theme pair, 3 reward picks), enemy-free Rest nodes (campfire +3 HP up to max),
  boss floor with a brute-led warden pack (HARD adds the theme support).
- **Items:** Treasure clears grant an *Ember Draught* quick item (+3 HP up to max, free action, max 3 charges).
- **Save/continue:** autosave at every node entry (`SaveCodec`, save schema v1), launch resumes the run, meta stats
  (runs, wins, best depth) persist. Rotation no longer restarts the run.
- **Procgen fix:** neighbouring seeds no longer collapse onto the same map, and HARD no longer always falls back to
  the corridor map. Path validation is ~3x faster (sparse max-flow).
- **Run-end screen:** victory/defeat summary with *Retry seed* / *Next seed*.
- **CI:** `.github/workflows/android-debug.yml` runs tests and uploads a debug APK artifact on every branch push / PR.

## V2-6 highlights
- **Post-combat reward momentum (vertical-slice refinement):**
  - `SALVAGE` now grants `+1 salvage` **and** a next-node scouting effect (`-1 enemy` on entry, deterministic, consumed once).
  - `SPARK_CORE` now unlocks trait **and** grants `+1 HP` on next node entry (consumed once).
  - Reward choice text now explicitly states immediate vs next-node consequences.

- **Branch/reward UX wording cleanup:**
  - Branch buttons now use player-facing labels (`Combat • Ember`, etc.) with short consequence hints.
  - Progression summary now exposes pending next-node reward effects (`NX ...`) so carryover is visible.

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
  - New run-state tests cover queued reward effects and consume-on-entry behavior.
  - Existing rules/catalog coverage from previous milestone remains in place.

## Modules
- `app` — Android UI/input/render loop + `GameViewModel`
- `core:model` — domain model, immutable state, hazard/tile definitions, glyph rendering, tuning catalog loader
- `core:rules` — reducer + staged effect pipeline + hazard/environment rules + `RunDirector` run orchestration
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

For day-to-day development rules, see `DEVELOPMENT.md`.
For product versioning and release rules, see `RELEASING.md`.

## Getting a demo APK
- **CI (no tag needed):** every push runs `Android Debug APK`; download the `glyphbound-debug-apk-<sha>` artifact
  from the workflow run page and `adb install -r app-debug.apk`.
- **Locally:** `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- Start a specific seed/profile: `adb shell am start -n com.sdvgdeploy.glyphbound/.MainActivity --el seed 1 --es profile NORMAL`.

## Release lane (existing flow)
Workflow: `.github/workflows/android-release.yml`

- Tag push `v*` runs tests and uploads one installable APK asset.
- `versionName` in `app/build.gradle.kts` must match the pushed tag without the leading `v`.
- If signing secrets exist → signed release APK.
- If signing secrets are missing → debug-signed fallback APK (`app-debug.apk`, uploaded as `glyphbound-<tag>-debug.apk`).
- Avoid `*-unsigned.apk` for device install: Android blocks unsigned packages.
