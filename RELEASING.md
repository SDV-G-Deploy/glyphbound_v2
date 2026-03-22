# Releasing and Versioning

This file defines how Glyphbound versions, tags, changelog generation, and schema versions should stay synchronized.

## 1. Version types in this repository

### 1.1 Product version
- Source of truth: `app/build.gradle.kts` → `versionName`.
- Format: `MAJOR.MINOR.PATCH`.
- Git tag format: `vMAJOR.MINOR.PATCH`.
- Rule: the pushed release tag must match `versionName` exactly, with an added `v` prefix.

Examples:
- `versionName = "0.2.6"` → release tag `v0.2.6`
- `versionName = "1.0.0"` → release tag `v1.0.0`

### 1.2 Android build number
- Source of truth: `app/build.gradle.kts` → `versionCode`.
- Rule: increment for every installable Android release build.
- Rule: keep it monotonically increasing.

### 1.3 Tuning config schema version
- Source of truth: `core/model/src/main/resources/assets/tuning/profiles.v1.json` → `configVersion`.
- Purpose: compatibility of the external difficulty/tuning config parser.
- Do **not** bump this for normal gameplay balancing if the file format and parser contract stay compatible.
- Bump it only when the config schema changes in a way that requires parser/migration handling.

### 1.4 Future save schema version
- Suspend/resume save compatibility must use a separate save schema version when active-run/meta saves are introduced.
- This version must stay independent from both product version and `configVersion`.

## 2. Release triggers

Release workflow: `.github/workflows/android-release.yml`.

A release is created when a git tag matching `v*` is pushed.

Current workflow behavior:
1. runs tests,
2. builds a signed release APK if signing secrets exist,
3. otherwise builds an installable debug fallback APK,
4. generates automatic release notes from git history,
5. creates or updates the matching GitHub Release.

## 3. Version bump rules

### PATCH
Use for:
- bug fixes,
- balance/tuning fixes,
- deterministic/repro fixes,
- documentation sync,
- small UX polish.

### MINOR
Use for:
- meaningful gameplay capability increases,
- new vertical-slice functionality,
- major UX additions,
- new systems that expand what a run can do.

### MAJOR
Use for:
- intentionally breaking compatibility expectations,
- major save/schema incompatibilities,
- large release-line reset decisions.

## 4. Required sync checklist before release

Before pushing a release tag:
1. confirm `versionName` is correct in `app/build.gradle.kts`;
2. confirm `versionCode` was incremented if this is a new installable Android release;
3. confirm `README.md` headline/version-specific highlights reflect the same product version;
4. confirm any schema changes are documented:
   - `configVersion` for tuning schema,
   - save schema version once save/load exists;
5. run the relevant checks;
6. create and push tag `v<versionName>`.

## 5. Changelog rules

Auto changelog generation currently comes from `scripts/generate_changelog.sh`, which formats git commit subjects.

Because of that, commit titles should stay clean and reviewable.

Recommended commit subject style:
- `feat(domain): ...`
- `fix(rules): ...`
- `docs: ...`
- `build(android): ...`
- `chore(dev): ...`

If a release needs more user-facing notes than the auto-generated list provides, edit the release notes manually after the workflow publishes the draft release content.

## 6. Documentation update rule

When versioning or release flow changes:
- update this file,
- update `README.md` if contributor-facing instructions changed,
- update `PLAN.md` if the operational status changed,
- update `RECOVERY_PLAN.md` only if the PR-splitting/recovery strategy changed.
