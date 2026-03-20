# Glyphbound (v0.2.0)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## V2-1 highlights
- **Single source of truth state** in `GameViewModel`:
  - immutable `GameUiState`
  - intent/action dispatch (`GameIntent`)
  - Activity only observes state + sends intents
- **System depth v1: environment reactions**:
  - tiles: `o` oil, `w` water, `*` spark (+ legacy `~` risk, `f` fire)
  - reactions:
    - oil + spark/fire => **Ignition** effect
    - water + spark => **Shock** effect
  - reactions surfaced via HUD + message log
- **Difficulty tuning table** centralized in `DifficultyProfile.EnvTuning`:
  - tile frequencies
  - hazard multipliers
  - reaction duration and tick intensity
- **Property/fuzz testing baseline**:
  - deterministic seed sweep (fixed seed set per profile)
  - connectivity + validator policy checks
  - deterministic reproducibility checks

## Modules
- `app` — Android UI/input/render loop + `GameViewModel`
- `core:model` — domain model, immutable state, effects, glyph rendering
- `core:rules` — movement, hazard and environment reaction rules
- `core:procgen` — deterministic generation + path validation API

## Reproducibility key
Map generation is reproducible by pair:
- `seed`
- `profile`

Generator key stays deterministic via `seedWithProfile(seed, profile)`.

## Run
```bash
./gradlew :app:assembleDebug
```
APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Tests
```bash
./gradlew test
```

### Property/fuzz tests details
- deterministic fixed seed sweep: `220` seeds per profile (`EASY/NORMAL/HARD`)
- checks:
  - map connectivity (`entry -> exit`)
  - validator policy correctness (`PathValidator` with profile config)
  - reproducibility (`same seed+profile => identical map`)
- runtime guard in test: seed sweep expected under ~15 seconds on CI runner

## Release lane baseline (signing-ready)
- `.github/workflows/android-release.yml`
- runs on tags (`v*`) and manual trigger
- steps:
  1. tests
  2. unsigned release APK (`assembleRelease`)
  3. optional signed skeleton step if keystore secrets are present
  4. auto-generate changelog from git tags/commit messages
  5. publish GitHub Release + APK asset

### Required secrets for signed lane (optional skeleton)
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

No secrets are committed to repo.
