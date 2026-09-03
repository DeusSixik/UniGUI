# UniGUI Widget Renderer Architecture Specification

Статус: draft

Этот документ описывает целевую архитектуру renderer-подсистемы UniGUI. Он дополняет
`docs/UNIGUI_WIDGET_REWRITE_SPEC.md` и не заменяет общий контракт виджетов из
`docs/WIDGETS_CONTRACT.md`.

Цель документа - убрать смешение семантики виджета, состояния, style/theme и способа
отрисовки. В частности, `Checkbox` не должен быть представлен как обычная `Button`
только потому, что оба виджета реагируют на нажатие мыши.

## 1. Краткое решение

Для каждого визуального виджета должна существовать одна основная семантическая роль,
один typed render state и один основной typed renderer:

```text
Widget role
    -> Widget render state
        -> Widget renderer
            -> reusable visual parts
                -> DrawScope / primitive stream / backend
```

Допустимо иметь несколько реализаций одного renderer-контракта. Например, тема может
предоставить `SlateCheckBoxRenderer`, а пользователь - `CompactCheckBoxRenderer`.
Недопустимо описывать один виджет одновременно как `ButtonRenderer`, `BoxRenderer`,
произвольный `RenderPlan` и direct `renderContent` без явного владельца итогового
рендеринга.

Главное правило:

```text
Несколько реализаций одного typed renderer - нормально.
Несколько несвязанных renderer-контрактов у одной семантической роли - архитектурная ошибка.
```

## 2. Почему текущая схема является проблемой

Текущая система уже содержит полезные части: typed state, renderer, render plan,
`WidgetsRender`, `StylePack` и выбор renderer через instance/style/default. Проблема в
том, что эти части применяются непоследовательно.

### 2.1. Пример `Checkbox`

Сейчас иерархия выглядит примерно так:

```text
Checkbox
  -> ToggleButton
      -> Button
          -> Box
              -> PanelWidget
                  -> WidgetBase
```

А визуальный контракт использует:

```text
ButtonRenderer
ButtonState
ButtonRenderType.CHECKBOX
```

Такой код может корректно работать, но он смешивает три разных понятия:

1. **Поведение** - клик, focus, toggle и смена состояния.
2. **Семантика** - checkbox с состояниями checked/unchecked/indeterminate.
3. **Внешний вид** - checkbox indicator, label, background и border.

Общее поведение не делает два виджета одним визуальным типом. Кнопка и checkbox могут
использовать одинаковый `PressBehavior`, но это не означает, что checkbox должен
рендериться через `ButtonRenderer`.

### 2.2. Разрастающийся `ButtonState`

В `ButtonState` уже появились параметры, относящиеся к checkbox:

```text
checked
indeterminate
indicatorSize
indicatorInnerSize
indicatorGap
indicatorColor
indicatorBorderColor
indicatorProgress
labelLeft
```

Если продолжать этот подход, в state попадут параметры radio button, toggle switch,
tool button и других контролов. Renderer будет обязан проверять `ButtonRenderType` и
игнорировать большую часть полей для конкретного случая.

Это приводит к следующим последствиям:

- сложнее понять обязательные данные renderer;
- custom renderer должен знать внутренние поля чужих контролов;
- style selector выбирает виджет одного типа, а state фактически относится к другому;
- добавление нового состояния повышает риск регрессии уже существующих контролов;
- renderer и render plan начинают дублировать друг друга;
- невозможно надёжно определить владельца порядка, clip и transform.

### 2.3. Почему `BoxRenderer` тоже не должен быть решением

`BoxRenderer` отвечает за структурный chrome: background, border, radius, shadow и
прочие свойства поверхности. Он не должен знать про checked, indeterminate, selection,
keyboard focus или toggle behavior.

Поэтому правильная схема для checkbox выглядит так:

```text
CheckBoxRenderer
  -> ControlChromePart
  -> CheckIndicatorPart
  -> LabelPart
```

`ControlChromePart` может быть общим, но `CheckBoxRenderer` остаётся владельцем
итогового результата и порядка отрисовки.

## 3. Цели

После внедрения архитектуры должно быть возможно:

- добавить новый визуальный вариант checkbox без изменения `ButtonState`;
- изменить тему checkbox через `Style`/`Theme`;
- заменить renderer checkbox через typed API;
- переиспользовать chrome, текст и focus ring без наследования от `ButtonRenderer`;
- использовать одинаковые behaviors для кнопки и checkbox;
- сохранить корректный порядок primitives, clip, transform, opacity и z-layer;
- кэшировать разрешённый renderer и state без поиска по типам на каждом кадре;
- мигрировать старый API поэтапно;
- диагностировать, какой renderer реально был выбран и по какой причине.

## 4. Не цели

В рамках этой задачи не требуется:

- сразу переписывать весь `WidgetBase`;
- менять backend Minecraft или GPU pipeline;
- создавать отдельный renderer-класс для каждого цвета, размера или состояния;
- делать renderer частью пользовательской бизнес-логики;
- удалять старые классы до появления compatibility adapter;
- оптимизировать allocations без профилирования.

## 5. Термины

### 5.1. Семантическая роль

Назначение виджета с точки зрения пользователя и accessibility:

- `BUTTON` - выполняет действие;
- `CHECKBOX` - включает, выключает или устанавливает indeterminate-состояние;
- `RADIO_BUTTON` - выбирает один вариант из группы;
- `TOGGLE_SWITCH` - переключает бинарное значение в switch-представлении;
- `SLIDER` - изменяет числовое значение по диапазону;
- `TEXT_INPUT` - редактирует текст;
- `PANEL` - группирует и размещает дочерние элементы.

Роль не определяется формой поверхности. Checkbox с прямоугольным background всё
равно остаётся checkbox.

### 5.2. Primary renderer

Единственный renderer-контракт, который виджет предоставляет как свой основной способ
отрисовки. Он отвечает за итоговый визуальный результат, включая вызов внутренних
parts.

### 5.3. Renderer part

Переиспользуемая внутренняя часть renderer. Part не выбирается как альтернативный
renderer всего виджета и не владеет lifecycle виджета.

Примеры:

- `ControlChromePart`;
- `LabelPart`;
- `CheckIndicatorPart`;
- `RadioIndicatorPart`;
- `FocusRingPart`;
- `ChevronPart`;
- `SliderTrackPart`;
- `SliderThumbPart`.

### 5.4. Behavior

Переиспользуемая логика input и состояния, не связанная с конкретным внешним видом:

- press;
- focus;
- hover;
- toggle;
- selection;
- drag;
- keyboard navigation;
- pointer capture.

## 6. Целевая модель renderer

### 6.1. Общий контракт

Точный package и имена могут быть адаптированы под текущий API, но типовая форма должна
быть такой:

```java
public interface WidgetRenderer<S> {
    void render(RenderContext context, S state);
}
```

Renderer не должен получать сам `Widget` и читать из него mutable-состояние. Он получает
подготовленный typed state и работает только с описанным в контракте набором данных.

Для parts используется отдельный внутренний контракт:

```java
interface RendererPart<S> {
    void render(RenderContext context, S state);
}
```

Part может быть обычным объектом или статическим helper, если это снижает overhead и не
ломает тестируемость. Он не должен самостоятельно искать parent, theme или UI context.

### 6.2. Состояние checkbox

Целевой state checkbox должен содержать только данные, необходимые его renderer:

```java
public final class CheckBoxRenderState {
    private RectView bounds;
    private RichText label;
    private CheckboxState state;
    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean enabled;
    private float checkProgress;
    private float boxSize;
    private float checkSize;
    private float textGap;
    private boolean labelLeft;
    private CheckBoxStyle style;
}
```

Состояние может быть immutable на холодном пути или переиспользуемым mutable snapshot
на горячем пути. Решение принимается по профилированию. В обоих случаях renderer не
должен зависеть от `ButtonState`.

### 6.3. Состояние обычной кнопки

```java
public final class ButtonRenderState {
    private RectView bounds;
    private RichText label;
    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean enabled;
    private ButtonStyle style;
}
```

В этот state не добавляются checkbox indicator или radio-specific поля.

### 6.4. Renderer-результат

Renderer должен строить primitives в переданном `RenderContext` и соблюдать:

- текущий transform;
- текущий clip;
- opacity и z-layer;
- порядок отрисовки parts;
- правила текстового и texture backend;
- режим invalidation и render cache.

Renderer не должен самостоятельно:

- менять layout;
- добавлять или удалять children;
- отправлять widget events;
- открывать popup;
- менять focus;
- создавать скрытое второе дерево.

## 7. Композиция renderer

### 7.1. Checkbox

Пример целевого renderer:

```java
public final class DefaultCheckBoxRenderer
        implements WidgetRenderer<CheckBoxRenderState> {
    private final ControlChromePart chrome;
    private final CheckIndicatorPart indicator;
    private final LabelPart label;
    private final FocusRingPart focusRing;

    @Override
    public void render(RenderContext context, CheckBoxRenderState state) {
        chrome.render(context, state);
        indicator.render(context, state);
        label.render(context, state);
        focusRing.renderIfNeeded(context, state);
    }
}
```

Все parts используют общий `CheckBoxRenderState` или узкий derived view. Они не должны
вызывать друг друга через `Widget`-иерархию.

### 7.2. DropDownBox

`DropDownBoxRenderer` отвечает за header и его итоговую раскладку:

```text
DropDownBoxRenderer
  -> ControlChromePart
  -> LabelPart
  -> ChevronPart
```

Popup content не должен случайно становиться вторым renderer того же виджета. Он
рендерится как отдельное popup-поддерево через общий `Popup/Overlay` pipeline.

### 7.3. Контейнеры

`Box`, `PanelWidget`, `HBox`, `VBox`, `GridBox`, `ScrollView` и подобные классы должны
разделять:

- layout policy;
- clip/viewport policy;
- background/border chrome;
- обход children.

Контейнер не должен выбирать renderer дочернего semantic widget по принципу "все дети
являются кнопками". Каждый child сам предоставляет свой primary renderer.

### 7.4. Canvas, map и graph

`CanvasWidget`, `MapCanvas`, `GraphView` и `NodeGraph` могут иметь сложный custom
renderer, но их domain data не должна становиться универсальным `WidgetState`.

Для них допускается отдельный ordered primitive builder, если он сохраняет:

- порядок слоёв;
- clip;
- selection и hover overlay;
- connections до или после nodes по явному правилу;
- transform viewport;
- pointer hit-test mapping.

## 8. Style и Theme

### 8.1. Разрешение renderer

Порядок разрешения renderer сохраняется совместимым с текущей системой:

```text
instance override
    -> style/theme renderer
        -> style render plan adapter
            -> default renderer
```

Но все источники должны возвращать renderer, совместимый с семантической ролью виджета.

`Checkbox` не может получить обычный `ButtonRenderer` только потому, что renderer
зарегистрирован в общем registry. Если нужен визуальный стиль кнопки для checkbox,
создаётся `CheckBoxRenderer`, который переиспользует `ControlChromePart` от кнопки.

### 8.2. Typed renderer registry

Registry должен проверять совместимость роли и state до установки renderer:

```java
interface TypedRendererRegistry {
    <S> void register(
            String id,
            WidgetRole role,
            Class<S> stateType,
            WidgetRenderer<S> renderer);
}
```

Внутри hot path предпочтительны integer ids и fastutil maps, если текущий registry это
позволяет. Строковые ids остаются на границе конфигурации и загрузки style.

Ошибку несовместимости нужно обнаруживать при регистрации или разрешении style, а не
во время случайного кадра.

### 8.3. Style parameters

Theme задаёт визуальные параметры, а не семантический тип:

- цвета;
- border и radius;
- typography;
- размеры indicator;
- spacing;
- focus ring;
- transition parameters;
- texture и shader references.

Не следует создавать отдельные renderer-классы для каждой комбинации цветов. Один typed
renderer получает typed style.

## 9. Behaviors и события

Renderer не обрабатывает input. Действия виджетов идут через существующую typed event
систему.

Пример структуры checkbox:

```text
Checkbox
  + PressBehavior
  + ToggleBehavior
  + FocusBehavior
  + CheckBoxRenderer
```

Пример структуры обычной кнопки:

```text
Button
  + PressBehavior
  + FocusBehavior
  + ButtonRenderer
```

Общие behaviors могут публиковать общие низкоуровневые события, а semantic widget
публикует собственные typed events:

- `ButtonClickEvent`;
- `CheckedChangedEvent`;
- `CheckboxStateChangedEvent`;
- `SelectionChangedEvent`;
- `ValueChangedEvent`.

Нельзя определять semantic action по renderer type. Renderer только отображает уже
вычисленное состояние.

## 10. Наследование и композиция

### 10.1. Что разрешено наследовать

Наследование допустимо для общего lifecycle и поведения:

```text
WidgetBase
  -> InteractiveWidget
      -> CheckableWidget
          -> Checkbox
```

При этом базовый класс не должен навязывать checkbox renderer или button chrome.

### 10.2. Что не следует наследовать

Не следует использовать наследование только для получения визуального результата:

```text
Checkbox extends Button только ради кликабельности
RadioButton extends Checkbox только ради checked
ToggleSwitch extends Button только ради hover/pressed
```

Если нужно переиспользовать input behavior, он выносится в behavior/delegate. Если нужно
переиспользовать отрисовку поверхности, используется renderer part.

### 10.3. Правило для нового виджета

Перед созданием нового класса нужно ответить:

1. Какая у него semantic role?
2. Какой typed event он публикует?
3. Какие behaviors ему нужны?
4. Какой у него один primary renderer?
5. Какие visual parts можно переиспользовать?
6. Какие параметры должны находиться в `Style`/`Theme`?
7. Какие данные нужны его render state?

Если на вопросы 1 и 4 нет однозначного ответа, виджет ещё нельзя добавлять в
реализацию.

## 11. Совместимость с текущим API

Переписывание выполняется поэтапно.

### Этап A. Контракты и диагностика

- добавить `WidgetRole` или эквивалентный внутренний role id;
- добавить typed renderer compatibility checks;
- логировать выбранные renderer, state type и источник выбора только в debug режиме;
- запретить новые зависимости semantic widgets от `ButtonRenderType`;
- не менять поведение существующих экранов.

### Этап B. Первый мигрируемый виджет

Первым мигрируется `Checkbox`:

- создать `CheckBoxRenderState`;
- создать `CheckBoxRenderer`;
- перенести checkbox-specific поля из `ButtonState`;
- оставить adapter `ButtonRenderer -> CheckBoxRenderer` только для старых style bindings;
- сохранить текущие public methods и typed events;
- сравнить screenshots, hit-test и allocation baseline.

### Этап C. Остальные checkable controls

В следующем порядке мигрировать:

1. `RadioButton` и `RadioGroup`;
2. `ToggleButton`;
3. `ToggleSwitch`;
4. `ToolButton`, `ToggleToolButton`, `IconButton`;
5. `HoldButton`.

Общее press/focus/toggle поведение переносится в behaviors, но renderer state каждого
semantic role остаётся отдельным.

### Этап D. Остальные visual widgets

После checkable controls мигрировать:

- `Slider`;
- `ProgressBar`;
- `TextInput`, `TextArea`, `PasswordField`;
- `DropDownBox`, `ComboBox`;
- `Popup`, `Tooltip`, `WindowWidget`;
- `CanvasWidget`, `MapCanvas`, `GraphView`;
- image, texture, shape и display widgets.

### Этап E. Удаление legacy discriminator

После миграции всех зависимостей:

- удалить checkbox-specific поля из `ButtonState`;
- сократить `ButtonRenderType` до совместимого legacy слоя или удалить его;
- удалить adapters, у которых больше нет потребителей;
- удалить direct renderer paths, дублирующие typed renderer;
- обновить `WidgetsRender` и style registry.

## 12. Invalidation и render cache

Изменение render state должно инвалидировать только нужный уровень:

- текст, spacing, размер indicator: `LAYOUT | VISUAL`;
- checked/indeterminate/checkProgress: `VISUAL`;
- hover/pressed/focused/enabled: `VISUAL` и при необходимости `INPUT`;
- renderer или computed style: `VISUAL`;
- изменение children: `TREE | LAYOUT | VISUAL | INPUT`.

Разрешённый renderer и style должны кэшироваться до изменения соответствующей версии.
Нельзя искать renderer по классу, собирать новый список parts или создавать временную
коллекцию на каждый кадр без измеренной причины.

Для state действуют правила:

- на холодном пути допустим immutable snapshot;
- для статического leaf widget state можно переиспользовать;
- animated/continuous widget может обновлять только изменяемые поля;
- mutable reusable state не отдаётся внешнему коду после завершения кадра;
- cache не должен менять порядок primitives или semantic overlay.

## 13. Ошибки, которые нельзя допускать

### Запрещено: универсальный state

```java
class UniversalControlState {
    boolean checked;
    boolean indeterminate;
    float sliderValue;
    String inputText;
    float progress;
}
```

### Запрещено: выбор renderer по случайному базовому классу

```java
if (widget instanceof Button) {
    return buttonRenderer;
}
```

Нужно выбирать renderer по зарегистрированной semantic role и typed state.

### Запрещено: renderer меняет модель

```java
renderer.render(...) {
    widget.checked(true);
    widget.openPopup();
}
```

### Запрещено: parts создают скрытую иерархию

Visual part не должен становиться child widget только для того, чтобы переиспользовать
его `render`.

## 14. Тестирование

### Contract tests

Для каждого primary renderer проверять:

- renderer принимает только правильный state type;
- style/theme не могут назначить renderer другой роли;
- instance override сохраняет typed compatibility;
- renderer не вызывает input, layout и mutation API;
- все primitives имеют правильный clip, transform и z-order.

### Widget tests

Для `Checkbox` проверить:

- unchecked, checked и indeterminate;
- hover, pressed, focused и disabled;
- keyboard activation;
- label слева и справа;
- animation progress;
- custom `CheckBoxRenderer` через theme;
- старый `ButtonRenderer` adapter;
- смену renderer во время жизни виджета;
- resize и GuiScale.

### Regression tests

Сравнить с текущим поведением:

- TestMod smoke screens;
- nested popup и overlay;
- `MapCanvas` и `NodeGraph` grid/background;
- post-effect boundaries;
- world/map coordinate mapping;
- render cache invalidation;
- mouse release outside и pointer capture.

### Performance tests

Измерять отдельно:

- allocations/frame;
- время renderer/style resolve;
- количество state snapshot allocations;
- количество draw commands и backend draw calls;
- cache hit rate;
- время composite renderer parts;
- время layout и input, чтобы renderer rewrite не переносил работу в эти этапы.

Оптимизация принимается только при сохранении визуального и input behavior.

## 15. Результат текущего этапа

На переходном этапе в `widgets.render` добавлены переиспользуемые части стандартных контролов:

- `ControlChromePart` отвечает только за фон и рамку;
- `LabelPart` отвечает за clip, вертикальное выравнивание и вызов текстового renderer-а;
- `CheckIndicatorPart` отвечает за квадратный indicator checkbox;
- `RadioIndicatorPart` отвечает за круглый indicator radio button.

Typed renderer-ы используют эти части и сохраняют прежний порядок draw-команд. Это не новый
renderer-контракт: parts являются небольшими переиспользуемыми building blocks, а владельцем
полного render path по-прежнему остаётся renderer semantic role.

`HoldButtonRenderer` также объявляет роль `HOLD_BUTTON`. Его основное состояние теперь содержит
собственные typed visual-поля и данные hold-прогресса. Метод `HoldButtonState.button()` и
конструктор из `ButtonState` сохранены как `@Deprecated` adapter только для переходной
совместимости со старыми renderer-ами.

Обычная `Button` теперь имеет основной typed-контракт `ButtonVisualRenderer` и
`ButtonRenderState`. Legacy `ButtonRenderer` и `ButtonState` остаются выше typed default в
порядке совместимости и используются для старых instance/style override и `RenderPlan`;
публичный instance API typed renderer будет добавляться после готовности role-specific bridges.

Для toolbar-контролов добавлен единый typed-контракт `ToolButtonRenderer` и состояние
`ToolButtonRenderState`. `ToggleToolButton` и `IconButton` используют этот контракт как
визуальные варианты `ToolButton`: их различия относятся к поведению и составу контента, а не
к отдельному renderer-контракту. Стандартный `ToolButtonRenderer` переиспользует существующий
button renderer как visual part, поэтому legacy `ButtonRenderer` и `RenderPlan` продолжают
работать во время миграции.

`Checkbox` и `ToggleSwitch` временно наследуют API `ToggleButton`. Чтобы inherited method не
становился silently ignored, старый `toggleButtonRenderer(...)` адаптируется к собственному
typed renderer-а и помечен `@Deprecated`. Новый код должен использовать соответственно
`checkboxRenderer(...)` и `toggleSwitchRenderer(...)`.

## 16. Definition of Done

Задача считается выполненной, когда:

- каждый visual widget имеет документированную semantic role;
- у каждого role есть один primary typed renderer contract;
- checkbox больше не зависит от `ButtonState` как от своего основного state;
- общие chrome, label и indicator переиспользуются через parts;
- behaviors не зависят от конкретного renderer;
- renderer выбирается через typed Style/Theme pipeline;
- несовместимый renderer отклоняется до render path;
- legacy adapters покрыты тестами и имеют план удаления;
- render cache и invalidation сохраняют порядок, clip, transform и overlays;
- TestMod smoke screens не имеют визуальных регрессий;
- allocation и draw baseline сравнены до и после миграции;
- `docs/UNIGUI_WIDGET_REWRITE_SPEC.md` и `docs/WIDGETS_CONTRACT.md` ссылаются на этот
  контракт.

## 16. Рекомендуемый первый PR

Первый PR не должен переписывать все виджеты. Его область:

1. Добавить этот контракт и внутренние role ids.
2. Добавить typed compatibility check в renderer resolver.
3. Ввести `CheckBoxRenderState` и `CheckBoxRenderer` без удаления старого API.
4. Перенести renderer checkbox на новый state.
5. Вынести `CheckIndicatorPart` из `ButtonRenderer`.
6. Добавить tests для checked/unchecked/indeterminate и theme override.
7. Снять baseline allocations и draw calls.

После этого станет понятно, какие части текущих `ButtonRenderer`, `ButtonState` и
`WidgetsRender` действительно являются общими, а какие были объединены только из-за
исторической иерархии.
