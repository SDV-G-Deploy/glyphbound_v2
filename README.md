# Glyphbound (v0.2.3)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## V2-4 highlights
- **Data-driven profile tuning (config-table + validation/fallback):**
  - Central catalog: `core/model/.../DifficultyProfile.kt` → `DifficultyTuningCatalog`
  - Tunable knobs per profile (without touching game logic):
    - `hazardDamageMultiplier`
    - `fireZoneTtl`, `shockZoneTtl`
    - `fireSpreadProfile` / `shockSpreadProfile` (`spreadChance`, `maxTargets`, `maxChainDepth`)
    - env densities: `ambientRiskChance`, `oilChance`, `waterChance`, `sparkChance`
    - procgen pressure: `wallChance`, `minDisjointPaths`, `useNodeDisjoint`
  - Validation clamps unsafe values; unknown profile name falls back to NORMAL profile baseline.
  - Deterministic repro key now includes tuning version: `seed:profile:v<configVersion>`.

- **Expanded deterministic property/fuzz sweep (stable seed-set):**
  - `core/rules/.../GameRulesTest.deterministicFuzzSweep_seedSpaceInvariants`
  - 1200 seeds total (`400 x EASY/NORMAL/HARD`) with fixed generation formula.
  - Invariants checked:
    - no runaway chain/hazard growth
    - hazard TTL always within configured bounds
    - HP remains in allowed range
    - reproducibility for fixed `seed+profile+configVersion`

- **Hazard overlay readability polish (small screens / high-contrast):**
  - Overlay mapper now marks mixed hazards on same tile with `&`.
  - Compact HUD legend format: `HZ F<fireCount> S<shockCount> ttl:<maxTtl>`.
  - High-contrast palette tweaked for clearer fire/shock/mixed distinction.
  - Legend line includes hazard overlay glyphs (`^`, `!`, `&`).

## Modules
- `app` — Android UI/input/render loop + `GameViewModel`
- `core:model` — domain model, immutable state, hazard/tile definitions, glyph rendering
- `core:rules` — reducer + staged effect pipeline + hazard/environment rules
- `core:procgen` — deterministic generation + path validation API

## How to tune profile balance
1. Open `core/model/src/main/kotlin/com/sdvgdeploy/glyphbound/core/model/DifficultyProfile.kt`.
2. Edit `DifficultyTuningCatalog.rawByProfile` values.
3. Keep values sane (validator clamps bounds automatically).
4. Run tests (`./gradlew test`) to confirm invariants and deterministic behavior.

## Extended property/fuzz tests
```bash
./gradlew :core:rules:test --tests "*deterministicFuzzSweep_seedSpaceInvariants"
```

Or run all tests:
```bash
./gradlew test
```

## Release lane (existing flow, unchanged)
Workflow: `.github/workflows/android-release.yml`

- Tag push `v*` runs tests, builds release APK, uploads release asset.
- If signing secrets exist → signed APK; otherwise unsigned fallback APK.
- CI/infra/workflows were intentionally not modified in this iteration.
