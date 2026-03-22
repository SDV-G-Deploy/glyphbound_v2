# Glyphbound Plan

Этот файл — короткий рабочий план по текущему состоянию репозитория.
Подробный дизайн- и roadmap-анализ сохранён в `roadmap_raw_test_md_md.md`.

## Что уже выполнено
- Базовый детерминированный тактический прототип собран в модульной структуре `app`, `core:model`, `core:rules`, `core:procgen`.
- Реализованы hazard/environment-системы, детерминированная симуляция и data-driven tuning.
- Добавлены базовые враги, их интенты, ходы врагов и отображение в HUD/рендере.
- Добавлен ранний `RunState`/run graph и первичный UI выбора ветки после зачистки узла.
- Добавлен ранний слой reward choice / progression summary, который уже связывает зачистку узла со следующим переходом по run graph.
- В репозитории уже есть расширенный дизайн-роадмап с целевым развитием проекта.
- Добавлены отдельные документы `RECOVERY_PLAN.md` и `DEVELOPMENT.md`, чтобы не смешивать long-term roadmap, короткий статус и практические правила разработки.

## Что делаем сейчас
- Поддерживаем документацию в синхроне с фактическим состоянием репозитория после слишком широкого предыдущего PR.
- Фиксируем recovery-стратегию: что сохраняем как baseline, что считаем provisional, и как дальше дробить работу на более узкие PR.
- Уточняем ближайший фокус: не расширять систему хаотично, а довести encounter/run vertical slice до более цельного состояния.
- Держим явным факт, что roadmap-файл содержит long-term дизайн, а не точное отражение каждой уже реализованной детали в коде.

## Что ещё предстоит
- Доработать enemy/combat loop: richer enemy roles, hazard synergy, death/loot/reward flow, encounter pacing.
- Развить run progression: осмысленные branch consequences, типы узлов, награды, recovery/event/shop layers.
- Добавить inventory/resources/perks/build choices, чтобы route choice влиял на стиль прохождения.
- Продумать persistence/save/meta progression и DTO/state serialization.
- Улучшить UI/UX: лучшее объяснение intents, branch choice, состояния run и post-combat transitions.
- Поддерживать `PLAN.md`, `RECOVERY_PLAN.md`, `DEVELOPMENT.md` и `README.md` в актуальном состоянии при каждом заметном milestone/PR.
