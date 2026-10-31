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
- [x] Фаза 1 целиком, см. раздел 3: бэкенд, нативный экран, ввод, команда `/unigui demo`
- [x] `UniGuiGtnh` (`@Mod`, `cpw.mods.fml.*`), `mcmod.info`

Фаза 1 проверена в `runClient21` визуально (демо `docking`), кроме ввода.

**Не сделано в уже перенесённом:** `GtnhLoadingScreenTest` и `GtnhLoadingHooks` ждут слои загрузочного экрана (фаза 6).

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

- [x] 1.1 `GtnhClientBootstrap`: `UniGuiRuntime.install`, шрифт ванили по умолчанию, `DemoAssets`, клиентская команда, проба GL на первом кадре. Вызывается из `UniGuiGtnh.preInit` на клиенте.
- [x] 1.2 `GtnhGlState` на `GL11`/`GL14` (blend + функция, texture2D, alpha test, depth, lighting, cull, scissor и его box, shade model, matrix mode, цвет). `GlStateManager` в 1.7.10 нет, поэтому кэша ваниллы для синхронизации нет; но Angelica/Hodgepodge в большой сборке могут перехватывать `GL11` — проверить на сборке с ними.
- [x] 1.3 `GtnhGuiRenderBackend` + `ScissorStack` + `GtnhShapeRenderer` (поверх `GtnhShapeTessellator`, вывод через `Tessellator.instance`, один draw на batch) + `GtnhTextRenderer` (ванильный `FontRenderer`, целые координаты: позиция и масштаб через матрицу) + `GtnhTextures`. Команды: `RECT`, `ROUNDED_RECT`, `LINE`, `CIRCLE`, `PATH`, `MESH`, `TEXTURE`, `TEXTURED_QUAD`, `TEXT`, `PUSH_CLIP`/`POP_CLIP`, `CUSTOM`. `SHADER` и `DRAW_CMD` пропускаются с одним warning'ом; `createRenderTarget` не поддержан.
- [x] 1.4 `GtnhWidgetScreen` (нативный экран `UniScreen`, рендер напрямую без render cache) и ввод: `GtnhKeyCodes` (`org.lwjglx.input.Keyboard`), `GtnhPointer`, `GtnhWheel`, `GtnhClipboardService`, `GtnhUiRuntimeBackend` (`openScreen`/`closeScreen`; слои пока `UnsupportedOperationException`).
- [x] 1.5 Клиентская команда `/unigui demo <name>` (или `/unigui <name>`, есть tab-completion) через `ClientCommandHandler` (`CommandUniGUI`, `GtnhDemoLauncher`; экран открывается через `GtnhClientTasks` в конце тика: `GuiChat.keyTyped` после отправки команды вызывает `displayGuiScreen(null)` и сразу закрыл бы экран, открытый прямо в команде; `-PuniguiAutoOpenViaCommand=true` воспроизводит этот порядок; без демо `overlay` и `postprocess`, им нужны слои и render targets).
- [~] 1.6 Тесты: перенесены `GtnhKeyCodesTest`, `GtnhPointerTest`, `GtnhWheelTest`, `GtnhDemoAssetsTest`, `GtnhDemoLauncherTest`. Не перенесены: `GtnhLoadingScreenTest` (ждёт слои и `GtnhLoadingHooks`), `Display` (SDL, фаза 6).
- [~] 1.7 Smoke-тест в `runClient21` (`./gradlew runClient21 -PuniguiAutoOpen=docking` открывает демо через несколько секунд после старта; заодно включает `-Dunigui.glDebug=true`). Проверено: экран открывается, `docking` рисует скруглённые прямоугольники, круги, линии, иконки-текстуры, клипы и текст, ошибок GL и исключений в логе нет. **Не проверено:** клики, hover и колесо (код ввода не гонялся: ручной клик мышью из скрипта в окне игры ненадёжен), Esc, ресайз, GUI Scale 1–4, fullscreen, фон `drawDefaultBackground` под экраном.

### Фаза 2 — Шейдеры и SDF-фигуры `P1`

- [x] 2.1 `GtnhShaderProgram` (GL20, кэш uniform'ов, ошибка компиляции даёт `null` и один лог), `GtnhGlMatrices` (`ModelViewMat`/`ProjMat` читаются из GL), `GtnhGlState.ShaderBindings` (программа, VAO, `GL_ARRAY_BUFFER`, 2D-текстура сохраняются и возвращаются), `GtnhSdfShapeRenderer`: INSTANCED (один draw на batch, те же GLSL 150, что в 1.20) и BASIC (quad на команду). Код тот же, что в Cleanroom: LWJGL 3 API. `-Dunigui.sdf=false` отключает SDF для сравнения.
- [x] 2.2 Диспетчер в `GtnhGuiRenderBackend.renderBatch`: сначала SDF для `RECT`/`ROUNDED_RECT`/`LINE`/`CIRCLE`, при отказе (нет шейдеров, ошибка, исключение) тесселяция `GtnhShapeRenderer`; `PATH` и `MESH` всегда через тесселяцию.
- [~] 2.3 Проверка: сборка и 151 тест зелёные; в `runClient21` SDF-путь инициализировался на демо `docking` (`UniGUI SDF shapes: instanced path ready`), ошибок GL и исключений в логе нет. **Не сверено** визуально «SDF против `-Dunigui.sdf=false`» и tier BASIC (на этом железе всегда INSTANCED).
- [ ] 2.4 Stencil для повёрнутого и скруглённого clip (сейчас AABB после transform).
- [ ] 2.5 Тесты и JMH: упаковка instance'ов уже покрыта (`GtnhSdfShapeInstancesTest`), JMH рядом не перенесён.

### Фаза 3 — Текстуры `P1`

- [x] 3.1 `GtnhTextures` (разбор id в `ResourceLocation` с кэшем, учёт mipmap по GL-имени, сброс при перезагрузке ресурсов) и `GtnhTextureHandle` (`withOptions`). Отсутствующий ресурс: `TextureManager` сам пишет в лог и биндит `missingno`.
- [x] 3.2 `GtnhDynamicTextures` (`register`/`replace`/`get`/`unregister`/`clear`/`size`): RGBA в ARGB (`GtnhPixels`), `ManagedTexture` хранит пиксели и переживает перезагрузку ресурсов, загрузка в GL на клиентском потоке. В 1.7.10 нет `addScheduledTask`, поэтому `GtnhClientTasks.runOnClientThread`: сразу на клиентском потоке, иначе в очередь на конец тика (поток помечается в `GtnhClientBootstrap.install`).
- [x] 3.3 `GtnhSamplerState` (`TextureOptions`: фильтры, wrap, mipmaps): sampler-объекты GL 3.3 по ключу `packed()`, без них `glTexParameteri` с откатом; default-опции ничего не меняют; mipmap-фильтр без `mipmaps()` понижается до базового (иначе чёрная текстура); `premultipliedAlpha` даёт `GL_ONE` как источник. Подключено в `GtnhShapeRenderer`.
- [x] 3.4 Размещение (`TexturePlacement`, `ImageFit`, `NineSlice`) раскладывает core, backend получает готовые `TEXTURE`/`TEXTURED_QUAD` с `uv`; rounded texture и пакетная отрисовка (один bind и один draw на batch) уже в `GtnhShapeTessellator`/`GtnhShapeRenderer`.
- [x] 3.5 Тесты: `GtnhTextureTest` (21: пиксели, таблицы фильтров и wrap, handle, UV, rounded image, flipV, opacity) и `GtnhClientTasksTest` (3).
- [~] 3.6 Проверка в `runClient21`: `/unigui textures` (или `-PuniguiAutoOpen=textures`) открывает `TextureLab` (динамическая шашка nearest/linear/repeat/mirrored, ванильный алмаз, tint, шум с mipmaps и без; подписи на плитках). Визуально проверена верхняя ячейка и часть нижней, ошибок GL и исключений в логе нет. **Не сверено:** нижние подписи (окно игры было частично закрыто), rounded-плитка, поведение после смены ресурспака. Плитки render target и shader из Cleanroom `TextureLab` не перенесены (фаза 5).
- [ ] 3.7 Освобождение GL-ресурсов при закрытии экрана (сейчас только `unregister`/`clear`) и sampler-объектов при выходе.

### Фаза 4 — Текст `P1`

- [x] 4.1 Ванильный путь: `MinecraftFontFace`/`MinecraftFonts` (метрики от `FONT_HEIGHT` = 9, baseline 7/9), `GtnhTextRenderer` (`FontRenderer.drawString(String, int, int, int)` принимает только целые, поэтому позиция, масштаб `pixelSize / 9` и transform команды идут через матрицу), `GtnhLegacyFormatting` (коды `§`, все стили, в SDF только цвета и `§r`), юникод через страницы глифов ванили.
- [x] 4.2 SDF: `GtnhSdfAtlas` (страницы 1024², `SdfGlyphProvider` из core, `GL_R8`, `glTexSubImage2D` с сохранением `GL_UNPACK_*`), `GtnhSdfTextRenderer` (instanced, 32 float на глиф, одна отрисовка на страницу; только tier INSTANCED, иначе SDF-run'ы рисуются ванильным шрифтом; `-Dunigui.sdfText=false` отключает). Код тот же, что в Cleanroom (LWJGL 3 API).
- [x] 4.3 Смешанный рендерер: `GtnhTextLayout` + `GtnhTextRenderer` (порядок run'ов сохраняется, SDF сбрасывается перед каждым ванильным сегментом и в конце batch'а), `TextBrush` (solid и linear gradient).
- [x] 4.4 Кастомные шрифты: `GtnhFonts.register(id, ResourceLocation)` читает TTF/OTF через менеджер ресурсов (jar модов и ресурспаки), перечитывает после перезагрузки ресурсов; пять встроенных шрифтов (`GtnhFonts.Builtin`, файлы в `assets/unigui/fonts`) регистрируются при первом обращении, чтобы не поднимать AWT на старте.
- [x] 4.5 Тесты: `GtnhFontsTest` (читает все пять шрифтов с classpath и растеризует глиф через AWT), `GtnhTextLayoutTest`, `GtnhTextColorsTest`, `GtnhLegacyFormattingTest` (перенесены раньше).
- [x] 4.6 Демо `TextLab` (`/unigui text` или `-PuniguiAutoOpen=text`): метрики, кириллица и CJK через ваниль, коды `§`, пять SDF-шрифтов, системный шрифт, смешанные run'ы, градиенты, inline-контент. Каждая строка подписана ожидаемым видом.
- [ ] 4.7 **Не проверено в игре** (см. чек-лист): метрики и baseline ванильного шрифта 1.7.10, кириллица/CJK, коды `§`, SDF-шрифты (AWT внутри процесса lwjgl3ify на Java 21), смешанные run'ы, градиенты, inline. Риск: AWT и SDL окно lwjgl3ify (на macOS AWT вместе с LWJGL 3 известен проблемами с главным потоком).

### Фаза 5 — Render targets, шейдеры, post-effects `P2`

- [x] 5.1 `GtnhRenderTarget` (обёртка над 1.7.10 `Framebuffer`, RGBA8 + depth по `RenderTargetOptions`): create/resize/close завёрнуты в `Guard` (возвращает привязанный FBO, viewport и depth: ванильные методы биндят FBO 0 и включают depth); цвет premultiplied, V переворачивается (`ColorTextureHandle.flipY`). Без FBO (`OpenGlHelper.isFramebufferEnabled() == false`) конструктор бросает `UnsupportedOperationException`.
- [x] 5.2 `GtnhGuiRenderBackend`: `createRenderTarget`, `render(drawList, target)` (ortho top-left `w/scale x h/scale`, identity modelview; `Pass` подставляет рендерерам плотность пикселей и размер FBO: scissor, AA-кайма, глифы считаются по target; вне кадра GL приводится к UI-defaults и возвращается), `RenderTargetCache` из core для ресайза. `GtnhShapeRenderer` умеет рисовать текстуру target'а (bind по GL-имени, flipV).
- [x] 5.3 `SHADER`: `GtnhShaderQuadRenderer` (программа на `ShaderHandle`, кэш, сброс после перезагрузки ресурсов через `GtnhResourceReloads`; встроенные uniform'ы как в 1.20: `ScreenSize`, `GuiScale`, `Time`, `SquareVertex`, `PoseStack`, `SourceTexture`/`Texture0`, `SourceSize`, `SourceFlipY`; доп. текстуры на юнитах 1..), загрузка `.fsh`/`.vsh` из ресурсов (`GtnhResourceShaderProvider`; шейдеры демо из jar `demo` попадают в `assets/unigui/shaders`).
- [x] 5.4 Post-effects: `GtnhGuiRenderBackend` реализует `UiPostEffectBackend` (layer target, ping-pong проходы, композит; при отсутствии FBO или GLSL 1.50 `supportsPostEffects() == false`, исключение отключает эффекты и слой рисуется напрямую). **Достижимо только через слои (`UniRenderLayer.postEffect()`), а слои появятся в фазе 6**, поэтому в игре не проверено.
- [x] 5.5 Демо: в `TextureLab` вернулись плитки «render target» и «shader plasma»; `/unigui retro` (демо с шейдерами `retro_terminal*.fsh`). `PostEffectLab` и демо `postprocess` — после фазы 6.
- [x] 5.6 Тесты: `GtnhShaderQuadRendererTest` (контракт vertex shader и `component`). Остальное требует GL.
- [ ] 5.7 **Не проверено в игре** (см. чек-лист в сообщении): FBO-плитка, plasma-шейдер, retro, возврат GL-состояния после FBO, ресайз окна.

### Фаза 6 — Интеграция с рантаймом `P1`

- [x] 6.1 Миксины на GTNH-шаблоне: `usesMixins = true`, `mixinsPackage = mixins`, `coreModClass = mixins.EarlyMixinsLoader` (`IEarlyMixinLoader` + enum `Mixins`, все миксины client/early), `mixins.unigui.early.json` и пустой `mixins.unigui.json` (плагин всегда регистрирует его в `MixinConfigs`: без файла UniMixins падает на старте), refmap генерируется annotation processor'ом. Классы `mixins/` не получают `@Lwjgl3Aware` (шаблоны, LWJGL не трогают). На старте `runClient21` три миксина загружаются (`Loading [mixins.unigui.early.json] ...`).
- [x] 6.2 HUD: `GtnhClientEvents.onHud` на `RenderGameOverlayEvent.Post` с `ElementType.ALL` (последнее, что стреляет `GuiIngameForge`), не рисуется при `hideGUI` (F1).
- [x] 6.3 Оверлей экрана: рисование в `GuiScreenEvent.DrawScreenEvent.Post` (LOWEST); ввод: миксин `MixinGuiScreenUniGuiOverlay` на `GuiScreen.handleMouseInput`/`handleKeyboardInput` (для всех экранов, включая контейнеры; `keyTyped` не годится, потому что `GuiContainer.keyTyped` не зовёт `super`), блокировка pointer'а `MixinGuiContainerUniGuiOverlay` на `GuiContainer.func_146978_c` (в 1.7.10 у неё нет MCP-имени; от неё зависят hover, тултип и клик по слоту). Все инъекции `require = 1`.
- [x] 6.4 Loading-screen слои (`UniRenderTarget.LOADING_SCREEN`): `MixinLoadingScreenRenderer` (`displayProgressMessage`/`resetProgressAndMessage` -> заголовок: «Saving level» отличается по `I18n("menu.savingLevel")`, `resetProgresAndWorkingMessage` -> сообщение, `setLoadingProgress` -> прогресс, слои рисуются перед `Minecraft.func_147120_f()` внутри него), экран `GuiDownloadTerrain` через `DrawScreenEvent.Post`. В 1.7.10 нет вызова «загрузка закончена», поэтому состояние `LEVEL_*` сбрасывает `WorldEvent.Load` клиентского мира.
- [x] 6.5 Порядок тултипов: `GuiScreen.drawHoveringText(List, int, int, FontRenderer)` (метод добавлен Forge, `remap = false`) откладывается в `GtnhTooltips` и проигрывается после оверлея. Тултипы, которые мод рисует мимо этого метода, остаются под оверлеем.
- [x] 6.6 Ввод: покадровый опрос указателя и колеса (`GtnhPointer`, `GtnhWheel`), курсоры ОС через SDL (`GtnhMouseCursors`, `SDLMouse.SDL_CreateSystemCursor`; SDL доступен, потому что lwjgl3ify создаёт окно через SDL3), key repeat (`GtnhKeyRepeat`), `GtnhDisplay` (частота монитора через SDL, fallback `org.lwjglx.opengl.Display`).
- [x] 6.7 `UiRenderPolicy`: `GtnhRenderCache` (рисует дерево в окно-размерную текстуру только когда решает `GtnhRenderPolicies.decide`, иначе один textured quad), для `UniScreen` и `UniRenderLayer`.
- [x] 6.8 Паузы и закрытие (`UniScreen.pauseGame` -> `doesGuiPauseGame`), debug-хоткеи слоёв, clipboard.
- [x] 6.9 Демо и лаборатории: `/unigui lab hud|overlay|loading|pause|nopause|policy` (`IntegrationLab`, без превью предметов и блоков: это фаза 7), `/unigui posteffect` (`PostEffectLab`), демо `overlay` и `postprocess` в `/unigui demo`.
- [ ] 6.10 **Не проверено в игре**: все пункты 6.2–6.9 (см. чек-лист). Тесты: `GtnhLoadingScreenTest`, `GtnhDisplayTest`, `GtnhMouseCursorsTest`, `GtnhLayerRegistryTest`, `GtnhPointerBlockersTest`, `GtnhTooltipsTest`, `GtnhKeyRepeatTest`, `GtnhRenderPoliciesTest`.

### Фаза 7 — Minecraft-виджеты `P2`

Контракт: пакет `dev.sixik.unigui.widgets.minecraft` с теми же именами классов и публичных методов, что в `common` (1.20 / 1.21) и `1.12.2-cleanroom` (сверено автоматически по публичным сигнатурам: `MinecraftPreviewWidget`, `MinecraftItemPreviewWidget`, `MinecraftBlockPreviewWidget`, `MinecraftEntityPreviewWidget`, `MinecraftItemPickerWidget`, `MinecraftTexturePickerWidget`, `MinecraftWidgets`). Отличаются только типы Minecraft, которых нет в 1.7.10: 

| Элемент контракта                                                                        | 1.20 / 1.21                          | 1.12.2                         | 1.7.10 (GTNH)                                                               |
|------------------------------------------------------------------------------------------|--------------------------------------|--------------------------------|-----------------------------------------------------------------------------|
| `MinecraftWidgets.entityPreview(label, X)`, `MinecraftEntityPreviewWidget.entityType(X)` | `EntityType<? extends LivingEntity>` | `EntityEntry`                  | `String`: имя в `EntityList` (`"Zombie"`)                                   |
| `MinecraftWidgets.itemPreview(label, X)`                                                 | `ItemLike`                           | `Item` / `Block` / `ItemStack` | `Item` / `Block` / `ItemStack`                                              |
| пустой стек (`ItemStack.EMPTY`, `isEmpty()`)                                             | `ItemStack.EMPTY`                    | `ItemStack.EMPTY`              | `null`; хелпер `GtnhItemStacks.isEmpty/EMPTY` (`stackSize <= 0` тоже пусто) |
| `MinecraftTexturePickerWidget.TextureEntry(id, handle)`                                  | `MinecraftTextureHandle`             | `CleanroomTextureHandle`       | `GtnhTextureHandle`                                                         |
| бэкенд в `renderMinecraftPreview(...)`                                                   | `MinecraftGuiRenderBackend`          | `CleanroomGuiRenderBackend`    | `GtnhGuiRenderBackend`                                                      |
| `ItemEntry.metadata()`                                                                   | нет (компонент)                      | metadata + NBT                 | metadata (damage) + NBT                                                     |

- [x] 7.1 Бэкенд: `GtnhGuiRenderBackend.renderItemPreview`/`renderItemPreviewLazy`/`cachedItemPreviewTexture`/`clearItemPreviewCache`/`renderEntityPreview`/`renderVanillaTooltip`/`renderNested` (те же имена, что у `CleanroomGuiRenderBackend`).
- [x] 7.2 `GtnhVanillaDraw`: предметы ванильным `RenderItem` (своим экземпляром: в 1.7.10 нет `Minecraft.getRenderItem()`), сущности `GuiInventory.func_147046_a`, тултип через protected `GuiScreen.drawHoveringText` (приватный экран-наследник), возврат GL в UI-состояние (`GtnhGlState.afterVanilla`), сломанный предмет логируется один раз и пропускается.
- [x] 7.3 `GtnhItemIcons`: иконки предметов один раз в FBO и дальше textured quad; кэшируются только предметы со стабильной картинкой (без glint, не часы/компас, без анимированной иконки, без `IItemRenderer` мода), LRU, бюджет 12 запеканий на кадр.
- [x] 7.4 Виджеты: `MinecraftItemPreviewWidget`, `MinecraftBlockPreviewWidget`, `MinecraftEntityPreviewWidget`, `MinecraftItemPickerWidget` (список по `Item.itemRegistry`, варианты через `getSubItems`), `MinecraftTexturePickerWidget` (`GtnhResourceListing` сканирует jar игры, моды и ресурспаки), `MinecraftWidgets`, `MinecraftItemTooltip`, `MinecraftTooltipRenderers`, `MinecraftZLayer`, `MinecraftScaledCustomDraw`.
- [x] 7.5 XML: `<MinecraftItemPreviewWidget>` и остальные пять регистрируются через `ServiceLoader` (`META-INF/services/...XmlWidgetRegistryContributor`); id предметов и блоков `namespace:name@metadata`, сущность - имя `EntityList`.
- [x] 7.6 Демо: `/unigui minecraft` (`MinecraftLab`), превью предмета и блока вернулись в `/unigui lab overlay`. Тесты: `GtnhItemIconsTest`, `GtnhVanillaDrawTest`, `GtnhResourceListingTest`, `BuiltInMinecraftXmlTest` (включая проверку, что ServiceLoader находит contributor), `GtnhItemStacksTest`.
- [ ] 7.7 **Не проверено в игре** (см. чек-лист). Известные ограничения: превью сущности создаёт экземпляр в текущем мире (в главном меню без мира показывается голова игрока); список текстур в dev-окружении не содержит ванильных (они не лежат в jar игры RFG); тултипы, которые мод рисует мимо `GuiScreen.drawHoveringText`, остаются под оверлеем.

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
