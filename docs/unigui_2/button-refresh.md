# Button / HoldButton refresh (unigui_2)

Видимый клавиатурный фокус у кнопок и честный прогресс удержания.
Весь рендер — в классах виджетов (как кольцо у `TextInput`), тема только
отдаёт цвета через `ACCENT_COLOR` / `BORDER_COLOR/FOCUSED`.

## Button: фокус-кольцо

Было: кнопка `focusable(true)` и активируется с клавиатуры, но
`styleState()` знал только pressed → hovered/normal — табом ходишь
вслепую. Тема при этом уже описывала `FOCUSED`-акцент, кнопка его
не читала.

Стало:
- `focused`-флаг из `FocusGained/LostEvent` (как у `TextInput`);
- `styleState()`: pressed → focused → hovered/normal, бордер в фокусе
  берётся из темы;
- `renderFocusRing`: кольцо outset/width 2px поверх текста, цвет —
  акцент с 30% alpha, гасится прозрачным `focusRingColor`.
- Кольцо по умолчанию выключено (`focusRingVisible`, default false):
  обычный фокус виден по акцентной рамке, кольцо — opt-in для мест
  с нужным более заметным индикатором. XML: `focusRingVisible`.
- Наследники с собственным `renderContent` (`Checkbox`, `RadioButton`,
  `ToggleSwitch`) кольцо не получают — у них свой индикаторный визуал;
  `ToggleButton`/`ToolButton` наследуют автоматически.

Тест: `testButtonFocusRing` (нет кольца → в фокусе по умолчанию тихо →
opt-in рисует ореол снаружи границ → blur гасит).

## HoldButton: прогресс по радиусу

Было: плоский `draw.rect` от края до края — острые углы вылезали за
скруглённый фон и перекрывали рамку слева.

Стало (`renderHoldProgress`): заливка `roundedRect` с радиусом кнопки
минус инсет бордера; инсет = `borderWidth` при видимой рамке.
Частичный прогресс — круглые левые углы + прямой правый край
(дорисовка `rect` поверх правой половины скругления); почти полный —
полное скругление. Переведено на garbage-free `roundedRectFilled` /
`rectFilled`.

Тест: `progressFillFollowsRadiusAndBorderInset` в `HoldButtonSelfTest`
(тип команды, инсет, половина ширины, радиус минус бордер; перед
ассёртами тема прогревается тиками до устоявшихся radius/borderWidth).

## SurfaceWidget: пачка текстур

Было: одна фоновая текстура (`backgroundTexture`). Нужно: слои поверх
базы — иконка-подложка, градиент, декор — без custom renderer'а.

Стало: `overlayTexture(handle|layer)` — ленивый список `TextureLayer`
(текстура + fit + UV source + tint + slice + visible). Рендерятся после
базового фона в порядке добавления через общий `renderTextureLayer`
(та же геометрия, что у базы: nine-slice или placement+tint). Ноль
слоёв — ноль накладных. `removeOverlayTexture` / `clearOverlayTextures`
отписывают live-колбэки слоёв.

Тест: в `SurfaceVisualSelfTest` (порядок base→first→second, пустые слои
пропускаются, clear оставляет только базу).

## Button: явные hover/pressed фоны

Было: hover/pressed красила только тема. Нужно: просто задать заливку
или текстуру на состояние без возни со стилями.

Стало: `hoverBackground/pressedBackground` (ленивые live-цвета) и
`hoverTexture/pressedTexture` (ленивые `TextureLayer`). Заданный фон
побеждает тему для своего состояния; приоритет pressed > hover > normal;
внутри состояния текстура поверх заливки. Рендер — `renderStateBackground`
поверх базового surface (переопределение `renderSurface`, не `renderContent`:
наследники со своим контентом получают бесплатно, текст всегда сверху).
Сброс — `clearHoverBackground/clearPressedBackground/clearHoverTexture/
clearPressedTexture`. XML: `hoverBackground/pressedBackground/hoverTexture/
pressedTexture` (ручная регистрация в `button()`: `box()` читает аннотации
только с `Box`).

Тест: `testButtonStateBackgrounds` (база в покое → hover-заливка поверх →
pressed бьёт hover → clear возвращает базу → текстура поверх заливки).

## ToggleSwitch: бегунок по прогрессу

Прогресс переключения уже анимировался (`switchProgress`, 0.16 c), но
бегунок читал `checked()` и прыгал в конец сразу, а дорожка меняла цвет
скачком.

Стало: `thumbX = padding + switchProgress * travel`, цвет дорожки —
линейная смесь unchecked → checked по тому же прогрессу
(`c = off + (on - off) * p`). Середина анимации показывает бегунок
между краями.

Тест: `testToggleCheckboxProgressAndNumberField` сравнивает X круга
до клика и на полукадре.

## Отложено

- Модерн-дефолты размеров кнопок (36/12/8 против текущих 18/8/5).
- ToggleButton: checked+hovered оттенок. Императивный
  `background().set` убран — checked-фон кнопки отдаёт тема через
  `WidgetState.CHECKED`, hover/pressed больше не затираются.
  `checkedBackground/uncheckedBackground` остались цветами индикатора
  для Checkbox, ToggleSwitch и XML.
- Danger-вариант из эталона (`.btn.danger`).
- Палитра темы: тёплый акцент `0xC98A4B` против холодного `#4a9eff`
  в эталоне — решение отдельно, не в рамках кнопок.
