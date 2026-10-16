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
