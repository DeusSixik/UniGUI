# GTNH 1.7.10 — порт backend'а UniGUI

Цель: `1.7.10-gtnh` — ещё один версионный модуль рядом с `1.12.2-cleanroom`: поверх нейтрального `core` реализует
render backend и ввод для Minecraft 1.7.10 (форк GTNH, запуск на Java 21+ через lwjgl3ify). Эталон по структуре и
решениям — `1.12.2-cleanroom` (см. его `PORTING_TODO.md`, архитектурные решения A1–A6 действуют и здесь).

Легенда: `[x]` сделано, `[ ]` не сделано, `[~]` частично, `[-]` не делаем.

---

## 0. Вывод по ExampleMod1.7.10: подходит

Проверено на этой машине (Windows, JDK 25 для Gradle, `jdk-21` для компиляции и запуска):

| Проверка                                                                 | Результат                                                                                                                                                                       |
|--------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `./gradlew setupDecompWorkspace` (RetroFuturaGradle, Forge 10.13.4.1614) | OK, ~2 мин на холодном кэше                                                                                                                                                     |
| `./gradlew build` с `core` и `demo` jar внутри мода                      | OK, class-файлы Java 21 (major 65)                                                                                                                                              |
| `./gradlew test` (JUnit 5)                                               | OK, 13 классов, 130+ тестов                                                                                                                                                     |
| `./gradlew runClient21` (lwjgl3ify + Hodgepodge + UniMixins)             | OK: мод грузится, `UniGUI LWJGL: 3.4.2+13`, `UniGUI OpenGL: version='4.6.0 NVIDIA 595.97' ... -> shader tier INSTANCED` (настоящий `GL.getCapabilities()` LWJGL 3 из кода мода) |

Что это даёт и чего нет:

- Шаблон — только сборка (плагин `com.gtnewhorizons.gtnhconvention`); код мода пишется как обычно. Из шаблона взяты
  `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `dependencies.gradle`, `repositories.gradle`, wrapper.
- `enableModernJavaSyntax = modern` + `forceToolchainVersion = 21`: нативный байткод Java 21. Это нужно, потому что
  `core` собран под Java 17 (records, sealed, `instanceof` pattern, `String.isBlank` и т.д.); режимы `jabel` и
  `jvmDowngrader` рассчитаны на Java 8 и для нас не подходят. Мод **не запустится на Java 8**; в GTNH для этого есть
  lwjgl3ify (Java 17/21/25).
- Gradle надо запускать на **JDK 25** (`JAVA_HOME`), плагин GTNH требует Java 25 для самого Gradle. Компилирует он с
  тулчейном 21 (скачивает/находит сам). `core` и `demo` по-прежнему собираются корневым билдом на 17.
- Миксины: UniMixins уже подтягивается рантаймом GTNH. Включаются в `gradle.properties`
  (`usesMixins = true`, `mixinsPackage = dev.sixik.unigui.gtnh.mixin`), см. фазу 6.
- Монорепо: git-версия плагина берётся из тегов корня (получалось `1.4.0-sixik.129+...-dirty`), поэтому в
  `addon.late.gradle` версия принудительно равна `mod_version` из корневого `gradle.properties`.
- `gradle.properties` в корне репозитория в `.gitignore`; в `1.7.10-gtnh/.gitignore` добавлено `!gradle.properties`.

## Как собирать

```
# в корне репозитория (JDK 17 для core/demo)
./gradlew :core:jar :demo:jar

# в 1.7.10-gtnh (JAVA_HOME = JDK 25)
./gradlew build
./gradlew test
./gradlew runClient21
```

После любой правки `core` сначала `:core:jar` в корне, иначе модуль возьмёт старый jar.

---

## 1. Что уже перенесено

Только классы без зависимости от Minecraft и GL (пакет `dev.sixik.unigui.gtnh.backend`, префикс `Gtnh`, логика один в
один с `Cleanroom*`), с их тестами:

- [x] `GtnhDrawStats`, `GtnhSdfStats`, `GtnhProfiling` (+ тест), `GtnhGpuTimer`
- [x] `GtnhRenderMath` (+ тест), `GtnhRenderPolicies` (+ тест)
- [x] `GtnhShapeTessellator` (+ тест), `GtnhSdfShapeInstances` (+ тест), `GtnhVertexList`
- [x] `GtnhTextLayout` (+ тест), `GtnhTextColors` (+ тест), `GtnhLegacyFormatting` (+ тест)
- [x] `GtnhLayerRegistry`, `GtnhPointerBlockers`, `GtnhTooltips`, `GtnhKeyRepeat`, `GtnhLoadingScreen`, `GtnhPixels`
- [x] `GtnhTextureHandle`
- [x] `GtnhGlCapabilities` (+ тест), `GtnhGpuTimer`: код Cleanroom без изменений (LWJGL 3 API), проверен в `runClient21`
- [x] `GtnhClientBootstrap`: пока только проба GL на первом кадре (лог `UniGUI OpenGL: ...`)
- [x] `UniGuiGtnh` (`@Mod`, `cpw.mods.fml.*`), `mcmod.info`

Не перенесено: всё, что рисует. На `runClient21` проверены загрузка мода и проба GL-контекста на первом кадре.

**Не сделано в уже перенесённом:** `GtnhLoadingScreenTest` и `GtnhLoadingHooks` ждут полноценный `GtnhClientBootstrap` с бэкендом (фаза 1).

---

## 2. LWJGL 3: как модуль его получает

Решение: модуль компилируется и работает против **настоящего LWJGL 3**, который даёт lwjgl3ify, а не против
LWJGL 2.9.4. Это тот же API, что в `1.12.2-cleanroom`, поэтому GL-код переносится почти без правок.

Как это устроено в lwjgl3ify (проверено по `lwjgl3ify-3.0.33-dev.jar` и по Angelica):

- В рантайме lwjgl3ify трансформером (`LwjglRedirectTransformer`) переписывает во **всех** загружаемых классах ссылки
  `org/lwjgl/*` на совместимый с LWJGL 2 слой `org/lwjglx/*`. Код, который хочет честный LWJGL 3, должен выйти из
  этой трансформации. Для этого есть два признака, действует любой:
  - `Lwjgl3ify-Aware: true` в манифесте jar пропускает все классы этого jar;
  - аннотация `@me.eigenraven.lwjgl3ify.api.Lwjgl3Aware` на классе пропускает этот класс (нужна в dev-окружении, где
    классы берутся из `build/classes` и манифеста нет).
- В `addon.gradle` сделано: `minecraft { mainLwjglVersion = 3 }` (RetroFuturaGradle кладёт на compile-classpath
  `org.lwjgl:lwjgl`, `lwjgl-opengl`, `lwjgl-sdl`, `lwjgl-stb`... той же версии 3.4.2, что и в рантайме),
  `compileOnly` на `lwjgl3ify:3.0.33:dev` (аннотация и `org.lwjglx`), после `compileJava` на каждый класс ставится
  `@Lwjgl3Aware` (ASM, идемпотентно, порт `injectLwjgl3Aware` из Angelica), в манифест `jar` пишется
  `Lwjgl3ify-Aware: true`.

Правила для кода модуля:

- GL, SDL и прочее по API LWJGL 3: `GL.getCapabilities()`, `GL11.glGetIntegerv`, `GL33`, `org.lwjgl.sdl.*`
  (SDL3 доступен, значит курсоры ОС как в Cleanroom `CleanroomMouseCursors` портируются; lwjgl3ify 3.x окно создаёт
  через SDL).
- Классы, которых нет в LWJGL 3 (`Display`, `Mouse`, `Keyboard`, `Cursor`), брать из слоя совместимости
  `org.lwjglx.input.Mouse` / `Keyboard` / `Cursor` и `org.lwjglx.opengl.Display` (проверено, что они есть в `lwjgl3ify-dev.jar`). Одноимённые `org.lwjgl.input.*` в этом jar только пустые заглушки с константами, использовать их нельзя.
- **Граница:** в сигнатуре вызова в код Minecraft или чужого мода (не «aware») не должно быть типов `org.lwjgl.*`:
  там дескриптор будет переписан в `org.lwjglx` и вызов не найдёт метод. Для `java.nio.*` буферов и примитивов
  это не относится. Angelica закрепляет это архитектурным тестом `Lwjgl3AwareBoundaryTest`; нам пригодится такой же,
  когда появятся вызовы в MC.
- Тесты запускаются на обычном classpath без lwjgl3ify: чистая логика (не вызывающая GL) работает, классы, которые
  зовут GL, из JUnit не запускаются.

Что остаётся проверить: поведение `org.lwjglx.*` (Mouse/Keyboard) в нашем вводе и что lwjgl3ify по умолчанию даёт
compatibility-контекст (с фиксированным конвейером), нужный `Tessellator` ванильного рендера.
### Таблица API 1.12.2 → 1.7.10

| 1.12.2 (Cleanroom)                                                        | 1.7.10 (GTNH)                                                                                                      |
|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `net.minecraftforge.fml.*`                                                | `cpw.mods.fml.*` (`Mod`, `SideOnly`, `SubscribeEvent`, `FMLPreInitializationEvent`)                                |
| `GlStateManager`                                                          | нет; сырой `GL11` + собственный кэш/сохранение состояния в `GtnhGlState` (D5 из 1.12.2 тут не актуален)            |
| `BufferBuilder`, `DefaultVertexFormats`                                   | `Tessellator.instance` (`startDrawing(mode)`, `setColorRGBA_I`, `setTextureUV`, `addVertex`, `draw`)               |
| `GL.getCapabilities()` / `GLCapabilities`                                 | то же (LWJGL 3), см. раздел 2                                                                                      |
| `Mouse`, `Keyboard`, `Display` (LWJGL 2, CleanroomPointer и др.)          | `org.lwjglx.input.Mouse` / `Keyboard` / `org.lwjglx.opengl.Display` (shim lwjgl3ify)                               |
| `ITextComponent`, `TextComponentString`                                   | `IChatComponent`, `ChatComponentText`                                                                              |
| `ItemStack.EMPTY`, `NonNullList`                                          | `null` вместо пустого стека, `List<ItemStack>`                                                                     |
| `Item.getSubItems(tab, NonNullList)`                                      | `Item.getSubItems(Item, CreativeTabs, List)`                                                                       |
| `ForgeRegistries.ITEMS`, `EntityEntry`                                    | `Item.itemRegistry` / `Block.blockRegistry`, `EntityList` (строковые имена)                                        |
| `IBakedModel`, `BakedQuad` (рендер предметов)                             | `RenderItem.renderItemAndEffectIntoGUI(FontRenderer, TextureManager, ItemStack, x, y)`                             |
| `ScaledResolution(mc)`                                                    | `new ScaledResolution(mc, mc.displayWidth, mc.displayHeight)`                                                      |
| `FontRenderer.drawString(String, float, float, ...)`                      | только `int x, int y`: субпиксель через `glTranslatef`                                                             |
| `CommandBase.execute(server, sender, args)`                               | `processCommand(sender, args)`, регистрация в `FMLServerStartingEvent`; клиентские команды: `ClientCommandHandler` |
| `Minecraft.addScheduledTask`                                              | нет; очередь на клиентском потоке своими силами                                                                    |
| `TextureUtil.uploadTextureImageAllocate`, `DynamicTexture`, `Framebuffer` | есть, сигнатуры сверять по декомпилированным исходникам RFG                                                        |
| Mixin на `GuiScreen.handleMouseInput` и т.д.                              | те же классы, но SRG-имена; UniMixins + refmap из RFG                                                              |

Это ожидаемые отличия по памяти о 1.7.10, не все из них проверены компиляцией: сверять по исходникам в
`~/.gradle/caches/retro_futura_gradle` при портировании каждого класса.

---

## 3. План

Порядок такой же, как у Cleanroom: сначала корректность базового пути, потом примитивы, текстуры, текст.

### Фаза 1 — Каркас и базовый путь `P0`

- [ ] 1.1 `GtnhClientBootstrap`: `UniGuiRuntime.install` (как `CleanroomClientBootstrap`), вызов из `UniGuiGtnh.preInit` на клиенте (`event.getSide().isClient()`).
- [ ] 1.2 `GtnhGlState`: сохранение/восстановление blend, depth, alpha test, lighting, cull, texture2D, scissor, color, matrix mode, стеков матриц **на `GL11`** (нет `GlStateManager`, значит и кэша ваниллы, с которым надо синхронизироваться; но Hodgepodge/Angelica могут держать свой кэш, проверить).
- [ ] 1.3 `GtnhGuiRenderBackend` + scissor stack + tessellation-fallback (`GtnhShapeRenderer` поверх готового `GtnhShapeTessellator`; вывод через `Tessellator.instance`).
- [ ] 1.4 `GtnhWidgetScreen` (нативный экран `UniScreen`) и ввод: `GtnhKeyCodes` (`org.lwjglx.input.Keyboard.KEY_*` → core), `GtnhPointer`, `GtnhWheel`, clipboard.
- [ ] 1.5 Команда `/unigui` через `ClientCommandHandler`.
- [ ] 1.6 Перенести тесты, зависящие от этих классов (`GtnhLoadingScreenTest`, `KeyCodes`, `Pointer`, `Wheel`, `Display`).
- [ ] 1.7 Ручной smoke-тест в `runClient21`: открытие/закрытие, ресайз, GUI Scale 1–4, fullscreen.

### Фаза 2 — Шейдеры и SDF-фигуры `P1`

- [ ] 2.1 `GtnhShaderProgram` (GL20), `GtnhSdfShapeRenderer` (instanced/basic по `GtnhGlCapabilities.tier()`), `GtnhGlMatrices`.
- [ ] 2.2 `GtnhShapeRenderer` как fallback, `DrawBatcher` в `renderBatch`.
- [ ] 2.3 `PATH`, `MESH`, rounded clip по возможности.

### Фаза 3 — Текстуры `P1`

- [ ] `GtnhTextures`, `GtnhDynamicTextures`, `GtnhSamplerState` (GL33 sampler objects, иначе `glTexParameteri`), rounded texture, batch.

### Фаза 4 — Текст `P1`

- [ ] Ванильный `FontRenderer` (1.7.10: целые координаты; `MinecraftFontFace`, `MinecraftFonts` с метриками 8 px вместо 9 проверить), SDF-атлас и `GtnhSdfTextRenderer`, смешанный рендерер, `GtnhFonts`.
- [ ] Риск: AWT внутри процесса GTNH (lwjgl3ify, Java 21) — проверить растеризацию глифов на реальном клиенте.

### Фаза 5 — Render targets, шейдеры, post-effects `P2`

- [ ] `GtnhRenderTarget` на `Framebuffer` (в 1.7.10 есть `OpenGlHelper.isFramebufferEnabled()`; при `false` post-effects недоступны), `SHADER`, `UiPostEffectBackend`.

### Фаза 6 — Интеграция с рантаймом `P1`

- [ ] Включить миксины в `gradle.properties` (`usesMixins = true`, `mixinsPackage`, `mixinPlugin` по необходимости), `unigui.mixins.json` генерируется плагином.
- [ ] HUD: `RenderGameOverlayEvent.Post` (`ElementType.ALL`), как в 1.12.2.
- [ ] Оверлей экрана: `GuiScreenEvent.DrawScreenEvent.Post`; ввод — миксин на `GuiScreen.handleMouseInput` / `handleKeyboardInput`; блокировка pointer'а — миксин на `GuiContainer` (проверка наведения на слот).
- [ ] Loading-screen: миксин на `LoadingScreenRenderer` + `GuiDownloadTerrain`.
- [ ] Порядок тултипов: в 1.7.10 нет `GuiUtils.drawHoveringText`, смотреть `GuiScreen.drawHoveringText` / `GuiContainer.func_146285_a`.
- [ ] Пауза, закрытие, key repeat, `UiRenderPolicy`.

### Фаза 7 — Minecraft-виджеты `P2`

- [ ] `dev.sixik.unigui.widgets.minecraft.*` на типах 1.7.10: предметы (`RenderItem`, без моделей), блоки, сущности (`EntityList`), тултипы, `GtnhItemIcons`, `GtnhVanillaDraw`.

### Фаза 8 — Демо и релиз `P3`

- [ ] `demo` команды (`/unigui lab ...`), добавление модуля в CI (release workflow), docs.
- [ ] Общий вынос чистой логики (`RenderMath`, `ShapeTessellator`, `TextLayout`, …) из `cleanroom` и `gtnh` в один нейтральный модуль: сейчас это копии с разными префиксами (решение A5 из 1.12.2 по демо относится сюда же).

---

## 4. Риски

- lwjgl3ify подменяет LWJGL: возможны расхождения в поведении `Display`, `Mouse` (grab, cursor) и контекстом GL. Считать
  код проверенным только после запуска.
- GTNH-моды (Angelica, Hodgepodge) патчат `Tessellator`, `FontRenderer`, `RenderItem`. Рисовать в обход них можно,
  но состояние GL после наших проходов должно возвращаться точно.
- Java 21+ обязательна, совместимость с «чистым» Forge 1.7.10 на Java 8 не обеспечивается.
- Версионный дубль логики (см. 8): правка формулы в `cleanroom` не попадёт в `gtnh` автоматически.
