# DropDownBox / ComboBox refresh (unigui_2, шаг 4)

Модерн-хром раскрывающихся списков по референсу: контрольная высота,
шеврон слотом, зазор попапа, пилюля выбранной строки. Логика и публичный
API не менялись (режимы, события, XML-атрибуты, фабрика `Widgets`).

## Контракт поведения

- **Хедер 36px** (`--control-h`) вместо legacy 22px — `DropDownBox` и
  `ComboBox` (константа `HEADER_HEIGHT` в обоих классах).
- **Шеврон слотом справа**: `Button.chevronVisible/chevronUp` — уголок
  из двух stroke-отрезков (вверх — открыт, вниз — закрыт) в правом слоте
  24px, текст прижат влево и обрезан до слота. Больше никакого
  inline-треугольника с ведущим пробелом в тексте хедера: `richText`
  хедера — чистое значение/плейсхолдер. Кастомный inline-индикатор
  `ComboBox.headerIndicator` работает как раньше (legacy-путь, слот
  шеврона при нём прячется, чтобы глиф не дублировался).
- **Паддинг текста 12px** (`--pad-x`): `headerButton.textPaddingX(12)`.
- **Хост контента**: радиус 8 (`--radius-md`), фон `--bg-1`, рамка
  `--border-subtle` вместо циана 0.75. Касается `contentHost`
  (`DropDownBox`) и `optionsHost` (`ComboBox`).
- **Попап с зазором 6px** (`--popup-gap`): `dropDownPopup.offset(0, 6)`
  вместо дефолтных 4px.
- **Selected-пилюля**: строки опций `ComboBox` — скругление 4px,
  горизонтальный отступ 4px от краёв списка, фон `--bg-selected`,
  невыбранные прозрачные (были сплошные тёмные плашки). Hover-пилюля —
  follow-up за рефрешем `ToggleButton` (у `Button` нет hover-фона),
  здесь не трогали.
- **Строки 28px, текст влево**: высота строки опций 20 → 28px,
  выравнивание `Button.textAlignment(START)` + паддинг 12px — текст строк
  на одной вертикали с текстом хедера (было центрирование + теснота).
  Новый API выравнивания на обычной кнопке ничего не меняет (default CENTER).
- **Без боксов и наезда скролла**: у строк-опций выключена собственная
  рамка (иначе каждая строка — отдельный бокс), скролл списка —
  reserved-полоса (`overlayScrollBars(false)`), а не поверх пилюль.
  То же для контента `DropDownBox`.
- **Фикс overscroll (ScrollView)**: `LayoutV3ScrollAdapter.measureContent`
  брал `max(measured, overflow)` синтетического flex-прохода, а overflow
  по определению `>= available`: любой скролл со style-размером меньше
  контекста получал раздутый extent (список опций 289 в контексте 400 →
  extent 400, в конце скроллилось в пустоту; меню 80 в контексте 800 →
  хост растягивался под maxHeight вместо контента). Сравнение с вьюпортом
  (`maxScroll = max(0, extent - viewport)`) — и так дело `ScrollView`,
  а `effectiveContent*` уже сами делают max с вьюпортом при раскладке,
  так что max в адаптере был чисто вредным: теперь extent — честный
  размер контента. Тесты: `testScrollViewNoOverscrollPastContent`
  (ядро + inline-реплика + overlay-реплика),
  `testDropDownBoxContentHostFitsArbitraryContent` (хост впритык).
- Дефолт `maxContentHeight` вырос 132 → 216 (6 строк по 36px);
  значение задаётся атрибутом, поведение капа то же.

## Совместимость

- Статические `DropDownBox.createHeaderArrow` и protected `headerArrow()`
  оставлены (использует `ExpandablePanel`, могут использовать наследники);
  базовый хедер их больше не встраивает в текст.
- Кастомный `headerIndicator` `ComboBox` — legacy inline-путь без изменений.
- `dropDownWidth`/`dropDownMatchesWidgetWidth`/`dropDownSameWidth`/
  `autoDropDownWidth`, режимы `INLINE`/`OVERLAY`, события, XML — без изменений.
- `Button` без включённого шеврона рисует текст как раньше (центровка,
  measure без слота) — регресса обычных кнопок нет.

## Тесты

- `testDropDownModernChrome` (`BasicControlsSelfTest`, вне агрегата —
  гонялся временным `basicControlsSelfTestTmp`): высота 36, паддинг 12,
  чистый текст + флаги шеврона, штрихи шеврона в закрытом хедере, флип
  шеврона при открытии, инсет-пилюля selected, строки 28px/влево/12px
  (флаги + геометрия текста), без рамок строк, reserved-скролл,
  точный низ списка без пустоты, зазор попапа 6px, хром `DropDownBox`.
- Обновлены `testComboBoxAndDropDownBoxContracts`: хедер без `" ?"`.
