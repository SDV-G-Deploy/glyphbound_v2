# Changelog

Curated, player-facing notes. Release-tag notes are still auto-generated from commit subjects by
`scripts/generate_changelog.sh` (see `RELEASING.md`); this file is the human-readable summary.

## 0.3.0 — complete demo run (unreleased)
Product `versionName` 0.3.0 / `versionCode` 12 · save schema **v1** (first persisted schema) · tuning `configVersion` unchanged.

### Added
- A run now goes all the way from start to finish: Entry → Combat or Treasure → Elite or Rest → Boss.
  You **win** by clearing the boss floor and **lose** when HP reaches 0.
- Elite nodes: two foes, then pick 1 of 3 rewards.
- Rest nodes: no foes, the campfire heals +3 HP (never above max).
- Boss floor: a brute-led warden pack guards the exit (HARD adds a third foe).
- Ember Draught quick item: found on Treasure floors, +3 HP (up to max) as a free action, holds 3 charges.
- Victory/defeat summary with *Retry seed* and *Next seed* buttons, plus a win/run record.
- Autosave at every floor entry; relaunching the app continues the run. Meta stats (runs, wins, best depth) persist.
- CI builds a downloadable debug APK and runs all tests on every branch push and pull request.

### Changed
- Combat floors still offer 2 reward picks; the boss no longer offers a reward, because clearing it ends the run.
- Branch buttons describe each node's consequence (e.g. "No foes, campfire heals +3 HP").

### Fixed
- Neighbouring seeds produced the same map (seeds 1–40 had only 1–7 distinct layouts), and HARD always used the
  fallback corridor map. Every seed now produces its own map.
- Rotating the device restarted the run.
- Map validation is about 3x faster.

## 0.2.7 and earlier
See git history and GitHub Releases.
