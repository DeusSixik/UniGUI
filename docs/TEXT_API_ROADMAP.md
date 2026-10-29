# Text API: дорожная карта (анимация букв, spacing, градиенты, текстура, шейдеры)

Цель: довести Text API до состояния, когда можно анимировать отдельные буквы, настраивать расстояния
между ними, получать предсказуемые градиенты, накладывать текстуру на текст и подключать свои шейдеры.

Статус этапов:

| Этап | Что | Статус |
|---|---|---|
| A | Исправление расчёта градиента (общий прямоугольник заливки) | **сделано** |
| B | Общий `TextLayout` в core, backend'ы только рисуют | не начато |
| C | Анимация букв (`GlyphAnimator`) | не начато |
| D | Градиенты и текстура через фрагментный шейдер | не начато |
| E | Публичный `TextShader` для своих эффектов | не начато |

---

## Этап A: что сделано

### Причины "странного" градиента

Градиент (`LinearGradientTextBrush.factor`) считался относительно `DrawCommand.bounds()` текстовой команды.
Формула сама по себе корректная (CSS-подобная), но bounds команды не совпадали с реальным текстом:

1. `TextEngine.draw(...)` передавал в `context.text(...)` ширину `width - (drawX - x)`, то есть остаток
   контейнера, а не ширину текста. При `CENTER`/`END` выравнивании и коротком тексте градиент растягивался
   на весь контейнер.
2. `TextEngine.drawInline(...)` (через него рисуют Button, TextWidget, Label, TextInput и др.) на plain-пути
   передавал размеры контейнера, на mixed-пути (текст + inline-иконки) рисовал каждый chunk отдельной
   командой со своими bounds. Градиент перезапускался на каждом chunk'е.
3. `TextWidget.wrappedSegments` рисует каждую перенесённую строку отдельной командой. Градиент перезапускался
   на каждой строке.
4. `MinecraftMixedTextRenderer.renderSegment` создавал для SDF-сегментов новые `DrawCommand` с bounds
   сегмента, теряя bounds исходной команды.

### Решение

- `DrawCommand` получил необязательный `brushBounds` (`brushBounds()`, `hasBrushBounds()`,
  `brushBounds(RectView)`, `brushBounds(x, y, w, h)`). Если не задан, `brushBounds()` возвращает `bounds()`,
  поведение старого кода не меняется. Поле копируется в `copy()`, `copyForReplayTo()`, сбрасывается в
  `resetForReuse()`.
- `RenderContext` получил scope `pushTextBrushBounds(x, y, w, h)` / `popTextBrushBounds()` /
  `textBrushBounds()`. Пока scope открыт, `submit`/`submitOwned` проставляют прямоугольник на TEXT-команды без
  собственного. Побеждает самый внешний scope (он описывает весь текст). Реализация в `DefaultRenderContext`.
- `TextEngine`: `usesPositionalBrush(RichText)` + приватные `pushBrushScope`. Scope открывается в `draw(...)`,
  в plain-ветке `drawInline(...)` по измеренному extent'у текста и в mixed-ветке `drawInline(...)` по всему
  тексту с иконками. Scope открывается только если в тексте есть не-solid brush.
- `TextWidget.renderText`: для нескольких сегментов (перенос строк) открывается scope по объединению
  сегментов. `MARQUEE_ON_HOVER` исключён, там сегменты должны ехать независимо.
- Backend'ы берут `command.brushBounds()` вместо `command.bounds()` для расчёта цвета:
  `MinecraftSdfTextRenderer`, `MinecraftMixedTextRenderer` (включая создаваемые SDF-сегменты и vanilla fallback),
  `CleanroomTextRenderer`.
- Места, которые сдвигают/масштабируют bounds команд, теперь делают то же с `brushBounds`:
  `MinecraftWidgetScreen.scaledCommand`, `PostProcessingLayer.offsetCommand`, `WidgetTextureRenderer.offset`.
- Тесты: `core/src/test/java/dev/sixik/unigui/impl/text/TextBrushBoundsTest.java`.

### Проверка и оставшиеся хвосты A

- `:core:test` и `:1.20.1:common:renderPolishSelfTest`, `widgetRenderCacheSelfTest`, `xmlWidgetSelfTest` проходят.
- `:1.20.1:common:basicControlsSelfTest` падает (`Button should apply NORMAL theme background at render time`).
  Падение воспроизводится и без изменений этапа A.
- Модуль `1.12.2-cleanroom` не удалось собрать в сессии (таймаут `gradlew`). Правка там тривиальная
  (`bounds()` -> `brushBounds()`, тот же тип `RectView`), но её нужно собрать и прогнать
  `CleanroomTextColorsTest` / `CleanroomTextLayoutTest`.
- Визуально не проверено в игре: нужны скриншоты градиента на выровненном по центру тексте, на переносе строк
  и на тексте с иконкой в Button.
- Все новые API в `RenderContext` default-методы, внешние реализации не ломаются.

---

## Наблюдения, которые влияют на следующие этапы

- Раскладка глифов живёт внутри backend'ов: цикл `penX` / `baseline` / `lineInfo` в `MinecraftSdfTextRenderer`
  (`layoutCommand`), похожий в `MinecraftMixedTextRenderer.renderCommand` и в `CleanroomTextLayout`
  / `CleanroomSdfTextRenderer`.
- Цвет считается в трёх местах: `MinecraftSdfTextRenderer.addColor`, `MinecraftMixedTextRenderer.argb`,
  `CleanroomTextColors`. Любая новая фича сейчас означает три реализации.
- `TextEngine.drawInline` рисует текстовые chunk'и отдельными командами и пересоздаёт `TextRun`
  (`drawTextChunk`) на каждый chunk.
- Tracking: javadoc `TextRun`/`RichText.Builder.tracking` говорит "UI-пиксели", но код
  (`TextEngine.trackingAdvance`, `MinecraftSdfTextRenderer.trackingAdvance`) умножает на `pixelSize`, то есть
  это em. Нужно выбрать одно и привести документацию/код в соответствие. Отрицательный tracking сейчас
  запрещён (`Math.max(0, ...)`). Кернинга в `impl/text` нет.
- SDF-шейдер: `SdfTextShaderSources` (core, общий GLSL `#version 150`), инстанс = 32 float (4 позиции углов по
  3, UV 4, 4 цвета углов по 4). Градиент сейчас идёт через цвета углов квада глифа.
- `FloatBuffer`/batch'и привязаны к странице атласа (`InstanceBatch.page`).
- `MinecraftMixedTextRenderer` (vanilla-шрифт) не сможет шейдеры и текстуры, нужна деградация до цвета.

---

## Этап B: общий `TextLayout` в core

Выделить раскладку из backend'ов в core. Результат раскладки `RichText` в плоских массивах (SoA, без аллокаций
на кадр): `x, y, w, h, u0, v0, u1, v1, glyphIndex, lineIndex, runIndex, clusterIndex`, плюс ссылки на run
и font. Backend'ы только превращают готовые глифы в instance'ы / vanilla-вызовы.

Что даёт:

- единый источник истины для 1.12.2, 1.20.1 и 1.21.1;
- общий extent текста для градиента (A закрывается честно, без scope-костылей);
- spacing: tracking (px или em, с отрицательными значениями), word spacing, позже кернинг;
- inline-иконки становятся частью общего layout, а не отдельным путём в `TextEngine.drawInline`;
- база для C, D, E.

Риски: самый большой по объёму этап; нужно переписывать тесты `CleanroomTextLayoutTest`; кэширование layout
между кадрами (ключ: `RichText` + width + font + scale).

Решения, которые надо принять до старта:

- единицы tracking (px против em) и обратная совместимость;
- где живёт кэш layout (в `RichText`, в `TextEngine` или в backend);
- как layout работает с pixel snap (`textPixelSnap`) и целочисленным `guiScale`.

## Этап C: анимация букв

- Интерфейс вида `GlyphAnimator.apply(GlyphState state, int glyphIndex, int glyphCount, float timeSeconds)`.
- `GlyphState` мутабельный и переиспользуемый (нулевые аллокации): `dx, dy, scaleX, scaleY, rotationDeg,
  pivot, alpha, tint`.
- Привязка к `TextRun`/`RichText` (по аналогии с `brush`) и к `TextWidget` (`textAnimator(...)`).
- Готовые эффекты: wave, shake, typewriter (количество видимых глифов), fade-in по буквам, rainbow.
- Применяется на CPU после layout и до формирования instance'ов. Для тяжёлых эффектов позже перенос в
  вершинный шейдер через `glyphIndex`/`time`.
- Время брать из `FrameContext`; анимированный текст не должен попадать в retained cache
  (`CachedSubtreeWidget`, `replayCached`), нужен флаг "dynamic".
- Pixel snap для анимированных глифов отключать.

## Этап D: градиенты и текстура через фрагментный шейдер

- Перенести расчёт brush из вершин в фрагментный шейдер. Для этого в instance передавать локальные координаты
  текста: прямоугольник глифа в нормализованном пространстве всего текста (0..1), индекс глифа и строки.
  Ожидаемый размер instance: ~40 float (см. `SdfTextShaderSources.FLOATS_PER_INSTANCE` и
  `INSTANCE_ATTRIBUTE_*`; поправить `SdfShaderSourcesTest`).
- `GradientSpace`: `TEXT` (по умолчанию), `LINE`, `GLYPH`, `WIDGET`, `CHAR_INDEX`.
- Новые brush'и: multi-stop линейный, радиальный, conic; парсер `TextBrushParser` (XML).
- Текстура на тексте: второй sampler, `texColor * sdfAlpha`; режимы `FILL`, `MULTIPLY`, `MASK`; UV из локальных
  координат текста (fit/tile). Использовать существующий `TextureHandle`.
- Ключ batch'а станет (страница атласа, текстура).
- Не раздувать `TextRun`: рассмотреть `TextPaint`/`TextStyle` как отдельный immutable объект.

## Этап E: публичный `TextShader`

- Контракт: пользователь даёт фрагментный сниппет (или полный источник) и uniform'ы; фиксированный набор
  входов: SDF-альфа, локальные координаты текста, UV атласа, индекс глифа/строки, время, цвет.
- Кэш компиляции по хэшу источника; fallback на стандартный шейдер при ошибке компиляции + лог.
- Переиспользовать существующие `ShaderHandle`/`ShaderUniforms`/`ShaderProviders` (`api/render/shaders`),
  а не заводить параллельную систему.
- Совместимость с Iris/Oculus (1.20.1/1.21.1) проверить отдельно.
- Hot reload шейдера в dev-режиме.

---

## Общие замечания

- Любое изменение формул/hot path сопровождать JUnit-тестом (и JMH для layout/instance-кода).
- Новые данные для hot path держать плоскими массивами и примитивами, без аллокаций на кадр.
- 1.12.2 (LWJGL2, отдельный Gradle-проект) и 1.20.1/1.21.1 имеют раздельную GL-обвязку: общим остаётся
  только core (layout, GLSL-источники, константы). Решить заранее, нужны ли C-E в 1.12.2 или достаточно
  1.20.1+.
- Vanilla-font fallback (`MinecraftMixedTextRenderer`) должен деградировать до обычного цвета и не падать.
