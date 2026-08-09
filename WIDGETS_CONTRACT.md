# Widgets Contract

Этот документ фиксирует обязательный контракт для виджетов UniGUI. Его цель — держать API виджетов единообразным, расширяемым и совместимым с общей event/render архитектурой.

## 1. Действия виджетов идут через events

Любое пользовательское действие виджета должно публиковаться через систему событий UniGUI.

### Обязательно

- Виджет должен предоставлять методы подписки вида `onXxx(...)`, возвращающие `EventSubscription`.
- Метод подписки должен принимать `EventListener<? super XxxEvent>`.
- Событие должно иметь собственный `EventType`.
- Для кликовых/выборочных/интерактивных действий событие должно быть routed, если действие относится к конкретному widget target.
- Dispatch должен идти по той же схеме, что у `Button.click()`:
  - если есть `UIContext` — через `context.routedEvents().dispatch(event)`;
  - если контекста нет — через `emit(event)`.

### Запрещено

- Не хранить пользовательские действия как прямые `Runnable`, `Consumer`, `BiConsumer` и т.п. в публичном widget API, если это именно UI action.
- Не делать API вида `onClick(Runnable)`, `onBarClick(Consumer<Bar>)`, `onNodeClick(Consumer<Node>)`.
- Не вызывать пользовательский код напрямую из `handle(...)`, минуя event system.

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

## 2. Визуальные виджеты поддерживают custom renderer

Если виджет является визуальным компонентом, а не layout/container-only виджетом, он обязан иметь поддержку кастомного рендера.

### Визуальный виджет

Визуальным считается виджет, который сам рисует UI-состояние: кнопку, поле, график, preview, chart, node, bar, point, input, indicator и т.п.

### Layout widget

Layout/container-only виджет занимается компоновкой детей и обычно не обязан иметь renderer API, если сам не рисует содержимое кроме структурного контейнера.

Примеры layout/container-only:

- `HBox`
- `VBox`
- `WrapPanel`
- `GridBox`
- простые panel/container wrappers, если они только размещают детей

### Обязательно для визуальных виджетов

- Вынести данные рендера в immutable state/record, если виджет имеет нетривиальное состояние.
- Предоставить renderer interface или использовать существующий renderer type из `common/src/main/java/dev/sixik/unigui/widgets/render/`.
- Добавить API:
  - `renderer()`
  - `renderer(CustomRenderer renderer)`
  - `useDefaultRenderer()`
- Default rendering должен идти через renderer, а не быть намертво зашитым в widget class.
- Renderer должен получать `DrawScope` и state/snapshot, а не обращаться к widget internals напрямую.

### Рекомендуемый паттерн

```java
private WidgetRenderer renderer;

public WidgetRenderer renderer() {
    return renderer;
}

public MyWidget renderer(WidgetRenderer renderer) {
    if (this.renderer == renderer) return this;
    this.renderer = renderer;
    invalidate(InvalidationFlags.VISUAL);
    return this;
}

public MyWidget useDefaultRenderer() {
    return renderer(null);
}

@Override
protected void renderContent(RenderContext context) {
    effectiveRenderer().render(new DrawScope(context, transform()), snapshot(context));
    super.renderContent(context);
}

private WidgetRenderer effectiveRenderer() {
    return renderer == null ? WidgetsRender.myWidget() : renderer;
}
```

## 3. Fine-grained render hooks допускаются, но не заменяют основной renderer

Для сложных виджетов допустимы дополнительные render hooks для частей виджета:

- bar renderer у `Chart`
- point renderer у `Sparkline`
- node renderer у `GraphView`
- tooltip renderer
- label renderer

Но если виджет сам является визуальным компонентом, такие hooks не заменяют общий renderer contract. Они являются дополнительной точкой расширения.

## 4. Состояние interaction должно быть доступно renderer'у

Renderer должен получать достаточно данных для отрисовки hover/pressed/selected/focused/disabled состояний.

Минимальный набор для интерактивных элементов:

- bounds
- enabled
- hovered
- pressed/dragging/focused/selected, если применимо
- value/index/id, если элемент представляет данные
- colors/style-derived values
- text/richText/label, если есть

## 5. Новые события должны быть типизированы

Для новых действий создаётся отдельный event class в `common/src/main/java/dev/sixik/unigui/api/event/`.

Событие должно:

- расширять `BaseEvent`;
- иметь `public static final EventType<XxxEvent> TYPE`;
- хранить `target`;
- для routed-событий хранить `currentTarget` и `phase`;
- реализовывать `RoutableWidgetEvent`, если событие должно всплывать/маршрутизироваться;
- копировать `cancelled` state в `routeTo(...)`.

## 6. Definition of Done для нового визуального виджета

Перед тем как считать визуальный виджет готовым:

- действия пользователя опубликованы через events;
- нет публичных action callbacks вместо events;
- есть renderer API или обоснование, почему виджет layout-only;
- renderer получает snapshot/state;
- есть default renderer;
- hover/focus/pressed/selected состояния не зашиты непрозрачно;
- `compileJava` проходит;
- если виджет добавлен в demo/test commands, он показывает event/render поведение.

