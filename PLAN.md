# Glyphbound Plan

Этот файл — короткий рабочий план по текущему состоянию репозитория.
Подробный дизайн- и roadmap-анализ сохранён в `roadmap_raw_test_md_md.md`.

_Last updated: 2026-03-22._

## Что уже выполнено
- Базовый детерминированный тактический прототип собран в модульной структуре `app`, `core:model`, `core:rules`, `core:procgen`.
- Реализованы hazard/environment-системы, детерминированная симуляция и data-driven tuning.
- Добавлены базовые враги, их интенты, ходы врагов и отображение в HUD/рендере.
- Добавлен ранний `RunState` / run graph, UI выбора ветки после зачистки узла и первый слой reward choice / progression summary.
- Добавлены отдельные документы `RECOVERY_PLAN.md`, `DEVELOPMENT.md` и `RELEASING.md`, чтобы не смешивать long-term roadmap, короткий статус, практические правила разработки и release/versioning policy.

## Что делаем сейчас
- Поддерживаем документацию в синхроне с фактическим состоянием репозитория.
- Держим recovery-стратегию и правила узких PR явными, чтобы не повторять oversized changesets.
- Уточняем ближайший фокус: не расширять систему хаотично, а довести encounter/run vertical slice до более цельного состояния.
- Формализуем versioning/release discipline: product version, config schema version и будущую save schema version нужно вести раздельно.

## Что ещё предстоит
- Доработать enemy/combat loop: richer enemy roles, telegraphing, death/loot/reward flow, encounter pacing.
- Развить run progression: осмысленные branch consequences, типы узлов, награды, recovery/event/shop layers.
- Добавить inventory/resources/perks/build choices, чтобы route choice влиял на стиль прохождения.
- Продумать persistence/save/meta progression и DTO/state serialization.
- Улучшить UI/UX: лучшее объяснение intents, branch choice, состояния run и post-combat transitions.
- Поддерживать `PLAN.md`, `RECOVERY_PLAN.md`, `DEVELOPMENT.md`, `RELEASING.md` и `README.md` в актуальном состоянии при каждом заметном milestone/PR.
