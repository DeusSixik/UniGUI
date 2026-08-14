# Destiny-like Inventory UI

Короткая заметка о том, что потребуется, чтобы собрать экран персонажа/инвентаря в стиле Destiny на UniGUI.

## Цель

Собрать полноэкранный character/inventory screen с:

- верхней навигацией по вкладкам;
- профилем игрока и прогрессом;
- центральным preview персонажа;
- левой сеткой оружия/инвентаря;
- правой колонкой экипировки;
- tooltip/compare overlays;
- AAA-полировкой: hover, glow, fade, subtle background motion.

## Что уже можно собрать на текущей базе

- Layout: HBox, VBox, GridBox, WrapPanel, Box, Border.
- Текст: Label, TextBlock, RichText/SDF.
- Карточки и панели: Box + custom renderer/style.
- Оверлеи: OverlayLayer, Tooltip, Popup.
- Minecraft preview: MinecraftItemPreviewWidget, MinecraftEntityPreviewWidget, MinecraftBlockPreviewWidget.
- Background/effects: CustomDraw, DrawScope, shader quad, texture layers.
- Анимации: transition API для opacity, position, scale, hover/selection states.
- Debug/perf: DebugOverlay + профилирование build/render/cache.

## Нужные reusable-виджеты

### InventoryScreen

Главный screen-компонент, который собирает весь layout:

- top navigation;
- player header;
- character preview area;
- inventory grid;
- equipment column;
- bottom action hints;
- overlay layer для tooltip/compare.

### TopNavBar

Верхнее меню вкладок:

- CLAN / CHARACTER / INVENTORY / SETTINGS;
- selected underline;
- disabled/available state;
- gamepad shoulder hints L1/R1.

### PlayerHeader

Блок профиля игрока:

- avatar/emblem;
- имя;
- level/power;
- currency summary;
- progress bar.

### ItemSlotWidget

Базовый кирпич для предметов:

- item icon;
- rarity background;
- border by rarity/selection;
- hover/pressed/focused states;
- optional quantity/power label;
- optional lock/new/equipped markers;
- tooltip anchor.

Это самый важный виджет. Без него экран быстро превратится в набор одноразовых Box.

### InventoryGridWidget

Сетка предметов:

- фиксированный размер слота;
- spacing;
- keyboard/gamepad navigation;
- selected item;
- hover item;
- фильтры/категории в будущем;
- virtualization опционально, если предметов много.

### EquipmentColumnWidget

Правая колонка экипировки:

- weapon/helmet/arms/chest/legs/class item slots;
- пустые ghost-slots рядом;
- current power/stat summary;
- focus navigation между слотами.

### CharacterPreviewWidget

Центральный paper-doll preview:

- на первом этапе можно использовать MinecraftEntityPreviewWidget;
- для AAA-качества нужен отдельный renderer игрока с armor/items/pose/lighting;
- желательно поддержать фоновые circular glyphs/effects за персонажем.

### ItemTooltip / CompareTooltip

Оверлей при hover/focus:

- название;
- rarity;
- stats;
- perks;
- compare with equipped;
- delayed open;
- smart placement, чтобы не выходить за экран.

## Что, скорее всего, нужно допилить в UniGUI

### Focus/Gamepad navigation

Для Destiny-like UX нужна уверенная навигация:

- d-pad/arrow перемещение по grid;
- shoulder buttons для вкладок;
- confirm/cancel actions;
- focus ring;
- remembered focus per tab.

### Better item rendering

Нужен удобный API для Minecraft item rendering внутри кастомного slot widget:

- item stack icon;
- decorations/overlays;
- controlled z-layer;
- tooltip-safe hover bounds.

### Paper-doll renderer

Простой entity preview не даст полностью Destiny-like feeling.

Желательно сделать отдельный Minecraft player preview widget:

- player model;
- armor layers;
- held weapon;
- configurable pose;
- lighting/rotation;
- optional idle animation.

### Screen background system

Для похожего визуала нужен layered background:

- base gradient/vignette;
- subtle noise/dust;
- circular glyph texture behind character;
- dim panels behind inventory columns;
- optional shader motion.

### Reusable visual states

Для слотов и кнопок желательно унифицировать состояния:

- normal;
- hover;
- pressed;
- focused;
- selected;
- equipped;
- disabled;
- rarity accent.

## Предлагаемый порядок разработки

1. Сделать статичный layout экрана без интерактива.
2. Выделить ItemSlotWidget и InventoryGridWidget.
3. Подключить реальные ItemStack icons через Minecraft renderer.
4. Добавить EquipmentColumnWidget и PlayerHeader.
5. Добавить overlay tooltip.
6. Добавить focus/gamepad navigation.
7. Улучшить CharacterPreviewWidget.
8. Полировать фон, анимации, glow, hover и transitions.
9. Прогнать через DebugOverlay и проверить buildDrawList/render/cache.

## Основные риски

- Центральный player preview может стать самым сложным backend-куском.
- Gamepad navigation легко недооценить: без неё UI будет выглядеть хорошо, но ощущаться не как Destiny.
- Много glow/shader/custom draw эффектов может ломать batching, нужно следить через DebugOverlay.
- ItemSlotWidget надо сразу делать reusable, иначе экран будет сложно поддерживать.
- Ассеты сильно влияют на качество: иконки rarity, glyphs, фоны и noise-текстуры лучше держать отдельным набором.

## Минимальный MVP

Для первого рабочего прототипа достаточно:

- InventoryScreen;
- PlayerHeader;
- TopNavBar;
- ItemSlotWidget;
- InventoryGridWidget;
- EquipmentColumnWidget;
- MinecraftEntityPreviewWidget как временный character preview;
- простой background через gradient + vignette;
- tooltip на hover.

После MVP уже можно заменить preview, добавить shader background и полировать анимации.