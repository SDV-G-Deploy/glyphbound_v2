# Glyphbound — roadmap and design analysis

> Status note: this is a **long-form design/archive document**, not a line-by-line mirror of the current codebase.
> Some sections below describe planned or earlier target states that have since been partially implemented in the repository.
> For the current operational status and recovery strategy, see `PLAN.md` and `RECOVERY_PLAN.md`.

## Goal
Evolve Glyphbound from a deterministic ASCII-like Android prototype into a deep ADOM-inspired tactical roguelite/roguelike while preserving a clear glyph-first interface, deterministic debugging, and mobile-friendly sessions.

---

## 1. Current repository assessment

### 1.1 What the project already does well
- Modular separation is already strong:
  - `app` handles Android UI, input, and rendering.
  - `core:model` holds domain state and tuning.
  - `core:rules` contains the gameplay reducer/pipeline.
  - `core:procgen` handles deterministic generation and validation.
- The gameplay loop is already deterministic and test-minded.
- Hazard/environment interactions are the most promising systemic pillar.
- Difficulty tuning is already data-driven through JSON.
- Test coverage already focuses on determinism, bounded behavior, and reproducibility.

### 1.2 What the project is right now
Current state is best described as:
- **solid technical prototype**
- **environmental tactics prototype**
- **pre-vertical-slice roguelite foundation**

It is not yet a full roguelike loop because it still lacks:
- a mature enemy ecology,
- real combat depth,
- inventory/resources,
- multi-floor / dungeon structure,
- fully realized run progression,
- persistent save/meta progression,
- meaningful long-term replay loops.

### 1.3 Core design DNA worth preserving
The current DNA worth preserving:
- deterministic systems,
- tile-based decision making,
- environment-driven danger,
- simple but expressive ASCII rendering,
- testability,
- tunable difficulty profiles,
- low visual production overhead.

---

## 2. ADOM-inspired direction

Glyphbound should not try to clone ADOM 1:1. Instead, it should borrow the parts of ADOM that matter most:
- systemic density,
- meaningful statuses and resistances,
- dangerous environments,
- enemy ecology,
- run-shaping progression,
- dungeon identity,
- long-term replayability.

### 2.1 What to borrow from ADOM
- status/intrinsic depth,
- multiple character archetypes,
- meaningful itemization,
- resource pressure,
- dangerous branching areas,
- identity-rich enemies,
- consequences that shape the rest of the run.

### 2.2 What not to copy blindly
- encyclopedic complexity too early,
- overloaded micromanagement,
- unreadable UI,
- too many opaque sub-systems at once,
- desktop-style interface burden that feels wrong on mobile.

### 2.3 Product identity for Glyphbound
Target identity:
- **short-to-medium run tactical ASCII dungeon roguelite**
- deep through system interactions,
- readable on mobile,
- replayable through generation, archetypes, hazards, enemy sets, and run modifiers,
- deterministic enough for shareable seeds and exact bug reports.

---

## 3. Strategic design goals

### 3.1 Primary game promise
Every run should create interesting decisions through:
- movement,
- environmental manipulation,
- enemy positioning,
- resource conservation,
- build selection,
- route choice.

### 3.2 Core principles
1. **Readable complexity**
   - Complexity should come from combinations of simple systems.
2. **Every glyph matters**
   - Symbols must communicate gameplay, not decoration.
3. **Determinism is a feature**
   - Seed reproducibility, tuning versioning, and stable simulation should be preserved.
4. **Short sessions, deep decisions**
   - This should stay mobile-friendly.
5. **Systemic depth over content spam**
   - Add expressive mechanics before flooding the game with content.

---

## 4. Desired future gameplay loop

### 4.1 Run structure
A future run should look like this:
1. Start a new run.
2. Choose or unlock a starting archetype.
3. Enter a dungeon branch/node.
4. Explore a tactical ASCII map.
5. Fight enemies and use the environment.
6. Gain loot, consumables, modifiers, or perks.
7. Move to the next floor/node.
8. Make branch decisions:
   - safer route,
   - riskier route,
   - elite branch,
   - recovery node,
   - merchant/shrine/event.
9. Reach boss/end condition or die.
10. Earn unlocks/meta progress.

### 4.2 Layers that must emerge over time
Glyphbound needs the following layers:
- encounter layer,
- combat layer,
- enemy ecology layer,
- run progression layer,
- dungeon graph layer,
- build/archetype layer,
- metaprogression layer,
- persistence/save layer.

---

## 5. Major systems roadmap

## 5.1 Enemies

### Current gap
A first enemy layer now exists in the repository, but it is still thin and should not yet be treated as a fully developed combat/ecology system.

### Goal
Enemies should become the second major gameplay pillar next to hazards.

### Recommended evolution
#### Phase E1 — baseline enemies
Add:
- enemy entity model,
- enemy type definitions,
- basic AI intent,
- glyph and palette support,
- enemy turn phase.

Starter enemy roles:
- **Chaser** — closes distance and pressures movement.
- **Ranged** — threatens line-of-sight space.
- **Hazard-user** — prefers sparks/fire/oil and environmental combos.
- **Blocker** — guards chokepoints and slows progress.

#### Phase E2 — identity-rich behaviors
Add traits such as:
- fire-resistant,
- shock-resistant,
- leaves hazard on death,
- splits,
- charges,
- telegraphs attacks,
- patrols,
- retreats,
- buffs nearby allies.

#### Phase E3 — enemy ecology
Introduce families and environmental preferences:
- water/shock creatures,
- oil/fire cultists,
- ash/scavenger enemies,
- poison/rot swarms,
- armored sentries for chokepoint play.

### Why this matters
Without enemies, the game remains mostly navigational. With enemies, it gains:
- tempo,
- pressure,
- target priority,
- kill-vs-avoid decisions,
- build relevance.

---

## 5.2 Combat system

### Recommended early design
Keep combat simple but expressive.

#### Phase C1 — first combat model
- bump attack,
- deterministic damage values or small ranges,
- enemy attack phase,
- basic death handling.

#### Phase C2 — tactical combat depth
- knockback,
- line attacks,
- zone attacks,
- shield/guard,
- enemy telegraphing,
- on-hit statuses.

#### Phase C3 — lightweight stats
Avoid overly large stat sheets. Use only what matters:
- Power,
- Guard,
- Speed,
- Range,
- Resistances,
- Mobility.

### Design warning
Do not overbuild RPG math too early. The project’s strength is tactical readability, not simulation bloat.

---

## 5.3 Status and effect system

### Current opportunity
The code already contains `EnvEffect` structures, but status gameplay is not yet a full subsystem.

### Goal
Unify:
- actor effects,
- hazard effects,
- terrain interactions,
- resistances,
- temporary and persistent traits.

### Recommended statuses
Early set:
- Burning,
- Shocked,
- Poisoned,
- Slowed,
- Stunned,
- Bleeding,
- Chilled,
- Marked.

### Secondary layer
Add resistances/intrinsics-lite:
- fire resistance,
- shock resistance,
- poison resistance,
- terrain affinity,
- visibility/vision traits,
- curse/blessing style modifiers.

### ADOM-inspired but simplified later layer
- corruption/pressure meter,
- hunger/fatigue-lite,
- cursed/doomed-like modifiers,
- rare permanent run-changing traits.

### Rule of thumb
Prefer 8 strong, combinatorial statuses over 30 weak ones.

---

## 5.4 Procedural generation and level structure

### Current strength
Generation already guarantees connectivity and disjoint-path policy.

### Current weakness
Maps are valid, but not yet dramatically authored or biome-rich.

### Recommended evolution
#### Phase P1 — room semantics
Move from pure random fill to semantically tagged rooms/zones:
- flooded chamber,
- oil store,
- spark nest,
- ash field,
- shrine room,
- vault,
- lair,
- rest room.

#### Phase P2 — authored motifs
Introduce tactical scene templates:
- corridor with ranged enemy,
- oil spill near spark source,
- split loot chamber,
- chokepoint ambush,
- optional trapped treasure route.

#### Phase P3 — branch-aware generation
Allow each floor/node to reflect branch identity:
- safer branch,
- elite branch,
- loot-heavy branch,
- recovery branch,
- boss branch.

#### Phase P4 — biome/dungeon identity
Examples:
- Ember Warrens,
- Flooded Archive,
- Cracked Bastion,
- Rot Pits,
- Glass Vault.

### Structural advice
Eventually split generation into two layers:
- **macro generation** — node graph / branch topology,
- **micro generation** — floor grid generation.

---

## 5.5 Run system

### Goal
Turn isolated maps into runs with pacing and consequences.

### Run model should include
- current floor/node,
- dungeon graph,
- player state,
- inventory,
- perk/build state,
- visited nodes,
- run modifiers,
- run history.

### Node/floor types
- combat,
- elite combat,
- treasure,
- merchant,
- shrine,
- rest,
- event,
- branch gate,
- boss.

### Recommended shape
Best fit for Glyphbound is a hybrid:
- branch/node graph for pacing and routing,
- most nodes still resolve as compact ASCII tactical maps.

This keeps the “dungeon” feeling but gives clearer run structure for mobile sessions.

---

## 5.6 Progression and builds

### Three progression layers
#### 1. In-run progression
- level-ups,
- perk choices,
- gear,
- consumables,
- resistances,
- abilities.

#### 2. Unlock progression
- new archetypes,
- new branches,
- new enemy pools,
- new relic pools,
- new events.

#### 3. Meta/mastery progression
- run history,
- codex,
- difficulty unlocks,
- badges,
- daily mode rewards,
- account-level achievements.

### Recommended first approach
Instead of heavy classes, start with **archetypes**:
- **Scavenger** — economy and utility.
- **Spark Adept** — shock interactions and control.
- **Pyre Walker** — fire synergy and hazard aggression.
- **Warden** — defense and control.
- **Shadow Runner** — stealth, mobility, repositioning.

Each archetype should have:
- one starting passive,
- one starting item,
- a biased perk pool.

---

## 5.7 Items, inventory, and resources

### Goal
Add reasons to adapt mid-run.

### Minimal inventory approach
Avoid deep inventory management at first. Use:
- a few equipment slots,
- a small consumable bar,
- simple list-based inventory UI.

### Item categories
- weapons,
- offhand/tools,
- armor/charms,
- consumables,
- throwables,
- relics/run modifiers.

### Systemic examples fitting Glyphbound
- insulated boots,
- oil lantern,
- grounding ring,
- spark rod,
- ash charm,
- smoke vial,
- brine flask,
- static trap kit.

These reinforce the existing hazard-first identity.

---

## 5.8 Replayability

Replayability should come from multiple axes at once:
- seed variance,
- branch topology,
- room motifs,
- enemy set variance,
- item/perk variance,
- route choice,
- archetypes,
- mutators/daily seeds,
- boss and elite variety.

### Important recommendation
Use determinism as a player-facing feature later:
- daily seed challenge,
- shareable runs,
- exact replay/debug seeds,
- competitive scoreboards.

---

## 5.9 Saves and persistence

### Two kinds of save are needed
#### 1. Suspend/resume save
For resuming an active run:
- current node/floor,
- current map seed,
- player state,
- enemy state,
- hazard state,
- inventory,
- log,
- progression state.

#### 2. Meta save
For long-term player profile:
- unlocks,
- codex,
- settings,
- achievements,
- statistics,
- run archive.

### Engineering recommendation
Plan DTO/state serialization early.
The domain model should evolve in a way that remains serializable and versionable.

---

## 6. Delivery roadmap by milestones

## M0 — Stabilization and pre-production foundation
### Goal
Prepare the codebase for large feature growth.

### Deliverables
- align versioning/release metadata,
- introduce save/run state scaffolding,
- prepare enemy/item/run domain structures,
- formalize extended turn pipeline,
- decide schema versioning strategy for save data,
- improve HUD contracts for future systems.

### Why first
Without this, later feature work will become tangled quickly.

---

## M1 — Vertical slice: combat dungeon
### Goal
Turn the prototype into a true run-based playable slice.

### Deliverables
- 3–4 enemy types,
- enemy turn phase,
- basic combat,
- death/win/restart flow,
- floor-to-floor progression,
- first loot/consumables,
- active run save.

### Outcome
Game becomes a real tactical roguelite prototype.

---

## M2 — Status and build layer
### Goal
Make runs play differently.

### Deliverables
- real actor statuses,
- resistances/intrinsics-lite,
- 3 archetypes,
- level-up/perk selection,
- 10–15 items,
- elites,
- improved combat/event clarity.

### Outcome
The player starts expressing a build, not just surviving a map.

---

## M3 — Dungeon graph and branching
### Goal
Add route strategy and pacing variety.

### Deliverables
- node graph for runs,
- optional branches,
- shrine/merchant/rest/event nodes,
- branch-specific generation themes,
- branch-specific enemy pools.

### Outcome
Runs gain strategic routing and stronger replayability.

---

## M4 — Replayability and metaprogression
### Goal
Improve long-term retention.

### Deliverables
- unlock system,
- codex,
- daily seed mode,
- mutators,
- score/run summary,
- achievements,
- new archetype and branch unlocks.

### Outcome
Runs matter even after failure.

---

## M5 — ADOM-inspired depth expansion
### Goal
Increase systemic density without sacrificing readability.

### Deliverables
- corruption/pressure system,
- curse/blessing systems,
- rare artifacts,
- faction-like enemy families,
- unusual events,
- stronger world identity.

### Outcome
The game approaches ADOM-like decision density in a compact format.

---

## 7. Practical implementation sequence

This is the recommended real implementation order:

1. Add state architecture for enemies/items/run/save.
2. Add enemy entities and enemy turn phase.
3. Add floor progression and proper run end states.
4. Add basic inventory, loot, and consumables.
5. Add statuses and resistances.
6. Add archetypes and perks.
7. Add branch graph and themed node types.
8. Add save/resume plus metaprogression.

This order is both technically and design-wise efficient.

---

## 8. Detailed six-iteration execution plan

## Iteration 1 — Core expansion
### Purpose
Prepare the engine for future systems.

### Tasks
- add run-related domain models,
- add save DTO skeletons,
- add actor/enemy abstractions,
- refactor state shape where needed,
- keep behavior unchanged where possible,
- preserve deterministic tests.

---

## Iteration 2 — First true run
### Purpose
Create a playable run from start to finish.

### Tasks
- enemy phase,
- floor progression,
- restart/death/win UX,
- first items,
- first active run save.

---

## Iteration 3 — Status and environment 2.0
### Purpose
Deepen interaction density.

### Tasks
- actor statuses,
- resistances,
- richer hazard combos,
- enemy-hazard interplay,
- better status rendering/logging.

---

## Iteration 4 — Build identity
### Purpose
Make runs feel different by play style.

### Tasks
- archetypes,
- perk choices,
- stronger loot identity,
- elite encounters,
- build-defining synergies.

---

## Iteration 5 — Strategic routing
### Purpose
Make path choice matter beyond the current floor.

### Tasks
- node graph,
- branch identity,
- rest/merchant/shrine/event rooms,
- risk-vs-reward route decisions,
- theme-specific content pools.

---

## Iteration 6 — Long-term retention
### Purpose
Strengthen return play and persistence.

### Tasks
- meta progression,
- codex,
- run history,
- daily challenge mode,
- unlock trees,
- advanced difficulty settings.

---

## 9. Technical implementation notes for future work

### 9.1 Domain model notes
The current state should evolve into a richer structure, ideally separated into:
- world state,
- combat state,
- progression state,
- run meta state,
- save state.

### 9.2 Rules engine notes
The current reducer pipeline is already a strong base. Future pipeline should eventually include:
1. validate input,
2. player action,
3. direct combat resolution,
4. environment triggers,
5. enemy intent/action,
6. status ticking,
7. hazard ticking,
8. death/drop cleanup,
9. progression updates,
10. message/event building,
11. transition checks.

### 9.3 Procgen notes
Generation should eventually become two-level:
- run graph generator,
- floor generator.

### 9.4 UI notes
ASCII map should remain the center of the interface.
Additional UI should be overlays/panels, not a replacement for glyph readability.

Future UI additions likely needed:
- start run screen,
- results screen,
- inventory overlay,
- inspect mode,
- run summary,
- continue/resume entry point.

---

## 10. Final recommendation

Glyphbound should become:
- not a giant clone of ADOM,
- but a focused, mobile-friendly, deterministic, ASCII tactical dungeon roguelite
- with enough systemic depth that every run feels expressive and risky.

The best near-term path is:
1. infrastructure for run/enemy/save state,
2. enemies and combat,
3. floor progression,
4. inventory/resources,
5. statuses/builds,
6. branching dungeons,
7. persistence and meta.

That path is the most realistic for implementation on top of the current repository.
