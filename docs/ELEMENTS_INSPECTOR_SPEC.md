# ElementsInspector SPEC

Статус: proposed.

## 1. Назначение

`ElementsInspector` — runtime-инструмент отладки UniGUI, похожий на вкладку
Elements/Inspector в браузерных DevTools или инспектор в WPF/Unity.

Инспектор работает с уже созданным деревом живых Java-виджетов. Он позволяет
выбрать виджет на экране или в дереве, посмотреть его структуру и изменить
параметры в реальном времени.

Инспектор не является XML-редактором. XML-подобный вид дерева используется
только как удобное представление структуры.

```text
live Widget tree
      |
      +-- ElementsInspector reads and mutates it
      |
      +-- no XML file is written
      +-- no CodeEditor is required
```

## 2. Требования

### 2.1 XML-подобное дерево без CodeEditor

- Дерево отображается в виде тегов и атрибутов:

  ```text
  <OfferGrid>
    <OfferCard id="sword_01">
      <ImageView>
      <Label text="Epic sword">
    </OfferCard>
  </OfferGrid>
  ```

- CodeEditor в ElementsInspector отсутствует.
- Выбор узла в дереве выбирает соответствующий живой `Widget`.
- Если у виджета есть `@XmlWidgetName`, используется это имя.
- Для пользовательских классов без XML-имени используется имя Java-класса.
- Для различения одинаковых узлов отображаются `id`, индекс среди siblings или
  другой стабильный runtime-label.
- Дерево должно обновляться после добавления, удаления, перемещения и изменения
  виджетов.

### 2.2 Панель Properties

- Все параметры изменяются через typed UI-контролы в Properties.
- Текстовый ввод допускается только как поле значения свойства, но не как
  свободное редактирование XML.
- Минимальные группы свойств:
  - Layout: `width`, `height`, `min/max`, `margin`, `padding`;
  - Flex: `flexDirection`, `flexWrap`, `flexGrow`, `flexShrink`, `flexBasis`,
    `gap`, `alignItems`, `alignSelf`, `justifyContent`;
  - Position: `position`, `left`, `top`, `right`, `bottom`;
  - Behavior: `visible`, `enabled`, `visibility`;
  - Widget: свойства конкретного типа, если для них зарегистрирован inspector
    descriptor.
- Изменение значения применяется сразу к живому виджету.
- Если setter отклоняет значение, старое значение остаётся, а Properties
  показывает validation error.
- Изменение layout вызывает обычную invalidation и пересчёт layout на следующем
  кадре.

### 2.3 Drag and Drop

Поддерживаются два вида drag-and-drop.

#### Перемещение в дереве

- Узел можно перетащить перед sibling, после sibling или внутрь контейнера.
- Drop indicator показывает предполагаемую позицию.
- Нельзя переместить root внутрь самого себя или своего потомка.
- Drop разрешён только после проверки child policy целевого контейнера.
- Одно завершённое перетаскивание является одной undo-командой.
- Если операция недопустима, дерево не изменяется и показывает причину отказа.

#### Перетаскивание из Palette

- Элемент из Palette можно перетащить в подходящий контейнер дерева.
- При drop создаётся новый экземпляр виджета через зарегистрированную фабрику.
- Фабрика возвращает виджет с безопасными значениями по умолчанию.
- Произвольное создание виджетов через reflection не используется.

### 2.4 Контекстное меню

ПКМ по строке дерева или выбранному виджету открывает контекстное меню.

Минимальные команды:

- `Add` — выбрать тип виджета и добавить его в выбранный контейнер;
- `Delete` — удалить выбранный узел, кроме root;
- `Duplicate` — зарезервировать для следующего этапа;
- `Move to parent`/`Move before` — зарезервировать, если они понадобятся
  отдельно от drag-and-drop.

Контекстное меню должно учитывать доступные операции. Например, `Add` для
не-контейнера отключается, а `Delete` для root отсутствует или disabled.

### 2.5 История действий

- Все изменения живого дерева проходят через команды.
- Поддерживаются `Undo` и `Redo`.
- `Ctrl+Z` отменяет последнюю команду ElementsInspector.
- `Ctrl+Y` и `Ctrl+Shift+Z` повторяют отменённую команду.
- Одна пользовательская операция должна создавать одну команду:
  - изменение одного или группы свойств;
  - add;
  - delete;
  - reorder/reparent;
  - добавление из Palette.
- Команда обязана хранить данные для обратной операции, а не полагаться только
  на текущий selection.
- Неудачная операция не попадает в undo stack.
- Undo/redo инвалидирует дерево, Properties и layout.
- История является runtime-состоянием инспектора. Она не сериализуется и
  очищается при смене inspected root или уничтожении screen.

### 2.6 Palette виджетов

Palette — отдельная панель со всеми разрешёнными типами виджетов.

- Поддерживает поиск и группировку по категориям.
- Показывает display name, XML name и короткое описание.
- Drag из Palette создаёт новый экземпляр.
- Add из контекстного меню использует ту же фабрику, что и Palette.
- Источником списка является явный `ElementsInspectorWidgetRegistry`, а не
  сканирование classpath.
- Registry может ограничивать доступные типы для конкретного host/mod/screen.
- Для каждого типа фабрика должна определить:
  - создание экземпляра;
  - допустимые parent-контейнеры;
  - default properties;
  - optional preview label/icon.

## 3. Поведение окна

- При наличии `DebugFlags.ELEMENTS_INSPECTOR` инспектор подключается к экрану.
- По умолчанию он появляется свёрнутым: виден только title bar.
- Сворачивание не закрывает инспектор и не сбрасывает selection/history.
- Разворачивание показывает Tree, Properties и Palette согласно текущему
  workspace layout.
- Инспектор не modal и не должен блокировать взаимодействие с игровым UI,
  кроме области самого окна и активного drag/picker режима.
- Закрытие окна скрывает инспектор, но не откатывает внесённые изменения.
- Повторное открытие перечитывает живое дерево и свойства. Данные не берутся из
  старого UI-снапшота.
- При завершении screen runtime-изменения исчезают вместе с этим runtime tree,
  если host отдельно не экспортировал их.

`WindowWidget` сейчас умеет `open/close`, но не имеет semantics collapsed.
Для инспектора требуется добавить отдельное состояние `collapsed`, которое
оставляет title bar и исключает content из measurement/arrange.

## 4. Selection и Picker

- Selection хранится как ссылка на живой `Widget` внутри текущего root.
- Для отображения пути используется вычисляемый путь ancestors + child index;
  путь не является источником истины.
- Picker переводит координаты курсора через текущий `HitTester`.
- При включённом Picker клик по игровому UI выбирает самый глубокий видимый
  hit-tested widget.
- Сам инспектор и его дочерние виджеты исключаются из Picker.
- После выбора Picker выключается, а Properties и дерево переходят на новый
  selection.
- Для выбранного элемента можно рисовать box-model overlay: bounds, margin,
  border и padding. Overlay является debug-визуализацией и не меняет layout.

## 5. Источник свойств

ElementsInspector не должен использовать XML как промежуточный источник
состояния.

Разрешено переиспользовать metadata из:

- `@XmlAttribute` и `XmlWidgetAnnotations`;
- `XmlWidgetRegistry`;
- `LayoutStyle` и `WidgetBase`.

Но для runtime-инспектора нужен отдельный typed property contract:

```text
PropertyDescriptor<T>
  name
  category
  valueType
  read(widget) -> value
  write(widget, value) -> result
  reset(widget)
  editable predicate
```

XML descriptor без getter нельзя считать достаточным runtime-контрактом. Для
виджетов, у которых нет runtime property descriptor, Properties показывает
только доступные универсальные свойства.

## 6. Архитектура

Предлагаемые компоненты:

```text
MinecraftWidgetScreen
  -> ElementsInspectorController
       -> ElementsInspectorWindow (WindowWidget)
       -> ElementsInspectorTree
       -> ElementsInspectorProperties
       -> ElementsInspectorPalette
       -> ElementsInspectorOverlay
       -> ElementsInspectorCommandHistory
       -> ElementsInspectorWidgetRegistry
```

### 6.1 Controller

`ElementsInspectorController` владеет runtime-состоянием:

- inspected root;
- selected widget;
- picker state;
- window open/collapsed state;
- command history;
- refresh/invalidation coordination.

Controller не должен владеть самим игровым UI-tree и не должен менять root
неявно.

### 6.2 Tree

`ElementsInspectorTree` — thin view над live `Widget` hierarchy.

- Не создаёт копии игровых виджетов.
- Рекурсивно читает `children()`.
- При rebuild сохраняет selection, если выбранный object ещё существует.
- После удаления selection переходит к parent.
- После add/reparent selection переходит к добавленному/перемещённому узлу.

### 6.3 Properties

`ElementsInspectorProperties` строит поля из `PropertyDescriptor` и текущего
selection. Layout properties должны работать напрямую с `LayoutStyle`, чтобы
не проходить через legacy `LayoutConstraints`.

### 6.4 Commands

```text
ElementsInspectorCommand
  description()
  canApply()
  apply()
  undo()
```

Команды add/delete/reparent должны хранить object references и исходные
позиции/родителей достаточно долго для undo, но не должны переживать lifetime
inspected screen.

Для batch-изменения нескольких полей используется composite command. Например,
изменение `margin` одним CSS shorthand считается одной операцией.

### 6.5 Input и overlay integration

`DebugOverlayRenderer` сейчас только добавляет draw commands и не принимает
input. ElementsInspector нельзя реализовать только через него.

Нужен интерактивный debug-root, который участвует и в render, и в hit testing.
Предпочтительный вариант:

- если screen root уже является `OverlayLayer`, добавлять окно туда;
- иначе `MinecraftWidgetScreen` должен поддерживать отдельный debug overlay
  widget root и объединять его с основным root в input priority;
- debug overlay должен быть выше игрового root, но ниже modal overlays, если
  host явно не меняет это правило.

Клики по инспектору не должны доходить до игрового root. При активном Picker
правило меняется только для одного выбора: инспектор временно пропускает клик
до inspected root и получает результат hit test.

## 7. Границы первой реализации

В первую реализацию входят:

- debug flag и runtime window;
- collapsed/expanded window state;
- XML-подобное дерево живых виджетов;
- selection и Picker;
- Properties для LayoutStyle и базовых WidgetBase properties;
- Add/Delete;
- reorder/reparent через tree drag-and-drop;
- Ctrl+Z/Ctrl+Y;
- Palette для базовых контейнеров и display/control widgets;
- transient box-model overlay.

В первую реализацию не входят:

- CodeEditor и редактирование XML-текста;
- сохранение XML или автоматический export runtime changes;
- arbitrary Java reflection;
- изменение callback/binding-кода;
- полноценный visual canvas editor с resize handles для каждого виджета;
- undo после уничтожения screen;
- редактирование свойств, для которых нет безопасного typed descriptor.

## 8. Этапы реализации

### Этап A: наблюдение

- Debug flag;
- debug-root integration;
- collapsed `WindowWidget`;
- live tree;
- selection;
- read-only Properties;
- Picker и box-model overlay.

### Этап B: безопасное редактирование

- typed `PropertyDescriptor`;
- LayoutStyle properties;
- command history;
- Add/Delete через context menu;
- keyboard shortcuts.

### Этап C: структура

- hierarchy drag-and-drop;
- reparent validation;
- duplicate;
- selection recovery после структурных изменений.

### Этап D: Palette

- registry factories;
- search/categories;
- drag из Palette в tree;
- host-specific allowed widget types.

## 9. Критерии готовности

- При активном `DebugFlags.ELEMENTS_INSPECTOR` окно появляется свёрнутым.
- Разворачивание показывает актуальное дерево текущего runtime root.
- Выбор строки дерева выбирает тот же `Widget`, что и Picker на экране.
- Изменение `width`, `height`, `margin`, `padding` и flex-свойств видно без
  перезапуска screen.
- ПКМ по узлу показывает Add/Delete с корректными enabled/disabled состояниями.
- Drag-and-drop может reorder и reparent допустимые узлы.
- Невалидный drop не меняет дерево.
- Каждое изменение можно отменить через `Ctrl+Z` и повторить через `Ctrl+Y`.
- Palette и context-menu Add используют одну систему фабрик.
- После закрытия и повторного открытия Properties читаются из живого дерева.
- При уничтожении screen никакие изменения ElementsInspector не записываются в
  XML или на диск.

## 10. Связь с XML UI Editor

`XML_UI_EDITOR_SPEC.md` описывает редактор, где XML является source of truth.
ElementsInspector описывает другой режим:

```text
XML UI Editor:
  XML document -> runtime preview

ElementsInspector:
  runtime widget tree -> transient live inspection/mutation
```

Общие metadata, registry и визуальные паттерны можно переиспользовать, но
командные модели и lifetime состояния должны оставаться раздельными. Нельзя
автоматически превращать runtime-изменения инспектора в XML edits без отдельной
явной команды `Export`/`Apply`, которой в первой версии нет.
