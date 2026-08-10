# UniGUI Widget Design Checklist

Документ фиксирует целевой дизайн и backlog реализации для типов из
'common/src/main/java/dev/sixik/unigui/widgets/'.

Основа документа:

- текущий каталог root-виджетов, без подпакета 'widgets/render';
- 'WIDGETS_CONTRACT.md' как общий контракт событий, renderer API и widget API;
- внешний responsibility contract 'UNIGUI_WIDGET_RESPONSIBILITY_CONTRACT.md';
- быстрая сверка текущей реализации с целевыми ответственностями.

## Легенда

- [x] Уже в целом соответствует роли.
- [ ] Нужно реализовать, перепроверить или явно задокументировать.
- Статус OK — ответственность в основном совпадает.
- Статус PARTIAL — базовая идея есть, но часть контракта отсутствует.
- Статус GAP — текущая реализация противоречит целевой роли или почти отсутствует.
- Статус ALIAS — тип сейчас почти только alias/facade и требует решения: оставить как alias или развести ответственность.
- Статус HELPER — служебный тип, не самостоятельный визуальный виджет.

## Global Definition of Done для каждого виджета

- [ ] Публичные действия пользователя идут через routed events и EventSubscription, а не через прямой Runnable/Consumer API.
- [ ] Визуальные виджеты имеют renderer/state в 'widgets/render' и default renderer через WidgetsRender.
- [ ] Layout-виджеты не рисуют лишнюю визуальную логику, кроме явно заявленной panel/chrome роли.
- [ ] Disabled/hidden/collapsed/focused/hovered состояния ведут себя единообразно.
- [ ] Pointer, keyboard и wheel input не конфликтуют с event capture/bubble моделью.
- [ ] Валидация input-значений не ломает редактирование промежуточных состояний.
- [ ] Размеры, padding, gaps, min/max и alignment идут через Layout V3 constraints/style.
- [ ] Overlay-контент проходит через OverlayLayer, имеет корректный z-order и outside-click поведение.
- [ ] Для complex state есть snapshot/restore или explicit state API, если это нужно пользователю.
- [ ] Есть demo/test coverage в dev.sixik.unigui.tests.TestCommands или UniGuiDemo.
- [ ] Javadoc или design note объясняет отличие от похожих типов.

---

# Layout-контейнеры

## Box — статус PARTIAL

Целевая роль по контракту: однодетный контейнер с padding/alignment, без лишней логики.

Текущее состояние: Box является визуальной panel-обёрткой поверх PanelWidget и поддерживает много детей, background, border, texture, radius, theme.

- [ ] Решить: Box остаётся visual multi-child panel или приводится к однодетному container contract.
- [ ] Если Box остаётся visual panel, обновить responsibility contract и явно отделить Box от Border/PanelWidget.
- [ ] Если Box становится single-child, добавить child/content API и запретить произвольный multi-child сценарий.
- [x] Сохранить renderer/state для background/border/texture.
- [ ] Добавить тесты на padding/alignment/children semantics после выбранного решения.

## LinearBox — статус OK

Целевая роль: базовая линейная раскладка по Orientation, без wrap.

- [x] Использовать Orientation для horizontal/vertical направления.
- [x] Раскладывать детей через flex adapter без wrap.
- [x] Поддерживать spacing через rowGap/columnGap.
- [ ] Проверить flex-grow/flex-shrink сценарии на mixed fixed/auto children.
- [ ] Добавить demo cases для horizontal и vertical overflow.

## HBox — статус OK

Целевая роль: синтаксический сахар над LinearBox с горизонтальной ориентацией.

- [x] Наследоваться от LinearBox.
- [x] Фиксировать Orientation.HORIZONTAL в конструкторе.
- [ ] Не добавлять отдельный layout/render алгоритм.
- [ ] Добавить smoke-demo с разными flex-grow/flex-shrink детьми.

## VBox — статус OK

Целевая роль: синтаксический сахар над LinearBox с вертикальной ориентацией.

- [x] Наследоваться от LinearBox.
- [x] Фиксировать Orientation.VERTICAL в конструкторе.
- [ ] Не добавлять отдельный layout/render алгоритм.
- [ ] Добавить smoke-demo с nested VBox/HBox.

## Orientation — статус HELPER/OK

Целевая роль: общий enum оси layout.

- [x] Иметь HORIZONTAL и VERTICAL.
- [ ] Использовать один enum в layout, scrollbar, separator, split panel, wrap panel.
- [ ] Не плодить альтернативные axis enum без причины.

## StackPanel — статус OK

Целевая роль: дети накладываются в одной области, z-order по порядку детей.

- [x] Использовать stack layout adapter.
- [x] Учитывать alignment и constraints каждого child.
- [ ] Проверить hit-test/render order при overlapping children.
- [ ] Добавить demo с несколькими overlayed children.

## WrapPanel — статус OK

Целевая роль: линейная раскладка с переносом строк/колонок.

- [x] Поддерживать Orientation.
- [x] Использовать FlexWrap.WRAP.
- [x] Поддерживать spacing и lineSpacing.
- [ ] Проверить перенос при constrained width/height.
- [ ] Добавить demo для horizontal wrap и vertical wrap.

## GridBox — статус PARTIAL

Целевая роль: фиксированная сетка ячеек со span минимум по одной оси.

Текущее состояние: есть columns и spacing, но явного row/column span API не видно.

- [x] Поддерживать фиксированное количество columns.
- [x] Раскладывать детей через grid adapter.
- [ ] Добавить row/column position API или явно описать auto-flow поведение.
- [ ] Реализовать span по колонке и/или строке.
- [ ] Добавить constraints для min/preferred cell size.
- [ ] Добавить demo с span item.

## PanelWidget — статус OK

Целевая роль: базовая multi-child panel с background/padding/layout hosting без специфичного layout алгоритма.

- [x] Владеть children и deferred mutations.
- [x] Уметь add/insert/remove/clear children.
- [x] Мерить и размещать обычных/absolute детей.
- [ ] Документировать, что PanelWidget — общий container base, не polished visual widget.
- [ ] Проверить style/padding/overflow clipping для child render.

## Border — статус GAP

Целевая роль: однодетный декоратор: рамка + фон вокруг ребёнка.

Текущее состояние: Border — standalone visual primitive без child/content.

- [ ] Решить: Border должен стать decorator или остаться primitive.
- [ ] Если decorator — добавить content child, padding/inset и child arrange.
- [ ] Добавить background API, если контракт требует фон вокруг ребёнка.
- [x] Сохранить renderer/state для толщины/радиуса/цвета.
- [ ] Добавить тест: Border не меняет layout child кроме border/padding inset.

## Separator — статус OK

Целевая роль: тонкая линия-разделитель с orientation, минимальный input.

- [x] Поддерживать Orientation.
- [x] Иметь renderer/state.
- [ ] Убедиться, что separator не перехватывает pointer input без необходимости.
- [ ] Добавить default sizes для horizontal/vertical.

## SplitPanel — статус OK

Целевая роль: два региона с пользовательски изменяемой границей.

- [x] Поддерживать first/second child.
- [x] Поддерживать Orientation.
- [x] Поддерживать splitRatio и min sizes.
- [x] Использовать Splitter как внутренний draggable handle.
- [ ] Проверить keyboard accessibility для split move.
- [ ] Добавить demo с minFirst/minSecond constraints.

## Splitter — статус OK

Целевая роль: draggable-разделитель внутри SplitPanel.

- [x] Держать ссылку на owner SplitPanel.
- [x] Рисовать состояние через renderer/state.
- [x] Выдавать resize cursor по orientation.
- [ ] Проверить pointer capture/release на drag cancel.
- [ ] Не использовать Splitter вне SplitPanel без явного standalone contract.

## PageView — статус ALIAS/GAP

Целевая роль: один видимый child из набора без анимации и без carousel controls по умолчанию.

Текущее состояние: PageView наследуется от Carousel без отличий.

- [ ] Развести PageView и Carousel.
- [ ] PageView должен владеть pages и selectedIndex без auto-scroll/indicator/prev-next chrome.
- [ ] Добавить API для selection by index и optional swipe.
- [ ] Решить: скрывать inactive pages или unmount/remount.
- [ ] Добавить migration note, если текущий PageView остаётся alias.

## Carousel — статус PARTIAL

Целевая роль: PageView с анимированным/cyclic transition, controls и indicator dots.

Текущее состояние: есть prev/next и indicator text, но нет явной анимации.

- [x] Поддерживать список pages и selectedIndex.
- [x] Поддерживать cyclic selectRelative.
- [ ] Добавить animation transition между pages.
- [ ] Добавить indicator dots mode.
- [ ] Добавить optional auto-scroll timer.
- [ ] Сделать Carousel композицией PageView, если PageView станет базой.

## Accordion — статус OK

Целевая роль: список ExpandablePanel, single-open и multi-open режимы.

- [x] Использовать ExpandablePanel как секции.
- [x] Поддерживать singleOpen true/false.
- [x] Закрывать другие панели при singleOpen.
- [ ] Проверить события ExpandedChangedEvent при programmatic и user changes.
- [ ] Добавить keyboard navigation между headers.

## ExpandablePanel — статус OK

Целевая роль: одна collapsible секция, building block для Accordion.

- [x] Иметь header ToggleButton и contentHost.
- [x] Поддерживать expanded/silentExpanded/toggleExpanded.
- [x] Переключать visibility contentHost.
- [ ] Добавить icon customization вместо fixed triangle text.
- [ ] Проверить nested content mutations.

---

# Базовые контролы

## Button — статус OK

Целевая роль: click/hover/pressed/disabled состояния.

- [x] Поддерживать text/richText.
- [x] Поддерживать pressed и hover-driven style state.
- [x] Disabled блокирует input.
- [x] Публиковать ButtonClickEvent через onClick/click.
- [x] Иметь renderer/state.
- [ ] Проверить keyboard activation Enter/Space, если button должен быть keyboard-first.

## Checkbox — статус PARTIAL

Целевая роль: bool toggle, optional tri-state.

- [x] Наследовать toggle behavior.
- [x] Иметь checkbox render type.
- [ ] Добавить indeterminate state.
- [ ] Добавить tri-state cycle mode: unchecked -> checked -> indeterminate или configurable order.
- [ ] Добавить CheckedChangedEvent или отдельный CheckboxStateChangedEvent для tri-state.
- [ ] Добавить demo для tree partial selection.

## RadioButton — статус OK

Целевая роль: отдельная radio-кнопка, эксклюзивность через RadioGroup.

- [x] Хранить checked visual state.
- [x] Интегрироваться с RadioGroup.
- [x] Не владеть эксклюзивностью самостоятельно при наличии group.
- [x] Поддерживать keyboard activation.
- [ ] Проверить невозможность снять единственный selected radio по случайному повторному клику, если group требует strict selection.

## RadioGroup — статус OK

Целевая роль: владелец взаимоисключающего selection.

- [x] Владеть списком RadioButton.
- [x] Хранить selectedButton/selectedValue.
- [x] Синхронизировать checked state у button.
- [ ] Добавить onSelectionChanged event, если публичный API должен быть event-driven.
- [ ] Добавить option: allowEmptySelection.

## ToggleButton — статус OK

Целевая роль: bool toggle в виде кнопки, не checkbox.

- [x] Наследоваться от Button.
- [x] Поддерживать checked/silentChecked.
- [x] Публиковать CheckedChangedEvent.
- [x] Иметь отдельный ButtonRenderType.TOGGLE_BUTTON.
- [ ] Проверить style state CHECKED vs HOVERED при checked+hovered.

## Slider — статус OK

Целевая роль: continuous/discrete range control с drag и keyboard.

- [x] Поддерживать min/max/value.
- [x] Поддерживать step.
- [x] Pointer drag меняет value.
- [x] Keyboard arrows меняют value.
- [x] Публиковать SliderValueChangedEvent.
- [ ] Добавить vertical orientation, если требуется как общий slider.
- [ ] Добавить page step/home/end, если нужно desktop-like поведение.

## NumberField — статус PARTIAL

Целевая роль: numeric text input с min/max/step и явной validation policy.

- [x] Наследоваться от TextInput.
- [x] Поддерживать range, value, step.
- [x] Clamp значения в setNumberValue.
- [x] Up/Down меняют value на step.
- [ ] Явно задокументировать validation timing: currently sync on accepted/cancelled text input, not blur-only.
- [ ] Добавить blur/commit policy, если нужно редактировать временно invalid text без auto-normalize.
- [ ] Добавить locale-independent decimal policy and negative sign placement.
- [ ] Добавить renderer/state coverage для invalid input state.

## TextField — статус ALIAS/PARTIAL

Целевая роль: single-line text field или chrome-specialization над TextInput.

Текущее состояние: TextField — TextInput с default chrome и TEXT_FIELD render type.

- [x] Наследоваться от TextInput.
- [x] Использовать textField renderer.
- [ ] Явно задокументировать отличие TextField от TextInput.
- [ ] Решить, должен ли TextInput быть abstract/base или публичный generic input.
- [ ] Добавить single-line guarantee и запрет line breaks, если TextField — single-line.

## TextInput — статус PARTIAL

Целевая роль: generic text input/editor base.

- [x] Поддерживать text, placeholder, cursor, selection.
- [x] Поддерживать copy/cut/paste.
- [x] Поддерживать renderer/state.
- [x] Публиковать TextChangedEvent через onTextChanged.
- [ ] Разделить base editor model и public widget API, если TextField должен быть основным public widget.
- [ ] Добавить undo/redo policy или явно указать, что undo отсутствует.
- [ ] Добавить IME/composition support, если требуется desktop text input.
- [ ] Добавить protected hooks для secure subclasses вроде PasswordField.

## PasswordField — статус PARTIAL

Целевая роль: TextField с маскированием и без утечек реального значения в logs/debug/clipboard.

- [x] Маскировать displayText.
- [x] Иметь PASSWORD_FIELD render type.
- [ ] Запретить или настроить copy/cut selected real text.
- [ ] Убедиться, что TextInputState/renderer/debug не получают реальное значение.
- [ ] Добавить secure reveal toggle только если он нужен явно.
- [ ] Добавить tests: clipboard не получает пароль.

## SearchField — статус PARTIAL

Целевая роль: TextField с debounce на изменение и clear icon.

- [x] Иметь placeholder 'Search...'.
- [x] Иметь clear button/clear zone.
- [x] Публиковать SearchSubmittedEvent на Enter.
- [ ] Добавить debounce onSearchChanged или delayed TextChangedEvent facade.
- [ ] Сделать debounce duration configurable.
- [ ] Добавить search icon в renderer/state.
- [ ] Добавить Escape clear или close integration, если используется в popup.

## TimeSpanField — статус PARTIAL

Целевая роль: ввод длительности часы/минуты/секунды.

- [x] Хранить Duration value.
- [x] Форматировать как HH:MM:SS.
- [x] Парсить colon-separated duration.
- [ ] Добавить onValueChanged event.
- [ ] Явно описать allowed formats: SS, MM:SS, HH:MM:SS.
- [ ] Добавить range/min/max, если требуется cooldown/promo windows.
- [ ] Добавить invalid input handling без silent reset в неожиданных случаях.

## DatePicker — статус OK

Целевая роль: выбор календарной даты через Popup.

- [x] Использовать Popup.
- [x] Использовать OverlayLayer attachment.
- [x] Поддерживать selected LocalDate и calendar grid.
- [x] Публиковать DateChangedEvent.
- [ ] Добавить min/max date.
- [ ] Добавить keyboard navigation по календарю.
- [ ] Добавить locale/week-start customization.

## ColorPicker — статус OK

Целевая роль: RGBA color picker с palette/hex/components.

- [x] Поддерживать ColorView/ARGB/RGBA255.
- [x] Иметь HSV и ARGB modes.
- [x] Использовать Popup/OverlayLayer.
- [x] Синхронизировать channel fields и sliders.
- [x] Публиковать ColorChangedEvent.
- [ ] Добавить hex text input, если он ещё не exposed публично.
- [ ] Добавить presets/palette API.

## ComboBox — статус OK

Целевая роль: выбор одного значения из списка строк/rich text.

- [x] Хранить items/richItems.
- [x] Поддерживать selectedIndex/selectedItem.
- [x] Поддерживать inline и overlay dropdown mode.
- [x] Использовать Popup для overlay.
- [x] Публиковать SelectionChangedEvent.
- [ ] Проверить keyboard open/close/up/down semantics.
- [ ] Добавить virtualized options для больших списков.

## DropDownBox — статус ALIAS/GAP

Целевая роль: dropdown с произвольным widget-content в popup, не только список строк.

Текущее состояние: alias-наследник ComboBox.

- [ ] Реализовать content(Widget) API для popup.
- [ ] Оставить ComboBox как specialization поверх DropDownBox или явно наоборот.
- [ ] Убрать дублирование selection list logic между ComboBox/DropDownBox.
- [ ] Добавить renderer/state для header/opened state, если отличается от ComboBox.
- [ ] Добавить demo: dropdown с произвольным TreeView/ColorPicker/custom panel.

---

# Текст

## Text — статус ALIAS/PARTIAL

Целевая роль: короткий одноцелевой text widget или public alias для TextWidget.

- [x] Наследоваться от TextWidget.
- [ ] Явно решить отличие Text от Label.
- [ ] Если Text остаётся alias, указать это в Javadoc и Widgets factory.
- [ ] Если Text — generic display text, Label должен получить accessibility association.

## TextWidget — статус OK

Целевая роль: базовый визуальный text widget.

- [x] Поддерживать String/RichText.
- [x] Поддерживать wrap, alignment, text renderer/state.
- [x] Измерять текст через TextEngine.
- [ ] Явно отделить base class API от public Text/Label/TextBlock/RichTextView.
- [ ] Добавить selectable text, если требуется.

## TextBlock — статус OK/PARTIAL

Целевая роль: многострочный текст с переносом.

- [x] Наследоваться от TextWidget.
- [x] Включать wrap.
- [x] Вертикально выравнивать от START.
- [ ] Явно отделить от RichTextView: plain multiline vs rich multiline.
- [ ] Добавить paragraph spacing, если нужен documentation-like text.

## Label — статус ALIAS/PARTIAL

Целевая роль: короткая подпись, связанная с другим контролом.

Текущее состояние: почти alias TextWidget.

- [x] Наследоваться от TextWidget.
- [ ] Добавить labeledControl/focusTarget API.
- [ ] Click on label должен фокусировать связанный control.
- [ ] Добавить accessibility role/metadata, если система accessibility появится.
- [ ] Явно отделить Label от Text.

## RichTextView — статус PARTIAL

Целевая роль: сегментированный rich text с разными стилями и потенциальными inline widgets.

Текущее состояние: wrap TextWidget с RichText, без inline widget/markdown pipeline.

- [x] Принимать RichText.
- [x] Включать wrap и START vertical alignment.
- [ ] Добавить inline widget/mention/emoji/link model, если нужен Discord-like уровень.
- [ ] Добавить markdown или parser adapter отдельно от render widget.
- [ ] Развести с TextBlock: RichTextView = rich/interactive, TextBlock = plain multiline.
- [ ] Добавить link click/hover events.

---

# Визуальные примитивы

## ImageView — статус PARTIAL

Целевая роль: растровое изображение с fit modes.

Текущее состояние: specialization TextureWidget с imageView renderer, источник всё ещё TextureHandle.

- [x] Наследоваться от TextureWidget.
- [x] Использовать TextureHandle и ImageFit через base.
- [ ] Добавить image/source abstraction, если ImageView должен загружать по resource/path.
- [ ] Убедиться, что fit включает stretch/contain/cover/tile.
- [ ] Явно отделить ImageView от TextureWidget: user-level image vs low-level texture.

## TextureWidget — статус OK

Целевая роль: низкоуровневый texture display с UV/source.

- [x] Поддерживать TextureHandle.
- [x] Поддерживать source UV rect.
- [x] Поддерживать tint, fit, radius.
- [x] Иметь renderer/state.
- [ ] Проверить null texture rendering path.

## Shape — статус PARTIAL

Целевая роль: базовый геометрический primitive, включая arbitrary path по контракту.

Текущее состояние: RECT/ROUNDED_RECT/CIRCLE/LINE; arbitrary path вынесен в Path.

- [x] Поддерживать базовые shapes.
- [x] Поддерживать fill/stroke/strokeWidth/radius.
- [ ] Обновить контракт: arbitrary path — ответственность Path, не Shape.
- [ ] Добавить triangle/ellipse/polygon, если нужно.
- [ ] Убедиться, что renderer batching используется для всех типов.

## Path — статус OK

Целевая роль: VectorPath для линий/Bezier/сложных контуров.

- [x] Владеть VectorPath.
- [x] Поддерживать stroke/fill mode через stroke boolean.
- [x] Поддерживать color и strokeWidth.
- [x] Иметь renderer/state.
- [ ] Добавить fill rule, line cap/join, если VectorPath это поддержит.

## CanvasWidget — статус OK/PARTIAL

Целевая роль: произвольная область отрисовки через callback.

- [x] Хранить список CanvasDrawCallback.
- [x] Вызывать callbacks на render pass.
- [ ] Решить, нужен ли DrawScope вместо raw RenderContext.
- [ ] Добавить callback removal/subscription API.
- [ ] Документировать, что CanvasWidget не управляет layout children.

## CanvasDrawCallback — статус HELPER/OK

Целевая роль: functional interface для CanvasWidget.

- [x] Иметь draw(RenderContext).
- [ ] Рассмотреть draw(DrawScope) для backend-neutral high-level drawing.
- [ ] Не добавлять stateful responsibility.

---

# Индикаторы состояния и обратная связь

## ProgressBar — статус PARTIAL

Целевая роль: determinate и indeterminate progress.

- [x] Поддерживать range/value/progress.
- [x] Иметь renderer/state.
- [ ] Добавить indeterminate mode.
- [ ] Добавить phase/tick animation для indeterminate.
- [ ] Добавить label/value formatting option, если нужен UI.
- [ ] Обновить renderer state под determinate/indeterminate.

## LoadingIndicator — статус OK

Целевая роль: общий loading indicator, Spinner — частный случай.

- [x] Поддерживать Mode.SPINNER, DOTS, BAR.
- [x] Поддерживать running/phase/speed.
- [x] Иметь renderer selection по mode.
- [ ] Проверить semantic отличие LoadingIndicator BAR от ProgressBar indeterminate.
- [ ] Добавить accessibility/status text, если нужно.

## Spinner — статус OK

Целевая роль: конкретный rotating indicator.

- [x] Наследоваться от LoadingIndicator.
- [x] Форсировать Mode.SPINNER.
- [x] Давать fluent API для style/speed/segments/dots/arcs.
- [ ] Не дублировать renderer/state отдельно от LoadingIndicator без причины.

## Toast — статус ALIAS/GAP

Целевая роль: карточка transient notification.

Текущее состояние: наследник NotificationView.

- [ ] Решить: Toast = карточка, NotificationView = host/queue.
- [ ] Если Toast карточка — вынести message/severity/duration/action в Toast.
- [ ] Добавить severity levels: info/success/warning/error.
- [ ] Добавить enter/exit animation hooks.
- [ ] Не делать Toast overlay host, если host role у NotificationView.

## NotificationView — статус PARTIAL

Целевая роль: host/queue для notifications или самостоятельная карточка — надо решить.

Текущее состояние: карточка Box с text/duration/show/hide, implements OverlayHostAware.

- [ ] Развести с Toast по ответственности.
- [ ] Если host — добавить queue, maxVisible, placement, stacking.
- [ ] Если карточка — переименовать/документировать и убрать дублирующий Toast.
- [ ] Добавить severity, actions, dismiss reason event.
- [ ] Добавить auto-hide tick based on duration.

## Tooltip — статус PARTIAL

Целевая роль: hover content с задержкой, через OverlayLayer.

Текущее состояние: Tooltip показывает при anchor.hovered, но delay API не видно.

- [x] Поддерживать anchor.
- [x] Поддерживать text/RichText.
- [x] Позиционировать относительно anchor в host.
- [ ] Добавить showDelay/hideDelay.
- [ ] Интегрировать с OverlayLayer registration lifecycle.
- [ ] Добавить pointer-follow mode, если нужен.

---

# Навигация и overlay

## TabControl — статус PARTIAL

Целевая роль: вкладки + selected content + keyboard navigation.

- [x] Хранить tabs и selectedIndex.
- [x] Скрывать inactive tab slot через Visibility.COLLAPSED.
- [x] Публиковать SelectionChangedEvent.
- [ ] Добавить Left/Right keyboard navigation.
- [ ] Решить и задокументировать hide vs unmount inactive content.
- [ ] Добавить closeable/reorderable tabs, если нужно для docking/editor UI.

## Breadcrumb — статус OK/PARTIAL

Целевая роль: путь по иерархии с selection/click на промежуточный item.

- [x] Хранить BreadcrumbItem list.
- [x] Рендерить buttons и separators.
- [x] Публиковать SelectionChangedEvent.
- [ ] Добавить explicit navigation event with item value/path.
- [ ] Добавить overflow/collapse для длинных paths.
- [ ] Добавить disabled item behavior tests.

## BreadcrumbItem — статус HELPER/OK

Целевая роль: data item для Breadcrumb.

- [x] Хранить text/richText/value/enabled.
- [ ] Добавить optional icon, если breadcrumbs должны быть IDE-like.
- [ ] Не добавлять widget/render logic внутрь item.

## ContextMenu — статус PARTIAL

Целевая роль: меню в точке клика, outside/Escape close, nested submenu.

- [x] Открывать меню в root coordinates через openAt(x, y).
- [x] Закрываться при item click.
- [x] Закрываться outside-click через OverlayLayer.
- [ ] Добавить Escape close.
- [ ] Добавить submenu support.
- [ ] Заменить public Runnable action API на event-driven MenuItemSelectedEvent или command model.
- [ ] Добавить keyboard navigation Up/Down/Enter.

## Popup — статус OK

Целевая роль: базовый overlay-контейнер arbitrary content.

- [x] Поддерживать anchor/content/opened.
- [x] Поддерживать configurable closeOnOutsideClick.
- [x] Размещаться через OverlayLayoutResolver.
- [ ] Добавить placement API: below/above/left/right/center/custom.
- [ ] Добавить Escape close option.
- [ ] Добавить focus trap option for modal-like popups.

## OverlayLayer — статус OK

Целевая роль: корневой host floating content с z-order и hit priority.

- [x] Поддерживать content + overlays.
- [x] Поддерживать overlay z-order.
- [x] Владеть WindowManager.
- [x] Закрывать outside-click popup/window/context menu.
- [x] Рендерить modal scrim.
- [ ] Добавить explicit hit-test priority documentation.
- [ ] Добавить Escape dispatch policy for top overlay.

## OverlayHostAware — статус HELPER/OK

Целевая роль: marker/interface для overlay-aware widgets.

- [x] Использоваться Popup/ContextMenu/Tooltip/WindowWidget/NotificationView.
- [ ] Документировать lifecycle: addOverlay/removeOverlay/arrangeInHost.
- [ ] Не использовать как standalone visual element.

---

# Списки, деревья и таблицы

## VirtualListView — статус OK

Целевая роль: виртуализированный flat list.

- [x] Использовать FixedRowVirtualizer.
- [x] Рендерить visible range + overscan.
- [x] Поддерживать itemFactory.
- [x] Поддерживать selection model.
- [x] Использовать ScrollBar напрямую.
- [ ] Добавить variable row height, если требуется.
- [ ] Добавить item reuse pool, если текущая realized map создаёт лишние widgets.

## VirtualTableView — статус OK

Целевая роль: virtualized table/data-grid.

- [x] Поддерживать columns с width.
- [x] Поддерживать row virtualization + overscan.
- [x] Поддерживать column resize/move.
- [x] Поддерживать sort policy.
- [x] Поддерживать selection и active cell.
- [x] Поддерживать optional editing.
- [ ] Добавить horizontal scrollbar, если contentWidth > viewport.
- [ ] Добавить frozen columns/headers, если нужно editor UI.

## VirtualTableColumn — статус HELPER/OK

Целевая роль: column descriptor.

- [x] Хранить header/richHeader/width.
- [ ] Добавить min/max/resizable/sortable metadata.
- [ ] Не хранить row data внутри column descriptor.

## TreeView — статус OK/PARTIAL

Целевая роль: hierarchical list with expand/collapse.

- [x] Хранить tree nodes.
- [x] Поддерживать expand/collapse.
- [x] Публиковать selection events.
- [ ] Проверить incremental expansion patch vs full rebuild.
- [ ] Добавить batch mode для массовых изменений.
- [ ] Добавить keyboard navigation Left/Right/Up/Down.

## TreeViewNode — статус HELPER/OK

Целевая роль: node data для TreeView.

- [x] Хранить title/value/children/expanded/selected-like state.
- [ ] Не смешивать widget rendering logic в node.
- [ ] Добавить stable id, если tree updates должны быть diffable.

## TreeList — статус OK

Целевая роль: facade над TreeView для path из строк в nodes.

- [x] Наследоваться от TreeView.
- [x] Давать addPath helper.
- [ ] Не дублировать TreeView selection/render logic.
- [ ] Добавить path separator customization, если нужно.

## TreeListPicker — статус GAP

Целевая роль: tree-picker через popup + TreeView.

Текущее состояние: flat ComboBox over List<T>.

- [ ] Реализовать popup + TreeView или переименовать в ListPicker.
- [ ] Поддерживать hierarchical values/path provider.
- [ ] Публиковать selection event with selected value/path.
- [ ] Поддержать filtering/search, если picker будет использоваться в больших trees.
- [ ] Добавить demo вместо текущего flat list example.

---

# Данные и визуализация

## Chart — статус OK

Целевая роль: chart по ряду данных, минимум line/bar.

- [x] Поддерживать Type.LINE и Type.BAR.
- [x] Наследовать common sparkline data/render model.
- [x] Поддерживать bar values, tooltip render hooks, click event.
- [ ] Добавить axis labels/ticks, если Chart должен быть полноценнее Sparkline.
- [ ] Добавить category/time labels.
- [ ] Добавить multiple series, если нужно.

## Sparkline — статус OK

Целевая роль: inline-график без осей/подписей по умолчанию.

- [x] Поддерживать values.
- [x] Поддерживать line/fill/points.
- [x] Поддерживать point renderer/label/tooltip hooks.
- [x] Публиковать SparkPointClickEvent.
- [ ] Убедиться, что default mode визуально минималистичен.
- [ ] Добавить null/NaN gaps policy.

## GraphView — статус PARTIAL/GAP относительно NodeGraph contract

Целевая роль по внешнему контракту: host canvas с pan/zoom, рендерит NodeGraph.

Текущее состояние: отдельный простой graph visualization с nodes/edges, не host для NodeGraph.

- [ ] Решить: GraphView остаётся generic graph visualization или становится NodeGraph host.
- [ ] Если generic visualization — переименовать/документировать отличие от NodeGraph.
- [ ] Если host — добавить NodeGraph model property и pan/zoom viewport.
- [ ] Не дублировать NodeGraph интерактивный редактор.
- [ ] Добавить migration/design note: GraphView vs NodeGraph.

---

# Скролл

## ScrollView — статус OK

Целевая роль: scroll container с clipping content.

- [x] Поддерживать content widget.
- [x] Поддерживать scrollX/scrollY.
- [x] Поддерживать horizontal/vertical ScrollBar.
- [x] Clip content по viewport.
- [x] Wheel scroll.
- [ ] Добавить inertia, если нужна mobile-like прокрутка.
- [ ] Добавить scrollTo child / ensureVisible API.

## ScrollBar — статус OK

Целевая роль: сам track/thumb control.

- [x] Поддерживать Orientation.
- [x] Поддерживать range/value/pageSize/step.
- [x] Pointer drag меняет value.
- [x] Иметь renderer/state.
- [ ] Добавить keyboard support Home/End/PageUp/PageDown.
- [ ] Проверить disabled/visibility behavior.

---

# Окна и Docking

## WindowWidget — статус OK

Целевая роль: floating window with title, drag, resize, close/minimize.

- [x] Поддерживать title/content.
- [x] Поддерживать drag по header.
- [x] Поддерживать resize handles.
- [x] Поддерживать close/open state.
- [x] Публиковать move/resize/open/close events.
- [ ] Проверить minimize behavior, если контракт требует именно minimize.
- [ ] Добавить modal/fixed modal docs.

## WindowManager — статус OK

Целевая роль: z-order и lifecycle floating windows.

- [x] Владеть registered windows.
- [x] Владеть activeWindow.
- [x] Bring-to-front через OverlayLayer.
- [x] Учитывать modal stack.
- [ ] Добавить explicit close-all/minimize-all policy, если нужно.

## DockPanel — статус GAP относительно внешнего Docking contract

Целевая роль по внешнему контракту: root docking widget.

Текущее состояние: обычный dock-layout container по DockSide.

- [ ] Переименовать или задокументировать: DockPanel = dock layout, DockingRoot = docking system root.
- [ ] Если DockPanel должен стать root docking — перенести/обернуть DockingRoot responsibility.
- [x] Для текущей роли сохранить DockSide layout behavior.
- [ ] Обновить contract, чтобы не путать DockPanel и DockingRoot.

## DockArea — статус HELPER/PARTIAL

Целевая роль: drop region for docking.

- [x] Иметь LEFT/RIGHT/TOP/BOTTOM/CENTER.
- [x] Иметь TAB/FLOAT extensions.
- [x] Маппить splitOrientation.
- [ ] Согласовать с DockSide: DockArea для docking drop, DockSide для layout DockPanel.
- [ ] Документировать CENTER vs TAB.

## DockPane — статус OK/PARTIAL

Целевая роль: concrete docked pane with content.

- [x] Хранить id/title/richTitle/content.
- [x] Поддерживать kind DOCUMENT/TOOL.
- [x] Поддерживать closable/dirty/pinned/autoHide metadata.
- [ ] Согласовать DockPaneKind с контрактом, где упоминались ordinary/tab-group/floating.
- [ ] Добавить lifecycle events для close/activate, если они не только в DockingRoot.

## DockPaneKind — статус HELPER/PARTIAL

Целевая роль: enum роли/типа pane.

- [x] Иметь DOCUMENT/TOOL.
- [ ] Решить, нужны ли TAB_GROUP/FLOATING или это состояние DockNode/WindowWidget.
- [ ] Обновить responsibility contract под текущее DOCUMENT/TOOL решение.

## DockNode — статус OK

Целевая роль: node дерева docking layout: split или leaf with panes.

- [x] Поддерживать Kind.LEAF/SPLIT.
- [x] Хранить panes и selectedIndex для leaf.
- [x] Хранить orientation/splitRatio/first/second для split.
- [ ] Добавить stable id restore policy, если tree diff/debug требует постоянных id.
- [ ] Проверить compact/remove behavior после closing panes.

## DockSide — статус HELPER/OK

Целевая роль: enum сторон для DockPanel layout.

- [x] Иметь LEFT/RIGHT/TOP/BOTTOM.
- [ ] Не использовать для docking drop zones, где нужен DockArea.
- [ ] Добавить CENTER только если DockPanel layout действительно его поддерживает.

## DockSplitOrientation — статус HELPER/OK

Целевая роль: split orientation for DockNode.

- [x] Иметь HORIZONTAL/VERTICAL.
- [x] Конвертировать в widget Orientation.
- [ ] Убедиться, что horizontal означает left/right split, vertical означает top/bottom split во всех местах.

## DockingRoot — статус OK

Целевая роль: root container для docking системы.

- [x] Владеть DockingManager.
- [x] Владеть DockDragController.
- [x] Рендерить dock root/panes/tabs/split handles/drop preview.
- [x] Поддерживать addDocument/addToolPane/splitPane/tabPane/selectPane.
- [x] Поддерживать restoreLayout.
- [ ] Сократить ответственность, если DockingRoot становится слишком большим: tab overflow, split drag, render chrome можно вынести.
- [ ] Добавить design note: DockingRoot = visual/root host, DockingManager = tree mutation logic.

## DockingManager — статус OK

Целевая роль: логика docking tree mutation/restore.

- [x] Добавлять/разделять/табать panes.
- [x] Закрывать/select panes.
- [x] Restore snapshot по pane registry.
- [ ] Проверить, что manager не содержит transient drag state.
- [ ] Добавить unit tests на split/tab/close/restore edge cases.

## DockDragController — статус OK

Целевая роль: transient drag state отдельно от persistent tree.

- [x] Владеть active drag/preview intent.
- [x] Begin/move/end drag.
- [x] Диспетчеризовать preview change events через DockingRoot.
- [ ] Проверить cancellation path when source pane disappears.
- [ ] Добавить tests на no-op self drop.

## DockDropIntent — статус OK

Целевая роль: snapshot будущего drop.

- [x] Использовать record.
- [x] Хранить source/target/area/preview/root coordinates.
- [x] Иметь none/floating/of helpers.
- [ ] Документировать semantic: area TAB/CENTER/FLOAT.

## DockLayoutSnapshot — статус OK

Целевая роль: сериализуемый snapshot layout tree.

- [x] Хранить root и activePaneId.
- [x] Node snapshot хранит kind/orientation/splitRatio/paneIds/selectedPaneId/children.
- [ ] Добавить version field, если snapshot будет стабильным file format.
- [ ] Добавить validation helpers.

## DockLayoutSnapshotCodec — статус GAP/PARTIAL

Целевая роль: codec должен переиспользовать FieldCodec infrastructure.

Текущее состояние: custom string format DLS1 с Base64.

- [x] Иметь encode/decode.
- [ ] Переписать на FieldCodec infrastructure или обновить контракт, что DockLayoutSnapshotCodec имеет собственный compact format.
- [ ] Добавить versioning/migration policy.
- [ ] Добавить tests на corrupted input, unicode pane ids, nested splits.

---

# Node Graph

## NodeGraph — статус PARTIAL/GAP относительно data-model contract

Целевая роль по внешнему контракту: data model без знания о renderer.

Текущее состояние: полноценный интерактивный WidgetBase с render/input/drag/pan/zoom/selection.

- [ ] Решить архитектуру: NodeGraph = widget editor или NodeGraphModel = data model + NodeGraphView = widget.
- [ ] Если текущий NodeGraph остаётся widget, обновить contract и переименовать data model expectation.
- [ ] Вынести pure model часть в отдельный class, если нужен clean MVC.
- [x] Поддерживать items/connections/viewport/selection/connection policy.
- [x] Поддерживать snapshot/restore.
- [ ] Убедиться, что renderer получает только state, а не widget internals.
- [ ] Добавить tests на connection validation, dragging, lasso, snapshot restore.

## NodeGraphItem — статус OK

Целевая роль: node graph item plus serializable state.

- [x] Хранить id/content/ports/position/size flags.
- [x] Поддерживать selectable/movable/resizable/visible.
- [x] Invalidate owner on mutation.
- [ ] Явно отделить runtime Widget content от snapshot contentType.
- [ ] Добавить metadata map, если item fields/values нужны по контракту.

## NodeGraphItemSnapshot — статус HELPER/OK

Целевая роль: serializable item state.

- [x] Хранить id/contentType/x/y/width/height/flags/ports.
- [ ] Добавить custom field values, если nodes должны сохранять настройки.
- [ ] Добавить schema/version, если snapshot public.

## NodeGraphPort — статус OK/PARTIAL

Целевая роль: pin/port descriptor.

- [x] Хранить id/kind/side/offset/type/visibility/enabled.
- [ ] Согласовать терминологию Kind/Side: сейчас Kind = INPUT/OUTPUT/BIDIRECTIONAL, Side = LEFT/RIGHT/TOP/BOTTOM.
- [ ] Если нужен Blueprint EXEC/DATA, добавить отдельный dataKind/flowKind.
- [ ] Добавить port label/tooltip metadata.

## NodeGraphPortKind — статус HELPER/GAP относительно внешнего contract

Целевая роль по контракту: EXEC/DATA.

Текущее состояние: INPUT/OUTPUT/BIDIRECTIONAL.

- [ ] Решить: переименовать текущий Kind в Direction или PortDirection.
- [ ] Добавить PortDataKind EXEC/DATA, если нужно Blueprint поведение.
- [ ] Обновить connection policy под новую модель.

## NodeGraphPortSide — статус HELPER/GAP относительно внешнего contract

Целевая роль по контракту: INPUT/OUTPUT.

Текущее состояние: LEFT/RIGHT/TOP/BOTTOM placement side.

- [ ] Решить: оставить Side как placement side и обновить контракт.
- [ ] Если нужен input/output side, использовать отдельный Direction enum.
- [ ] Проверить renderer hit-test для всех four sides.

## NodeGraphPortRef — статус HELPER/OK

Целевая роль: lightweight ref itemId+portId.

- [x] Использовать record.
- [x] Хранить itemId и portId.
- [ ] Добавить empty/isValid helpers, если они используются повсеместно.

## NodeGraphPortSnapshot — статус HELPER/OK

Целевая роль: serializable port snapshot.

- [x] Хранить id/kind/side/offset/type/visible/enabled.
- [ ] Обновить fields после решения Kind/Side терминологии.
- [ ] Добавить label metadata при необходимости.

## NodeGraphConnection — статус OK

Целевая роль: connection between two port refs.

- [x] Хранить id/from/to.
- [x] Поддерживать selected/enabled/type.
- [x] Invalidate owner on mutation.
- [ ] Добавить connection style metadata, если renderer должен различать exec/data.

## NodeGraphConnectionPolicy — статус OK/PARTIAL

Целевая роль: правила совместимости connections.

- [x] Иметь functional interface validate(...).
- [x] Default policy проверяет missing/self/disabled/type mismatch/duplicate.
- [ ] Обновить под EXEC/DATA или Direction/DataKind split.
- [ ] Добавить implicit conversion policy hook, если нужен int->float style.

## NodeGraphConnectionSnapshot — статус HELPER/OK

Целевая роль: serializable connection state.

- [x] Хранить id/from/to/enabled/type.
- [ ] Добавить selected only if selection should persist.
- [ ] Добавить style/metadata, если нужно.

## NodeGraphConnectionValidation — статус HELPER/OK

Целевая роль: validation result.

- [x] Хранить valid и reason.
- [ ] Добавить severity/code enum, если UI подсветка должна различать причины.
- [ ] Использовать accepted/invalid factory consistently.

## NodeGraphSelectionMode — статус HELPER/OK

Целевая роль: single/multiple selection mode.

- [x] Иметь selection mode enum.
- [ ] Проверить keepOnlyFirstSelection при switch multiple -> single.

## NodeGraphSnapshot — статус HELPER/OK

Целевая роль: full graph snapshot.

- [x] Хранить viewportX/viewportY/zoom/items/connections/nextConnectionId.
- [ ] Добавить version/schema.
- [ ] Добавить validation of dangling connections.

## NodeGraphViewport — статус HELPER/OK

Целевая роль: camera state.

- [x] Хранить x/y/zoom.
- [ ] Использовать повсеместно вместо разрозненных viewportX/Y/zoom, если нужен immutable state API.
- [ ] Добавить clamp/sanitize factory.

## NodeGraphWidgetResolver — статус HELPER/OK

Целевая роль: mapping snapshot item -> Widget content.

- [x] Использоваться в restoreSnapshot.
- [ ] Возможно сменить signature на resolve(NodeGraphItemSnapshot snapshot), чтобы resolver видел все metadata.
- [ ] Добавить fallback widget factory для missing contentType.

---

# Cached subtree

## CachedSubtreeWidget — статус OK

Целевая роль: FBO/render-to-texture cache for widget subtree.

- [x] Хранить content.
- [x] Render content to texture через WidgetTextureRenderer.
- [x] Re-render only on dirty/resize/backend/options/no texture.
- [x] Хранить cache stats and miss reason.
- [x] Рисовать cached texture через renderer/state.
- [ ] Проверить invalidation propagation от child subtree.
- [ ] Добавить cache policy: max size, disabled cache, debug overlay toggle.

## CachedSubtreeStats — статус HELPER/OK

Целевая роль: counters for debug overlay.

- [x] Хранить renderCalls/cacheHits/cacheMisses/textureRenders/size/lastMissReason.
- [x] Иметь hitRate.
- [ ] Добавить reset snapshot interval helper, если debug overlay считает окна времени.

## CachedSubtreeMissReason — статус HELPER/OK

Целевая роль: enum причин cache miss.

- [x] Иметь NONE/NO_TEXTURE/MANUAL_DIRTY/RESIZED/TARGET_OPTIONS_CHANGED/BACKEND_CHANGED/CONTENT_INVALIDATED.
- [ ] Согласовать имена с внешним контрактом: FIRST_RENDER/SIZE_CHANGED/FORCED_REFRESH/CONTENT_CHANGED.
- [ ] Использовать reason в debug UI consistently.

---

# Базовые и служебные типы

## View — статус PARTIAL/GAP

Целевая роль по контракту: нужно явно определить отличие View от Widget.

Текущее состояние: styled card/container with title and VBox content.

- [ ] Переименовать, если это не base View abstraction, а titled card/panel.
- [ ] Если View — base abstraction, вынести current implementation в TitledView/CardView.
- [ ] Документировать отличие от Box/PanelWidget.
- [ ] Добавить factory naming that reflects actual role.

## Widgets — статус OK

Целевая роль: factory entry points for built-in widgets.

- [x] Быть final utility class.
- [x] Предоставлять static factories.
- [ ] Проверить coverage всех public widget classes.
- [ ] Не хранить mutable global registry, если это только factory.
- [ ] Добавить factories для новых/разведённых типов после рефакторинга.

---

# Приоритетный backlog рефакторинга

## P0 — явно противоречит design contract

- [ ] Развести DropDownBox и ComboBox.
- [ ] Развести Toast и NotificationView.
- [ ] Развести PageView и Carousel.
- [ ] Решить Box vs Border vs PanelWidget ответственность.
- [ ] Решить NodeGraph как widget vs data model split.
- [ ] Исправить TreeListPicker: tree-picker или переименование.

## P1 — недостающие user-facing возможности

- [ ] Checkbox indeterminate/tri-state.
- [ ] ProgressBar indeterminate.
- [ ] SearchField debounce.
- [ ] TabControl Left/Right keyboard navigation.
- [ ] ContextMenu Escape close, keyboard navigation и submenu.
- [ ] PasswordField secure clipboard/cut/copy policy.

## P2 — документация и согласование терминов

- [ ] Text/TextWidget/TextBlock/Label/RichTextView — явно развести роли.
- [ ] TextField/TextInput — явно развести public control и base editor.
- [ ] DockPanel vs DockingRoot — обновить contract names.
- [ ] DockArea vs DockSide — закрепить разницу.
- [ ] NodeGraphPortKind/NodeGraphPortSide — переименовать или обновить contract.
- [ ] DockLayoutSnapshotCodec — FieldCodec или documented custom format.

## P3 — улучшения качества

- [ ] Добавить demo/test block для каждого виджета в TestCommands.
- [ ] Добавить Javadocs для alias/facade/helper типов.
- [ ] Добавить edge-case tests для overlay z-order/outside-click/focus.
- [ ] Добавить snapshot/restore tests для Docking и NodeGraph.
- [ ] Добавить keyboard accessibility для всех interactive widgets.
