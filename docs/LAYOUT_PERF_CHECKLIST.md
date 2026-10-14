# Layout Performance Checklist

Живой чек-лист расследования тормозов layout (старт: скролл VBox + `clearLayout` 90% времени).
Статусы: `DONE` — сделано и замерено; `SUSPECT` — под подозрением, нужна проверка;
`DISMISSED` — проверено, не виновато / отброшено с причиной.

## DONE

- [x] **Шаг 1. Прямой O(N) сброс LAYOUT-флагов.**
  `WidgetBase.clearInvalidationDirect` (сброс обоих полей без `recompute`) +
  использование в `MinecraftWidgetScreen.clearSubtreeInvalidation`.
  Квадратичный пузырьковый пересчёт убран, очистка линейная.
- [x] **Шаг 2. `ScrollView.scrollTo` без флага LAYOUT.**
  Только `arrangeScrolledChildrenNow()` (O(1), нужен для хит-теста) +
  `invalidate(VISUAL)`. `shouldRunLayout` больше не срабатывает на скролл.
- [x] **Шаг 3. Кэш повторного measure (`measureCached`).**
  `Widget.measureCached` default + override в `WidgetBase` с ключом
  (availableW, availableH) и проверкой чистоты поддерева; `FlexLayoutEngine.preparedItems`
  переведён на `measureCached`. Дефолтные `min/maxContentSize` сбрасывают кэш,
  т.к. затирают desired вне кэша. Счётчики hits/misses для диагностики.
- [x] **Шаг 4 (найден в процессе). `layoutBoundsChanged` без самопереинвалидации.**
  `arrange → layoutBounds.set → onChanged → layoutBoundsChanged` ставил LAYOUT
  при любом arrange — layout сам требовал следующий layout, что убивало шаг 2.
  LAYOUT убран, FLIP-трекинг и VISUAL оставлены. Покрыто `LayoutTransitionSelfTest`.
- [x] **Шаг 4b (регрессия шага 4, найдена в игре). VISUAL обязателен в `layoutBoundsChanged`.**
  Первая версия шага 4 убрала `invalidate()` целиком — вместе с LAYOUT умер и
  `renderCacheDirty`, который ставит любой `invalidate()`. Симптомы: дети окон при
  drag и маркеры карты при pan отдавали протухший retained-фрагмент со старой
  позиции, «чинилось» наведением (hover ставит VISUAL). Фикс: `invalidate(VISUAL)`
  вместо `invalidate(LAYOUT)`. Покрыто `LayoutInvalidationSelfTest`.

## В работе

- [ ] Замер эффекта шагов 1–4 профайлером на реальном экране (скролл VBox, ресайз окна).

## SUSPECT (под подозрением)

- [ ] **Intrinsic-каскад в `preparedItems`.**
  Для детей с intrinsic/auto-размерами дёргаются `maxContentSize()` + `minContentSize()`,
  дефолт каждого — полный рекурсивный measure поддерева (`Widget.java:187-203`).
  Ребёнок с auto-шириной может измеряться 4+ раз за кадр. Нужен замер + кэш min/max
  при неизменном контенте.
- [ ] **Кэш measure по (контекст + чистота поддерева).**
  Пропуск measure чистых поддеревьев при неизменном `LayoutContext`. Зависит от шага 3.
- [ ] **Аллокации в `preparedItems`/`FlexSolver`.**
  `ObjectArrayList`, `ItemInput`, `CellPlacement`, `MutableRect` на ребёнка каждый кадр.
  Линейны, приоритет низкий — только по показаниям профайлера после шагов 1–3.

## DISMISSED (отброшено)

- [x] **`LayoutV3ScrollAdapter` / WebLayoutEngine как горячая точка — НЕТ.**
  Проверено (`LayoutV3ScrollAdapter.java:19-68`): контент измеряется один раз
  (строка 32), `WebLayoutEngine.compute` решает задачу из 2 нод с готовой мерой.
  Десятки объектов, не тысячи. Не чинить.
- [x] **Сдвиг скролла через transform вместо arrange — НЕТ.**
  Хит-тест (`TransformHitTester`) ходит по layoutBounds, обычные контейнеры не
  реализуют `HitTestCoordinateMapper` — клики по проскролленному контенту поедут.
  `arrangeContent` остаётся, убирается только LAYOUT-флаг.
- [x] **Post-order обход в `clearSubtreeInvalidation` как фикс O(N²) — НЕТ.**
  Порядок обхода не меняет сути: каждый лист всё равно дёрнет родителя через
  `recomputeSubtreeInvalidation`. Нужен прямой сброс полей (шаг 1).
- [x] **Слепое удаление `child.measure` из arrange-пути flex — НЕТ.**
  `arrange` вызывается и напрямую без measure (`arrangeScrolledChildrenNow`,
  `StackPanel.arrangeChild`, тесты) — desiredSize может быть протухшим.
  Только guard по флагам (шаг 3).
