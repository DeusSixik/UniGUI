# UniGUI Animation Engine — roadmap реализации

Проект сейчас не имеет анимационного движка вообще. План — по фазам, каждая следующая опирается на предыдущую, ничего не строится "на будущее" раньше реальной потребности.

---

## Phase 0 — Фундамент (без этого не работает ничего дальше)

- [x] **Clock/DeltaTime источник**, не завязанный напрямую на render loop — отдельная абстракция `AnimationClock.deltaSeconds()`, чтобы позже можно было подменить (fixed timestep для детерминированного воспроизведения Storyboard, variable step для остального).
- [x] **`Easing`** — библиотека стандартных кривых (linear, easeIn/Out/InOut — quad/cubic/expo) + генерик cubic-bezier evaluator (`Easing.cubicBezier(x1,y1,x2,y2)`) через Newton-Raphson по X с bisection fallback — нужен и для обычных Tween, и для `SplineKeyframe` в Storyboard.
- [x] **`Interpolator<T>`** — добавлены типизированный контракт, generic `Tween<T>`, primitive `FloatInterpolator`/`IntInterpolator` без boxing и `AngleInterpolator` (shortest-path wrap). Интерполяторы `Vec2`/`ColorView` будут добавлены вместе с соответствующими типами данных.
- [x] **`AnimationController`** — per-owner реестр активных анимаций по ключу, повторный `play()` с тем же key заменяет текущую, не копит дубликаты. Подключён к update-проходу `WidgetBase` для spring-анимаций.
- [x] **Thread-safety контракт** — анимации и другое изменяемое состояние UI обновляются только с UI-потока. `AnimationController`, если ему передан `UiDispatcher`, выполняет mutation сразу при `isUiThread() == true`, иначе передаёт её в dispatcher; прямые изменения свойств виджета из фоновых callbacks по-прежнему должны использовать `UIContext.dispatcher()`.

---

### Правило потоков

`AnimationController` и виджеты не являются многопоточными объектами. Для контроллера, привязанного к `UiDispatcher`, методы изменения реестра (`play`, `stop`, `clear`, `update` и т.п.) сами проверяют `isUiThread()`: на UI-потоке выполняются сразу, на другом потоке передаются в dispatcher. Прямые изменения свойств виджета из сетевого обработчика, worker-потока или другого фонового callback должны передаваться через dispatcher владельца UI:

```java
uiContext.dispatcher().execute(() ->
        widget.animateOpacity(1.0f, TransitionSpec.of(0.16f)));
```

Автоматический dispatch внутри каждого метода анимации не используется: он создавал бы скрытые задержки и дополнительные задачи, а также усложнял бы порядок обновления. Это правило относится ко всему изменяемому UI-состоянию, а не только к animation engine.

---
## Phase 1 — Tween (детерминированный переход A→Б)

- [x] `Tween<T>` — reader/writer + `Interpolator<T>` + easing + duration; поддерживает repeat, yoyo, cancel и retarget. Для горячих `float`/`int` путей оставлены отдельные primitive transition'ы.
- [x] `Anchor` enum + `resolve(RectView bounds)` → абсолютная точка pivot; для горячего пути доступен переиспользуемый mutable result.
- [x] Rotation-tween поверх `AngleInterpolator`.
- [x] `ShakeAnimation` — процедурная затухающая осцилляция, аддитивная поверх base-позиции (не заменяет её); подключена к transform-эффектам `WidgetBase`.
- [ ] Тестовый кейс на реальном виджете — shake на error, scale-in/out на press (уже прогнали как первый пример).

---

## Phase 2 — Retarget-safe прерывание

- [x] `Tween.currentValue()` — для текущего `FloatTransition` это `value()`.
- [x] `retarget(newTarget)` — стартует новый переход от `currentValue()`, не от старого `from`.
- [ ] Ручная проверка на hover-in/hover-out быстрым наведением — не должно быть визуального "скачка".

---

## Phase 3 — Spring (реактивная альтернатива Tween)

- [x] `SpringAnimation` — stiffness/damping модель, непрерывна по скорости при retarget по конструкции.
- [ ] Явно закрепить, когда использовать что: `Tween` — детерминированные, спроектированные переходы; `Spring` — интерактивный фидбэк (hover/press/drag).
- [ ] Не делать Spring seekable — не пытаться подружить с timeline-scrubbing (это роль Tween/Storyboard).

---

## Phase 4 — Sequencing/композиция

- [x] Общий интерфейс `PlayableAnimation` (`update(delta)` + `isFinished()`), который реализуют `FloatTransition`, `SpringAnimation` и составные конструкции.
- [x] `Timeline.sequence(...)` — по очереди.
- [x] `Timeline.parallel(...)` — одновременно, завершение когда закончились все.
- [x] `Timeline.delay(ms)`.
- [x] `Timeline.stagger(items, factory, offsetMs)` — создаёт анимации один раз и запускает элементы с заданным временным сдвигом, не создавая вложенные delay/sequence во время кадрового обновления.

---

## Phase 5 — Style-driven переходы (связка с уже спроектированным Style/StylePack)

- [x] Добавить `transition.duration`/`transition.easing` как типизированные style-свойства с XML/editor codec'ами.
- [x] При смене resolved style автоматически сравнивать цели встроенных визуальных свойств `Box`/`Button` и запускать либо retarget'ить transition через `AnimationController`; повторный render с той же целью не перезапускает анимацию.
- [x] Проверить на реальном примере: `DefaultTheme` задаёт декларативный hover/pressed-переход цветов `Button` без event-handler'ов и ручного запуска анимации в пользовательском коде.

---

## Phase 6 — Storyboard / keyframe-анимации

- [x] Модель: immutable `Storyboard`, типизированный `PropertyTrack<T>` и sealed `Keyframe<T>` с вариантами `DiscreteKeyframe`/`SplineKeyframe`.
- [x] `PropertyPathResolver` разрешает `"RenderTransform.Y"`, opacity, scale, rotation и pivot в готовые accessor'ы один раз при создании player; пользовательские типизированные accessor'ы регистрируются без reflection.
- [x] `NamedWidgetRegistry` один раз индексирует существующие `Widget.id()` XML/runtime-дерева и запрещает неоднозначные дубликаты.
- [x] `StoryboardPlayer` компилирует float-треки в primitive-массивы и выполняет бинарный поиск текущего сегмента по времени без разбора строк и временных коллекций на tick.
- [x] Discrete-семантика: предыдущее значение удерживается до timestamp, затем выполняется мгновенный snap.
- [x] Spline-семантика: easing входящего сегмента, включая `Easing.cubicBezier`, применяется между соседними keyframe'ами.
- [x] Для авторинга выбран и реализован компактный XAML-like формат `StoryboardXml`, согласованный с существующим безопасным XML pipeline UniGUI. Lottie/bodymovin остаётся отдельным будущим импортёром поверх той же runtime-модели.

---

## Phase 7 — FLIP layout-переходы

- [x] Хук в `MutableRect` callback — на каждый layout pass, где позиция виджета реально сдвинулась, считать дельту First/Last.
- [x] Invert — временно применять additive visual offset так, чтобы виджет визуально остался на старом месте.
- [x] Play — сводить offset к нулю через отдельную allocation-free `LayoutTransitionAnimation` с общим `Easing` API.
- [x] Переход явно ограничен opt-in флагом конкретного виджета: `layoutTransitionsEnabled(true)` или `layoutTransition(...)`.

Размер сам по себе в Phase 7 не анимируется. Реальные `layoutBounds` не изменяются обратно, поэтому hit-test и следующий layout используют актуальную геометрию. FLIP-offset складывается с additive shake-эффектом и не меняет базовую пользовательскую позицию `Transform`.

---

## Phase 8 — Цвет/текстура/шейдер кроссфейд

- [ ] `Interpolator<ColorView>` с опцией linear-space lerp (устраняет "грязный" переход на больших дистанциях по hue).
- [ ] Texture-crossfade шейдер — `mix(texture(texA, uv), texture(texB, uv), progress)`, один draw вместо двух перекрывающихся quad'ов.
- [ ] Shader-to-shader crossfade — переиспользовать `CachedSubtreeWidget`/FBO: бейк A и Б в отдельные текстуры, дальше обычный texture-crossfade между готовыми bitmap'ами (не пытаться сливать произвольные шейдеры в рантайме, если они не под одним контролем).

---

## Phase 9 — Editor/tooling интеграция

- [ ] `AnimationDefinition` — данные (`List<PropertyTween>`), не Java-код, редактируемые через уже спроектированную schema-driven inspector-инфраструктуру (`ComponentSerializer`-паттерн, применённый к анимациям).
- [ ] Hot-reload анимационных описаний — переиспользовать уже существующий file-watcher (тот же механизм, что для CSS-стилей/GLSL-hints/ResourceStorage).
- [ ] Debug overlay — счётчик активных `Tween`/`Spring`/`Storyboard` за кадр, per-widget breakdown — та же форма, что уже спроектированный perf-overlay (draw calls/GPU ms).

---

## Порядок работы — что реально нужно прямо сейчас, что можно отложить

**Нужно почти сразу (Phase 0–2):** без этого не работает вообще ничего, включая уже показанный shake/press-пример.

**Нужно скоро (Phase 3–5):** spring и авто-переходы по Style — то, что реально даёт ощущение "AAA"-полировки на обычных, повседневных интеракциях (hover/press/toggle), а не только на спроектированных cutscene-эффектах.

**Можно отложить до конкретной потребности (Phase 6–8):** Storyboard/keyframes, FLIP, shader-кроссфейд — мощные, но нишевые возможности, оправданные только когда реально понадобится конкретный сложный intro-эффект или список с плавной пересортировкой. Строить их раньше реального кейса — тот же premature-generalization риск, который мы весь разговор старались не повторять.

**Tooling (Phase 9)** — по мере того, как сама анимационная модель стабилизируется; смысла интегрировать в визуальный редактор до того, как API самих анимаций устоялся, нет — придётся переделывать.
