# Docking & Window Management System — реализационный план

## Цель

Построить поверх существующих `DockPanel`, `OverlayLayer`, `Popup` и `WindowWidget` полноценную систему управления окнами и dock-панелями для AAA UI Framework: floating windows, dockable tool panels, tabbed documents, split docking, modal dialogs, z-order/focus, drag/drop docking preview и сохранение layout-состояния.

Система должна оставаться backend-neutral, работать в Minecraft UI loop, соблюдать `WIDGETS_CONTRACT.md` и не ломать текущий retained widget tree / Layout V3 path.

## Текущая база

Уже есть:

- `DockPanel` с sequential edge docking и `lastChildFill`.
- `LayoutV3DockAdapter` и тестовое покрытие DockPanel V2/V3 parity.
- `OverlayLayer` как root-level overlay host с deterministic overlay ordering.
- `Popup`/`ContextMenu`/`Tooltip` basics поверх overlay host.
- `WindowWidget` с title/header renderer, close button, open/close/toggle, pointer-captured dragging, host constraints и `WindowRenderer`/`WindowState`.
- Event routing, pointer capture, focus manager, hover tracking и capture/target/bubble phases.

Главный пробел: нет центрального window/docking manager, layout tree для dock nodes, dock drag lifecycle, z-order/focus policy, modal layer, docking preview, persistence и typed events для window/docking semantics.

## Архитектурные принципы

- Window/docking actions идут через typed events, а не через публичные `Runnable`/`Consumer` callbacks.
- Default rendering идёт через renderer/state snapshot; preview, tab strip, split handle, title bar и modal scrim должны иметь renderer extension points.
- Public API использует обычные Java interfaces/records; fastutil допустим только внутри hot-path implementation.
- Docking layout должен быть model-first: widget tree отображает модель, а не является единственным источником истины.
- Floating windows живут в overlay layer, docked panes живут в normal layout tree.
- Drag/drop docking использует pointer capture и routed semantic events.
- Layout changes идут через deferred mutations / safe points, без изменения traversal во время event dispatch.

## Public model и API

### Core classes

- `WindowManager` — управляющий слой для floating windows, modal stack, z-order, active window, focus handoff.
- `DockingManager` — управляющий слой для dock tree, dock targets, drag preview, split/tab operations.
- `DockingRoot` — composite/root widget, объединяющий docked content area и overlay host для floating windows.
- `DockNode` — model node: split, tab group, leaf pane.
- `DockPane` — dockable content descriptor: id, title, content widget, allowed areas, close/pin/float flags.
- `DockTabGroup` — tabbed group model + widget surface.
- `DockSplitNode` — horizontal/vertical split model с ratio/min constraints.
- `DockFloatingHost` — bridge между `DockPane` и `WindowWidget` для floating state.

### Ключевые enums/records

- `DockArea`: `LEFT`, `RIGHT`, `TOP`, `BOTTOM`, `CENTER`, `TAB`, `FLOAT`.
- `DockSplitOrientation`: `HORIZONTAL`, `VERTICAL`.
- `DockDropIntent`: target pane/group/root + area + split ratio/tab index.
- `WindowActivationPolicy`: click-to-front, modal-only, focus-follows-click.
- `WindowStateFlags`: active, focused, modal, dragging, resizing, dockPreviewVisible.
- `DockLayoutSnapshot`: serializable snapshot для сохранения layout.

### Базовый user-facing API

~~~java
DockingRoot root = new DockingRoot()
        .addPane("inspector", "Inspector", inspectorWidget, DockArea.RIGHT)
        .addPane("assets", "Assets", assetsWidget, DockArea.LEFT)
        .addDocument("scene", "Scene", sceneWidget);

root.docking().floatPane("inspector");
root.docking().dockPane("assets", "scene", DockArea.LEFT);
root.windows().bringToFront(window);
~~~

API должен поддерживать fluent setup, но пользовательские действия должны публиковаться через events.

## События

Добавить typed events в `common/src/main/java/dev/sixik/unigui/api/event/`.

### Window events

- `WindowOpenedEvent`
- `WindowClosedEvent`
- `WindowActivatedEvent`
- `WindowDeactivatedEvent`
- `WindowMoveStartedEvent`
- `WindowMovedEvent`
- `WindowMoveEndedEvent`
- `WindowResizeStartedEvent`
- `WindowResizedEvent`
- `WindowResizeEndedEvent`
- `ModalOpenedEvent`
- `ModalClosedEvent`

### Docking events

- `DockDragStartedEvent`
- `DockDragMovedEvent`
- `DockDragEndedEvent`
- `DockDropPreviewChangedEvent`
- `PaneDockedEvent`
- `PaneFloatedEvent`
- `PaneTabbedEvent`
- `PaneClosedEvent`
- `DockLayoutChangedEvent`
- `DockLayoutRestoredEvent`

Для интерактивных действий события должны быть `RoutableWidgetEvent`, хранить `target`, `currentTarget`, `phase`, и копировать `cancelled` state в `routeTo(...)`.

## Renderer/state контракт

Добавить renderer/state типы в `common/src/main/java/dev/sixik/unigui/widgets/render/`.

- `DockingRootRenderer` / `DockingRootState`
- `DockPaneRenderer` / `DockPaneState`
- `DockTabGroupRenderer` / `DockTabGroupState`
- `DockSplitHandleRenderer` / `DockSplitHandleState`
- `DockDropPreviewRenderer` / `DockDropPreviewState`
- `WindowManagerOverlayRenderer` / `WindowManagerOverlayState`
- `ModalScrimRenderer` / `ModalScrimState`

State должен содержать:

- bounds;
- enabled/hovered/pressed/dragging/focused/active/modal;
- pane id/title/icon/dirty state;
- selected tab index и visible tab range;
- split orientation/ratio/min sizes;
- dock preview target area;
- z-order/focus state для windows;
- colors/style-derived values.

Подключить defaults через `WidgetsRender`, `WidgetsRenderImpl`, `DefaultWidgetsRenderImpl`.

## Фаза 1 — WindowManager MVP

Цель: централизовать текущее поведение `WindowWidget` без изменения внешней совместимости.

Задачи:

- Добавить `WindowManager` как model/controller для windows внутри `OverlayLayer`.
- Ввести registration lifecycle: add/remove/open/close/activate.
- Добавить z-order: active window поднимается наверх, modal windows выше обычных.
- Добавить active/focused window state и синхронизацию с `FocusManager`.
- Перевести open/close/activate/move actions на typed events.
- Расширить `WindowState`: active, focused, dragging, modal, resizable flags.
- Добавить `WindowManagerState` и renderer для overlay-level affordances.
- Сохранить `WindowWidget` как публичный retained window shell.

Acceptance criteria:

- Клик по окну активирует его и поднимает в z-order.
- Закрытие/открытие публикует typed events.
- Существующие window examples работают без API break.
- `:common:compileJava` и `:common:test` проходят.

## Фаза 2 — Resizable windows

Цель: добавить production-ready resize mechanics.

Задачи:

- Добавить resize handles: edges/corners.
- Добавить min/max window size и host constraints.
- Добавить resize cursor policy.
- Реализовать pointer-captured resize lifecycle.
- Добавить `WindowResizeStartedEvent`, `WindowResizedEvent`, `WindowResizeEndedEvent`.
- Добавить renderer state для resize hover/active handles.
- Покрыть tests: min size clamp, host clamp, resize event lifecycle.

Acceptance criteria:

- Resize работает по всем краям/углам.
- Resize не выводит окно за host при включённом `constrainToHost`.
- Renderer получает hover/drag handle state.

## Фаза 3 — Modal window stack

Цель: добавить корректные dialog/modal semantics.

Задачи:

- Добавить modal stack в `WindowManager`.
- Добавить modal scrim renderer и input blocking ниже top modal.
- Добавить focus trap внутри active modal.
- Добавить `ModalWindow` как specialization или builder над `WindowWidget`.
- Добавить escape/close policy и close veto через cancellable events.
- Добавить `ModalOpenedEvent` / `ModalClosedEvent`.

Acceptance criteria:

- Pointer/key input не уходит в widgets под modal layer.
- Top modal получает focus priority.
- Scrim rendering кастомизируется renderer/state.

## Фаза 4 — Docking model MVP

Цель: ввести model-first dock layout без drag/drop.

Задачи:

- Реализовать `DockNode` tree: leaf, tab group, split.
- Реализовать `DockPane` descriptor с stable id.
- Реализовать `DockingRoot` как composite widget.
- Поддержать операции add pane, close pane, select tab, split pane, tab pane into group, float pane into `WindowWidget`.
- Добавить `DockLayoutChangedEvent`.
- Добавить snapshot generation `DockLayoutSnapshot`.
- Добавить минимальные renderers для tab group/split handle/pane chrome.

Acceptance criteria:

- Dock tree корректно measure/arrange через Layout V3.
- Pane можно программно dock/tab/float без drag UI.
- Snapshot стабилен и не зависит от object identity.

## Фаза 5 — Dock drag/drop lifecycle

Цель: добавить интерактивное dock-перетаскивание.

Задачи:

- Добавить drag threshold и capture pointer на pane title/tab.
- Реализовать `DockDragController`.
- Реализовать hit-testing dock targets: root edges, tab group center, pane split zones, floating drop area.
- Добавить `DockDropIntent`.
- Добавить dock preview overlay renderer.
- Добавить typed events: drag started/moved/ended, preview changed.
- На drop выполнять dock/tab/float operation через model mutation.

Acceptance criteria:

- Drag pane показывает preview зоны.
- Drop в edge создаёт split.
- Drop в center tab group создаёт tab.
- Drop вне dock root создаёт floating window.
- Cancel drag возвращает pane в исходное состояние.

## Фаза 6 — Tabbed documents и tool windows

Цель: покрыть типичный editor layout: center documents + side tool windows.

Задачи:

- Ввести различие `document pane` и `tool pane`.
- Document panes по умолчанию dock в center tab group.
- Tool panes поддерживают pin/auto-hide future flag, но auto-hide оставить вне MVP.
- Добавить close/dirty/active affordances в tab state.
- Добавить tab overflow strategy: scroll buttons или clipped visible range.
- Добавить keyboard shortcuts hooks через events: close active tab, next/previous tab.

Acceptance criteria:

- Можно собрать layout: left assets, right inspector, bottom log, center documents.
- Tab group корректно выбирает active pane.
- Close active tab не ломает dock tree.

## Фаза 7 — Persistence

Цель: сохранять и восстанавливать dock/window layout.

Задачи:

- Добавить `DockLayoutSnapshot` immutable records.
- Добавить codec layer без прямой зависимости public API от JSON library.
- Сохранять dock tree, selected tabs, floating positions/sizes, closed pane ids и active pane id.
- Восстанавливать layout по pane registry.
- Добавить fallback для missing/unknown pane ids.

Acceptance criteria:

- Snapshot round-trip сохраняет tree shape.
- Missing pane не крашит restore.
- Restore публикует `DockLayoutRestoredEvent`.

## Фаза 8 — Styling, render polish и UX

Цель: довести visual/interaction до production уровня.

Задачи:

- Добавить themes/style keys для active/inactive title bar, dock preview, selected/hovered/dirty tab, split handle и modal scrim.
- Добавить transitions для hover/active state, если animation API готов.
- Добавить debug overlay для dock target bounds и window z-order.
- Добавить demo page `Docking & Windows` в `TestCommands`.

Acceptance criteria:

- Все визуальные состояния доступны renderer state.
- Demo показывает floating, docking, tabbing, modal, persistence snapshot action.

## Фаза 9 — Tests

Минимальное тестовое покрытие:

- `WindowManagerSelfTest`: open/close lifecycle, activate/z-order, modal input blocking, move/resize clamp, event cancellation.
- `DockingModelSelfTest`: split/tab/float operations, close pane pruning, selected tab behavior, snapshot round-trip.
- `DockingInteractionSelfTest`: drag threshold, preview intent calculation, drop operation mapping.
- `LayoutV3SelfTest`: dock root arrange parity, split min constraints, tab group desired size.
- Demo smoke commands: interactive docking page, modal dialog example, restore layout example.

## Definition of Done

- Все user actions идут через typed events.
- Нет публичных action callbacks вместо events.
- Все новые visual surfaces имеют renderer API, state records и default renderers.
- Window/Docking renderers получают interaction state: active, hovered, dragging, focused, selected, modal.
- Floating windows и docked panes не конфликтуют по layout ownership.
- Dock drop preview работает через overlay и не ломает normal layout.
- Snapshot persistence round-trip покрыт тестами.
- `:common:compileJava`, `:common:test`, `build` проходят.
- `TestCommands` содержит demo page, демонстрирующую dock, float, tab, modal и restore behavior.

## Рекомендуемый порядок реализации

1. WindowManager MVP.
2. Window resize + events.
3. Modal stack.
4. Docking model без drag/drop.
5. Dock drag/drop preview и drop operations.
6. Tabbed documents/tool panes.
7. Persistence.
8. Styling/demo polish.
9. Расширенные tests и edge cases.

Такой порядок снижает риск: сначала стабилизируется window lifecycle/z-order/focus, затем dock model, и только после этого добавляется интерактивный drag/drop поверх уже проверенных операций.
