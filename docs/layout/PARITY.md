# Таблица веб-паритета

Что layout-движок UniGUI поддерживает из CSS, а что осознанно отличается.
Статус на текущую итерацию (фазы 0–5, Grid core, intrinsic v1).

## Flexbox Level 1

| Возможность | Статус | Примечание |
|---|---|---|
| `flex-direction: row/column/row-reverse/column-reverse` | ✅ | |
| `flex-wrap: nowrap/wrap/wrap-reverse` | ✅ | |
| `order` | ✅ | стабильная сортировка |
| `flex-flow` (сокращение) | ✅ | |
| `flex-grow` с заморозкой при max | ✅ | |
| `flex-shrink` масштабированный (`shrink × base`) с заморозкой при min | ✅ | |
| `flex-basis`, приоритет над width/height | ✅ | |
| `flex-basis: content` | ✅ | snapshot-путь отдаёт сырой замер; live клампит desired |
| `flex: <number>/auto/initial/none`, `expand()` | ✅ | |
| `min-width/min-height: auto` как контентный минимум | ✅ | только при явном `auto`; дефолт `min` = `0` |
| `align-items/self: start/center/end/stretch` | ✅ | |
| `align-items/self: baseline` | ✅ | только row-оси; `first/last-baseline` нет |
| `align-content` | ✅ | включая stretch одиночной wrap-линии |
| `justify-content` (все режимы) | ✅ | на каждой линии; auto-margin отменяет |
| `margin: auto` | ✅ | главная ось делит свободное место; поперечная центрирует/прижимает; в absolute = 0 |
| `gap` (px) | ✅ | только между, не по краям |
| `gap` в процентах | ❌ | отложено (нужны SizeValue-гэпы) |
| static-позиция abspos-детей | ❌ | отложено (нужен parent style в AbsoluteLayoutEngine) |

## Размеры

| Возможность | Статус | Примечание |
|---|---|---|
| px / % / auto | ✅ | проценты от indefinite → fallback/auto |
| `min/max-content`, `fit-content()` | ✅ | `Widget.min/maxContentSize()`; текст — самое длинное слово; snapshot без overrides = measured |
| `aspect-ratio` | ✅ | ровно одна auto-ось; min/max могут нарушить ratio |
| `box-sizing` | ❌ | border вне box model |
| `min()/max()/clamp()` | ❌ | низкий приоритет |

## Grid

| Возможность | Статус | Примечание |
|---|---|---|
| `grid-template`: px/%/fr/auto/minmax/repeat | ✅ | |
| explicit placement (старт + span) | ✅ | без негативных линий и имён |
| `grid-auto-flow: row/column/dense` | ✅ | sparse прыгает курсором, dense добивает дыры |
| `grid-auto-columns/rows` | ✅ | |
| `justify-content`/`align-content` треков | ✅ | stretch тянет auto-треки |
| abspos-дети в grid | ✅ | вне потока |
| spanning в auto-sizing | ⚠️ | spanning-элементы игнорируются (упрощение) |
| `place-*`, именованные линии, subgrid | ❌ | |

## Позиционирование

| Возможность | Статус | Примечание |
|---|---|---|
| `static` | ✅ | в потоке, insets игнорируются |
| `relative` + сдвиг | ✅ | без reflow, overflow разрешён |
| `absolute` | ✅ | |
| `fixed` | ⚠️ | как absolute от ближайшего хоста; вьюпорт — через overlay-хосты |
| `sticky` | ❌ | поздняя фаза |
| `z-index` | ✅ | рендер + хит-тест; без stacking contexts |
| `display: none` / `visibility: hidden` | ✅ | `COLLAPSED` / `HIDDEN` (hidden держит место) |
| `overflow-x/y` комбинационное правило | ✅ | visible + не-visible → auto |
| `direction: rtl`, logical properties | ❌ | |

## Вне scope (осознанно)

Floats/clear, inline-фрагментация, multicol, таблицы, CSS-каскад/селекторы,
margin collapsing (flex/grid не схлопывают), `position: sticky` (пока).
