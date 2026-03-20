# Glyphbound Plan

Этот файл — короткий рабочий план по текущему состоянию репозитория.
Подробный дизайн- и roadmap-анализ сохранён в `roadmap_raw_test_md_md.md`.

## Что уже выполнено
- Базовый детерминированный тактический прототип собран в модульной структуре `app`, `core:model`, `core:rules`, `core:procgen`.
- Реализованы hazard/environment-системы, детерминированная симуляция и data-driven tuning.
- Добавлены базовые враги, их интенты, ходы врагов и отображение в HUD/рендере.
- Добавлен ранний `RunState`/run graph и первичный UI выбора ветки после зачистки узла.
- В репозитории уже есть расширенный дизайн-роадмап с целевым развитием проекта.

## Что делаем сейчас
- Приводим план обратно к явному, поддерживаемому артефакту в репозитории после замечания по предыдущему PR.
- Синхронизируем короткий статусный план с уже существующим большим roadmap-документом.
- Фиксируем следующий ближайший фокус: довести encounter-loop до более цельного vertical slice, а не просто наращивать список систем.
- Уточняем статус текущего vertical slice: базовый run graph, branch choice, enemy phase и HUD уже есть, но слой наград/ресурсов/пост-боя ещё не доведён до полноценного run loop.

## Что ещё предстоит
- Доработать enemy/combat loop: richer enemy roles, telegraphing, death/loot/reward flow, encounter pacing.
- Развить run progression: осмысленные branch consequences, типы узлов, награды, recovery/event/shop layers.
- Добавить inventory/resources/perks/build choices, чтобы route choice влиял на стиль прохождения.
- Продумать persistence/save/meta progression и DTO/state serialization.
- Улучшить UI/UX: лучшее объяснение intents, branch choice, состояния run и post-combat transitions.
- Поддерживать этот файл в актуальном состоянии при каждом заметном milestone/PR.
