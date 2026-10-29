# Cleanroom 1.12.2 — план портирования рендера и backend API

Цель: довести `1.12.2-cleanroom` до функционального паритета с `common` (1.20.1 / 1.21.1) поверх
нейтрального `core`, не копируя логику в версионный модуль. Эталон — `common/.../backend/minecraft_impl`
(`MinecraftGuiRenderBackend` и соседние ~9 000 строк). Текущая реализация — `cleanroom/backend/*`
(~1 300 строк, immediate-mode GL11).

Легенда: `[x]` сделано, `[ ]` не сделано, `[~]` сделано частично или с дефектами.
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

---

## Архитектурные решения (принять ДО фаз 2, 4, 5)

Это развилки, от которых зависит объём работ. Рекомендация указана, но решение за тобой.

- [ ] **A1. Способ рисования примитивов.**
  - (a) CPU-тесселяция через `Tessellator` / `BufferBuilder` (`POSITION_COLOR`, `POSITION_TEX_COLOR`). Работает везде, без шейдеров. AA через feather-кольцо с alpha=0.
  - (b) SDF-шейдеры, как в `MinecraftSdfShapeRenderer`. Лучшее качество, но нужен GLSL и dialect-адаптер.
  - **Рекомендация:** сначала (a) за интерфейсом `ShapeRenderer`, потом (b) как опциональный апгрейд (задача 2.9).
- [ ] **A2. GLSL dialect.** Шейдеры core написаны под `#version 150`, а 1.12.2 даёт compat-контекст (на Mesa/Linux может быть только 3.0 или 2.1).
  - Варианты: требовать GL ≥ 3.2 compat и делать fallback на CPU-путь, либо транслировать 150 → 120 (`in/out` → `attribute/varying`, `texture()` → `texture2D()`, `out vec4` → `gl_FragColor`).
  - **Рекомендация:** capability-проверка при старте + `ShaderDialectAdapter`, который переписывает 150 → 120 простыми заменами. Шейдеры, не поддающиеся адаптации, отключаются с warning.
- [ ] **A3. Текст.**
  - (a) только ванильный `FontRenderer` + масштабирование матрицей;
  - (b) свой glyph-atlas (`java.awt.Font` или STB) с поддержкой `FontRegistry`;
  - (c) SDF-текст, как в 1.20.
  - **Рекомендация:** (a) для паритета «выглядит как ваниль» (фаза 4), (b) для кастомных шрифтов (4.8), (c) только после 2.9.
- [ ] **A4. MC-специфичные виджеты** (`common/.../widgets/minecraft`: item, entity, block, tooltip). Они завязаны на типы 1.20 (`ItemStack`, `Component`). Нужны нейтральные capability-интерфейсы в core (по образцу `UiPostEffectBackend`): `UiItemRenderer`, `UiTooltipService`, `UiEntityPreview`. Иначе каждую версию придётся писать виджеты заново.
- [ ] **A5. Общее демо.** Вынести `Unity*` и `ComplexUiDemo` из `TestMod` и `cleanroom` в один version-neutral источник (отдельный gradle-модуль `demo` или `core` testFixtures), оставив в версионных модулях только вход (команда / screen opener).

---

## Фаза 1 — Корректность базового пути `P0`

Цель: то, что уже заявлено в README, должно работать детерминированно. Без новых фич.

- [ ] **1.1 GL-state guard.** Класс `CleanroomGlState`: сохранить и восстановить blend, depth, alpha test, lighting, cull, texture2D, scissor, color, matrix mode и стеки матриц. Менять состояние через `GlStateManager`, а не через `GL11` (D5, D9). *Готово, когда:* после `/unigui` ваниль-HUD и чат рисуются без артефактов, `GlStateManager` не расходится с реальным GL (проверка через `GL11.glGetBoolean` в debug-режиме).
- [ ] **1.2 Scissor stack.** Использовать `core impl/render/ScissorStack` (пересечение вложенных clip). Учитывать `Framebuffer` и HiDPI (`displayWidth/Height` против `ScaledResolution`). Поведение при transform: axis-aligned bounds или предупреждение (D1). Unit-тест на математику пересечения и пересчёт в окно (вынести в `CleanroomRenderMath`).
- [ ] **1.3 Color и texture state.** Единая точка `bindColor(argb)` / `resetColor()`, `disableTexture2D()` для не-текстурных команд (D2, D3).
- [ ] **1.4 Tint и blend для текстур.** `Paint.color` → `glColor4f`, `BlendMode` → `blendFunc`, `opacityMultiplier` из `RenderContext` (D4). Тест на таблицу `BlendMode → (src, dst)`.
- [ ] **1.5 Перейти с `glBegin/glEnd` на `Tessellator`.** Минимум для `LINE`, `CIRCLE`, `TEXTURED_QUAD`. Устраняет зависимость от deprecated immediate mode и готовит батчинг.
- [ ] **1.6 `CleanroomRenderHost`: единый расчёт масштабов.** Сейчас формулы `backendScale` / `effectiveScale` дублируются в `CleanroomWidgetScreen` и `CleanroomWidgetLayer`. Вынести в `CleanroomRenderMath` (чистые функции, покрытые тестами).
- [ ] **1.7 Перенести тесты `api.input`, `api.screen` в `core`** (D10).
- [ ] **1.8 Smoke-тест в клиенте.** Чеклист: `/unigui` открывается, закрывается по Esc, ресайз окна, смена GUI Scale 1–4, fullscreen, HUD-слой + открытый инвентарь. Зафиксировать результат в этом файле.

**Критерий завершения фазы:** `runClient` без GL-ошибок в `logs/` (включить `-Dorg.lwjgl.util.Debug=true` и проверку `glGetError` после кадра в debug-флаге), скриншоты до и после совпадают.

---

## Фаза 2 — Геометрия и примитивы `P1`

Зависит от A1.

- [ ] **2.1 `DrawList` → batch.** Подключить `core impl/render/DrawBatcher` / `SimpleDrawBatcher` (как `renderBatches` в 1.20: сборка по state-ключам: texture, blend, clip). Меньше `bind` и draw-call'ов.
- [ ] **2.2 `ROUNDED_RECT` fill.** Тесселяция углов (`segments` из команды или авто по `radius`), AA-кольцо (D6).
- [ ] **2.3 `ROUNDED_RECT` stroke** с толщиной и корректными внутренними/внешними краями.
- [ ] **2.4 `CIRCLE` / эллипс.** AA, `segments`, stroke через кольцо, а не `GL_LINE_LOOP`.
- [ ] **2.5 `LINE`.** Толстые линии как quad'ы (а не `glLineWidth`), dash через `Paint.dashLength/dashGap/dashOffset` (D8), caps.
- [ ] **2.6 `PATH`.** Обход `VectorPath` (`MOVE_TO`, `LINE_TO`, `QUADRATIC_TO`, `CUBIC_TO`, `CLOSE`), flatten кривых, fill (ear-clipping / стенсил) и stroke (miter / round join). Использовать `PathAntiAliasing` из core. Образец: `renderPath` / `renderFilledPath` в 1.20 (строки ~1390–1560).
- [ ] **2.7 `MESH` и `DrawVertex`.** Цветной и текстурированный меш, per-vertex color (покрывает `addRectFilledMultiColor`, `addRibbon`, градиенты).
- [ ] **2.8 Clip с transform и скруглением.** Повёрнутый clip через stencil или fallback на bounding rect. Rounded clip — по возможности.
- [ ] **2.9 *(опц.)* SDF-шейдерный `ShapeRenderer`.** Порт `MinecraftSdfShapeRenderer`, активируется capability-проверкой A2.
- [ ] **2.10 Тесты.** Чистая геометрия (вершины углов, dash-разбиение, flatten) — в `CleanroomRenderMath` / `GeometryBuilder`, без GL. Для горячего пути (тесселяция rounded rect, dash) добавить JMH рядом с `core/src/jmh`.

---

## Фаза 3 — Текстуры `P1`

- [ ] **3.1 `CleanroomTextureRegistry`.** `TextureHandle.id()` → `ResourceLocation` + кэш размеров. Проверка существования ресурса, `missingno` fallback с warning один раз.
- [ ] **3.2 Динамические текстуры.** Порт `UniGuiTextures`: `register(id, w, h, RGBA ByteBuffer, options)`, `replace`, `unregister`, `clear`. На 1.12.2 — `DynamicTexture` / `TextureUtil.uploadTextureImageAllocate`. Нужен для генерации карт, FBO-превью, `TextureManagementSelfTest`.
- [ ] **3.3 `TextureOptions`.** `minFilter/magFilter` (NEAREST/LINEAR), `wrapS/T`, mipmaps, `premultipliedAlpha` → `glTexParameteri` (D4). Состояние сэмплера кэшируется на текстуру (образец: `MinecraftTextureSamplerState`).
- [ ] **3.4 Размещение.** Проверить, кто раскрывает `TexturePlacement`, `ImageFit`, `NineSlice`, `SliceBorders`: если core выдаёт готовые `TEXTURE` / `TEXTURED_QUAD` — ничего не нужно, если нет — реализовать в backend. Покрыть тестом UV-математику.
- [ ] **3.5 Rounded texture** (`addImageRounded`, `texture(..., radius, ...)`): клип углов через меш (зависит от 2.7).
- [ ] **3.6 Пакетная отрисовка текстур** по `TextureHandle` (образец: `MinecraftTextureBatchRenderer`).
- [ ] **3.7 Освобождение GL-ресурсов** при `UniScreen.close`, смене ресурспака (`IResourceManagerReloadListener`), выходе из мира.

---

## Фаза 4 — Текст `P1`

Зависит от A3.

- [ ] **4.1 Метрики.** `MinecraftFonts.defaultFace()`: убедиться, что `ascent/descent/lineHeight` совпадают с реальной базовой линией `FontRenderer` (сейчас 7/9 и 2/9 — константы). Тест: `measureTextWidth` ≡ сумма `advance`.
- [ ] **4.2 Размер текста.** Масштабировать матрицей по `TextRun.pixelSize / FONT_HEIGHT` (D7). Pixel snap по `DrawCommand.textPixelSnap()`.
- [ ] **4.3 Rich text.** Рисовать run за run'ом: цвет, tracking, `TextTransform`, shadow. Образец: `MinecraftMixedTextRenderer` (`lineInfo`, `measure`, `argb`).
- [ ] **4.4 `TextBrush`.** `SolidTextBrush` — просто цвет. `LinearGradientTextBrush` — цвет на глиф (CPU, по позиции глифа).
- [ ] **4.5 Layout.** Выяснить, кто делает перенос, выравнивание и `TextOverflowMode` (ellipsis): core (`TextEngine`) или backend. Использовать `measureTextWidth` бэкенда, чтобы переносы совпадали с отрисовкой.
- [ ] **4.6 Unicode и цветовые коды.** `§`-коды: решить, трактовать ли их как форматирование или как литерал. Юникод / Russian / CJK через `unicodeFlag`.
- [ ] **4.7 Inline content** (`InlineContentRenderer`, `InlineContentSpan`): иконки и текстуры внутри текста.
- [ ] **4.8 *(опц.)* Кастомные шрифты** (`FontRegistry`, TTF/OTF из `TestMod/assets/fonts`): glyph-atlas на `java.awt.Font`.
- [ ] **4.9 *(опц.)* SDF-текст** (порт `MinecraftSdfTextRenderer`).
- [ ] **4.10 Тесты.** Метрики, переносы, tracking, выбор run'ов — на моках `FontFace` (без Minecraft).

---

## Фаза 5 — Render targets, шейдеры, post-effects `P2`

Зависит от A2.

- [ ] **5.1 `CleanroomRenderTarget`.** Обёртка над `net.minecraft.client.shader.Framebuffer`: `RenderTarget.width/height/colorTexture()`, `ManagedRenderTarget.resize/isDisposed/close`. Depth по `RenderTargetOptions.useDepth`, clear по `clearBeforeRender`.
- [ ] **5.2 `createRenderTarget` + `RenderTargetCache`** (core). Ресайз при смене размера окна.
- [ ] **5.3 `render(drawList, target)`.** Привязка FBO, проекция под размеры target'а, сохранение и восстановление viewport и ortho-матрицы (образец: `applyRenderTargetProjection`).
- [ ] **5.4 Текстурный handle для target'а.** `colorTexture()` должен биндиться обычным путём из фазы 3.
- [ ] **5.5 `CleanroomShaderProvider` + `ShaderHandle`.** Компиляция через `OpenGlHelper`/`ARBShaderObjects`/GL20, кэш uniform location (`MinecraftUniformLocationCache`), `ShaderUniforms`, `ShaderDrawOptions`, текстурные слоты `shaderTextures` (A2).
- [ ] **5.6 Команда `SHADER`** (quad с шейдером) + загрузка из ресурсов (`ShaderResourcePaths`, `ClasspathShaderProvider`).
- [ ] **5.7 `UiPostEffectBackend`.** `supportsPostEffects`, `renderWithPostEffect` (ping-pong FBO, bloom, blur). Проверка на `TestMod/SolarNavigationPostEffects`, `PostProcessingLayerTestScreen`, `retro_terminal*.fsh`.
- [ ] **5.8 Capability fallback.** Нет шейдеров / FBO → `supportsPostEffects() == false`, команды `SHADER` пропускаются с одним warning'ом, тесты не падают.

---

## Фаза 6 — Интеграция с рантаймом 1.12.2 `P1`

- [ ] **6.1 HUD.** Сейчас хук на `GuiIngame` через миксин. Проверить: порядок относительно hotbar, чата и debug-экрана; отсутствие рендера при `hideGUI` (F1); кадры в паузе и в меню; `RenderGameOverlayEvent` как альтернатива миксину (проще для совместимости с другими модами).
- [ ] **6.2 Screen overlay.** `UniScreenOverlayRender` для `GuiScreen` и `GuiContainer`; блокировка pointer'а (аналог `ScreenOverlayRender.PointerBlocker`); `isTextInputActive` — гасить hotkey'и инвентаря (E, цифры) при фокусе в `TextField`.
- [ ] **6.3 Loading-screen слои** (`UniRenderTarget.LOADING_SCREEN`, сейчас `UnsupportedOperationException`). На 1.12.2 нет `LoadingOverlay`. Решить, что считается loading: `GuiDownloadTerrain`, `LoadingScreenRenderer` («Loading level», «Saving»), ресурс-релоад. Early-window FML вне зоны. Перенести `LoadingScreenContext`.
- [ ] **6.4 Tooltip order.** Аналог `MixinGuiGraphicsTooltipOrder`: отложенный рендер ванильных тултипов поверх overlay (`DeferredMinecraftTooltips`).
- [ ] **6.5 World UI surface** (`MinecraftWorldUiSurface`): панели в мире через `RenderWorldLastEvent` + projection / unproject, ввод по лучу.
- [ ] **6.6 Ввод.**
  - key repeat (`Keyboard.enableRepeatEvents(true)` на время фокуса в текстовом поле);
  - `mouseClickMove` / drag;
  - double-click;
  - горизонтальный scroll (если доступен);
  - IME и composition;
  - порядок `keyTyped` и `handleKeyboardInput` без двойной обработки.
- [ ] **6.7 Паузы и закрытие.** `doesGuiPauseGame`, `onGuiClosed`, restoring grab курсора, `Esc` / `E` для `UniScreen`. Добавить в `UniScreen` настройку «pause game», если её нет.
- [ ] **6.8 Debug-хоткеи** (`MinecraftDebugHotkeys`, `DebugOverlayRenderer`, inspector).
- [ ] **6.9 Clipboard.** Сейчас `GuiScreen.getClipboardString()`. Вынести в `CleanroomClipboardService` (общая реализация для screen и layer, сейчас дублируется анонимным классом в двух местах).
- [ ] **6.10 Resource reload и lifecycle.** Корректное закрытие всех экранов и слоёв при выходе из мира и закрытии клиента.
- [ ] **6.11 `UiRenderPolicy`** (`CONTINUOUS`, `ON_DIRTY`, `FIXED_FPS`, `vsync`): сейчас игнорируется. Реализовать кэш последнего кадра в FBO (после 5.x).

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
| **M2 «Выглядит как 1.20»**  | 2.1–2.8, 3.1–3.6, 4.1–4.7 | Скругления, пути, меши, rich text, текстуры с tint и фильтрами. `UnityInspectorScreen` визуально совпадает |
| **M3 «Продвинутый рендер»** | 5.x, 2.9, 4.8–4.9, 6.11   | FBO, шейдеры, post-effects, UiRenderPolicy                                                                 |
| **M4 «Интеграция с игрой»** | 6.x, 7.x                  | Loading, tooltips, world surface, предметы / сущности                                                      |
| **M5 «Паритет и релиз»**    | 8.x, 9.x, 10.x            | Матрица паритета закрыта, CI собирает 1.12.2                                                               |

Критический путь: **1.1 → 1.2 → 2.1 → 2.2/2.7 → 3.3 → 4.2/4.3 → 5.1 → 5.5**. Задачи 6.x (кроме 6.3, 6.5, 6.11) можно вести параллельно с фазами 2–4.

---

## Правила работы с этим файлом

- Новая функциональность backend'а начинается с чистой математики в `CleanroomRenderMath` или соседнем классе и unit-теста; GL-код тонкий (по скиллам mandatory-junit-jmh-testing и physics-math-library-consolidation).
- Версионный код остаётся в `1.12.2-cleanroom`. Всё, что не зависит от Minecraft (батчинг, геометрия, clip-математика, контракты) — переносить в `core`, чтобы остальные версии (в т. ч. будущие) получали это бесплатно.
- Каждая завершённая задача: отметить `[x]`, дописать дату и коммит; найденные баги добавлять в таблицу дефектов.
