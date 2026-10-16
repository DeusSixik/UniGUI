# TextField / TextInput refresh (unigui_2, шаг 2)

Модерн-хром однострочного поля: видимый фокус, hover-рамка, мигающая каретка,
полупрозрачное выделение, контрольная высота и честные паддинги.

## Контракт поведения

- **Хром по умолчанию** (`enableDefaultTextInputChrome`, только `TextField`
  и наследники; голый `TextInput` без хрома как раньше): фон `#14161a`
  (`--bg-0`), бордер subtle `#2e333b`-эквивалент, радиус 8.
- **Высота** — 36px (`DEFAULT_FIELD_HEIGHT`) вместо legacy 18px.
  `TextArea` не затронут (свой `measure`).
- **Паддинги** — `TEXT_PADDING` 4 → 12px (`--pad-x`). Влияет на
  `indexAtLocalX`/каретку: тесты пересчитаны (22px вместо 14px, drag 30px).
- **Состояния рамки** (приоритет): focused → `focusBorderColor` (акцент,
  темится из `ACCENT_COLOR`) + внешнее кольцо (`focusRingColor`, outset 2px,
  ширина 2px); hover без фокуса → `hoverBorderColor`; иначе базовая.
  Раньше фокус был невидим, хотя тема уже описывала `FOCUSED`-акцент.
- **Каретка**: 2px × высота текста (было 1px × весь вьюпорт), мигание 1с/50%
  через `tick`, инвалидация только на фронтах. Сброс фазы при фокусе,
  вводе и движении курсора. Без тиков рисуется сплошной (бэксовместимо).
- **Выделение**: `selectionColor` (акцент 35% alpha) вместо сплошной заливки
  цветом каретки. Отдельного theme-ключа нет — часть хрома, не палитры.
- Рендер переведён на garbage-free перегрузки (`roundedRectFilled/Stroke`,
  `rectFilled`).

## Крестик очистки SearchField

Был буквой `x` (`RichText.plain("x")` — зависит от шрифта, выглядит
триггером с буквой). Стал два штриха крестом (`renderClearGlyph`),
центрация в зоне очистки, цвет как раньше (текст в hover, плейсхолдер).
Хит-зона, паддинг текста и hover-пилюля без изменений.
Тест: `testPasswordAndSearchFields` (нет lone-`"x"`, ровно 2 LINE справа).

## Текстура фона не хоронится хром-заливкой (ISF-поиск)

Кейс: `TextField` с nine-slice текстурой (`boxVisualEnabled(true)`,
`backgroundVisible(true)`) — плоская хром-заливка `renderTextInput`
рисовалась поверх и хоронила текстуру под непрозрачным цветом
(регресс refresh-хрома: раньше заливки не было).

Фикс: заливка пропускается, когда поверхность рисует собственный фон
(`surfaceDrawsBackgroundVisual`: TEXTURE с текстурой / SHADER с шейдером,
с учётом явного `backgroundKind`). Без surface-визуала заливка остаётся
единственным фоном; обычное поле без текстуры рисуется как раньше.
Тест: `testTexturedFieldBackgroundNotBuried` (текстура есть, хром-заливка
ровно одна и до текстуры; плоское поле с заливкой).

## Фокус-кольцо не воскресает при выключенной теме (ISF-поиск)

Симптом: у поиска с `themeEnabled(false)` и прозрачным кольцом синяя
обводка возвращалась — `TextInput.applyTheme` пересчитывал
`focusRingColor` из акцента каждый рендер, не глядя на флаг (в отличие
от `SurfaceWidget.applyTheme`, где early-return был).

Фикс: `TextInput.applyTheme` уважает `themeEnabled()` (early-return,
как в поверхности). ISF-сторона: `search.focusRingColor().set(0,0,0,0)`
рядом с остальным кастомным стайлингом.
Тест: `testThemeDisabledFieldKeepsCustomFocusRing`.

## Настраиваемый отступ текста от краёв (ISF-поиск)

Симптом: текст в поиске сидел далеко от краёв — горизонтальный отступ
был захардкожен (`leftTextPadding/rightTextPadding = max(12, layout)`):
layout-padding мог только расширять, но не сужать.

API (в `TextInput`, наследники `TextField/SearchField/PasswordField/
NumberField/TimeSpanField` переопределяют для fluent-возврата):
- `textPaddingX(v)` — симметрично слева/справа (шорткат для кейса
  «сжать воздух»: `search.textPaddingX(6.0f)`);
- `textPaddingLeft(v)` / `textPaddingRight(v)` — по отдельности.
Отрицательные клампятся к 0; бесконечные — к дефолту 12.
XML: `textPaddingX/textPaddingLeft/textPaddingRight` (аннотации,
подхватываются наследниками автоматически).

Семантика: дефолт без изменений (`max(12, layout)`); после первого
явного вызова layout-подъём отключается и вьюпорт/каретка/хит-тест
используют ровно заданные значения (флаг `explicitTextPadding`).
`SearchField` сохраняет clear-зону поверх правого отступа.
Тест: `testTextFieldPaddingMovesText` (TEXT в bounds+padding, явный
выигрывает у layout 30, асимметрия).

## Совместимость

- `PasswordField` / `SearchField` наследуют новый вид автоматически.
- Явные цвета/рамки пользователя не затираются (только дефолты конструктора).
- `TextArea` — свой measure и скролл, не затронут.

## Тесты

- `testTextFieldModernChrome`, `testTextFieldFocusRingAndBorder`,
  `testTextFieldCaretBlink` (BasicControlsSelfTest — файл выключен из
  агрегата из-за пре-existing падения TextArea, проверено ручным прогоном).
- Пересчитаны `testTextInputClippingMetricsAndSelection` (каретка 22px,
  drag-координаты) — падений от паддинга 12 больше нет.
