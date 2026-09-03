# UniGUI Widget Rewrite Specification

Статус: draft

Этот документ описывает переписывание widget-подсистемы UniGUI. Он нужен для исправления
архитектуры, а не для косметического изменения отдельных классов. Цель - получить небольшое,
предсказуемое и расширяемое ядро, на котором одинаково хорошо работают обычные контролы,
сложные редакторы, overlay-элементы и world UI.

## 1. Цели

После завершения работ виджет должен:

- иметь одну понятную ответственность;
- проходить единый lifecycle attach, measure, arrange, input, tick, render, dispose;
- использовать типизированные events для пользовательских действий;
- получать внешний вид из `Style`/`Theme`, а не из захардкоженных цветов в конструкторе;
- позволять заменить renderer без наследования от внутреннего класса;
- корректно работать в обычном дереве, popup/overlay и world UI;
- инвалидировать только необходимые подсистемы;
- не создавать снимки, списки и команды на каждый кадр без необходимости;
- иметь тестируемые state/model/behavior-части отдельно от Minecraft backend.

## 2. Не цели

В рамках этого rewrite не нужно:

- переписывать layout V3 с нуля;
- менять backend Minecraft, SDF, PostEffect и world-surface pipeline без доказанной необходимости;
- делать один универсальный класс вместо всех виджетов;
- превращать каждый helper, snapshot или data model в `Widget`;
- сохранять каждую историческую перегрузку навсегда;
- оптимизировать код до профилирования.

Layout V3, текущая система `DrawScope`/`DrawList`, typed event API, `StylePack` и текущая
поддержка версий Minecraft считаются внешними контрактами. Их можно адаптировать через facade,
но изменение контракта должно быть отдельным решением.

## 3. Аудит текущего состояния

### 3.1. Размер базового слоя

Текущая реализация уже содержит хорошие отдельные механизмы, но они собраны в слишком большой
иерархии:

- `Widget` содержит контракт дерева, layout, visibility, focus, style, transform, input и render;
- `WidgetBase` имеет около 1900 строк и одновременно отвечает за id/style, invalidation,
  animation, transform, opacity, render cache, event emitter и lifecycle;
- `PanelWidget` хранит детей, очередь мутаций, snapshot детей, layout и render обход;
- визуальные классы дополнительно собирают state и выбирают renderer из instance/style/default;
- `widgets/render` содержит около 126 state/renderer/render-plan классов;
- в каталоге `widgets` около 306 Java-файлов, включая реальные виджеты, model/helper типы,
  snapshot-объекты и backend-specific adapters.

Это не означает, что все 306 файлов нужно удалить. Сначала нужно разделить их по ответственности.

### 3.2. Смешанные обязанности `WidgetBase`

В `WidgetBase` сейчас сосуществуют:

1. Данные дерева: `parent`, `uiContext`, id, style scope и style classes.
2. Геометрия: `layoutBounds`, `desiredSize`, `LayoutStyle`, layout transitions.
3. Визуальное состояние: opacity, transform, hover, style resolution.
4. Поведение: animation controller, pointer/focus-related state.
5. Rendering: `render`, retained leaf cache, style version tracking и replay.
6. Event infrastructure: emitter и базовая обработка hover events.

Из-за этого простое изменение свойства часто знает слишком много о соседних подсистемах. Сложно
определить, что именно должно инвалидироваться, а наследнику приходится переопределять методы,
которые принадлежат разным слоям.

### 3.3. Дерево и дети

`PanelWidget` использует deferred mutations через `ConcurrentLinkedQueue`, а актуальный список
детей и отдельный `Widget[]` snapshot существуют одновременно. Это полезно для обхода, но текущий
контракт недостаточно явно различает:

- owner дерева;
- момент, когда child действительно attach-ится;
- момент применения mutation;
- detach без dispose;
- detach с dispose;
- перенос child в другой parent.

Из-за этого composite-виджеты начинают вручную симулировать второе дерево. Пример - `ScrollView`
содержит отдельный `content`, две scrollbar-ссылки и свой `children()` view. `OverlayLayer`,
`Popup`, `WindowManager`, `DropDownBox` и часть editor/docking классов также имеют собственные
правила владения дочерними элементами.

### 3.4. Render cache

`WidgetBase.renderCached()` кэширует только виджеты без детей. Для composite-виджета cache обходится,
а `render()` и render children вызываются в каждом кадре. `CachedSubtreeWidget` решает другую задачу:
рендерит поддерево в texture/FBO и не должен становиться заменой базовому cache для каждого контейнера.

Целевой вывод:

- render traversal каждого кадра допустим;
- построение статических draw-команд каждого leaf не должно быть обязательным;
- cache policy должна быть явной;
- composite cache допустим только с сохранением порядка, clip, transform, overlay и input semantics;
- любой cache должен иметь понятные причины miss и правила освобождения ресурсов.

### 3.5. Рендереры

Текущая схема `Widget -> State -> Renderer/RenderPlan -> WidgetsRender` уже является хорошим
направлением, но она неоднородна:

- часть классов имеет `State` и renderer;
- часть использует direct `render`/`renderContent`;
- часть имеет render plan, который собирается в primitives;
- некоторые состояния являются большими records и создаются прямо в render path;
- выбор renderer и применение theme смешаны с подготовкой состояния;
- для разных widgets повторяется код `instance renderer -> style renderer -> plan -> default renderer`.

Нужно сохранить идею typed renderer, но унифицировать lifecycle подготовки state и выбора style.

### 3.6. Input и события

Typed routed events и `EventSubscription` уже существуют и должны остаться основой. При этом
низкоуровневый input сейчас распределён между `WidgetBase`, отдельными контролами, focus manager,
hover manager, pointer capture и backend screen. При переписывании нужно отделить:

- hit test и построение route;
- pointer capture;
- focus traversal;
- перевод input в typed widget events;
- реакцию конкретного behavior на эти события.

`Optional` в холодных API допустим. Убирать его из всех контрактов ради идеи zero-allocation нельзя,
если это ухудшает читаемость. Для горячих внутренних циклов можно использовать nullable/internal
result, но это должно быть измерено.

## 4. Целевая архитектура

### 4.1. Разделение слоёв

Целевое устройство одного widget:

```text
Widget API
  -> WidgetNode / WidgetBase lifecycle
      -> LayoutNode
      -> InteractionState + Behaviors
      -> StyleResolver
      -> RenderNode
          -> RenderState
          -> Typed Renderer or RenderPlan
```

Это логическое разделение. Не обязательно делать семь публичных классов для каждого виджета.
Внутренние части могут быть compact final classes, если они не смешивают ответственность.

### 4.2. Тонкий `WidgetBase`

`WidgetBase` должен оставить только:

- ссылку на runtime context;
- parent/attachment lifecycle;
- id и базовые style classes;
- visibility/enabled/focusable flags;
- layout bounds и desired size;
- invalidation entry point;
- базовый event emitter;
- базовые transform/opacity properties;
- шаблонные методы lifecycle.

Из него нужно вынести или логически изолировать:

- animation controller и property adapters;
- render cache и cache statistics;
- style resolution и computed style;
- pointer capture helpers;
- popup anchoring;
- selection/value models;
- scrolling;
- text editing.

Наследник не должен быть обязан вызывать цепочку из нескольких `super`-методов, чтобы корректно
обновить unrelated подсистемы. Для этого вводится явный lifecycle:

```java
protected void onAttached(WidgetAttachment attachment);
protected void onDetached(WidgetAttachment attachment);
protected void onMeasure(LayoutContext context);
protected void onArrange(RectView bounds);
protected void onEvent(Event event);
protected void onTick(FrameContext frame);
protected void buildRender(RenderBuildContext context);
protected void onDispose();
```

Имена могут отличаться от примера, но порядок и семантика должны быть едиными. Публичные
`measure`, `arrange`, `handle`, `tick`, `render`, `dispose` остаются facade для совместимости.

### 4.3. Attachment и дерево

Ввести внутренний `WidgetAttachment` или эквивалентный объект состояния дерева:

- один owner parent;
- один UI context;
- attachment generation/version;
- состояние attached/detached/disposed;
- корректная передача context вниз по дереву;
- запрет циклов;
- понятная ошибка при попытке добавить child с другим owner.

Правила:

1. Добавление child не должно уничтожать уже существующих детей.
2. Операции `add`, `insert`, `remove`, `move` и `clear` должны иметь документированную семантику.
3. Deferred mutation разрешена только на границе traversal, а не как неявная случайность API.
4. `detach` не вызывает `dispose` автоматически без явно заявленного режима.
5. После detach child не должен продолжать получать input, tick или render через старого parent.
6. Один widget не может одновременно находиться в двух деревьях.
7. Публичные коллекции детей возвращают read-only view; mutation выполняется методами контейнера.

Для hot paths допускается immutable/reused array snapshot. Создание snapshot должно происходить
только при структурном изменении дерева.

### 4.4. Invalidation

Оставить bitmask-подход, но закрепить минимальный набор независимых флагов:

- `TREE` - изменился состав или порядок детей;
- `LAYOUT` - изменился desired size или placement;
- `VISUAL` - изменились цвета, state, text, renderer или draw parameters;
- `INPUT` - изменились hit-test bounds, enabled, visibility или input policy;
- `STYLE` - изменился computed style;
- `ANIMATION` - нужно продолжать tick;
- `RESOURCE` - нужно пересоздать backend resource.

Каждый setter обязан выбрать минимальный набор. Например:

- изменение текста обычно: `LAYOUT | VISUAL`;
- изменение цвета: `VISUAL`;
- изменение `enabled`: `VISUAL | INPUT`;
- изменение padding: `LAYOUT | VISUAL`;
- изменение child list: `TREE | LAYOUT | VISUAL | INPUT`;
- изменение texture handle: `VISUAL | RESOURCE`.

Инвалидация родителя должна быть агрегированной и не порождать цепочку новых объектов на каждый
вызов. После применения флага runtime обязан уметь его сбросить на конкретном уровне.

### 4.5. Layout

Layout должен оставаться чистым относительно rendering и input:

- `measure` только измеряет;
- `arrange` только устанавливает bounds и размещает детей;
- layout не создаёт draw commands;
- layout не открывает popup и не вызывает пользовательские callbacks;
- скрытый виджет не участвует в input/render;
- collapsed виджет сообщает нулевой desired size;
- absolute/overlay placement должен быть отдельным layout policy.

Общие шаблоны (`linear`, `stack`, `grid`, `wrap`, `dock`, `scroll`, `split`) должны жить в layout
policies или базовых контейнерах. Новый виджет не должен копировать обход детей ради того же поведения.

### 4.6. Input, focus и pointer capture

Input pipeline:

```text
backend input
  -> coordinate mapping
  -> hit test / capture target
  -> route capture
  -> target
  -> bubble
  -> typed semantic event
  -> behavior/model update
```

Обязательные правила:

- pointer capture хранится в `UIContext`, а не в каждом контроле отдельно;
- release/cancel всегда освобождает capture, включая уход указателя за viewport и disable/detach;
- hover меняется только через единый `HoverManager`;
- focus меняется через `FocusManager`;
- `Enter`/`Space`, стрелки, `Escape`, wheel и text input имеют единые typed events;
- disabled, invisible и collapsed widgets не принимают input;
- semantic events (`ButtonClickEvent`, `ValueChangedEvent`, `SelectionChangedEvent`, `PopupClosedEvent`)
  не должны быть прямыми `Runnable`/`Consumer` полями.

### 4.7. Behavior-компоненты

Повторяющееся поведение нужно оформить как переиспользуемые internal/public components:

- `PressBehavior` - pressed state, pointer capture, click eligibility;
- `ToggleBehavior` - checked state и typed change event;
- `SelectionBehavior` - selected index/key, keyboard navigation;
- `TextEditingBehavior` - caret, selection, clipboard, key commands;
- `ScrollBehavior` - offsets, wheel, scrollbar synchronization;
- `DragBehavior` - start/update/end/cancel;
- `PopupBehavior` - open state, anchor, placement, outside click, escape;
- `FocusBehavior` - focus request, focus trap, tab order;
- `ValidationBehavior` - valid/invalid/pending state;
- `VirtualizationBehavior` - visible range и reuse item views.

Компонент не должен владеть widget tree, если это не его прямая ответственность. Например,
`ScrollBehavior` не должен создавать собственный `ScrollView`; он обслуживает единый `ScrollView`
или layout policy.

### 4.8. State и model

Нужно различать три вида state:

1. **Model state** - публичные данные, например selected item, value, text, nodes.
2. **Interaction state** - hovered, pressed, focused, dragging, open, checked.
3. **Render state** - подготовленные к renderer значения, bounds, colors, segments и primitives.

Model state не должен зависеть от `DrawScope`, Minecraft и конкретного renderer. Render state может
быть mutable/reusable внутренним объектом, если это убирает аллокации. Immutable record допустим
для публичного snapshot API и cold path, но не должен автоматически создаваться на каждый кадр.

## 5. Целевой render pipeline

### 5.1. Единый renderer contract

Сохранить typed API:

```java
interface WidgetRenderer<S> {
    void render(DrawScope draw, S state);
}
```

Но добавить единый resolver/pipeline:

```text
widget state/model
  -> computed style
  -> reusable RenderState
  -> instance renderer
  -> style renderer
  -> render plan
  -> default renderer
```

Выбор renderer не должен быть реализован отдельным копипастом в каждом visual widget.

### 5.2. Render node и cache policy

Для каждого визуального виджета ввести внутренний `RenderNode` с политикой:

- `STATIC` - rebuild только после visual/style/layout invalidation;
- `STATEFUL` - rebuild после interaction/model changes;
- `CONTINUOUS` - rebuild каждый кадр, например animated canvas;
- `SUBTREE` - explicit subtree/FBO cache;
- `DISABLED` - direct render без retained cache.

Требования:

- cache не должен менять порядок primitives;
- clip и transform должны сохраняться при replay;
- cache не должен скрывать overlay children;
- backend/resource mismatch вызывает miss;
- style version входит в cache key только если renderer реально зависит от style;
- на dispose освобождаются команды, texture/FBO и backend resources;
- статистика cache собирается только при включённом debug/profiling режиме.

Составной контейнер не обязан кэшироваться автоматически. Сначала нужен общий ordered primitive
stream с корректными scopes, и только потом можно безопасно кэшировать composite subtree.

### 5.3. Команды и allocation budget

В render hot path запрещается:

- `new List`/`Map`/`Optional` для каждого виджета без измеренной причины;
- новый `RenderState` record на каждый кадр для статического виджета;
- повторное разрешение renderer/style без изменения версии;
- вызов `glGet*` для уже закэшированных данных;
- временные `RichText`, если можно переиспользовать подготовленное значение.

Использовать fastutil по правилам `docs/WIDGETS_CONTRACT.md`. Primitive fastutil collection имеет
приоритет; стандартная Java Collection допустима, если fastutil не даёт требуемой семантики
(identity, weak, concurrency, immutable view и т.п.).

## 6. Style и Theme

### 6.1. Ответственность

Widget хранит semantic state. `Style`/`Theme` определяют визуальные значения:

- colors;
- dimensions;
- border/radius;
- typography;
- spacing;
- interaction state mapping;
- default renderer/render plan;
- части chrome, например chevron, checkbox mark, scrollbar thumb.

В конструкторе widget нельзя задавать production palette как обязательное оформление. Допустимы
только безопасные fallback значения, необходимые до подключения context/theme.

### 6.2. Renderer customization

Приоритет остаётся единым:

1. renderer instance;
2. renderer из style/theme;
3. declarative render plan;
4. renderer из `WidgetsRender`.

Renderer может быть custom через typed interface. Дублирование визуальных состояний через
`RichText` допускается только для inline content, а не для самостоятельных элементов управления.

## 7. DropDownBox и общий popup pipeline

`DropDownBox` использовать как пилотный виджет переписывания.

### 7.1. Целевая роль

`DropDownBox` - generic host для одного popup content. `ComboBox` - selection control, который
использует тот же popup behavior, но владеет selection model. Они не должны копировать popup,
scroll и outside-click код.

### 7.2. Общие части

Создать `PopupController`/`PopupBehavior` с:

- anchor provider, а не поиском parent в произвольный момент;
- explicit overlay host и context-level fallback;
- open/close/toggle;
- outside click и `Escape`;
- placement `BELOW`, `ABOVE`, `START`, `END`, `AUTO`;
- auto-flip и clamp к viewport;
- вычислением bounds после measure anchor/content;
- z-order через `OverlayLayer`;
- анимацией open/close как опциональной policy;
- закрытием при detach/disable/смене screen.

### 7.3. Header

Header должен быть обычной структурой виджетов или единым специализированным header renderer:

- text/label слева;
- chevron справа в отдельной области;
- стабильная ширина и alignment;
- `closed` и `opened` state из style;
- поворот/смена chevron не через пробел и inline `RichText`.

Если для inline content есть отдельная осознанная задача, `RichText` может остаться extension point,
но он не должен быть способом позиционировать независимую стрелку по правому краю кнопки.

### 7.4. Content API

У generic dropdown должен быть явный API:

```text
content(Widget)
contentContainer()      // optional multi-child container
open()/close()/toggle()
opened()
popupController()
```

`addChild` не должен молча затирать предыдущий content. На переходный период старый метод может
добавлять элементы во внутренний `VBox` и помечается deprecated, либо при втором child выдаёт
понятную ошибку. Выбор фиксируется до реализации.

## 8. Карта пакетов и решение по ответственности

### `widgets/containers`

- `PanelWidget` - базовый owner детей и общий child traversal.
- `Box` - простой panel/chrome контейнер, без специальных popup обязанностей.
- `LinearBox`, `HBox`, `VBox` - только linear layout policy.
- `StackPanel`, `GridBox`, `WrapPanel`, `DockPanel` - отдельные layout policies.
- `ScrollView` - viewport + scroll behavior + scrollbar widgets; не второе скрытое дерево.
- `SplitPanel`/`Splitter` - split layout и drag behavior.
- `Border` - либо декоративный single-child wrapper, либо переименовать в `BorderBox`; решить
  до миграции, потому что сейчас его роль пересекается с `PanelWidget`/`Box`.
- `View`, `PanelRowWidget`, `SettingRow` - уточнить роли и убрать неявные alias-классы.

### `widgets/display`

- `TextWidget` - единая основа text layout, wrap, overflow и alignment.
- `Text`, `Label`, `TextBlock`, `RichTextView` - оставить только если различия видны в public API;
  иначе сделать aliases/facades с документацией.
- `TextureWidget`/`ImageView` - texture model + image renderer, без копирования layout.
- `Shape`, `Path`, `CanvasWidget` - display primitives с явной cache policy.
- `Chart`/`Sparkline` - data visualization; общий sampling/points helper, но разные semantic roles.
- `Separator` - простой display widget.

### `widgets/interaction`

- `Button` - базовый press/click behavior.
- `ToggleButton`, `Checkbox`, `RadioButton`, `ToggleSwitch`, `ToolButton`, `IconButton` -
  композиция press/toggle/selection behaviors и typed state.
- `RadioGroup` - selection model, не визуальный widget.
- `Slider` - value model + drag/key behavior.
- `TextInput`, `TextField`, `TextArea`, `PasswordField`, `SearchField`, `NumberField`,
  `TimeSpanField`, `DatePicker`, `ColorPicker` - общий text editing/value/validation слой;
  специфические форматы не должны копировать caret/selection/clipboard.
- `ComboBox`/`DropDownBox` - общий popup behavior.
- `ScrollBar` - value/drag renderer, используемый `ScrollView`.
- `TreeListPicker`/`SearchableGridPickerWidget` - composite selection controls; вынести models,
  filtering и virtualization из визуального класса.
- `AdminConsole`, `CodeEditor`, tokenizers и diagnostics - editor domain; базовый input/text
  behavior должен быть общим, domain parser остаётся отдельным.

### `widgets/feedback`

- `Popup`, `Tooltip`, `ContextMenu`, `WindowWidget` - использовать общий overlay/placement/lifecycle.
- `OverlayLayer` - единственный owner z-order и modal input policy.
- `WindowManager` - model/controller окон, не дублировать overlay traversal.
- `Toast`, `NotificationView`, `LoadingIndicator`, `Spinner`, `ProgressBar` - feedback/display;
  временные состояния и timers не должны быть зашиты в renderer.

### `widgets/navigation`

- `TabControl`, `PageView`, `Carousel` - page/selection model + transition policy; явно определить
  разницу между вкладками, страницами и слайдовой навигацией.
- `Menu`, `MenuBar`, `MenuItem`, `ToolBar` - command/selection model и overlay popup behavior.
- `TreeView`, `TreeList`, `TreeViewNode` - tree model, selection, expansion и virtualization;
  node data не смешивать с widget lifecycle.
- `Accordion`, `ExpandablePanel`, `Breadcrumb` - композиционные controls, использующие общие
  toggle/selection/animation behaviors.

### `widgets/data`

`VirtualListView` и `VirtualTableView` должны быть построены на общей virtualization policy:
visible range, item reuse, selection, keyboard navigation и scroll integration. Column и row
state остаются data/render snapshots, а не самостоятельными widgets без необходимости.

### `widgets/graph`

Разделить:

- graph model: nodes, ports, connections, snapshots, validation;
- viewport/pan/zoom/selection behavior;
- widget renderer и editor input.

`NodeGraphItem`, `NodeGraphPort` и `NodeGraphConnection` не должны одновременно быть model,
visual child и persistence record.

### `widgets/editor` и `widgets/docking`

Оставить как domain composites, но перевести их на базовые `Panel`, `Scroll`, `Popup`, `Selection`,
`Drag` и `Focus` behaviors. `DockNode`, snapshots, codec и drag intent должны оставаться моделями
и контроллерами. `DockingRoot` отвечает за composition, а не за сериализацию.

### `widgets/map`, `widgets/world`, `widgets/minecraft`, `widgets/effects`

Это adapters над общим widget ядром:

- `MapCanvas` и `WorldCanvas` используют canvas/viewport/pointer policies;
- `AnchorLayer` и `AnchorWidget` используют единый coordinate mapping;
- Minecraft preview/tooltip classes не должны протекать в common interaction contracts;
- `PostProcessingLayer` остаётся render boundary и не должен менять input/layout semantics.

### `widgets/caching`

`CachedSubtreeWidget` оставить как explicit FBO/texture cache. Его policy не должна автоматически
включаться для любого `PanelWidget`; пользователь должен понимать стоимость и invalidation rules.

### `widgets/render`

Сохранить typed renderer API, но сгруппировать типы по единому шаблону:

```text
<Widget>State
<Widget>Renderer
<Widget>Renderers
<Widget>RenderPlan       // только если нужен declarative plan
```

Удалять `RenderPlan` там, где он просто повторяет один direct renderer. Большие state snapshots
перевести на reusable internal state, если профилирование подтвердит allocation pressure.

## 9. Публичный API и совместимость

### 9.1. Правила API

- Fluent setter возвращает concrete widget type.
- Семантические действия имеют typed event и `EventSubscription`.
- `Runnable` допустим только для internal lifecycle hooks, не для пользовательского действия.
- Публичные коллекции не раскрывают внутренний mutable storage.
- `null` нормализуется единообразно или отклоняется через `Objects.requireNonNull`; смешанная
  семантика запрещена.
- Названия `open`, `opened`, `visible`, `enabled`, `selected`, `checked` должны быть одинаковыми
  по смыслу во всех widgets.
- Для сложного состояния нужен snapshot/restore или явная model API.

### 9.2. Миграция без big bang

1. Новый internal API вводится рядом со старым.
2. Старые методы делегируют в новую реализацию.
3. Старые ambiguous методы помечаются `@Deprecated` с альтернативой.
4. Два цикла версий поддерживается facade, если это не требует дублирования логики.
5. После перевода всех внутренних callers facade можно удалить в major release.

Нельзя одновременно менять WidgetBase, event routing, layout и Minecraft backend без промежуточных
компилируемых этапов.

## 10. План реализации

### Phase 0 - baseline

- зафиксировать текущие compile/test результаты для 1.20.1 и 1.21.1;
- запустить unit tests common;
- добавить smoke demo всех базовых групп;
- снять allocation/draw/cache baseline для `ComplexUiDemo`, `SolarNavigationScreen`, map и editor;
- не менять behavior.

### Phase 1 - tree and lifecycle

- ввести attachment state;
- убрать неявное владение child из специальных widgets;
- формализовать mutation boundary;
- исправить detach/dispose/pointer capture;
- добавить tree/lifecycle tests.

### Phase 2 - invalidation and WidgetBase split

- выделить `WidgetRuntimeState`, `LayoutState`, `InteractionState` или эквивалент;
- унифицировать invalidation propagation;
- вынести animation adapter;
- сохранить старый `WidgetBase` facade до миграции всех наследников.

### Phase 3 - input behaviors

- общий pointer press/release/capture;
- общий focus/keyboard navigation;
- selection/toggle/drag/scroll behaviors;
- regression tests для ухода мыши, detach, disable и nested overlay.

### Phase 4 - style/render pipeline

- общий `ComputedStyle`/resolver;
- единый renderer selection;
- reusable render state;
- explicit cache policy;
- тесты порядка primitives, clip, transform, opacity и style invalidation.

### Phase 5 - overlay and popup

- `PopupBehavior`/controller;
- `OverlayLayer` z-order, outside click, auto placement и focus trap;
- миграция `Popup`, `Tooltip`, `ContextMenu`, `DropDownBox`, `ComboBox`, `WindowWidget`.

### Phase 6 - base controls

- `Button`, toggles, checkbox, radio, slider;
- общий text editor для всех field controls;
- migration of `DropDownBox` as pilot composite;
- keyboard and accessibility behavior.

### Phase 7 - complex composites

- ScrollView/virtualization;
- navigation;
- graph/editor/docking;
- map/world/Minecraft adapters.

### Phase 8 - remove legacy paths

- удалить duplicate popup/scroll/input logic;
- удалить dead render plans and alias code;
- снять deprecated facade только после migration matrix;
- обновить docs and examples.

## 11. Тестовая стратегия

### Unit tests common

Обязательно покрыть:

- parent ownership, mutation order, detach/dispose;
- invalidation flags and propagation;
- measure/arrange invariants;
- hit test with transforms and clipping;
- pointer capture release on outside release/detach/disable;
- focus traversal and keyboard activation;
- typed event dispatch and cancellation;
- popup placement, auto-flip, clamp and outside close;
- dropdown selection and content lifecycle;
- scroll bounds, nested scrolling and scrollbar sync;
- cache hit/miss reasons and style version changes.

### Render tests

На fake `RenderContext` проверять:

- primitive order;
- no draw for invisible/collapsed;
- opacity/transform inheritance;
- clip scopes are balanced;
- static widget does not rebuild state/cache without invalidation;
- custom style renderer wins over default renderer;
- renderer failure does not leave context in broken state.

### Visual smoke tests

В `TestMod` сделать отдельный screen с:

- Button/toggles/radio/slider;
- TextInput/TextArea/PasswordField;
- DropDownBox/ComboBox вверху, центре и у нижнего края;
- nested Popup/Tooltip/ContextMenu;
- ScrollView с большим содержимым;
- tab/page/tree/table/graph;
- post effect и world/map canvas.

Проверять viewport, GuiScale 1/2/4, resize, mouse release outside, Escape и смену screen.

### Performance tests

Профилировать отдельно:

- allocations/frame;
- CPU time layout/input/render;
- draw command count;
- backend draw calls;
- cache hit rate;
- количество renderer/style resolve;
- FBO/texture memory для explicit subtree cache.

Оптимизация считается принятой только при сохранении визуального и input behavior.

## 12. Definition of Done

Переписывание считается завершённым, когда:

- `WidgetBase` больше не является контейнером всех unrelated responsibilities;
- каждый public widget имеет документированную роль и единственного владельца состояния;
- `DropDownBox` и `ComboBox` используют общий popup pipeline;
- `ScrollView`, `OverlayLayer`, `Popup` и window widgets не создают параллельные скрытые деревья;
- все пользовательские действия идут через typed events;
- визуальные значения идут через Style/Theme и typed renderer;
- composite render сохраняет порядок, clip и transform;
- cache policy явна и подтверждена тестами;
- common tests и compile для поддерживаемых Minecraft versions проходят;
- TestMod smoke screen проходит сценарии resize, GuiScale, input edge cases и nested overlays;
- allocation/draw baseline сравнен с результатом rewrite;
- deprecated API имеет migration note.

## 13. Приоритеты

### P0

- tree ownership и lifecycle;
- pointer capture/focus cleanup;
- separation of `WidgetBase` responsibilities;
- общий popup/overlay controller;
- безопасный content API `DropDownBox`.

### P1

- unified invalidation;
- common input behaviors;
- unified style/renderer resolver;
- text editing foundation;
- ScrollView and virtualization foundation.

### P2

- render node/cache policy;
- navigation, menus, trees;
- graph/editor/docking migration;
- allocation reduction in render state and snapshots.

### P3

- legacy facade removal;
- naming cleanup (`View`, `Border`, aliases);
- documentation and full demo coverage;
- optional public behavior APIs for advanced users.

## 14. Решения, которые нужно принять до Phase 1

1. Deferred child mutations остаются публично наблюдаемыми только после `apply`, или API получает
   immediate logical tree плюс deferred physical traversal.
2. `Border` остаётся wrapper или переименовывается в более точный `BorderBox`.
3. `View` является базовой абстракцией или текущий класс становится `TitledPanel`/`CardView`.
4. `DropDownBox.addChild` на переходном этапе агрегирует детей во внутренний контейнер или выдаёт
   ошибку при втором child.
5. Будет ли `RenderState` reusable mutable object внутреннего runtime, а immutable records останутся
   только для public snapshots.
6. Нужен ли публичный `Behavior` API модам, или behaviors остаются internal до стабилизации.

До ответов на эти вопросы нельзя начинать массовое переписывание всех widgets: иначе старые
неоднозначности будут перенесены в новый слой.
