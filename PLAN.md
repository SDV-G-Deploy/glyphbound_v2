# Glyphbound Plan

Этот файл — короткий рабочий план по текущему состоянию репозитория.
Подробный дизайн- и roadmap-анализ сохранён в `roadmap_raw_test_md_md.md`.

_Last updated: 2026-10-29 (v0.3.0)._

## Что уже выполнено
- Базовый детерминированный тактический прототип собран в модульной структуре `app`, `core:model`, `core:rules`, `core:procgen`.
- Реализованы hazard/environment-системы, детерминированная симуляция и data-driven tuning.
- Добавлены базовые враги, их интенты, ходы врагов и отображение в HUD/рендере.
- Добавлен ранний `RunState` / run graph, UI выбора ветки после зачистки узла и первый слой reward choice / progression summary.
- В post-combat flow добавлены «reward momentum» эффекты: часть наград теперь явно переносит эффект на следующий узел (`+HP` на входе или `-1 enemy`), с consume-on-entry логикой.
- Улучшен UX выбора: reward/branch copy стал более player-facing (понятные названия и consequence-тексты вместо технических labels).
- Добавлены отдельные документы `RECOVERY_PLAN.md`, `DEVELOPMENT.md` и `RELEASING.md`, чтобы не смешивать long-term roadmap, короткий статус, практические правила разработки и release/versioning policy.
- **v0.3.0 — полный demo-run:**
  - Логика забега вынесена из `GameViewModel` в чистый `RunDirector`/`RunSession` (`core:rules`): фазы EXPLORING → CHOOSING_REWARD → CHOOSING_BRANCH → VICTORY/DEFEAT, покрыто unit-тестами и детерминированным bot-playthrough.
  - Граф забега: Entry → Combat|Treasure → Elite|Rest → Boss → Exit. Elite (2 врага, 3 награды), Rest (без врагов, +3 HP до max), Boss (стая во главе с brute).
  - Первый предмет: Ember Draught (+3 HP, free action) с Treasure-узлов.
  - Save/load: `SaveCodec` (save schema v1), автосейв на входе в узел, continue при запуске, meta-статистика (runs/wins/best depth).
  - Исправлен procgen: соседние сиды больше не схлопываются в одну карту, HARD больше не всегда fallback-коридор; валидатор путей ~3x быстрее.
  - CI собирает debug APK и гоняет тесты на каждый push/PR; `docs/DEMO_SCRIPT.md`, `CHANGELOG.md`.

## Что делаем сейчас
- Проверяем v0.3.0 на реальном устройстве по `docs/DEMO_SCRIPT.md` (UI-изменения проверены только сборкой и unit-тестами ядра, без эмулятора).
- Поддерживаем документацию в синхроне с фактическим состоянием репозитория.
- Держим recovery-стратегию и правила узких PR явными, чтобы не повторять oversized changesets.
- Уточняем ближайший фокус: не расширять систему хаотично, а довести encounter/run vertical slice до более цельного состояния.
- Проверяем баланс и читаемость нового reward→next-node перехода, чтобы progression ощущался сразу после выбора награды.
- Формализуем versioning/release discipline: product version, config schema version и будущую save schema version нужно вести раздельно.

## Что ещё предстоит
- Ближайшее для демо: стартовый экран (seed/profile/continue), визуальная обратная связь (вспышка урона, подсветка интентов), баланс HARD (наивный bot выигрывает ~2 из 44 сидов, NORMAL ~28/44, EASY ~43/44), SHRINE-событие, второй тип предмета.
- Тесты на app-слой (Robolectric или instrumented) для `GameViewModel`/`SaveStore`.
- Доработать enemy/combat loop: richer enemy roles, telegraphing, death/loot/reward flow, encounter pacing.
- Развить run progression дальше: расширить branch consequences поверх текущего reward momentum (типы узлов, recovery/event/shop layers, более глубокие route trade-offs).
- Добавить inventory/resources/perks/build choices, чтобы route choice влиял на стиль прохождения не только через +HP/-enemy эффекты.
- Продумать persistence/save/meta progression и DTO/state serialization.
- Улучшить UI/UX: лучшее объяснение intents, branch choice, состояния run и post-combat transitions.
- Поддерживать `PLAN.md`, `RECOVERY_PLAN.md`, `DEVELOPMENT.md`, `RELEASING.md` и `README.md` в актуальном состоянии при каждом заметном milestone/PR.
