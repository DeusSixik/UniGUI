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
| `flex-wrap: nowrap` | `flexWrap(FlexWrap.NOWRAP)` (дефолт) |
| `flex-wrap: wrap` | `flexWrap(FlexWrap.WRAP)` |
| `flex-wrap: wrap-reverse` | `flexWrap(FlexWrap.WRAP_REVERSE)` |
| `flex-flow: row wrap` | `flexFlow(FlexDirection.ROW, FlexWrap.WRAP)` |
| `order: 2` / XML `order="2"` | `order(2)` |
| `gap: 8px` | `gap(8)` |
| `row-gap: 4px; column-gap: 8px` / XML `rowGap`/`columnGap` | `gap(4, 8)` / `rowGap(4).columnGap(8)` |
| `justify-content` | `justifyContent(Justify...)` |
| `align-items` | `alignItems(Align...)` |
| `align-self` | `alignSelf(Align...)` |
| `align-self: baseline` | `alignSelf(Align.BASELINE)` |
| `align-content` (только при wrap, иначе `align-items`) | `alignContent(AlignContent...)` |
| `place-content` / центрирование детей | `center()` (= `alignItems(CENTER)` + `justifyContent(CENTER)`) |
| `align-self: center` для одного элемента | `centerSelf()` |
| `flex-grow` | `flexGrow(...)` |
| `flex-shrink` (дефолт UniGUI — `0`, в CSS — `1`) | `flexShrink(...)` / `noShrink()` |
| `flex-basis` | `flexBasis(SizeValue...)` / `flexBasis(px)` |
| `flex-basis: content` | `flexBasis(SizeValue.content())` |
| `flex: 1` (в UniGUI = `1 1 0%`) | `flex(1)` |
| `flex: auto` | `flexAuto()` |
| `flex: initial` | `flexInitial()` |
| `flex: none` | `flexNone()` |
| занять свободное место главной оси (= `flex(1, 1, 0)`) | `expand()` |
| `flex: 2 1 100px` | `flex(2, 1, 100)` / `flex(2, 1, SizeValue...)` |
| `width: 100%; height: 100%` | `fill()` |
| `width: 120px; height: 40px` / XML `width`/`height` | `size(120, 40)` / `size(SizeValue...)` / `width(...)` / `height(...)` |
| `width: 50%` / XML `width="50%"` | `widthPercent(50)` / `sizePercent(50, ...)` |
| `width: min-content` | `width(SizeValue.minContent())` |
| `width: max-content` | `width(SizeValue.maxContent())` |
| `width: fit-content(30px)` | `width(SizeValue.fitContent(30))` |
| `min-width` / `min-height` / XML `minWidth`/`minHeight` | `minWidth(...)` / `minHeight(...)` / `minSize(...)` |
| `max-width` / `max-height` / XML `maxWidth`/`maxHeight` | `maxWidth(...)` / `maxHeight(...)` / `maxSize(...)` |
| `min-width: auto` (контентный минимум) | `minWidth(SizeValue.auto())` |
| `aspect-ratio: 16 / 9` / XML `aspectRatio="1.78"` | `aspectRatio(16.0f / 9.0f)` |
| `margin` / XML `margin="10 8 10 8"` | `margin(all)` / `margin(h, v)` / `margin(l, t, r, b)` / `margin(EdgeInsets...)` |
| `margin: auto` / XML `marginAuto="horizontal"` | `marginAuto(AutoMargins.HORIZONTAL)` / `marginAutoHorizontal()` / `marginAutoVertical()` |
| `padding` / XML `padding="10 8 10 8"` | `padding(all)` / `padding(h, v)` / `padding(l, t, r, b)` / `padding(EdgeInsets...)` |
| `overflow` / XML `overflow`/`overflowX`/`overflowY` | `overflow(...)` / `overflowX(...)` / `overflowY(...)` |
| фиксированные ширина и высота | `fixed(width, height)` |
| `position: static` | `position(PositionType.STATIC)` |
| `position: relative; left: 8px` | `position(PositionType.RELATIVE).left(8)` |
| `position: absolute` | `position(PositionType.ABSOLUTE)` |
| `position: fixed` (как absolute от хоста) | `position(PositionType.FIXED)` |
| `left/top/right/bottom` / XML `left`/`top`/... | `left(...)` / `top(...)` / `right(...)` / `bottom(...)` |
| `inset: 8px` | `inset(8)` / `inset(h, v)` / `inset(l, t, r, b)` |
| `margin: 4px 8px` | `EdgeInsets.css(4, 8)` |
| `z-index: 5` / XML `zIndex="5"` | `zIndex(5)` |

## Grid

| CSS | UniGUI |
| --- | --- |
| `display: grid` | `new GridBox()` + `gridColumns(...)` / `gridRows(...)` |
| `grid-template-columns: 100px 1fr auto` | `gridColumns(GridTrack.px(100), GridTrack.fr(1), GridTrack.auto())` / XML `gridTemplateColumns="100px 1fr auto"` |
| `grid-template-rows: repeat(2, 30px)` | `gridRows(...)` / XML `gridTemplateRows="repeat(2, 30px)"` |
| `grid-template-columns: 50% 1fr` | `GridTrack.percent(50)`, `GridTrack.fr(1)` |
| `grid-template-columns: minmax(100px, 1fr)` | `GridTrack.minmax(GridTrack.px(100), GridTrack.fr(1))` |
| `grid-auto-flow: column dense` | `gridAutoFlow(GridAutoFlow.COLUMN_DENSE)` / XML `gridAutoFlow="column-dense"` |
| `grid-auto-columns/rows` | `gridAutoColumns(...)` / `gridAutoRows(...)` / XML `gridAutoColumns`/`gridAutoRows` |
| `grid-column: 2 / span 2` | `gridColumn(2, 2)` / XML `gridColumn="2" gridColumnSpan="2"` |
| `grid-row: 1 / span 3` | `gridRow(1, 3)` / XML `gridRow="1" gridRowSpan="3"` |
| `justify-content` / `align-content` для треков | `justifyContent(...)` / `alignContent(...)` на гриде |

Спарсенные XML-значения треков (`GRID_TRACKS`) поддерживают `repeat(n, track)`
и те же токены, что `GridTrack.parse`: `px`, `%`, `fr`, `auto`, `minmax(...)`.

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

Дефолты `LayoutStyle`: `position = RELATIVE`, `width/height = auto`,
`minSize = 0`, `maxSize = auto`, `flexDirection = COLUMN`, `flexWrap = NOWRAP`,
`flexGrow = 0`, `flexShrink = 0`, `flexBasis = auto`, `alignItems = STRETCH`,
`alignSelf = AUTO`, `alignContent = STRETCH`, `justifyContent = START`,
`overflow = VISIBLE`, `order = 0`, `zIndex = 0`.

Ключевые отличия от CSS:

- `flex-shrink` по умолчанию `0` (в CSS — `1`): игровые UI обычно задают
  размеры явно, и автосжатие даёт неожиданные сплющивания.
- `position: fixed` работает как `absolute` от ближайшего хоста, а не от
  вьюпорта; настоящая привязка к вьюпорту — через overlay-хосты.
- `align-content` действует только при `flex-wrap: wrap`; одиночная линия
  всегда ведёт себя по `align-items`.
- `overflow: visible` на одной оси при невидимом на другой вычисляется в
  `auto` (как в CSS-spec).
- Legacy `Alignment`/`LayoutConstraints` API помечено `@Deprecated`; для
  flex-выравнивания используйте `alignItems`/`alignSelf`.
