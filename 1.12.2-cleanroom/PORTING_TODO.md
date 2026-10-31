# Cleanroom 1.12.2 — план портирования рендера и backend API

Цель: довести `1.12.2-cleanroom` до функционального паритета с `common` (1.20.1 / 1.21.1) поверх
нейтрального `core`, не копируя логику в версионный модуль. Эталон — `common/.../backend/minecraft_impl`
(`MinecraftGuiRenderBackend` и соседние ~9 000 строк). Текущая реализация — `cleanroom/backend/*`
(~1 300 строк, immediate-mode GL11).

Легенда: `[x]` сделано, `[ ]` не сделано, `[~]` сделано частично или с дефектами, `[-]` не делаем (нет данных в core или платформа не даёт).
Приоритеты: **P0** блокирует остальное, **P1** нужно для паритета, **P2** качество и оптимизация, **P3** по желанию.

---

## 0. Текущее состояние (аудит)

Сделано:

- [x] Bootstrap: `CleanroomClientBootstrap` → `UniGuiRuntime.install`, `@Mod` и `mcmod.info`.
- [x] `UniGuiBackend`: `openScreen` / `closeScreen`, `registerLayer` / `clearLayers` для `HUD` и `SCREEN_OVERLAY`.
- [x] `CleanroomWidgetScreen`: tick → measure/arrange → `root.render` → `DrawList` → backend.
- [x] Ввод: pointer, scroll, key, text, `KeyModifiers`, `CleanroomKeyCodes` (LWJGL2 → core), `InputHandle` в core.
- [x] Миксины: HUD (`GuiIngame`), оверлей экрана (`GuiScreen`, `GuiContainer`), команда `/unigui`.
- [x] Команды `DrawCommandType`: `RECT`, `LINE`, `CIRCLE`, `TEXT` (plain), `TEXTURE`, `TEXTURED_QUAD`, `PUSH_CLIP`, `POP_CLIP`, `CUSTOM`.
- [x] Unit-тесты: `CleanroomKeyCodes`, `CleanroomLayerRegistry`, `CleanroomRenderMath`.

Найденные дефекты текущего `CleanroomGuiRenderBackend` (чинить в фазе 1, не ждать новых фич):

| #   | Дефект                                                                                                                                                   | Место                                   |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| D1  | `POP_CLIP` просто делает `glDisable(SCISSOR_TEST)`: вложенные clip ломаются. Нет стека (в core есть `impl/render/ScissorStack`).                         | `CleanroomGuiRenderBackend:104`, `:198` |
| D2  | `LINE` и `CIRCLE` не отключают `GL_TEXTURE_2D`. Результат зависит от того, что рисовали раньше.                                                          | `:138`, `:147`                          |
| D3  | `glColor4ub` не сбрасывается в белый после рисования. Следующие `TEXTURE` / `TEXTURED_QUAD` получают чужой tint.                                         | `:140`, `:153`, `:185`                  |
| D4  | `TEXTURE` и `TEXTURED_QUAD` игнорируют `Paint.color` (tint, opacity), `blendMode`, `TextureOptions` (filter, wrap).                                      | `:169`, `:181`                          |
| D5  | Прямые `GL11.*` вызовы в обход `GlStateManager`. Кэш состояния ваниллы рассинхронизируется (классическая причина «после UniGUI пропал текст или тайлы»). | весь класс                              |
| D6  | `ROUNDED_RECT` рисуется обычным `RECT`, `radius` игнорируется. `PATH`, `MESH`, `SHADER`, `DRAW_CMD` молча пропускаются.                                  | `:96`, `:108`                           |
| D7  | `TEXT` берёт только `plainText()`: теряются шрифт, размер, цвет и tracking по run'ам, brush, transform, перенос и выравнивание.                          | `:114`                                  |
| D8  | Для `TEXTURE` и `TEXTURED_QUAD` не заданы `Paint.blend`. `dashed` stroke и толщина линий > 1 зависят от драйвера (`glLineWidth` в compat-контексте).     | `:138`                                  |
| D9  | `beginFrame` делает `glPushAttrib`, но не сохраняет depth, lighting, alpha test, matrix mode. HUD-слой может испортить кадр.                             | `:66`                                   |
| D10 | Тесты `dev.sixik.unigui.api.input.*` и `api.screen.*` лежат в cleanroom-модуле, хотя тестируют `core`.                                                   | `src/test/.../api`                      |
| D11 | `ComplexUiDemo` и `demo/unity/*` (~7 000 строк) скопированы из `TestMod`. Две копии разойдутся.                                                          | `cleanroom/demo`                        |
| D12 | `@Mod(version = "1.5.4")` захардкожен, а не берётся из `mod_version`. Release workflow не собирает cleanroom.                                            | `UniGuiCleanroom`, `release.yml`        |

**Статус дефектов (фаза 1):** D1, D2, D3, D4, D5, D9, D10 исправлены. D8 частично: линии теперь рисуются quad'ами нужной толщины, dash остаётся на 2.5. D6, D7 ждут фаз 2 и 4. D11, D12 открыты.

**Изменения поведения в фазе 1** (проверять при ручном прогоне):

- `TEXTURE` и `TEXTURED_QUAD` берут UV как нормализованные `0..1`, как в `common` (раньше `TEXTURED_QUAD` делил на размер текстуры, а `TEXTURE` игнорировал размер региона `uv`).
- Текст с `alpha < 4` не рисуется: `FontRenderer` трактует такой alpha как «не задан» и рисует непрозрачно.
- Кольцо `CIRCLE` stroke теперь внутри bounds (как у `RECT`), раньше линия шла по краю. Заливка круга замкнута (раньше терялся последний сектор веера).
- Clip с rotate / scale превращается в axis-aligned bounding box после всех transform'ов команды и стека.
- Включить диагностику GL: `-Dunigui.glDebug=true` (логирует `glGetError` после каждого UI-кадра).

**Находки по окружению:**

- Cleanroom 1.12.2 даёт LWJGL **3** под старыми именами пакетов: `GL11.glGetIntegerv/glGetFloatv(int, Buffer)` вместо LWJGL2 `glGetInteger(int, Buffer)`. Писать GL-код по API LWJGL3.
- Сборка cleanroom-модуля: JDK 25 (`jdk-25.0.4.101-hotspot`), `core` собирается на JDK 21. В `core` добавлена тестовая инфраструктура JUnit (`./gradlew :core:test`).
- `1.6` закрыта частично: формула масштаба вынесена в `CleanroomRenderMath.effectiveUiScale`, но пересчёт координат мыши по-прежнему дублируется в `CleanroomWidgetScreen` и `CleanroomWidgetLayer` (вынести вместе с 6.6).

---

## Архитектурные решения (принять ДО фаз 2, 4, 5)

Это развилки, от которых зависит объём работ. Рекомендация указана, но решение за тобой.

- [x] **A1. Способ рисования примитивов — решено: как в `common`.** Основной путь — SDF-шейдеры (`MinecraftSdfShapeRenderer`: один quad на фигуру, форма считается в fragment shader через `fwidth`), запасной — CPU-тесселяция (`MinecraftShapeBatchRenderer`: `POSITION_COLOR`, feather-кольцо). `renderBatch` пробует SDF и при отказе (нет шейдеров, ошибка компиляции, исключение) падает на тесселяцию. Код тесселяции из фазы 1 остаётся как этот fallback.
- [x] **A2. Какие шейдеры и на каком GL — решено по логу: tier `INSTANCED`.** Шейдеры в `common` — `#version 150`, с VAO и instancing (`glDrawArraysInstanced`, `flat` varyings, `ARB_instanced_arrays` / GL 3.3). Что реально выдаёт Cleanroom — неизвестно, поэтому выбор по лестнице, реализован `CleanroomGlCapabilities` (чистая функция `selectTier`, покрыта тестами, логирует `UniGUI OpenGL: ...` при первом кадре):

  | Tier        | Условие                         | Путь                                                                 |
  |-------------|---------------------------------|----------------------------------------------------------------------|
  | `INSTANCED` | GL ≥ 3.2, VAO, instanced arrays | те же исходники GLSL 150, что в 1.20, один draw на batch             |
  | `BASIC`     | GL ≥ 3.2, VAO                   | GLSL 150, один quad на команду, uniform'ы на draw                    |
  | `LEGACY`    | GL ≥ 2.0                        | GLSL 1.20: адаптер 150 → 120, fixed-function матрицы, без instancing |
  | `NONE`      | иначе                           | только тесселяция                                                    |

  **Факт с dev-машины:** `version=4.6.0 NVIDIA 595.97, renderer=RTX 5070 -> INSTANCED`. Значит шейдеры 1.20 работают как есть. `LEGACY` (адаптер 150 → 120) откладывается до появления реального железа без GLSL 1.50 (Mesa / старый Intel); до тех пор такие машины идут на тесселяцию.
- [x] **A3. Текст — решено (уточнено по коду).** Шрифт по умолчанию у экрана — **ванильный** (`MinecraftWidgetScreen.defaultFont = MinecraftFonts.defaultFace()`); SDF получают только явные `AwtFontFace` (TestMod, свои TTF). Поэтому в `common` есть `MinecraftMixedTextRenderer`: runs с ванильным и SDF-шрифтом идут в одном `TEXT` в порядке следования. SDF-часть держится на `core` (`DefaultFontRegistry`, `SdfGlyphProvider`, `AwtFontFace`), в `MinecraftSdfTextRenderer` только GL. **Решение:** один смешанный рендерер: чистая раскладка (`CleanroomTextLayout`) + два вывода (ванильный `FontRenderer` и SDF). Ванильный путь определяет вид обычного текста, SDF нужен для кастомных шрифтов; если SDF недоступен, SDF-runs рисуются ванильным шрифтом.
- [x] **A6. Общие исходники шейдеров — сделано (частично).** Сейчас GLSL лежит строками внутри `common` (`MinecraftSdfShapeRenderer`, `MinecraftSdfTextRenderer`), и cleanroom пришлось бы копировать их. Предложение: вынести в `core` версионно-нейтральные константы (GLSL-строки, раскладка instance-буфера: `INSTANCE_FLOATS = 30` для фигур и `32` для текста, смещения атрибутов, коды `ShapeType`) плюс `ShaderDialectAdapter` (150 → 120). В `core` нет LWJGL, поэтому только строки и числа, а GL остаётся в версионных модулях. Тогда шейдер правится в одном месте. Сделано: `core/.../impl/render/shaders/SdfShapeShaderSources` и `SdfTextShaderSources` (GLSL, раскладка, имена атрибутов, константы атласа), `common` переведён на них (компилируется на 1.20.1 и 1.21.1), контракт GLSL ↔ раскладка закреплён тестами `SdfShaderSourcesTest`. Не сделано: `ShaderDialectAdapter` (нужен только для `LEGACY`). **Важно:** cleanroom берёт jar `core`, поэтому после любых правок `core` сначала `./gradlew :core:jar` (JDK 21) в корне, потом сборка модуля.
- [ ] **A4. MC-специфичные виджеты** (`common/.../widgets/minecraft`: item, entity, block, tooltip). Они завязаны на типы 1.20 (`ItemStack`, `Component`). Нужны нейтральные capability-интерфейсы в core (по образцу `UiPostEffectBackend`): `UiItemRenderer`, `UiTooltipService`, `UiEntityPreview`. Иначе каждую версию придётся писать виджеты заново.
- [ ] **A5. Общее демо.** Вынести `Unity*` и `ComplexUiDemo` из `TestMod` и `cleanroom` в один version-neutral источник (отдельный gradle-модуль `demo` или `core` testFixtures), оставив в версионных модулях только вход (команда / screen opener).

---

## Фаза 1 — Корректность базового пути `P0`

Цель: то, что уже заявлено в README, должно работать детерминированно. Без новых фич.

- [x] **1.1 GL-state guard.** Класс `CleanroomGlState`: сохранить и восстановить blend, depth, alpha test, lighting, cull, texture2D, scissor, color, matrix mode и стеки матриц. Менять состояние через `GlStateManager`, а не через `GL11` (D5, D9). *Готово, когда:* после `/unigui` ваниль-HUD и чат рисуются без артефактов, `GlStateManager` не расходится с реальным GL (проверка через `GL11.glGetBoolean` в debug-режиме).
- [x] **1.2 Scissor stack.** Использовать `core impl/render/ScissorStack` (пересечение вложенных clip). Учитывать `Framebuffer` и HiDPI (`displayWidth/Height` против `ScaledResolution`). Поведение при transform: axis-aligned bounds или предупреждение (D1). Unit-тест на математику пересечения и пересчёт в окно (вынести в `CleanroomRenderMath`).
- [x] **1.3 Color и texture state.** Единая точка `bindColor(argb)` / `resetColor()`, `disableTexture2D()` для не-текстурных команд (D2, D3).
- [x] **1.4 Tint и blend для текстур.** `Paint.color` → `glColor4f`, `BlendMode` → `blendFunc`, `opacityMultiplier` из `RenderContext` (D4). Тест на таблицу `BlendMode → (src, dst)`.
- [x] **1.5 Перейти с `glBegin/glEnd` на `Tessellator`.** Минимум для `LINE`, `CIRCLE`, `TEXTURED_QUAD`. Устраняет зависимость от deprecated immediate mode и готовит батчинг.
- [~] **1.6 `CleanroomRenderHost`: единый расчёт масштабов.** Сейчас формулы `backendScale` / `effectiveScale` дублируются в `CleanroomWidgetScreen` и `CleanroomWidgetLayer`. Вынести в `CleanroomRenderMath` (чистые функции, покрытые тестами).
- [x] **1.7 Перенести тесты `api.input`, `api.screen` в `core`** (D10).
- [ ] **1.8 Smoke-тест в клиенте (ручной, нужен запуск `runClient`).** Чеклист: `/unigui` открывается, закрывается по Esc, ресайз окна, смена GUI Scale 1–4, fullscreen, HUD-слой + открытый инвентарь. Зафиксировать результат в этом файле.

**Критерий завершения фазы:** `runClient` без GL-ошибок в `logs/` (включить `-Dorg.lwjgl.util.Debug=true` и проверку `glGetError` после кадра в debug-флаге), скриншоты до и после совпадают.

---

## Фаза 2 — Геометрия и примитивы `P1`

Схема как в `common`: `DrawBatcher` режет draw list на batch'и по типу, `renderBatch` пробует SDF-путь, при отказе рисует CPU-тесселяцией. Фаза 1 уже дала тесселяцию для `RECT` / `LINE` / `CIRCLE`; здесь она дополняется до полного fallback'а, а основным путём становятся шейдеры. Зависит от A2 (tier) и желательно от A6.

**2A. Инфраструктура batch'ей и шейдеров**

- [x] **2.1 `DrawBatcher` → `renderBatch`.** Подключить `core impl/render/DrawBatcher` / `DrawBatch` (batch по `type()`, `texture()`, `blendMode()`, `isBarrier()`). `PUSH_CLIP` / `POP_CLIP` / `CUSTOM` — barrier-batch'и: перед ними сбрасывается накопленное. Диспетчер как `MinecraftGuiRenderBackend.renderBatch` (строки ~985–1012).
- [x] **2.2 `CleanroomShaderProgram`.** Компиляция / линковка (`GL20`), привязка attribute location'ов, кэш uniform location'ов (порт `MinecraftUniformLocationCache`), удаление в `close()`. Ошибка компиляции → пометить путь недоступным, один раз залогировать, упасть на fallback (как `unavailable` в `MinecraftSdfShapeRenderer`).
- [x] **2.3 Матрицы для шейдеров.** `ModelViewMat` / `ProjMat` читаются из GL (`glGetFloatv(GL_MODELVIEW_MATRIX / GL_PROJECTION_MATRIX)`), т.к. в 1.12.2 нет `RenderSystem.getModelViewMatrix()`. Базовая pose = текущий modelview (в нём уже `glScalef(uiScale)`), на CPU применяются только transform'ы команды (`CleanroomRenderMath.applyTransform`, уже есть).
- [~] **2.4 Save / restore состояния шейдерного прохода.** (сделано: программа, VAO, `GL_ARRAY_BUFFER` через `CleanroomGlState.ShaderBindings`; texture unit добавится с текстовым и текстурным шейдерами.) Расширить `CleanroomGlState`: текущая программа (`GL_CURRENT_PROGRAM`), VAO (`GL_VERTEX_ARRAY_BINDING`), `GL_ARRAY_BUFFER_BINDING`, активный texture unit. Ванилья 1.12.2 VAO не использует, после прохода биндить 0 / сохранённое.

**2B. Фигуры**

- [~] **2.5 SDF-фигуры (`RECT`, `ROUNDED_RECT`, `CIRCLE`, `LINE`)** — написано и покрыто тестами упаковки, **не проверено в игре** (`CleanroomSdfShapeRenderer`, `CleanroomSdfShapeInstances`; `-Dunigui.sdf=false` отключает для сравнения). Порт `MinecraftSdfShapeRenderer`: `needsSdf` (скругление, stroke, дробные bounds, круг, линия), один quad на команду, AA через `fwidth`. Сначала `BASIC` (uniform'ы на draw), затем `INSTANCED` (30 float'ов на экземпляр, порядок команд сохраняется). Закрывает D6 для `ROUNDED_RECT`.
- [x] **2.6 Fallback-тесселяция фигур** (`CleanroomShapeTessellator` + `CleanroomShapeRenderer`: один `GL_TRIANGLES` draw на batch, CPU-transform через affine команды; `RECT` больше не округляется до целых пикселей, как в 1.20). Доработать код фазы 1 до `MinecraftShapeBatchRenderer`: `ROUNDED_RECT` fill / stroke (сегменты угла по радиусу, feather-кольцо), эллипс, линии с толщиной, один buffer на batch (`POSITION_COLOR`, `GL_TRIANGLES`).
- [x] **2.7 `LINE`: dash и pixel-center.** (dash режет сам `core` в `RenderContext.line`, backend получает готовые отрезки; pixel-center для тонких осевых линий сделан в тесселяторе.) `Paint.dashLength / dashGap / dashOffset` (в core уже есть `emitDashedLine`, нужно проверить, что backend получает готовые отрезки), выравнивание тонких линий по центру пикселя (`appendLineCommand` в 1.20).
- [x] **2.8 `PATH`** — написан и покрыт тестами геометрии, **не проверен в игре**. (Заливка `PATH` только выпуклая, вогнутые контуры `core` сам триангулирует в `MESH`.)  Обход `VectorPath`, flatten кривых по допуску в физических пикселях, fill и stroke с AA-бахромой через `PathAntiAliasing` из core. Образец: `MinecraftShapeBatchRenderer.appendPath` и `renderPath` / `renderFilledPath`.
- [x] **2.9 `MESH` и `DrawVertex`** — цветной и текстурированный (текстура биндится по id, без sampler state до фазы 3), **не проверен в игре**. Цветной и текстурированный меш, per-vertex color (покрывает `addRectFilledMultiColor`, `addRibbon`, градиенты). Образец: `renderMeshBatch`.
- [~] **2.10 Clip с transform и скруглением.** (сделано: axis-aligned AABB после transform; не сделано: stencil для повёрнутого и скруглённого clip.) Повёрнутый clip: сейчас axis-aligned bounding box (готово в фазе 1). Точный clip через stencil — по желанию; rounded clip — по возможности.

**2C. Проверка**

- [~] **2.11 Тесты без GL.** (сделано: `CleanroomShapeTessellatorTest` — 24, `CleanroomSdfShapeInstancesTest` — 16; не сделано: JMH.) Чистая геометрия (вершины углов, dash-разбиение, flatten, упаковка instance-данных в `FloatBuffer`, выбор SDF / fallback по `needsSdf`) выносится в классы без LWJGL и покрывается JUnit. JMH рядом с `core/src/jmh` для упаковки instance'ов и тесселяции rounded rect.
- [ ] **2.12 Визуальное сравнение 1.12.2 и 1.20.** Один экран с rounded rect, кругом, линией, stroke'ом на дробных координатах и UI Scale 1–4; скриншоты рядом (часть 8.6).

---

## Фаза 3 — Текстуры `P1`

Ручная проверка: `/unigui textures` открывает `TextureLab` (dynamic, nearest/linear, repeat/mirrored, mipmaps, скругление, tint, vanilla-ресурс; у каждой плитки подпись с ожидаемым видом).

- [x] **3.1 Handle и расположение** (`CleanroomTextureHandle` — аналог `MinecraftTextureHandle` с `withOptions`; `CleanroomTextures.location` кэширует разбор id; отсутствующий ресурс: `TextureManager` сам пишет в лог и биндит `missingno`). Исходная формулировка: `CleanroomTextureRegistry`. `TextureHandle.id()` → `ResourceLocation` + кэш размеров. Проверка существования ресурса, `missingno` fallback с warning один раз.
- [x] **3.2 Динамические текстуры** (`CleanroomDynamicTextures`: `register/replace/get/unregister/clear/size`; RGBA -> ARGB `CleanroomPixels`; текстура хранит пиксели и переживает resource reload; загрузка в GL на клиентском потоке, из другого потока ставится в очередь). **Не проверено в игре**. Исходная формулировка: Порт `UniGuiTextures`: `register(id, w, h, RGBA ByteBuffer, options)`, `replace`, `unregister`, `clear`. На 1.12.2 — `DynamicTexture` / `TextureUtil.uploadTextureImageAllocate`. Нужен для генерации карт, FBO-превью, `TextureManagementSelfTest`.
- [x] **3.3 `TextureOptions`** (`CleanroomSamplerState`: sampler-объекты GL 3.3 по `packed()`-ключу, без них `glTexParameteri` с откатом; default-options ничего не меняют, как в 1.20; mipmap-фильтр без `mipmaps()` понижается до базового, чтобы не было чёрной текстуры; `premultipliedAlpha` -> `GL_ONE` как источник). **Не проверено в игре**. Исходная формулировка: `minFilter/magFilter` (NEAREST/LINEAR), `wrapS/T`, mipmaps, `premultipliedAlpha` → `glTexParameteri` (D4). Состояние сэмплера кэшируется на текстуру (образец: `MinecraftTextureSamplerState`).
- [x] **3.4 Размещение** (core выдаёт готовые `TEXTURE` / `TEXTURED_QUAD` с `uv`, backend только раскладывает; UV покрыты тестами). Исходная формулировка: Проверить, кто раскрывает `TexturePlacement`, `ImageFit`, `NineSlice`, `SliceBorders`: если core выдаёт готовые `TEXTURE` / `TEXTURED_QUAD` — ничего не нужно, если нет — реализовать в backend. Покрыть тестом UV-математику.
- [x] **3.5 Rounded texture** (веер по скруглённому контуру, UV из позиции; `CleanroomShapeTessellator.appendTexture`). Исходная формулировка: (`addImageRounded`, `texture(..., radius, ...)`): клип углов через меш (зависит от 2.7).
- [x] **3.6 Пакетная отрисовка текстур** (один bind, один sampler, один draw на batch; CPU-трансформ, GL-matrix для текстур больше не используется). Исходная формулировка: по `TextureHandle` (образец: `MinecraftTextureBatchRenderer`).
- [~] **3.7 Освобождение GL-ресурсов** (сделано: `unregister`/`clear`, сброс записи mipmap при resource reload; не сделано: автоматическое освобождение при закрытии экрана). Исходная формулировка: при `UniScreen.close`, смене ресурспака (`IResourceManagerReloadListener`), выходе из мира.

---

## Фаза 4 — Текст `P1`

Решение A3: SDF-текст — основной путь (как в 1.20), ванильный `FontRenderer` — fallback. Генерация глифов (`DefaultFontRegistry`, `SdfGlyphProvider`, `AwtFontFace`) уже в `core`, в cleanroom нужен только GL.

- [~] **4.1 Метрики ванильного шрифта.** (гарнитура стала именованным классом `MinecraftFontFace`, чтобы рендерер отличал её от SDF; метрики пока прежние 7/9 и 2/9, сверить с картинкой.) `MinecraftFonts.defaultFace()`: убедиться, что `ascent / descent / lineHeight` совпадают с реальной базовой линией `FontRenderer` (сейчас константы 7/9 и 2/9). Тест: `measureTextWidth` ≡ сумма `advance`.
- [~] **4.2 `CleanroomSdfTextRenderer`** — написан (`CleanroomSdfAtlas` + `CleanroomSdfTextRenderer`), тесты раскладки есть, **в игре не проверен** (`-Dunigui.sdfText=false` отключает SDF-текст). Порт `MinecraftSdfTextRenderer`: атлас-страницы (1024², SPREAD 8, base 48 px), загрузка глифов через `SdfGlyphProvider`, instance-буфер (32 float'а на глиф), шейдер с `fwidth`-AA. Требует tier `INSTANCED` / `BASIC`; для `LEGACY` — отдельная ветка без instancing (quad на глиф через `Tessellator`-подобный буфер) или fallback на 4.3.
- [x] **4.3 Смешанный рендерер** (`CleanroomTextLayout` + `CleanroomTextRenderer`; порядок runs сохраняется, flush SDF перед каждым ванильным сегментом). Порт `MinecraftMixedTextRenderer`: run'ы с ванильным и SDF-шрифтом в одном `TEXT`-batch'е, порядок сохраняется. `lineInfo`, `measure`, tracking, `TextTransform`, shadow.
- [x] **4.4 Размер и snap** (ванильный текст масштабируется `pixelSize / 9`, snap по `textPixelSnap` и порогу 18 px; D7 закрыт). `TextRun.pixelSize / FONT_HEIGHT` для ванильного пути (D7); pixel snap по `DrawCommand.textPixelSnap()` и `PIXEL_SNAP_MAX_SIZE` для SDF.
- [x] **4.5 `TextBrush`** (`CleanroomTextColors`: solid, linear gradient; в SDF цвет на угол глифа, в ванильном пути градиент рисуется по глифам). `SolidTextBrush` — цвет, `LinearGradientTextBrush` — четыре цвета на углы глифа (в SDF-пути это `InstanceColor0..3`, бесплатно).
- [x] **4.6 Layout.** Перенос, выравнивание и `TextOverflowMode` делает core (`TextEngine`); `measureTextWidth` бэкенда считает через `TextEngine` и без `§`-кодов. См. ограничение в 4.7.
- [x] **4.7 Юникод и цветовые коды.** Решено: **ванильный путь, все стили**. `CleanroomLegacyFormatting` (семантика `FontRenderer.renderStringAtPos`: `§` + символ это код и не занимает места, `0-9a-f` цвет и сброс стилей, `k l m n o` стили, `r` сброс, неизвестный символ как `§f`, `§` в конце строки обычный символ). В ванильном шрифте коды остаются в строке, которую рисует `FontRenderer` (работают все стили), `CleanroomTextLayout` не даёт им ширины и при рисовании по одному глифу (tracking, градиент) переносит состояние префиксом, состояние живёт через `\n` внутри run'а и сбрасывается на новом run'е. В SDF-шрифтах применяются цвета и `§r` (цвет заменяет RGB, альфа сохраняется, как в ваниле), стили отбрасываются. `backend.measureTextWidth` считает без кодов. **Ограничение:** перенос строк и каретка в core считают по `FontFace.advance` каждого символа, поэтому в строках с кодами они ошибаются на 2 символа на код: исправить можно только в core. Юникод: оставлен ванильный (кириллица и CJK через unicode-страницы глифов, чего нет в ваниле рисуется как в ваниле). **Не проверено в игре** (`/unigui text`).
- [x] **4.8 Inline content.** Раскладка и отрисовка целиком в core (`TextEngine.drawInline` режет текст на куски и вызывает `InlineContentRenderer`), backend получает обычные `TEXT`, `RECT`, `TEXTURE`, поэтому отдельного кода не нужно. Резолвер текстур по маркерам (`InlineContentResolvers.textureMarkers`) не делали: по решению оставлен только код-путь. Проверочная строка в `/unigui text` (квадрат и круг внутри строки). **Не проверено в игре**.
- [x] **4.9 Кастомные шрифты.** `CleanroomFonts.register(id, ResourceLocation)` читает TTF/OTF через менеджер ресурсов Minecraft (jar модов и ресурспаки) и регистрирует в `DefaultFontRegistry`; после перезагрузки ресурсов файлы читаются заново, при ошибке остаётся прежняя гарнитура. Встроенные шрифты (`CleanroomFonts.Builtin`: Cuprum Bold, Monocraft, Oswald, Perfect DOS VGA, Play; файлы скопированы из `common` в `assets/unigui/fonts`) регистрируются при первом обращении, чтобы не поднимать AWT на старте. `CleanroomClientBootstrap.install` теперь делает ванильный шрифт дефолтом реестра (A3), иначе первый зарегистрированный шрифт стал бы им сам. Тест `CleanroomFontsTest` читает все пять файлов с classpath и растеризует глиф через AWT. **Не проверено в игре**: AWT внутри процесса Cleanroom (SDL3), на macOS AWT вместе с LWJGL3 известен проблемами с главным потоком.
- [x] **4.10 Тесты.** (`CleanroomTextLayoutTest` — 14, `CleanroomTextColorsTest` — 8; без GL.) Метрики, переносы, tracking, выбор run'ов, упаковка glyph-instance'ов — на моках `FontFace` без Minecraft.

---

## Фаза 5 — Render targets, шейдеры, post-effects `P2`

Зависит от A2.

- [x] **5.1 `CleanroomRenderTarget`** (обёртка над `Framebuffer`; create/resize/close завёрнуты в `Guard`, который возвращает привязанный FBO, viewport и depth, потому что ванильные методы биндят FBO 0 и включают depth). **Не проверено в игре**. Исходная формулировка: Обёртка над `net.minecraft.client.shader.Framebuffer`: `RenderTarget.width/height/colorTexture()`, `ManagedRenderTarget.resize/isDisposed/close`. Depth по `RenderTargetOptions.useDepth`, clear по `clearBeforeRender`.
- [x] **5.2 `createRenderTarget` + `RenderTargetCache`** (`createRenderTarget` в бэкенде; ресайз делает `RenderTargetCache` пересозданием). Исходная формулировка: (core). Ресайз при смене размера окна.
- [x] **5.3 `render(drawList, target)`** (ortho top-left `w/scale x h/scale`, identity modelview, `Pass` подставляет рендерерам плотность пикселей и размер FBO: scissor, AA-кайма, размер глифов считаются по target; вне кадра GL приводится к UI-defaults и возвращается). **Не проверено в игре**. Исходная формулировка: Привязка FBO, проекция под размеры target'а, сохранение и восстановление viewport и ortho-матрицы (образец: `applyRenderTargetProjection`).
- [x] **5.4 Текстурный handle для target'а** (`ColorTextureHandle`: GL-имя биндится напрямую, V переворачивается, источник premultiplied; `TextureLab` рисует offscreen-сцену). Исходная формулировка: `colorTexture()` должен биндиться обычным путём из фазы 3.
- [x] **5.5 Шейдеры** (`CleanroomShaderQuadRenderer`: программа на `ShaderHandle`, кэш, сброс после resource reload; встроенные uniform'ы как в 1.20: `ScreenSize`, `GuiScale`, `Time`, `SquareVertex`, `PoseStack`, `SourceTexture/Texture0`, `SourceSize`, `SourceFlipY`; доп. текстуры на юнитах 1..; `CleanroomSamplerState` стал стеком с указанием юнита). **Не проверено в игре**. Исходная формулировка: `CleanroomShaderProvider` + `ShaderHandle`. Компиляция через `OpenGlHelper`/`ARBShaderObjects`/GL20, кэш uniform location (`MinecraftUniformLocationCache`), `ShaderUniforms`, `ShaderDrawOptions`, текстурные слоты `shaderTextures` (A2).
- [x] **5.6 Команда `SHADER`** (quad на весь viewport как в 1.20; под-прямоугольник задаётся своим vertex shader из `SquareVertex`; загрузка `.fsh/.vsh` из ресурсов через `CleanroomResourceShaderProvider`; плитка `TextureLab` показывает такой шейдер). **Не проверено в игре**. Исходная формулировка: (quad с шейдером) + загрузка из ресурсов (`ShaderResourcePaths`, `ClasspathShaderProvider`).
- [x] **5.7 `UiPostEffectBackend`** (layer target -> ping-pong проходы -> композит; применяется к `UniRenderLayer.postEffect()`; у `UniScreen` в core нет настройки post effect, поэтому для нативного экрана не подключено; `/unigui posteffect` переключает эффекты на overlay-слое). **Не проверено в игре**. Исходная формулировка: `supportsPostEffects`, `renderWithPostEffect` (ping-pong FBO, bloom, blur). Проверка на `TestMod/SolarNavigationPostEffects`, `PostProcessingLayerTestScreen`, `retro_terminal*.fsh`.
- [x] **5.8 Capability fallback** (нет FBO или GLSL 1.50 -> `supportsPostEffects() == false` и слой рисуется без эффекта; `SHADER` пропускается с одним warning'ом; исключение в цепочке отключает эффекты и рисует напрямую). Исходная формулировка: Нет шейдеров / FBO → `supportsPostEffects() == false`, команды `SHADER` пропускаются с одним warning'ом, тесты не падают.

---

## Фаза 6 — Интеграция с рантаймом 1.12.2 `P1`

- [x] **6.1 HUD.** **Найден дефект:** `GuiIngameForge.renderGameOverlay` переопределяет метод `GuiIngame` и не вызывает `super`, поэтому миксин на `GuiIngame.renderGameOverlay` под Forge/Cleanroom не срабатывал и HUD-слои не рисовались. Теперь `RenderGameOverlayEvent.Post` с `ElementType.ALL` (`CleanroomClientEvents.onHud`): это последнее, что стреляет `GuiIngameForge` (после хотбара, чата и списка игроков); при `hideGUI` (F1) не рисуется. Миксин `MixinGuiIngameUniGuiHud` удалён. **Не проверено в игре** (`/unigui lab hud`).
- [x] **6.2 Screen overlay.** Рисование перенесено с миксина `GuiScreen.drawScreen` TAIL на `GuiScreenEvent.DrawScreenEvent.Post` (LOWEST): оно стреляет после всей цепочки `drawScreen` реального класса, а не из середины (например, `InventoryEffectRenderer` и креатив-вкладки рисуют уже после `GuiContainer.drawScreen`). Ввод: **найден второй дефект**: в 1.12.2 `GuiContainer` не переопределяет `handleMouseInput`, старый миксин на него молча не применялся (Mixin без `require` игнорирует пропавшую цель), а миксин `GuiScreen` пропускал контейнеры, то есть мышь оверлея на экранах-контейнерах не работала. Теперь один миксин `GuiScreen.handleMouseInput` для всех экранов; клавиатура через `GuiScreen.handleKeyboardInput` (нажатие, повтор и отпускание), потому что `GuiContainer.keyTyped` не вызывает `super` (там E и цифры хотбара). Все инъекции с `require = 1`. Блокировка pointer'а: `CleanroomUiRuntimeBackend.addPointerBlocker` (аналог `ScreenOverlayRender.PointerBlocker`) + миксин на `GuiContainer.isPointInRegion` (hover слота, тултип и клик по слоту). `isTextInputActive(screen)` есть; с фокусом в `TextInput`/`TextArea` слой съедает E и цифры, потому что `keyTyped` оверлея идёт раньше ванили. **Не проверено в игре** (`/unigui lab overlay`).
- [x] **6.3 Loading-screen слои** (`UniRenderTarget.LOADING_SCREEN`). На 1.12.2 нет `LoadingOverlay`, поэтому «загрузка» это: `LoadingScreenRenderer` («Loading level» / «Saving level», миксин `MixinLoadingScreenRenderer`: слои рисуются прямо перед `updateDisplay`, после фона и полосы ванили) и экран `GuiDownloadTerrain` (через `DrawScreenEvent.Post`). Состояние в `CleanroomLoadingScreen.context()` (вид, заголовок, сообщение, прогресс 0..1 или -1). Ресурс-релоад после старта ничего не показывает; startup splash вне зоны. Ошибка в слое отключает их до конца сессии и логируется. **Не проверено в игре** (`/unigui lab loading`).
- [x] **6.4 Tooltip order.** `MixinGuiUtilsTooltipOrder` перехватывает самую общую перегрузку `GuiUtils.drawHoveringText(ItemStack, ...)` (в неё сходятся `GuiScreen.drawHoveringText` и `renderToolTip`), `CleanroomTooltips` складывает вызовы в очередь между `DrawScreenEvent.Pre` и `Post` и проигрывает их после оверлея в исходном порядке. Включается только если у экрана есть видимый оверлей. Тултипы, которые мод рисует напрямую своим кодом без `GuiUtils`, остаются под оверлеем. **Не проверено в игре**.
- [x] **6.5 World UI surface** (`MinecraftWorldUiSurface`): **пропущено.** В 1.20 это не проекция UI в мир, а offscreen-поверхность «UI -> texture» без ввода, и класс помечен `@Deprecated` («огромный пласт работы, пока заброшено»). Основа для порта (`CleanroomRenderTarget`, `render(drawList, target)`, пост-эффекты) уже есть, поэтому вернуться к этому можно за небольшой объём, если появится потребность.
- [x] **6.6 Ввод.**
  - [x] движение мыши и колесо берутся каждый кадр, а не на тике 20 Гц (`CleanroomPointer`, `CleanroomWheel`); нажатия остаются на событиях;
  - [x] форма курсора ОС (`CleanroomMouseCursors`): `CursorResolver` из core -> `SDL_CreateSystemCursor`/`SDL_SetCursor` (Cleanroom работает на LWJGL 3.4 + SDL3, не GLFW; `lwjgly` стандартных форм не даёт). Сброс при закрытии экрана и в мире без экрана; **не проверено в игре**;
  - [x] key repeat: нативный экран включает `Keyboard.enableRepeatEvents(true)` в `initGui` и выключает в `onGuiClosed`; оверлей включает его, пока в слое фокус в текстовом поле, и возвращает как было (`CleanroomKeyRepeat`: не трогает повтор, который включил сам экран);
  - [x] `mouseClickMove` / drag: отдельный вход не нужен, захваченный виджет получает `pointerMoved` из покадрового опроса `CleanroomPointer`;
  - [x] порядок `keyTyped` и `handleKeyboardInput`: нажатие обрабатывается один раз (оверлей до экрана, отпускание в `handleKeyboardInput`), отладочные хоткеи не срабатывают на повторах (`DebugHotkeys` различает первое нажатие);
  - [-] double-click: в core нет счётчика кликов в `PointerPressedEvent`, делать нечего на уровне backend'а;
  - [-] горизонтальный scroll: LWJGL2-совместимый слой `lwjgly` даёт только вертикальное колесо;
  - [-] IME и composition: недоступно через `lwjgly`.
- [x] **6.7 Паузы и закрытие.** В `UniScreen` добавлено `pauseGame(boolean)` / `pauseGame()` (по умолчанию `true`, как у ванильных экранов; доки en/ru добавлены), нативный экран отдаёт его через `doesGuiPauseGame`; то же подключено в `common` (`MinecraftWidgetScreen.isPauseScreen`, 1.20.1 компилируется). `onGuiClosed` чистит hover/focus/клавиши/capture, возвращает курсор и повтор клавиш. `Esc` закрывает через `GuiScreen.keyTyped`, захват курсора мыши возвращает ваниль в `displayGuiScreen(null)`; `E` экран-не-контейнер не закрывает (как в ваниле). **Не проверено в игре** (`/unigui lab pause`, `/unigui lab nopause`).
- [x] **6.8 Debug-хоткеи.** Слои теперь рисуют `DebugOverlayRenderer` и обрабатывают `DebugHotkeys`, если `UniRenderLayer.debugHotkeysEnabled()`; нативный экран делал это и раньше. Инспектор остаётся в демо `demo/unity`.
- [x] **6.9 Clipboard.** `CleanroomClipboardService` (общий для экрана, слоя и backend'а), анонимные классы удалены.
- [x] **6.10 Resource reload и lifecycle.** Вынос из мира закрывает текущий экран ванилью (`onGuiClosed` освобождает FBO, программы и кэш); слои принадлежат моду и переживают смену мира, их GL-объекты закрываются на клиентском потоке; перезагрузка ресурсов сбрасывает кэши текстур и шейдеров (фазы 3 и 5). Отдельных хуков не потребовалось.
- [x] **6.11 `UiRenderPolicy`** (`CONTINUOUS`, `ON_DIRTY`, `FIXED_FPS`, `vsync`). `CleanroomRenderCache` рисует дерево в окно-размерную текстуру только когда решает `CleanroomRenderPolicies.decide` (порт решения из 1.20, покрыт тестами), в остальных кадрах рисует один textured quad; отладочный оверлей и пост-эффекты остаются живыми. Работает для `UniScreen` и `UniRenderLayer`. **Не проверено в игре** (`/unigui lab policy`).
---

## Фаза 7 — Minecraft-виджеты и интеграции `P2`

Зависит от A4.

- [ ] **7.1 Нейтральные capability-интерфейсы в core:** `UiItemRenderer`, `UiTooltipService`, `UiEntityPreview`. Реализации в `common` (1.20/1.21) и `cleanroom`.
- [ ] **7.2 Item preview.** `RenderItem.renderItemAndEffectIntoGUI` + `RenderHelper.enableGUIStandardItemLighting`, overlay (`renderItemOverlayIntoGUI`), кэш иконок в FBO (порт `FastItemRenderer`). `MinecraftRendererPlatformHook.hasCustomItemRenderer` → TESR / custom IBakedModel.
- [ ] **7.3 Vanilla tooltip.** `GuiScreen.drawHoveringText` / `GuiUtils.drawHoveringText` (Forge), `renderToolTip(ItemStack, x, y)`.
- [ ] **7.4 Entity preview.** `GuiInventory.drawEntityOnScreen`.
- [ ] **7.5 Block preview.**
- [ ] **7.6 Item и Texture picker, `MinecraftItemTooltip`.**
- [ ] **7.7 XML built-ins** (`MinecraftXmlBuiltIns`, `MinecraftXmlRegistryContributor`) на нейтральных типах.
- [ ] **7.8 Container-экраны.** Проверка слотов `GuiContainer` под overlay'ем (Z-order между слотами, ванильным курсором-предметом и UniGUI).

---

## Фаза 8 — Демо, паритет, тесты `P1`

- [ ] **8.1 A5: единый источник демо.** Убрать дубли `cleanroom/demo/*` ↔ `TestMod` (D11).
- [ ] **8.2 Matrix паритета.** Таблица «экран из `TestMod` → статус в 1.12.2» (рабочий, артефакты, не поддержан и причина):
  `TestWidgetsScreen`, `DockingWindowsDemoScreen`, `UnityInspectorScreen`, `LoadingScreenDemo`, `OverlayRenderDemo`, `MarkerCullingDemoScreen`, `MarkerScalingDemoScreen`, `RibbonRoadsDemoScreen`, `PostProcessingLayerTestScreen`, `SolarNavigationScreen`, `RetroTerminalScreen`, минигры (Fork/Plug/Spanner/Sync/Wire/WrenchNut).
- [ ] **8.3 Команда `/unigui <demo>`** со списком демо и tab-completion, вместо единственного `UnityInspectorScreen`.
- [ ] **8.4 Contract-тесты backend'а.** Общий набор тестов `RenderBackend` на mock'е (команда → ожидаемый вызов), чтобы любой новый backend (26.x, Fabric и др.) прогонял тот же набор. Кандидат на `core` testFixtures.
- [ ] **8.5 Прогон существующих `*SelfTest` из `common/src/test`**, где они не зависят от Minecraft, под cleanroom-сборкой.
- [ ] **8.6 Golden-скриншоты** (ручные или авто через `glReadPixels` в debug-команде) для базовых виджетов: сравнение 1.12.2 и 1.20.

---

## Фаза 9 — Производительность `P2`

- [ ] **9.1 Профиль кадра:** число draw-call'ов, `bind`, смен state на кадр для `UnityInspectorScreen` (счётчик в debug-оверлее).
- [ ] **9.2 Zero-alloc на hot path.** Без `new` на кадр в `render` (переиспользуемые буферы вершин, примитивные массивы вместо `List<Float>`).
- [ ] **9.3 JMH** для тесселяции и батчера (`core/src/jmh`).
- [ ] **9.4 Кэш статичных слоёв в FBO** (связано с 6.11).
- [ ] **9.5 GPU-таймеры** (`lastFrameGpuMillis` аналог через `GL_TIME_ELAPSED`, если расширение доступно).

---

## Фаза 10 — Сборка и релиз `P2`

- [ ] **10.1 Версия из `gradle.properties`:** `@Mod(version)`, `mcmod.info` через `processResources` (D12).
- [ ] **10.2 CI.** Отдельный job для cleanroom (JDK 25, сначала `:core:jar` на JDK 21) в `.github/workflows/release.yml`. Артефакт `unigui-cleanroom-1.12.2-<ver>.jar`.
- [ ] **10.3 Проверка bundled jar:** `fastutil` и `core` внутри мода, нет конфликтов с другими модами (relocate при необходимости).
- [ ] **10.4 Проверка совместимости:** запуск вместе с JEI и рендер-модами (Optifine-подобными, Celeritas и т. п.) — проверить миксины и GL-state.
- [ ] **10.5 README:** убирать пункты из раздела «not implemented yet» по мере закрытия задач; добавить таблицу capability'ев (`supportsPostEffects`, shaders, loading screen, world surface).

---

## Рекомендуемый порядок и вехи

| Веха                        | Содержимое                | Результат                                                                                                  |
|-----------------------------|---------------------------|------------------------------------------------------------------------------------------------------------|
| **M1 «Стабильная база»**    | 1.1–1.8, A1–A5 решены     | Нет GL-артефактов, вложенные clip работают, тесты в правильном модуле                                      |
| **M2 «Выглядит как 1.20»**  | 2.1–2.10, 3.1–3.6, 4.1–4.8 | SDF-фигуры и SDF-текст с fallback на тесселяцию, пути, меши, текстуры с tint и фильтрами. `UnityInspectorScreen` визуально совпадает |
| **M3 «Продвинутый рендер»** | 5.x, 4.9, 6.11            | FBO, пользовательские шейдеры, post-effects, UiRenderPolicy                                                                 |
| **M4 «Интеграция с игрой»** | 6.x, 7.x                  | Loading, tooltips, world surface, предметы / сущности                                                      |
| **M5 «Паритет и релиз»**    | 8.x, 9.x, 10.x            | Матрица паритета закрыта, CI собирает 1.12.2                                                               |

Критический путь: **1.1 → 1.2 → A2 (лог GL) → 2.1 → 2.2–2.4 → 2.5 → 4.2 → 4.3 → 5.1 → 5.5**. Задачи 6.x (кроме 6.3, 6.5, 6.11) можно вести параллельно с фазами 2–4.

---

## Правила работы с этим файлом

- Новая функциональность backend'а начинается с чистой математики в `CleanroomRenderMath` или соседнем классе и unit-теста; GL-код тонкий (по скиллам mandatory-junit-jmh-testing и physics-math-library-consolidation).
- Версионный код остаётся в `1.12.2-cleanroom`. Всё, что не зависит от Minecraft (батчинг, геометрия, clip-математика, контракты) — переносить в `core`, чтобы остальные версии (в т. ч. будущие) получали это бесплатно.
- Каждая завершённая задача: отметить `[x]`, дописать дату и коммит; найденные баги добавлять в таблицу дефектов.

