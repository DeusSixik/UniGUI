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
| `flex-wrap: wrap` | `flexWrap(FlexWrap.WRAP)` |
| `gap: 8px` | `gap(8)` |
| `row-gap: 4px; column-gap: 8px` | `gap(4, 8)` |
| `justify-content` | `justifyContent(Justify...)` |
| `align-items` | `alignItems(Align...)` |
| `align-self` | `alignSelf(Align...)` |
| `flex-grow` | `flexGrow(...)` |
| `flex-shrink` | `flexShrink(...)` |
| `flex-basis` | `flexBasis(SizeValue...)` |
| `flex: 1` | `flex(1)` |
| `flex: auto` | `flexAuto()` |
| `flex: initial` | `flexInitial()` |
| `flex: none` | `flexNone()` |
| `width: 100%; height: 100%` | `fill()` |
| фиксированные ширина и высота | `fixed(width, height)` |
| `position: absolute` | `position(PositionType.ABSOLUTE)` |
| `inset: 8px` | `inset(8)` |
| `margin: 4px 8px` | `EdgeInsets.css(4, 8)` |

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
