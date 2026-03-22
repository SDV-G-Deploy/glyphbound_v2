# Glyphbound Recovery / Next-PR Plan

This document exists because the previous large PR mixed together several categories of change:

- gameplay/domain evolution,
- Android UI changes,
- developer environment/bootstrap changes,
- CI/tooling changes,
- roadmap/documentation changes.

That made it harder to review, harder to merge safely, and harder to answer a simple question: **what do we keep, what do we rework, and what do we tackle next?**

This file is the compact answer.

---

## 1. Current audit summary

### 1.1 What is already in the repository now
- Deterministic hazard-driven prototype foundation remains intact.
- Early enemy support exists:
  - `Enemy`, `EnemyArchetype`, `EnemyIntent`,
  - enemy spawning in `EnemyDirector`,
  - enemy movement/attacks inside the rules loop.
- Early run support exists:
  - `RunState`,
  - small deterministic dungeon graph,
  - branch choice,
  - post-combat reward choice stubs.
- Android UI already exposes:
  - branch buttons,
  - reward buttons,
  - progression summary,
  - enemy intent summary.
- Developer environment support exists:
  - devcontainer,
  - Android SDK bootstrap,
  - explicit JDK 17 guidance,
  - Gradle toolchain auto-detect/auto-download settings.

### 1.2 What is *not* true yet
The current implementation is **not** a complete run loop and should not be treated as one.

The new systems are still a thin vertical-slice scaffold:
- run graph is tiny and mostly hard-coded,
- reward system is placeholder-level,
- progression values are not yet meaningfully consumed by the game,
- enemy variety is still minimal,
- UI/state flow is serviceable but not yet elegant.

---

## 2. What we keep

These parts are worth keeping as the new baseline.

### 2.1 Environment / tooling
- Explicit **JDK 17** project requirement.
- `scripts/bootstrap-android-env.sh`.
- `.devcontainer/` Android SDK setup helpers.
- Updated CI Java setup and Gradle toolchain support.

Reason: this removes repeated setup friction and makes the repo much easier to run consistently.

### 2.2 Core gameplay direction
- Enemies as a second gameplay pillar next to hazards.
- Enemy intents as a readable tactical signal.
- `RunState` as a place to grow future node/floor progression.
- Reward/branch gating as the first step toward a real run structure.

Reason: these are strategically aligned with the long-term product direction.

### 2.3 Documentation direction
- Keep a short plan (`PLAN.md`).
- Keep the long-form design archive (`roadmap_raw_test_md_md.md`).
- Keep environment/setup guidance in docs.

Reason: the project now clearly needs both short operational docs and long-term design docs.

---

## 3. What should be reworked, clarified, or treated as provisional

These pieces should **not** be treated as final architecture.

### 3.1 `RunState` content is a scaffold, not a finished design
Current limitations:
- fixed tiny graph,
- hard-coded node topology,
- placeholder reward catalog,
- progression mostly stored but barely consumed.

Action:
- keep the file,
- but treat it as **temporary vertical-slice structure**,
- evolve it through smaller PRs instead of expanding it blindly.

### 3.2 Reward/progression UI is useful but still rough
Current limitations:
- reward choices are minimal,
- branch labels are technical rather than player-facing,
- progression summary is compact but not yet very informative,
- the screen has grown more crowded.

Action:
- keep the reward/branch flow,
- but improve language, affordances, and pacing before adding many more systems.

### 3.3 Enemy implementation should remain intentionally small for now
Current limitations:
- only a few archetypes,
- simplistic AI,
- limited interaction with hazards,
- limited encounter composition.

Action:
- do **not** add many more enemy types immediately;
- first stabilize:
  - intent readability,
  - encounter pacing,
  - death/reward flow,
  - clearer tactical roles.

### 3.4 The previous PR scope was too wide
The last big merge bundled together too many categories:
- gameplay,
- UI,
- docs,
- devcontainer,
- CI/tooling.

Action:
- future work must be split more aggressively into focused PRs.

---

## 4. How future work should be split

To reduce merge risk and review friction, future work should follow this split:

### PR type A — environment/tooling
Examples:
- JDK/SDK setup,
- devcontainer,
- CI workflow fixes,
- Gradle/toolchain configuration.

Rule:
- no gameplay logic changes in the same PR.

### PR type B — documentation only
Examples:
- roadmap updates,
- plan updates,
- environment docs,
- architecture notes,
- implementation notes.

Rule:
- no gameplay or UI logic changes in the same PR.

### PR type C — domain/rules gameplay
Examples:
- enemies,
- rewards,
- node effects,
- inventory/resources,
- procgen/domain evolution.

Rule:
- keep Android UI changes minimal and directly necessary.

### PR type D — Android presentation/UX
Examples:
- HUD improvements,
- branch/reward screens,
- map readability improvements,
- result/restart UX.

Rule:
- avoid sneaking in unrelated core/rules changes unless strictly required.

---

## 5. Immediate next implementation focus

The project should **not** immediately branch into many new systems.

### 5.1 Recommended next milestone
Refine the existing vertical slice instead of widening it:

1. improve post-combat flow,
2. make branch consequences more meaningful,
3. make rewards actually affect the next node in visible ways,
4. improve wording/UX of reward and branch choices,
5. tighten the relationship between enemy roles and hazards.

### 5.2 Explicitly *not* next
These should wait until the current slice feels coherent:
- large inventory expansion,
- save/load implementation,
- meta progression,
- many new archetypes,
- many new dungeon node types,
- large procgen overhaul.

---

## 6. Documentation maintenance rule

After every meaningful milestone/PR:

1. update `PLAN.md` with:
   - what is finished,
   - what is in progress,
   - what is next;
2. update `README.md` only if setup/run instructions changed;
3. update `roadmap_raw_test_md_md.md` only for long-form design revisions;
4. update this file if the keep/rework/split strategy changes.

---

## 7. Working conclusion

The correct response to the previous over-large PR is **not** to throw everything away.

It is to:
- keep the good foundation,
- clearly mark provisional systems as provisional,
- split future changes into smaller PRs,
- and keep documentation synchronized with the real state of the repo.
