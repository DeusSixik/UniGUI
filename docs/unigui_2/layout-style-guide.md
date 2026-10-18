# LayoutStyle: базовые сценарии

`LayoutStyle` настраивает размеры, ограничения и размещение виджета. Для большинства случаев его удобно изменять через `widget.layout(...)`:

```java
button.layout(style -> style
        .width(120.0f)
        .height(32.0f)
        .flexNone());
```

## Размеры

`width(float)` и `height(float)` задают пиксели. Для процентов есть отдельные методы или универсальный `SizeValue`:

```java
panel.layout(style -> style
        .widthPercent(50.0f)
        .minWidth(180.0f)
        .maxWidth(SizeValue.fitContent(480.0f)));
```

## `fill()` и `expand()`

Эти методы решают разные задачи:

```java
child.layout(style -> style.fill());
```

`fill()` задаёт `width: 100%` и `height: 100%`. Размер зависит от доступных размеров родителя и не означает, что ребёнок заберёт остаток места у siblings.

```java
VBox column = new VBox();
header.layout(style -> style.height(28.0f).flexNone());
content.layout(LayoutStyle::expand);
```

`expand()` распределяет свободное место по главной оси flex-родителя: по высоте для `VBox`, по ширине для `HBox` или `FlexBox` с направлением `ROW`. Для пропорционального распределения между несколькими детьми задайте веса через `flexGrow(...)`.

## Grid

Явные треки описывают шаблон, а `gridAutoColumns/Rows` — размеры треков, создаваемых при автоматическом размещении. Методы с суффиксом `Of` принимают треки напрямую, не меняя существующие перегрузки со списками:

```java
grid.layout(style -> style
        .gridTemplateColumns(GridTrack.px(180.0f), GridTrack.fr(1.0f))
        .gridAutoRowsOf(GridTrack.auto(), GridTrack.minmax(GridTrack.px(24.0f), GridTrack.fr(1.0f)))
        .gridAutoFlow(GridAutoFlow.ROW));
```

## Текущие границы модели

- `margin` и `padding` принимают пиксельные `EdgeInsets`; проценты для них пока не поддерживаются.
- Фиксированные размеры и отступы неотрицательны. Отрицательные margin/inset для выноса элементов за границу контейнера пока не предусмотрены.
- Толщина визуальной рамки пока не включена в расчёт размеров `LayoutStyle`.
- `PositionType.FIXED` в текущей версии не привязывает виджет к экранному viewport; для такого поведения используются overlay-хосты.
- RTL и логические стороны (`start/end`) ещё не реализованы.

Эти ограничения также отмечены в [`docs/layout/TODO.md`](../layout/TODO.md) и сохраняются явно, чтобы разметка не зависела от неподдерживаемых предположений.
