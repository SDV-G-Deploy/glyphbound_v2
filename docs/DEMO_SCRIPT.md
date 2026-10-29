# Glyphbound demo script (v0.3.0)

A 5–7 minute walkthrough that shows the full run loop on a real device or emulator.

## 0. Setup (before the audience arrives)
1. Get the APK: download the `glyphbound-debug-apk-<sha>` artifact from the latest **Android Debug APK**
   workflow run, or build with `./gradlew :app:assembleDebug` (`app/build/outputs/apk/debug/app-debug.apk`).
2. Install it with `adb install -r app-debug.apk`.
3. Launch on a known-good seed. These seeds are checked by `RunDirectorTest.demoSeeds_areClearableByTheBot`, so a
   simple bot can clear them:
   - **Safe demo:** seed `1337` on EASY.
     ```bash
     adb shell am start -n com.sdvgdeploy.glyphbound/.MainActivity --el seed 1337 --es profile EASY
     ```
   - **Standard demo:** seed `1` on NORMAL.
     ```bash
     adb shell am start -n com.sdvgdeploy.glyphbound/.MainActivity --el seed 1 --es profile NORMAL
     ```
   Without extras, the app continues the autosaved run, or starts seed 1337 on NORMAL.
4. Optional: turn on **High contrast** if you are projecting the screen.

## 1. The floor (≈1 min)
- The map is a deterministic 24×16 glyph grid: `@` is you, `S` is the start, `E` is the exit, and `#` is a wall.
- Hazards: `~` risk, `o` oil, `w` water, `*` spark. Stepping next to a spark ignites nearby oil (`f`, which leaves
  `^` fire zones) or electrifies water (`z`, `!` shock zones). Walk through it and point out the log line.
- Read the HUD: `HP`, then `Seed 1337:EASY:v1` (the reproduction key), the node label, `D0/3` (depth),
  `Dr0` (draughts), `NX` (pending next-node effects), and `EN M/R/A` (enemy intents: melee, ranged, advancing).
- Move with the D-pad or by swiping on the map.

## 2. Enemies and intents (≈1 min)
- `g` stalker, `O` brute (2 damage), `s` spitter (ranged, needs a straight line of sight).
- Point at `EN M1`: an adjacent foe will strike after your move. Bump into an enemy to hit it for 1.
- Reach `E` to clear the floor.

## 3. Branch choice (≈1 min)
- After the entry floor, two buttons appear: **Combat • <theme>** (reward pick) vs **Treasure • <theme>**
  (lighter pressure, finds an Ember Draught).
- Choose **Treasure** for the safe demo. Clearing it shows `Found Ember Draught (1)` and the Draught button lights up.

## 4. Depth 2: Elite vs Rest (≈1 min)
- **Rest** has no foes, and the campfire heals +3 HP (`Campfire: +3 HP`). It's a good place to show recovery.
- **Elite** has two foes, then a pick of 3 rewards (Field Mend / Salvage Cache / Spark Core).
  - Salvage and Spark Core carry over to the next floor. Point out `NX -1 foe` / `NX +1 HP` in the HUD, then
    `Reward scouting` / `Reward surge` when you enter the next floor.

## 5. Boss and victory (≈1–2 min)
- The boss floor log says *"Boss floor: defeat or evade the warden pack and reach E"*. A brute leads the pack.
- Drink the Draught when HP is low. It's a free action, so enemies don't move.
- Reach `E` to finish the run. The summary shows *Run cleared • N moves • 4 nodes • HP x • record 1/1*.
- Tap **Next seed** to show that a new seed gives a new layout (seeds no longer share maps).

## 6. Persistence (≈30 s)
- Start a run and enter the second floor, then swipe the app away and relaunch it. You're back at the start of
  that floor with the same HP, map and enemies. The save is a node-entry checkpoint, schema v1.
- Rotate the device mid-floor: the run is not reset.

## 7. Talking points / honest limitations
- Everything gameplay-related is deterministic and runs on the plain JVM in `core:*`, with 70+ unit tests,
  including bot playthroughs across seeds.
- Saves are per floor (checkpoint at floor entry), not per move.
- There's one consumable and three reward types so far. No shop or shrine events yet, and the run is a fixed
  4-depth graph. HARD is intentionally punishing.
- There's no title screen or seed entry UI yet. Seeds come from intent extras or **Next seed**.
