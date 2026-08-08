# UniGUI Layout V3 Design

## Зачем нужен Layout V3

Текущая система layout уже выросла из простого набора контейнеров. В проекте есть LayoutStyle, LayoutConstraints, FlexLayoutEngine, AbsoluteLayoutEngine, а часть логики всё ещё живёт внутри самих виджетов: LinearBox, WrapPanel, StackPanel, DockPanel, GridBox, ScrollView, OverlayLayer, Popup, SplitPanel.

Из-за этого новые виджеты становятся дорогими в поддержке: каждый контейнер немного по-своему считает measure/arrange, overlay-виджеты могут случайно двигать layout, clipping/scissor конфликтует с popup, а поведение процентов, min/max, grow/shrink и absolute-позиционирования сложно сделать одинаковым везде.

Layout V3 должен стать единым layout-ядром в стиле Taffy/Yoga: дерево layout-нод, единая модель стилей, один проход расчёта, отдельный слой применения результатов к Widget.

## Цели

- Сделать один источник правды для layout-расчёта вместо набора локальных реализаций в контейнерах.
- Сохранить текущий публичный API виджетов, где это возможно: preferredSize(...), grow(...), margin(...), align(...), layout(style -> ...).
- Сделать LayoutStyle главным style-object для V3, а LayoutConstraints оставить compatibility facade для старого API.
- Получить предсказуемое поведение для flex row/column, wrap, gap, margin, padding, min/max, percent, auto и absolute overlay.
- Развести normal layout и overlay layout, чтобы ComboBox, DropDownBox, Popup, context menus и tooltip-like элементы могли открываться поверх контента и не двигать родительский layout.
- Подготовить миграцию на Taffy/Yoga-совместимые semantics без большого одномоментного переписывания всех виджетов.
- Покрыть layout snapshot-тестами и визуальными smoke-примерами в TestCommands.java.

## Не цели

- Не делать полноценный CSS engine.
- Не менять rendering pipeline, font rendering, batching или framebuffer-логику в рамках Layout V3.
- Не переписывать все виджеты за один PR.
- Не привязывать публичный UniGUI API напрямую к конкретной библиотеке Taffy/Yoga.
- Не добавлять специфичные виджеты до стабилизации базовых layout-контрактов.

## Главная идея

Layout V3 должен быть не набором новых контейнеров, а отдельным engine-слоем:

    Widget tree
        -> LayoutTreeBuilder
        -> LayoutNode tree + resolved LayoutStyle
        -> LayoutEngine.compute(root, constraints)
        -> LayoutOutput tree
        -> apply bounds back to Widget.arrange(...)

Контейнеры становятся тонкими настройками style/model, а не владельцами собственного layout-алгоритма. Например HBox и VBox должны отличаться в основном flexDirection, а WrapPanel — flexWrap.

## Engine strategy

Рекомендуемый путь: сначала сделать Java-owned V3 abstraction с Taffy/Yoga-like semantics, затем подключать конкретный backend как adapter.

Почему так:

- UniGUI уже имеет LayoutStyle, SizeValue, FlexDirection, FlexWrap, Align, Justify, Overflow, PositionType; их можно маппить на V3 без ломки API.
- Taffy/Yoga semantics подходят под нужный набор: flex, absolute positioning, min/max, percent, gaps, measure callbacks.
- Прямой публичный binding к одной библиотеке опасен: если backend придётся заменить, весь widget API не должен ломаться.
- На первом этапе можно иметь TaffyLayoutEngine как внутренний Java engine/adapter, а Yoga-compatible adapter добавить позже.

Backend interface должен быть стабильнее реализации:

    public interface LayoutEngine {
        LayoutOutput compute(LayoutNode root, LayoutInput input);
    }

## Предлагаемые пакеты и классы

### dev.sixik.unigui.api.layout.v3

- LayoutEngine — общий контракт layout backend.
- LayoutNode — immutable или controlled-mutable node с children, style, measure function и widget reference id.
- LayoutInput — root constraints: available width/height, scale, layout direction, frame id/debug flags.
- LayoutOutput — рассчитанные bounds, content size, overflow size, per-node results.
- LayoutResult — результат одной ноды: x, y, width, height, contentWidth, contentHeight, clipped/overflow flags.
- LayoutMeasureFunc — callback для leaf widgets, которым нужен custom measure: text, entity preview, image, block preview.
- LayoutStyleMapper — mapping из текущего LayoutStyle / LayoutConstraints в V3 style.
- LayoutNodeId — стабильный id для связи результата с Widget.

### dev.sixik.unigui.impl.layout.v3

- LayoutTreeBuilder — строит layout tree из widget tree.
- LayoutApplier — применяет LayoutOutput обратно к widget bounds.
- TaffyLayoutEngine — первый backend с Taffy-like flex semantics.
- YogaLayoutEngine — optional backend/adapter, если позже появится зависимость на Yoga.
- LayoutCache — кэш measure/layout результатов по style version, constraints и content version.
- OverlayLayoutResolver — выносит popup/overlay nodes в root overlay layer.
- LayoutDebugDumper — печатает дерево layout для debug overlay и тестов.

### Compatibility layer

- LayoutConstraints остаётся immutable старым контрактом.
- LayoutStyle.applyLegacyConstraints(...) остаётся мостом из старого API.
- WidgetBase.layout(style -> ...) становится preferred API.
- Старые helper-методы (preferredSize, grow, margin, align) должны писать в LayoutStyle, а не создавать параллельную реальность.

## Mapping текущей модели в V3

| Current API | V3 semantic | Notes |
| --- | --- | --- |
| LayoutStyle.width/height | size | px, percent, auto |
| minWidth/minHeight | min size | percent считается от parent content size |
| maxWidth/maxHeight | max size | auto = undefined / infinity по backend semantics |
| margin | outer spacing | участвует в flex line calculation |
| padding | inner content inset | влияет на children content box |
| overflowX/overflowY | overflow policy | layout не равен render clipping; clipping применяет render layer |
| flexDirection | row/column | основа LinearBox, HBox, VBox |
| flexWrap | nowrap/wrap | основа WrapPanel |
| rowGap/columnGap | gap | единое поведение между flex/wrap containers |
| flexGrow/flexShrink/flexBasis | flex item sizing | заменить локальные grow расчёты контейнеров |
| alignItems | cross-axis parent alignment | container style |
| alignSelf | cross-axis child override | child style |
| justifyContent | main-axis distribution | start/center/end/space variants |
| position | relative/absolute | absolute child не участвует в normal flow |
| left/top/right/bottom | inset | используется только для absolute/fixed overlay |
| Visibility.COLLAPSED | display none | node не участвует в measure/arrange |

## Overlay и clipping

Overlay-поведение нужно заложить в V3 сразу, потому что ComboBox, DropDownBox, Popup, tooltip, context menu и floating panels не должны ломать layout.

Правила:

- Floating widget по умолчанию закрыт и не участвует в normal layout.
- При открытии floating widget создаёт overlay node в root OverlayLayer или ближайшем overlay host.
- Overlay node позиционируется относительно anchor widget через screen/root coordinates.
- Overlay node всегда рисуется выше normal content своего host.
- Overlay node не должен обрезаться scissor-ом маленькой панели, внутри которой находится anchor.
- Для root-window clipping можно оставить отдельную политику: CLIP_TO_ROOT, ALLOW_OUTSIDE_PARENT, ALLOW_OUTSIDE_SCREEN.
- Hit-testing overlay должен идти раньше normal content, но учитывать close-on-outside-click.

Это устраняет класс багов, где открытый ComboBox/DropDownBox по умолчанию двигает layout или режется родительским ScrollView/Panel.

## Контракт measure/arrange

V3 должен разделить три вещи:

1. measure leaf widget — сколько контент хочет места при заданных constraints.
2. compute layout — где и какого размера будут ноды.
3. apply layout — записать bounds в реальные widgets.

Leaf widgets не должны сами решать, где они стоят. Они только отвечают на вопрос размера.

Container widgets не должны вручную раскладывать children, если их поведение выражается style-ом.

Исключения допустимы только для виджетов со специальной внутренней механикой:

- ScrollView — viewport + scrollbars + content extent.
- SplitPanel — пользовательские splitter positions.
- GridBox — если будет полноценный grid, отличный от flex-wrap.
- DockPanel — если остаётся dock-specific behavior.

## Миграция контейнеров

### Phase 0 — зафиксировать текущее поведение

- Добавить layout snapshot-тесты для текущих LinearBox, WrapPanel, StackPanel, ScrollView, OverlayLayer, Popup.
- Зафиксировать edge cases: percent size, min/max, grow, shrink, padding, margin, gap, collapsed children, absolute child.
- Добавить визуальный smoke-screen в TestCommands.java: nested panels, scroll area, dropdown in clipped parent, wrap grid, split panel.

### Phase 1 — добавить V3 модель без включения по умолчанию

- Добавить api.layout.v3 classes.
- Добавить impl.layout.v3 backend skeleton.
- Сделать LayoutStyleMapper из текущего LayoutStyle.
- Добавить feature flag: unigui.layout.v3.enabled=false.
- Добавить debug dump старого и нового layout результата для сравнения.

### Phase 2 — vertical slice: LinearBox/HBox/VBox

- Перевести LinearBox на V3 через flexDirection + gap.
- HBox и VBox оставить thin wrappers.
- Сравнить snapshot-тесты V2/V3.
- После совпадения включить V3 для этих контейнеров через opt-in flag.

### Phase 3 — WrapPanel и StackPanel

- WrapPanel перевести на flexWrap=WRAP.
- StackPanel перевести на relative flow + absolute children.
- Убрать дублирование basic flex line calculation из контейнеров.

### Phase 4 — Popup, OverlayLayer, DropDownBox, ComboBox

- Ввести overlay portal/host resolver.
- Проверить, что opened popup не меняет desired size родителя.
- Проверить, что popup не clipped by parent scissor, если включена соответствующая policy.
- Проверить close-on-outside-click и hit-test priority.

### Phase 5 — ScrollView

- Разделить viewport layout и content extent.
- Scrollbars должны быть internal overlay/decoration nodes либо отдельными managed children с reservation policy.
- Проверить auto/scroll/hidden/visible overflow.
- Проверить вложенный dropdown внутри scroll content.

### Phase 6 — DockPanel, GridBox, SplitPanel

- SplitPanel: panes как flex items, splitter как interactive absolute/overlay handle.
- DockPanel: либо custom V3 layout algorithm, либо composition через flex/stack.
- GridBox: решить, нужен ли CSS-grid-like model или достаточно flex-wrap/table-like layout.

### Phase 7 — cleanup

- Удалить или сузить FlexLayoutEngine и AbsoluteLayoutEngine, если они полностью заменены V3 backend.
- Оставить compatibility shims только для старого API.
- Обновить UI_FRAMEWORK_ARCHITECTURE.md после стабилизации V3.

## Layout cache и invalidation

Для производительности V3 должен кэшировать layout там, где это безопасно.

Invalidation keys:

- style version виджета;
- visibility version;
- content measure version для text/image/entity/block preview;
- children list version;
- root constraints: available width/height;
- UI scale / font scale;
- scrollbars reservation state для ScrollView.

Важно: cache не должен скрывать frame-to-frame изменения text size, animated content или entity preview. Для таких leaf widgets нужен явный content version или measureAlways flag.

## Тестирование

### Unit/snapshot tests

- VBox с фиксированными детьми.
- HBox с grow/shrink.
- WrapPanel с переносами и gap.
- Percent child внутри fixed parent.
- Min/max clamp.
- Padding + margin nesting.
- Collapsed child.
- Absolute child внутри relative parent.
- Popup overlay outside clipped parent.
- ScrollView content larger than viewport.
- SplitPanel drag changes pane sizes without breaking total size.

Snapshot формат лучше делать текстовым и стабильным:

    root 0,0 300x200
      vbox 8,8 284x184
        label 8,8 120x16
        button 8,28 80x24

### Visual smoke examples

В common/src/main/java/dev/sixik/unigui/tests/TestCommands.java стоит держать отдельный раздел Layout V3 Smoke:

- flex row/column;
- wrap cards;
- scroll view with dropdown;
- popup near screen edge;
- split panel;
- nested panels with percent/min/max;
- overlay layer with multiple floating widgets.

## Acceptance criteria

- Одинаковые layout semantics для всех базовых containers.
- Открытие Popup/ComboBox/DropDownBox не меняет размер и позицию normal layout.
- Overlay может рисоваться поверх нижних элементов и не режется parent scissor, если policy это разрешает.
- Percent/min/max/grow/shrink работают одинаково в HBox, VBox, WrapPanel и nested panels.
- LayoutConstraints продолжает работать для старого кода.
- Новые виджеты можно собирать через style/composition без написания нового layout engine.
- Есть snapshot-тесты на ключевые edge cases.
- Есть визуальные examples для быстрой ручной проверки.

## Риски

- Taffy/Yoga semantics могут отличаться от текущего handmade поведения; часть старых examples визуально изменится.
- Popup/overlay portal затрагивает не только layout, но и render order, hit-testing, focus и close-on-outside-click.
- ScrollView сложнее обычного контейнера: content extent, viewport, scrollbar reservation и clipping должны быть явно описаны.
- Если сразу мигрировать все контейнеры, будет сложно понять источник регрессий.
- Если оставить V2 и V3 жить параллельно слишком долго, появится новый слой несовместимости.

## Открытые решения

- Делать ли первый backend полностью internal Java или сразу подключать external Yoga/Taffy binding.
- Нужен ли отдельный display property (FLEX, NONE) или достаточно Visibility.COLLAPSED.
- Какую overlay clipping policy сделать default: root-only clipping или allow outside screen.
- Нужен ли полноценный grid model для GridBox или можно временно оставить custom layout.
- Должны ли scrollbars занимать layout space или быть overlay decoration по умолчанию.

## Рекомендованный первый PR

Первый PR должен быть маленьким и проверяемым:

1. Добавить V3 package skeleton: LayoutEngine, LayoutNode, LayoutInput, LayoutOutput, LayoutMeasureFunc.
2. Добавить LayoutStyleMapper без изменения поведения существующих widgets.
3. Добавить snapshot test harness для synthetic widget tree.
4. Реализовать V3 flex column/row только для простых fixed-size children.
5. Добавить debug dump V2 vs V3 для сравнения.

После этого уже можно мигрировать LinearBox как первый настоящий контейнер.
