# ScrollBar / ScrollView refresh (unigui_2, шаг 1)

Первый виджет визуального рефреша: modern overlay-полосы с hover-анимацией
и честное разделение зон контента и полос.

## Контракт поведения

- **Overlay default.** `ScrollView.overlayScrollBars == true`: полосы плавают
  поверх контента у края вьюпорта, контент занимает всю ширину/высоту
  (резерв места = 0). `overlayScrollBars(false)` возвращает классику:
  резерв `scrollbarSize + scrollbarGap`, контент сужается.
- **Зоны.** Overlay: вертикальная полоса — `x = right - overlayInset -
  scrollbarSize`, на всю высоту; горизонтальная — `y = bottom - inset - size`,
  ширина минус угол под вертикальную. `overlayInset` default 3px.
  Хит-зона = layout-полоса (стабильна); анимируется только визуал.
- **Тамб.** Покой 3px (`idleThickness`), hover/drag 6px (`hoverThickness`),
  якорь к внешнему краю (растёт внутрь вьюпорта). Переход 0.15s CUBIC_OUT
  через `animateParameter` с ретаргетом + `animateColor` тамба к
  `thumbHoverColor`. Нейтральные серые дефолты вместо legacy-циана
  (тема по-прежнему побеждает через `TRACK_COLOR` / `THUMB_COLOR` /
  `THUMB_COLOR+HOVERED`).
- **Tick.** Анимации едут в `ScrollBar.tick`; `ScrollView.tick` тикает только
  видимые полосы. Скрытая полоса догоняет плавно при показе (ретаргет от
  текущего значения).

## Совместимость

- Поведение по умолчанию изменилось (overlay вместо резерва 14px):
  контент стал шире. Prod-код с ручным вычитанием (`DEFAULT_SIZE + gap`,
  как в ISF-каталоге) получит ~7px запаса — проверить сетки, либо
  `overlayScrollBars(false)` на конкретный ScrollView.
- `ScrollBar.DEFAULT_GAP` не менялся. `preferredThickness`/`scrollbarSize`
  теперь означают хит-полосу, не визуал.

## Тесты

- `testOverlayScrollBarZones`, `testScrollBarHoverExpandsThumb`
  (BasicControlsSelfTest — файл выключен из агрегата из-за пре-existing
  падения TextArea, проверено ручным прогоном).
- Reserved-геометрия: `LayoutV3SelfTest` переведён на явный
  `overlayScrollBars(false)`, старый контракт покрыт.
