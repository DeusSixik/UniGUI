# Layout → Web: полный TODO-лист

Цель: уровень CSS Flexbox + CSS Grid core + positioning + box model.
Не-цели (осознанно, это игровой UI, а не браузер): floats/clear,
inline-фрагментация, multicol, таблицы, полный CSS-каскад.

Состояние на момент написания: два почти одинаковых flex-солвера
(`impl/layout/FlexLayoutEngine.java` — live, `impl/layout/v3/TaffyLayoutEngine.java` —
snapshot), V2/V3 dual-path с opt-in флагами, legacy-слой `LayoutConstraints` параллельно
с `LayoutStyle`, Grid — equal-cell (`LayoutV3GridAdapter`), в `FlexDirection` только
ROW/COLUMN, нет reverse/order/align-content/baseline/aspect-ratio.

Update (фаза 1.1 выполнена): единое ядро `impl/layout/flex/FlexSolver.java` +
`FlexStyleView` поверх `LayoutStyle`/`LayoutStyleSnapshot`; `FlexLayoutEngine` — live-фронтенд,
`impl/layout/v3/WebLayoutEngine.java` — snapshot-фронтенд (старое имя `TaffyLayoutEngine`
осталось deprecated-алиасом). Паритет live/snapshot покрыт `FlexSolverParitySelfTest`.
Дальнейшие ссылки на старые имена файлов ниже читать с учётом переименования.

Порядок: 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7/8 параллельно → 9.
Фаза 1 обязательна первой: наращивать фичи на дублированный солвер — множить долг.

## Фаза 0. Архитектурные решения (зафиксировать до кода)

- [ ] 0.1 Зафиксировать scope: Flexbox Level 1 + Grid core + absolute/fixed + box model.
      Dock/Split/Stack/Overlay — игровые расширения вне web-ядра.
- [ ] 0.2 Один live-path (прямой arrange по виджетам) + один snapshot-path
      (`LayoutNode` → `LayoutOutput` для тестов/тулзов/кэша).
      Запрет строить фейковый `LayoutOutput` в адаптерах.
- [x] 0.3 Зафиксировать терминологию: главная/поперечная ось (main/cross axis),
      гипотетический размер (hypothetical size), flex-база (flex base), заморозка
      (freeze), трек (track), линия (line), статическая позиция (static position).
      (это был не Taffy) → `WebLayoutEngine`, старое имя — deprecated-алиас.
      Терминология зафиксирована в javadoc `FlexSolver`.
- [ ] 0.4 Решить судьбу V2-пути: удалить после паритета, а не держать dual-path
      с флагами вечно.

## Фаза 1. Консолидация (убрать сложность, не добавляя фич)

- [x] 1.1 Единое flex-ядро. Было: `impl/layout/FlexLayoutEngine.java`
      (~424 строки) + `impl/layout/v3/TaffyLayoutEngine.java` (~575 строк)
      с копипастой распределения/линий. Стало: один `impl/layout/flex/FlexSolver`
      с инжектируемыми измеренными размерами, два входа (live-виджеты через
      `FlexLayoutEngine`, snapshot-ноды через `WebLayoutEngine`). Паритет покрыт
      `FlexSolverParitySelfTest` (live vs snapshot, 11 фикстур).
- [x] 1.2 Убить внутренние использования legacy: `LayoutV3DockAdapter`, `LayoutV3GridAdapter`,
      `LayoutV3SplitAdapter`, `LayoutV3StackAdapter`, `StackPanel`, `GridBox`, `PanelWidget`,
      `PanelRowWidget`, `DropDownBox`, `FlexLayoutEngine`, `AbsoluteLayoutEngine` переведены
      на `layoutStyle()` через общий `impl/layout/SlotLayout.java` (slot-математика в одном месте,
      проценты резолвятся вместо деградации). `LayoutStyleLegacyAdapter` остался только на границе
      для сторонних `Widget`-имплементаций (`SlotLayout.childStyleOf`, `LayoutTreeBuilder`,
      движки для не-`WidgetBase`).
- [x] 1.3 CSS-набор выравниваний (частично): новый `AlignContent`
      (STRETCH/START/CENTER/END/SPACE_*) + `LayoutStyle.alignContent`, снапшот, solver,
      XML-атрибут/сериализатор. Полное переименование `Align`/`Justify`/`Alignment`
      не делать: `Alignment` сидит в десятках виджетов, снос = публичный брейк без
      layout-выгоды. `BASELINE` заведётся только вместе с 2.10.
- [ ] 1.4 Распилить `api/layout/LayoutStyle.java` (1431 строка): ядро + группы
      (Size / Flex / Align / Insets) с сохранением fluent-типа.
- [ ] 1.5 Удалить V2/V3 opt-in флаги и паритет-тесты `*MatchesV2*`
      в `LayoutV3SelfTest.java` после 1.1 (там ~40 таких тестов — балласт
      после унификации).
- [x] 1.6 `EdgeInsets` (только px) → `AutoMargins` (left/top/right/bottom) +
      `LayoutStyle.marginAuto`, снапшот, solver, `SlotLayout`, absolute (auto = 0),
      XML `marginAuto` + сериализатор. Главная ось: auto делят free space и отменяют
      justify; поперечная: пара центрирует, один прижимает. Покрыто 7 тестами.
      Процентные margin остаются открытыми (резолв от ширины containing block).

## Фаза 2. Flexbox: полный spec

- [x] 2.1 `FlexDirection`: добавлены `ROW_REVERSE`, `COLUMN_REVERSE`.
- [x] 2.2 `FlexWrap`: добавлен `WRAP_REVERSE` (линии стеклятся снизу вверх).
- [x] 2.3 Новое свойство `order` (стиль, снапшот, XML, сериализатор). Стабильная
      сортировка в `FlexSolver.buildItems`, зеркалирование reverse через
      `mirrorReverse` после раскладки. Покрыто `FlexSolverParitySelfTest`.
- [x] 2.4 `flex-flow` shorthand в `LayoutStyle`.
- [x] 2.5 Spec-алгоритм grow/shrink с **заморозкой**: grow-цикл уже был freeze-эквивалентом,
      зафиксирован тестом `testGrowFreezeAtMax` (max-кламп в середине распределения).
- [x] 2.6 Spec scaled-shrink: вес = `flex-shrink × flex-base-size` (была эвристика
      `shrink * max(1, resolved)`). Покрыто `testScaledShrink` с ручным расчётом,
      старые бейзлайны не изменились (были симметричными).
- [x] 2.7 `flex-basis: content` + точный приоритет `flex-basis > width/height`
      на главной оси. Новый `SizeUnit.CONTENT`/`SizeValue.content()` (+XML `content`,
      сериализатор). Snapshot-путь отдаёт сырой замер листа в solver
      (`measureForItem`), live-путь пока клампит desired preferred-размером
      (полный intrinsic — в 3.1). Покрыто `testFlexBasisContentUsesMeasuredSize`.
- [x] 2.8 `min-width: auto` / `min-height: auto` — см. запись в фазе 3.1.
- [x] 2.9 `align-content` (STRETCH/START/CENTER/END/SPACE_*): распределение
      **линий** по поперечной оси в `FlexSolver.arrange()` (одиночная wrap-линия тоже
      тянется при STRETCH, как в Chrome). Покрыто `FlexSolverParitySelfTest`
      с ручными числами.
- [x] 2.10 `align-items/self: BASELINE` — сделано: `Align.BASELINE` +
      `Alignment.BASELINE`, `Widget.contentBaseline()`, `TextWidget`,
      `LayoutNode.baseline`, в solver максимум baseline и descent на линию
      (только row-оси).
- [x] 2.11 `justify-content` по spec: поведение на каждой линии отдельно (уже было
      в `arrangeLine`) + взаимодействие с `margin: auto` (auto-margin съедает
      free space до justify, `testMarginAutoOverridesJustify`).
- [ ] 2.12 Static position abspos-детей (ОТЛОЖЕНО): требует проброса parent style
       в `AbsoluteLayoutEngine` — breaking change сигнатур. Редкий кейс.
- [ ] 2.13 Gap в процентах (ОТЛОЖЕНО): требует `SizeValue`-гэпов вместо float
       по всему solver + XML. Редко в game UI, после Grid.
- [x] 2.14 Conformance: `FlexConformanceSelfTest` (16 canonical-кейсов с ручными
      числами) + детерминированный фазз live-vs-snapshot (200 сидов) в
      `FlexSolverParitySelfTest`. Фазз уже нашёл 2 бага: `LinearBox`/`WrapPanel`
      игнорировали direction/wrap из стиля (теперь единый источник — стиль)
      и snapshot-absolute с сырыми margin (теперь fixed везде).

## Фаза 3. Модель размеров (intrinsic sizing)

- [x] 3.1 `min-content` / `max-content` / `fit-content()` как значения ширины/высоты:
      `SizeUnit` + `SizeValue` + XML + сериализатор, `Widget.min/maxContentSize()`
      (дефолт через экстремальные measure, `TextWidget` — longest word),
      `FlexSolver.resolveIntrinsic` + `IntrinsicContent` в `ItemInput`,
      фронтенды меряют экстремумы только при `needsIntrinsicContent`.
      Snapshot: overrides из `LayoutTreeBuilder` + экстремальные measure funcs.
      Покрыто 4 тестами.
- [x] 2.8 `min-width: auto` / `min-height: auto` — контентный минимум для
      flex-элементов (обе оси, только при явном `auto`; дефолт `min` остаётся `0`).
      ВАЖНО: дефолт `flex-shrink` фактически `0`, а не CSS `1` (конструктор
      `WidgetBase` синхронизирует стиль из legacy-дефолта). Поле `LayoutStyle`
      исправлено на честные `0` + javadoc про отличие. Для сжатия shrink
      задаётся явно.
- [x] 3.2 `aspect-ratio`: `LayoutStyle.aspectRatio(float)` (NaN = none), снапшот,
      solver (flex main/cross, min/max могут нарушить ratio как в вебе),
      `SlotLayout` (place + preferred), XML-атрибут/сериализатор. Обе оси auto —
      ratio игнорируется (контент). Покрыто 6 тестами с ручными числами.
- [ ] 3.3 Проценты от indefinite-родителя: fallback-правила по spec (auto вместо
      процента), варнинги в debug-дамп.
- [ ] 3.4 `box-sizing: content-box | border-box` + учёт толщины border в layout
      (сейчас border вообще вне box model).
- [ ] 3.5 `min()/max()/clamp()` для размеров (позже; низкий приоритет).

## Фаза 4. Grid (замена equal-cell) — ВЫПОЛНЕНО

`LayoutV3GridAdapter` переписан на настоящий grid: `GridTrack` (px/%/fr/auto/minmax +
`repeat()`), `GridAutoFlow` (row/column/dense), explicit placement
(`gridColumn/gridRow` start+span), constrained placement с прыжком курсора (sparse)
и backfill (dense), неявные треки (`gridAutoColumns/Rows`), `SlotLayout` в ячейках.
Упрощения: spanning items не участвуют в auto-sizing, fr-минимумы по measured,
проценты от indefinite = auto. `GridBox`: `gridColumns/gridRows/gridAutoFlow`,
без шаблонов — legacy `repeat(columns, 1fr)`. XML: `gridTemplateColumns/Rows`,
`gridAutoFlow`, `gridColumn/Span`, `gridRow/Span` + сериализатор. Покрыто 7 тестами.

- [x] 4.1 `grid-template-columns/rows`: треки `px | % | fr | auto | minmax() | repeat()`.
- [x] 4.2 Explicit placement: старт 1-based + span (без негативных линий и имен).
- [x] 4.3 Auto-placement: `row | column | dense` (sparse прыгает курсором,
      dense backfill), `grid-auto-columns/rows` для implicit-треков.
- [x] 4.4 `fr`-распределение с min/max и content-минимумами.
- [x] 4.5 Выравнивание треков: `justify-content` для колонок и `align-content`
      для строк (`START/CENTER/END/SPACE_*`, `STRETCH` тянет auto-треки).
      Item `alignSelf` работает через `SlotLayout`. Покрыто 4 тестами.
- [x] 4.6 Gap в гриде через `rowGap/columnGap` (px; проценты — см. 2.13).
- [x] 4.7 Abspos-дети в grid-областях (вне потока, через `AbsoluteLayoutEngine`).
- [ ] 4.8 Grid conformance-фикстуры аналогично 2.14.

## Фаза 5. Позиционирование

- [x] 5.1 `position: static | relative | absolute` + `fixed`: `STATIC` (in-flow,
      insets игнорируются), `RELATIVE` + сдвиг (`left/top`, иначе `-right/-bottom`,
      без reflow, overflow разрешён), `FIXED` = out-of-flow как absolute v1
      (настоящая привязка к вьюпорту — через overlay-хосты). Сдвиги в flex live +
      snapshot, `SlotLayout` (=> grid/dock/stack/split/panel), `PanelRowWidget`.
      XML работает через generic enum. Покрыто 5 тестами.
- [ ] 5.2 `position: sticky` (поздняя фаза, высокая сложность — за флагом).
- [ ] 5.3 Insets с `auto` + static-position fallback + процентные insets (проценты
      уже резолвятся: left/right от ширины, top/bottom от высоты хоста).
- [x] 5.4 `z-index` для positioned/overlay-детей: `LayoutStyle.zIndex`,
      `ZOrder.paintOrder` (stable sort, zero-alloc fast path при всех нулях),
      `PanelWidget.renderChildren` + `TransformHitTester` в обратном порядке.
      Без stacking contexts (v1). XML + сериализатор. Покрыто хит-тестом.
- [x] 5.5 `display: none` = `COLLAPSED`, `visibility: hidden` = `HIDDEN`
      (место в layout сохраняется — проверено: из потока исключается только
      `COLLAPSED`). Отдельного `display` не нужно, соответствия зафиксированы.

## Фаза 6. Box model и мелочи

- [ ] 6.1 Margin collapsing — решить осознанно: **не делать** (flex/grid не схлопывают;
      нужно только для block-раскладки, которой нет).
- [x] 6.2 Процентные padding/margin: проверять нечего — `EdgeInsets` только
      в пикселях, проценты неприменимы. Процентные insets уже корректны:
      left/right от ширины хоста, top/bottom от высоты.
- [x] 6.3 `overflow-x/y` комбинационные правила: `Overflow.effectiveHorizontal/Vertical`
      (visible + не-visible → visible вычисляется в auto), применено в `ScrollView`.
      Клиппинг уже был эквивалентен. Покрыто combo-кейсами.
- [ ] 6.4 Scroll-контейнер: extents из overflow-size (механизм уже есть
      в `LayoutResult.overflowWidth/Height`), резервирование скроллбаров
      (overlay vs reserve), sticky-шапка позже.
- [ ] 6.5 `direction: ltr | rtl`: разворот row/row-reverse + inline-start/end.
      Logical properties (`margin-inline-start` и т.п.) — фаза 2 RTL, сначала сам direction.

## Фаза 7. Перфоманс

- [ ] 7.1 Инкрементальный layout: dirty-биты + пропуск чистых поддеревьев
      (версионный `LayoutCache` уже есть — довести до subtree-гранулярности).
- [ ] 7.2 Горячий путь без аллокаций: пулинг `Item/Line`-списков, прямой arrange
      без `LinkedHashMap` в live-path (см. 0.2).
- [ ] 7.3 Guard от layout-thrash: варнинг `measure` после `arrange` без инвалидации
      в debug-сборке.
- [ ] 7.4 Бенч-харнес: глубокое дерево / широкий flex / wrap-стресс / grid-стресс,
      регрессионные пороги.

## Фаза 8. Тесты и тулзы

- [x] 8.1 Data-driven conformance-таблица: `FlexConformanceSelfTest`
      (формат — Java-таблица кейсов, без JSON-библиотеки; кейс = имя + дерево
      + вход + ожидаемые прямоугольники).
- [ ] 8.2 Апгрейд `LayoutDebugDumper`: дерево со стилями, constraints, нарушениями
      (percent-of-indefinite, min>max, muted flex-basis).
- [ ] 8.3 Визуализация overflow (подсветка вылезающих детей).
- [x] 8.4 Fuzz-тест: детерминированный (seed `0xC0FFEE`, 200 деревьев)
      live-vs-snapshot в `FlexSolverParitySelfTest`; уже нашёл 2 бага.
      Инварианты неявно покрыты равенством путей; визуализации overflow нет (см. 8.3).

## Фаза 9. Контейнеры, XML, доки

- [ ] 9.1 Dock/Split/Stack зафиксировать как first-class **расширения**
      (не эмулировать через flex/grid): свои стратегии поверх общего
      measure-контракта, без фейкового `LayoutOutput`.
- [x] 9.2 XML-атрибуты для всех новых свойств (`XmlLayoutAttributes` + парсер):
      order, aspectRatio, marginAuto, alignContent, gridTemplateColumns/Rows,
      gridAutoColumns/Rows, gridAutoFlow, gridColumn/Span, gridRow/Span, zIndex,
      SIZE `content/min/max-content/fit-content()`, position static/fixed,
      direction/wrap reverse, baseline; сериализатор + дескрипторы + типы
      редактора; экспортеры FlexBox/GridBox (+columns/spacing).
      Покрыто `testLayoutAttributesRoundTrip`.
- [ ] 9.3 Переписать главу Layout в `docs/UI_FRAMEWORK_ARCHITECTURE.md`: ментальная
      модель (measure→arrange, basis/grow/shrink/freezing, треки, static position),
      таблица web-паритета.
- [x] 9.4 Таблица паритета с вебом: `docs/layout/PARITY.md`.
