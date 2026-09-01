# Widgets Contract

Этот документ фиксирует обязательный контракт для виджетов UniGUI. Его цель - держать API виджетов единообразным, расширяемым и совместимым с общей event, style, layout и render-архитектурой.

## 1. Коллекции: в приоритете fastutil

Во внутренней реализации виджетов и связанных с ними подсистем в первую очередь используются коллекции из `fastutil`.

### Обязательно

- Перед использованием `ArrayList`, `HashMap`, `LinkedHashMap`, `HashSet` и других Java Collections проверить, есть ли подходящая коллекция в `fastutil`.
- Для примитивных ключей и значений использовать специализированные коллекции без boxing:
  - `Int2ObjectOpenHashMap` вместо `Map<Integer, T>`;
  - `Object2IntOpenHashMap` вместо `Map<T, Integer>`;
  - `IntArrayList` вместо `List<Integer>`;
  - соответствующие `Long*`, `Float*`, `Double*` варианты.
- Для object-коллекций использовать подходящие реализации `fastutil`, например `ObjectArrayList`, `ObjectOpenHashSet`, `Object2ObjectOpenHashMap` или reference-based коллекции, если сравнение должно идти по `==`.
- Если `fastutil` не предоставляет нужную семантику, использовать подходящее решение из Java Collections или другой уже подключенной библиотеки.
- Выбор fallback-коллекции должен определяться требуемой семантикой: порядок, identity-сравнение, weak references, concurrency, immutability и т.п.

### Публичный API

Внутреннее хранение в `fastutil` не обязывает раскрывать конкретную реализацию наружу. Публичный API по возможности возвращает стандартные интерфейсы `List`, `Set`, `Map`, `Collection` или read-only view, если конкретный fastutil-тип не является осознанной частью контракта.

### Запрещено

- Не использовать boxed Java-коллекцию, если существует подходящий primitive-specialized тип `fastutil`.
- Не создавать Java-коллекцию только ради временного преобразования данных между двумя внутренними этапами.
- Не заменять подходящую коллекцию `fastutil` на Java-аналог без причины, особенно в render, layout, input и tick hot paths.

## 2. Действия виджетов идут через events

Любое пользовательское действие виджета должно публиковаться через систему событий UniGUI.

К действиям относятся click, submit, selection change, value change, drag, connection creation, закрытие окна, подтверждение, отмена и другие результаты пользовательского взаимодействия.

### Обязательно

- Виджет предоставляет методы подписки вида `onXxx(...)`, возвращающие `EventSubscription`.
- Метод подписки принимает `EventListener<? super XxxEvent>`.
- Для кликовых, выборочных и других действий, относящихся к конкретному widget target, используется routed event.
- Dispatch идёт по той же схеме, что у `Button.click()`:
  - если есть `UIContext` - через `context.routedEvents().dispatch(event)`;
  - если контекста нет - через `emit(event)`.
- Программный вызов действия, например `click()`, `submit()` или `select(...)`, публикует то же событие, что и действие пользователя.

### Запрещено

- Не хранить пользовательские действия как прямые `Runnable`, `Consumer`, `BiConsumer` и подобные callback-поля в публичном widget API, если это UI action.
- Не делать API вида `onClick(Runnable)`, `onBarClick(Consumer<Bar>)`, `onNodeClick(Consumer<Node>)`.
- Не вызывать пользовательский код напрямую из `handle(...)`, минуя event system.
- Не создавать отдельный callback-путь для программного действия и отдельный event-путь для input-действия.

### Пример

Правильно:

```java
public EventSubscription onClick(EventListener<? super ButtonClickEvent> listener) {
    return on(ButtonClickEvent.TYPE, listener);
}

public ButtonClickEvent click() {
    ButtonClickEvent event = new ButtonClickEvent(this);
    UIContext context = uiContext();
    if (context == null) {
        emit(event);
    } else {
        context.routedEvents().dispatch(event);
    }
    return event;
}
```

Неправильно:

```java
private Consumer<Item> clickHandler;

public Widget onItemClick(Consumer<Item> handler) {
    this.clickHandler = handler;
    return this;
}
```

## 3. Новые события должны быть типизированы

Для каждого нового действия создаётся отдельный event class в `common/src/main/java/dev/sixik/unigui/api/event/`.

### Обязательно

- Событие расширяет `BaseEvent`.
- Событие объявляет `public static final EventType<XxxEvent> TYPE`.
- `EventType` и listener используют конкретный тип события, без raw types.
- Данные события имеют конкретные типы предметной области: widget, item, node, value, index, id и т.п.
- Для routed-события класс реализует `RoutableWidgetEvent` и хранит `target`, `currentTarget` и `EventPhase`.
- `routeTo(...)` создаёт routed-копию события и переносит `cancelled` state.
- Если действие можно отменить, вызывающая сторона проверяет `event.isCancelled()` до применения отменяемого результата.

### Запрещено

- Не использовать raw `EventType`, `EventListener` или `Event` там, где известен конкретный тип.
- Не передавать payload через `Object`, `Map<String, Object>` или строковые ключи, если можно объявить typed fields/accessors.
- Не переиспользовать несвязанный generic event только ради того, чтобы не создавать предметный event class.

## 4. Визуальные виджеты интегрируются со Style и Theme

Штатная визуальная кастомизация виджета должна идти через систему `Style`. `Theme` является внешним источником стилей для типов и состояний виджетов, а не отдельным API, который каждый виджет реализует самостоятельно.

### Визуальный виджет

Визуальным считается виджет, который сам рисует UI-состояние: кнопку, поле, график, preview, chart, node, bar, point, input, indicator и т.п.

Layout/container-only виджет, который только размещает детей, не обязан объявлять собственные визуальные style keys. Если контейнер рисует фон, border, selection или другое состояние, соответствующая часть также подчиняется Style/Theme.

### Обязательно для визуальных виджетов

- Виджет имеет стабильный `styleType()`; по умолчанию достаточно имени класса из `WidgetBase`.
- Визуальные параметры разрешаются через существующие `StyleKeys` или через новые типизированные `StyleKey<T>`.
- Новые общие параметры добавляются в `StyleKeys`; узкоспециализированные параметры могут оставаться API виджета, если они не являются частью общей темы.
- Значения темы применяются через `styleValue(...)` и стандартный `applyTheme()`/style-resolution путь `WidgetBase`.
- Интерактивные состояния представлены через `WidgetState`: `NORMAL`, `HOVERED`, `PRESSED`, `CHECKED`, `SELECTED`, `FOCUSED`, `DISABLED` и другие применимые состояния.
- Изменение визуального параметра вызывает `invalidate(InvalidationFlags.VISUAL)`; параметр, влияющий на размеры, также вызывает `LAYOUT`.
- Если рендер нетривиален, данные рендера выносятся в typed immutable state/snapshot.
- Default renderer или `RenderPlan` получает `DrawScope` и state/snapshot, а не читает закрытое mutable-состояние виджета напрямую.

### Style или Theme

- `Style` описывает визуальные свойства конкретного типа, id, class и состояния виджета.
- `Theme` объединяет и предоставляет стили всему UI-дереву.
- Сам виджет реализует поддержку `Style`; поддержка `Theme` получается через стандартный `UIContext` и `WidgetBase`.
- Не нужно добавлять в каждый виджет отдельные методы вида `theme(...)` или вручную читать глобальную тему.

### Renderer customization

Прямой API `renderer(...)` не является обязательным для каждого нового виджета. Основной путь кастомизации - Style/Theme и declarative `RenderPlan`.

Если низкоуровневая замена renderer действительно нужна:

- renderer регистрируется в `WidgetRendererRegistry`;
- renderer выбирается через `StyleKeys.RENDERER`, renderer id в `StylePack` или существующий typed override;
- прямой instance renderer допустим как дополнительный override для сложных или editor-oriented виджетов, но не заменяет поддержку Style/Theme;
- порядок разрешения должен соответствовать существующему пути виджетов: instance override, style renderer, style render plan, default renderer.

### Пример интеграции

```java
@Override
protected void applyTheme() {
    super.applyTheme();
    textColor.set(styleValue(StyleKeys.TEXT_COLOR, textColor));
}

@Override
protected WidgetState styleState() {
    if (!enabled()) return WidgetState.DISABLED;
    if (pressed) return WidgetState.PRESSED;
    return hovered() ? WidgetState.HOVERED : WidgetState.NORMAL;
}

@Override
protected void renderContent(RenderContext context) {
    MyWidgetState state = snapshot(context);
    DrawScope draw = new DrawScope(context, transform(), layoutBounds());
    if (renderStylePlan(context, MyWidgetState.class, state)) return;
    WidgetsRender.myWidget().render(draw, state);
}
```

## 5. Шаблонное поведение переиспользует существующие виджеты

Если новый виджет требует уже реализованное шаблонное поведение, он должен переиспользовать существующий виджет или механизм, а не дублировать его внутреннюю логику.

### Примеры

- Прокручиваемое содержимое строится на `ScrollView`, а большие повторяющиеся наборы данных - на `VirtualListView`/`VirtualTableView`.
- Выпадающий или временный слой использует существующий popup/window/modal механизм.
- Редактируемая строка использует `TextInput`, а многострочное редактирование - `TextArea`.
- Готовое поведение кнопки, выбора, focus, pointer capture, clipping или viewport переиспользуется через существующий виджет/API.

### Правила

- В приоритете composition: сложный виджет содержит `ScrollView`, `TextInput`, `Button` и другие готовые части.
- Наследование используется, если новый виджет действительно является специализацией существующего виджета и соблюдает его контракт.
- Перед реализацией wheel scrolling, scrollbar dragging, focus handling, selection, popup lifetime или text editing необходимо проверить существующие widgets и API.
- Новый общий механизм сначала выносится в переиспользуемый базовый виджет/API, после чего используется предметным виджетом.
- События дочернего виджета можно преобразовывать в typed события составного виджета, но нельзя обходить event system прямым callback-вызовом.

### Запрещено

- Не копировать реализацию scrolling, scrollbar hit testing, clipping и pointer capture в каждый новый список или панель.
- Не создавать визуальную имитацию существующего control без его interaction/accessibility контракта.
- Не дублировать исправления одного поведения в нескольких несвязанных виджетах.

## 6. Состояние interaction доступно style и renderer

Style resolver и renderer должны получать достаточно данных для отображения hover, pressed, selected, focused, checked и disabled состояний.

Минимальный набор для интерактивных элементов:

- bounds;
- enabled;
- hovered;
- pressed, dragging, focused, selected или checked, если применимо;
- value, index или id, если элемент представляет данные;
- style-derived colors и параметры;
- text, `RichText` или label, если есть.

Состояние не должно определяться renderer-ом через повторный hit test или чтение глобального input state. Виджет вычисляет interaction state, Style/Theme выбирает оформление, renderer только рисует snapshot.

## 7. Fine-grained render hooks являются дополнительным API

Для сложных виджетов допустимы дополнительные render hooks для отдельных частей:

- bar renderer у `Chart`;
- point renderer у `Sparkline`;
- node renderer у `GraphView`;
- tooltip renderer;
- label renderer.

Такие hooks не заменяют Style/Theme contract всего виджета. Цвета, размеры, состояния и выбор стандартного renderer/render plan по-прежнему должны разрешаться через общий style-путь.

## 8. Definition of Done для нового виджета

Перед тем как считать виджет готовым:

- для внутренних коллекций сначала рассмотрены подходящие типы `fastutil`;
- действия пользователя опубликованы через typed events;
- нет публичных action callbacks вместо events;
- routed event корректно переносит target, currentTarget, phase и cancelled state;
- визуальный виджет интегрирован со Style/Theme;
- visual и layout invalidation выставляются согласно изменяемым параметрам;
- renderer/render plan получает typed snapshot/state;
- готовое шаблонное поведение переиспользовано, а не скопировано;
- hover, focus, pressed, checked и selected состояния доступны Style/Theme и renderer-у;
- `compileJava` проходит для поддерживаемых версий;
- для нового поведения добавлен self-test или demo-сценарий, проверяющий events, style и interaction contract.
