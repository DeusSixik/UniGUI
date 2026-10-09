# Layout UniGUI и CSS Flexbox

Layout UniGUI повторяет полезную часть CSS Flexbox. `FlexBox` — прямой аналог
CSS-элемента с `display: flex`, а `HBox` и `VBox` — удобные контейнеры с
фиксированным направлением.

```java
FlexBox toolbar = new FlexBox();
toolbar.layout(style -> style
        .flexDirection(FlexDirection.ROW)
        .justifyContent(Justify.SPACE_BETWEEN)
        .alignItems(Align.CENTER)
        .gap(8.0f));
```

| CSS | UniGUI |
| --- | --- |
| `display: flex` | `new FlexBox()` |
| `flex-direction: row` | `flexDirection(FlexDirection.ROW)` |
| `flex-direction: column` | `flexDirection(FlexDirection.COLUMN)` |
| `flex-direction: row-reverse` | `flexDirection(FlexDirection.ROW_REVERSE)` |
| `flex-direction: column-reverse` | `flexDirection(FlexDirection.COLUMN_REVERSE)` |
| `flex-wrap: wrap` | `flexWrap(FlexWrap.WRAP)` |
| `flex-wrap: wrap-reverse` | `flexWrap(FlexWrap.WRAP_REVERSE)` |
| `flex-flow: row wrap` | `flexFlow(FlexDirection.ROW, FlexWrap.WRAP)` |
| `order: 2` / XML `order="2"` | `order(2)` |
| `gap: 8px` | `gap(8)` |
| `row-gap: 4px; column-gap: 8px` | `gap(4, 8)` |
| `justify-content` | `justifyContent(Justify...)` |
| `align-items` | `alignItems(Align...)` |
| `align-self` | `alignSelf(Align...)` |
| `align-self: baseline` | `alignSelf(Align.BASELINE)` |
| `align-content` | `alignContent(AlignContent...)` |
| `flex-grow` | `flexGrow(...)` |
| `flex-shrink` (дефолт UniGUI — `0`, в CSS — `1`) | `flexShrink(...)` |
| `flex-basis` | `flexBasis(SizeValue...)` |
| `flex-basis: content` | `flexBasis(SizeValue.content())` |
| `flex: 1` | `flex(1)` |
| `flex: auto` | `flexAuto()` |
| `flex: initial` | `flexInitial()` |
| `flex: none` | `flexNone()` |
| `width: 100%; height: 100%` | `fill()` |
| `width: min-content` | `width(SizeValue.minContent())` |
| `width: max-content` | `width(SizeValue.maxContent())` |
| `width: fit-content(30px)` | `width(SizeValue.fitContent(30))` |
| `min-width: auto` (контентный минимум) | `minWidth(SizeValue.auto())` |
| `aspect-ratio: 16 / 9` / XML `aspectRatio="1.78"` | `aspectRatio(16.0f / 9.0f)` |
| `margin: auto` / XML `marginAuto="horizontal"` | `marginAuto(AutoMargins.HORIZONTAL)` |
| фиксированные ширина и высота | `fixed(width, height)` |
| `position: static` | `position(PositionType.STATIC)` |
| `position: relative; left: 8px` | `position(PositionType.RELATIVE).left(8)` |
| `position: absolute` | `position(PositionType.ABSOLUTE)` |
| `position: fixed` (как absolute от хоста) | `position(PositionType.FIXED)` |
| `inset: 8px` | `inset(8)` |
| `margin: 4px 8px` | `EdgeInsets.css(4, 8)` |
| `z-index: 5` / XML `zIndex="5"` | `zIndex(5)` |

## Grid

| CSS | UniGUI |
| --- | --- |
| `display: grid` | `new GridBox()` + `gridColumns(...)` / `gridRows(...)` |
| `grid-template-columns: 100px 1fr auto` | `gridColumns(GridTrack.px(100), GridTrack.fr(1), GridTrack.auto())` |
| `grid-template-rows: repeat(2, 30px)` | `gridRows(...)` / XML `gridTemplateRows="repeat(2, 30px)"` |
| `grid-auto-flow: column dense` | `gridAutoFlow(GridAutoFlow.COLUMN_DENSE)` |
| `grid-auto-columns/rows` | `gridAutoColumns(...)` / `gridAutoRows(...)` |
| `grid-column: 2 / span 2` | `gridColumn(2, 2)` / XML `gridColumn="2" gridColumnSpan="2"` |
| `grid-row: 1 / span 3` | `gridRow(1, 3)` |
| `justify-content` / `align-content` для треков | `justifyContent(...)` / `alignContent(...)` на гриде |

Правила осей те же, что в CSS: `justifyContent` работает на главной оси, а
`alignItems` — на поперечной. У строки главная ось горизонтальная, у колонки —
вертикальная.

```java
VBox screen = new VBox();
screen.layout(style -> style
        .justifyContent(Justify.CENTER)
        .alignItems(Align.CENTER));
```

Этот код центрирует группу дочерних элементов по вертикали и горизонтали.

## Отличия UniGUI

API похож на CSS, но не является реализацией браузерного CSS. В UniGUI нет
селекторов, каскада, наследования и браузерных режимов `display`. Значения
задаются в пикселях UI-пространства, если не используется
`SizeValue.percent(...)`. Виджеты сами измеряют своё содержимое, а масштабирование
Minecraft применяется на уровне screen/context.

`LayoutStyle` остаётся изменяемым, потому что виджеты обновляют его во время
работы. Перед вычислением Layout V3 копирует его в неизменяемый
`LayoutStyleSnapshot`.
