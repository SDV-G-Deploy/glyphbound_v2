# Development Notes

This file is the practical day-to-day companion to `README.md`.

Use it when working on the repository, especially in ephemeral runner / Codex / Codespaces environments.

---

## 1. Java rule for this repository

This project should be built with **JDK 17**.

Important:
- the environment default `java` may point to a newer JDK such as 21;
- that does **not** mean the project should be migrated to Java 21;
- instead, explicitly point Gradle to JDK 17 before running checks if needed.

Typical explicit setup:

```bash
export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew -version
```

---

## 2. Android SDK bootstrap rule

If Android/app tasks fail because the SDK is not configured, run:

```bash
bash scripts/bootstrap-android-env.sh
```

This script is the preferred one-shot bootstrap for non-devcontainer lifecycle runners.

---

## 3. Recommended check order

### 3.1 JVM-only checks first
```bash
./gradlew :core:model:test :core:rules:test :core:procgen:test
```

### 3.2 Then Android/app checks
```bash
bash scripts/bootstrap-android-env.sh
./gradlew :app:compileDebugKotlin
./gradlew test
./gradlew :app:assembleDebug
```

---

## 4. Change management rules

### 4.1 Keep PRs narrow
Do not mix all of the following in one PR unless there is a very strong reason:
- gameplay/domain changes,
- Android UI changes,
- environment/bootstrap changes,
- CI/tooling changes,
- documentation restructuring.

### 4.2 Update docs with the code
If a milestone changes the actual project state, update:
- `PLAN.md` for short operational status,
- `README.md` if setup/run instructions changed,
- `RECOVERY_PLAN.md` if the keep/rework/split strategy changed,
- `roadmap_raw_test_md_md.md` only for large design/roadmap revisions.

### 4.3 Treat current run/progression systems as early scaffold
The repo already contains:
- `RunState`,
- reward choices,
- branch choices,
- enemy intents,
- progression summary.

These are useful, but still early-stage. Extend them carefully and validate each step with tests and UI checks.

---

## 5. Current implementation posture

Right now the repository is best treated as:
- a deterministic hazard-first prototype,
- with a first enemy layer,
- and a first run-structure layer,
- but not yet a polished full roguelite loop.

That means the safest near-term work is:
- refinement,
- simplification where needed,
- focused vertical-slice improvements,
- better documentation discipline.
