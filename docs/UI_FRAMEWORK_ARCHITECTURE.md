# UniGUI UI Framework Architecture Draft

## Цель

Сделать портируемый UI framework на базе Minecraft render pipeline, но без жёсткой привязки к конкретной версии Minecraft, Fabric, Forge или конкретному rendering API.

Главные требования:

- retained-mode UI, а не immediate-only API;
- отложенный render через command queue;
- батчинг виджетов для снижения draw calls;
- возможность рендерить любой виджет или subtree виджетов в текстуру;
- абстракции поверх Minecraft rendering, OpenGL и будущих backend-ов;
- поддержка Layout V3 как backend-neutral layout engine с Yoga/Taffy-like semantics;
- готовность к DockPanel, DragAndDrop, ModalWindow, GraphView и Animation API;
- поддержка transform-ов: width, height, scale, rotation, pivot;
- отдельный механизм для Minecraft item/block/entity rendering.
- mutable value objects для частых параметров вроде position, size, scale, color;
- deferred mutation model для безопасного добавления/удаления widgets;
- возможность встраивать UniGUI widget/render subtree в UI других модов;
- event-driven API по стилю Unigine.

---

## Главный принцип

Виджеты не должны напрямую вызывать Minecraft rendering API.

Вместо этого виджеты должны производить промежуточные render commands, которые затем собираются, сортируются, батчатся и исполняются backend-ом.

Общий pipeline:

```text
Widget Tree
   ↓
Layout V3 Engine / adapters
   ↓
Visual Tree + Transforms
   ↓
Draw Command Queue
   ↓
Batcher / Render Graph
   ↓
Backend: Minecraft / OpenGL / Offscreen Texture
```

Такой подход даёт:

- переносимость между версиями Minecraft;
- контроль над batching;
- render-to-texture без костылей;
- возможность добавлять shader passes;
- возможность кэшировать сложные UI subtrees;
- удобную основу для GraphView и сложных animated UI.

---

## Предлагаемые пакеты

Проект уже разделён на `common`, `fabric` и `forge`. Основная логика UI должна жить в `common`, а loader-specific glue — в `fabric` и `forge`.

Логическая структура внутри `common`:

```text
dev.sixik.unigui.core
  Widget
  Panel
  Root
  Scene
  UIContext
  FrameScheduler
  UiDispatcher
  lifecycle
  invalidation
  deferred mutations

dev.sixik.unigui.math
  Vec2View
  MutableVec2
  RectView
  MutableRect
  MutableColor
  Transform
  Matrix3x2

dev.sixik.unigui.api.layout / api.layout.v3 / impl.layout.v3
  LayoutNode
  LayoutStyle
  LayoutResult
  LayoutEngine
  TaffyLayoutEngine
  LayoutStyleMapper
  OverlayLayoutResolver
  custom layout interfaces

dev.sixik.unigui.render
  DrawCommand
  DrawList
  Batch
  Batcher
  Material
  Paint
  TextureHandle
  RenderTarget
  RenderPass
  RenderGraph

dev.sixik.unigui.render.backend
  RenderBackend
  MinecraftRenderBackend
  OpenGLRenderBackend

dev.sixik.unigui.shader
  ShaderProgram
  ShaderDescriptor
  ShaderSource
  ShaderLoader
  UniformSet

dev.sixik.unigui.input
  InputEvent
  PointerEvent
  KeyboardEvent
  FocusManager
  HitTest

dev.sixik.unigui.event
  Event
  EventBus
  EventEmitter
  EventListener
  EventSubscription
  WidgetEvent
  ButtonClickEvent

dev.sixik.unigui.animation
  AnimatableProperty
  Timeline
  Transition
  Easing
  AnimationClock

dev.sixik.unigui.widgets
  Box
  Text
  Image
  TextureWidget
  Shape
  Border
  Separator
  Path
  Button
  ScrollView
  StackPanel
  CanvasWidget
  controls
  containers
  data
  overlays
  graph

dev.sixik.unigui.interop
  WidgetHost
  WidgetRenderer
  WidgetTextureRenderer
  WidgetExtern
  WidgetExternHost
  WidgetExternAdapter
  ExternalWidgetWrapper
  MinecraftScreenBridge
```

---

## Core model

Виджет должен быть retained объектом, который хранит состояние, layout style, visual properties и children.

Примерный контракт:

```java
public interface Widget {
    void measure(LayoutContext context);

    void render(RenderContext context);

    void handle(Event event);

    LayoutNode layoutNode();

    Transform transform();
}
```

При этом `render(...)` не должен рисовать немедленно. Он должен добавлять команды в `RenderContext`.

```java
public interface RenderContext {
    void rect(Rect rect, Paint paint);

    void text(TextRun text, Transform transform, Paint paint);

    void image(TextureHandle texture, Rect dst, Rect uv, Paint paint);

    void custom(DrawCommand command);
}
```

---

## WidgetExtern contract

WidgetExtern обязателен как стабильный контракт для внешних пользовательских виджетов.

Идея:

~~~text
Core widgets:
  пишутся внутри UniGUI и могут наследоваться от WidgetBase.

WidgetExtern:
  пишется пользователем/другим модом/интеграцией
  и подключается к UniGUI через adapter.
~~~

Это нужно, чтобы любой мод мог создать свой виджет, но не зависел от внутренних классов engine-а, которые могут меняться.

Пример API:

~~~java
public interface WidgetExtern {
    void onAttach(WidgetExternHost host);

    void onDetach();

    void measure(ExternMeasureContext context);

    void render(ExternRenderContext context);

    void handle(Event event);

    default void tick(FrameContext frame) {
    }

    default void dispose() {
    }
}
~~~

Host API:

~~~java
public interface WidgetExternHost {
    Widget widget();

    UIContext ui();

    UiDispatcher dispatcher();

    void invalidateLayout();

    void invalidateVisual();

    void invalidateTexture();

    void emit(Event event);
}
~~~

Adapter:

~~~java
public final class WidgetExternAdapter extends WidgetBase {
    private final WidgetExtern extern;
}
~~~

Правила WidgetExtern:

- extern-widget не должен хранить RenderContext после render-вызова;
- extern-widget не должен напрямую менять children list во время traversal;
- любые add/remove child должны идти через host/dispatcher и deferred mutation queue;
- render должен создавать DrawCommands, CanvasCommands или вызывать разрешённый RenderContext API;
- прямой OpenGL/Minecraft render внутри WidgetExtern допустим только через явный CustomDrawCommand/barrier;
- WidgetExtern должен получать events через тот же event pipeline, что и обычные widgets;
- WidgetExtern должен работать в render-to-texture без отдельного специального пути.

WidgetExtern отличается от ExternalWidgetWrapper:

~~~text
WidgetExtern:
  внешний код создаёт custom widget для UniGUI.

ExternalWidgetWrapper:
  UniGUI widget встраивается в UI другого framework-а или другого мода.
~~~

Оба механизма нужны.

~~~text
Other mod creates custom UniGUI widget:
  WidgetExtern -> WidgetExternAdapter -> UniGUI tree

Other mod wants to draw existing UniGUI widget inside its own UI:
  Widget -> ExternalWidgetWrapper -> foreign UI render
~~~

---

## Mutable value objects

Mutable параметры нужны. Для UI это нормальный и даже желательный подход, потому что position, size, scale, color, uv, transform и animation values могут меняться каждый кадр. Если на каждое изменение делать `new Vector2`, `new Rect` или `new Color`, сложный UI быстро начнёт создавать лишний GC pressure.

Но mutable API опасен, если сделать его бесконтрольным.

Проблемы плохого mutable-подхода:

- внешний код может сохранить ссылку на внутренний `MutableVec2` и менять её когда угодно;
- изменение поля напрямую может не вызвать invalidation;
- render command может ссылаться на mutable объект, который изменится до исполнения batch-а;
- другой поток может менять widget tree во время layout/render;
- сложно понять, какие изменения требуют layout dirty, visual dirty или texture dirty.

Поэтому правило такое:

```text
Mutable внутри engine — да.
Бесконтрольные публичные mutable-ссылки — нет.
```

Нужны read-only view интерфейсы и controlled mutation methods.

```java
public interface Vec2View {
    float x();

    float y();
}
```

```java
public final class MutableVec2 implements Vec2View {
    private float x;
    private float y;
    private Runnable onChanged;

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public void set(float x, float y) {
        if (this.x == x && this.y == y) return;
        this.x = x;
        this.y = y;
        if (onChanged != null) onChanged.run();
    }

    public void copyFrom(Vec2View other) {
        set(other.x(), other.y());
    }
}
```

Виджет может отдавать наружу read-only view:

```java
Vec2View position();
```

А изменение делать через controlled API:

```java
void setPosition(float x, float y);

void editPosition(Consumer<MutableVec2> editor);
```

`editPosition(...)` полезен, когда нужно изменить несколько полей и вызвать invalidation один раз.

```java
widget.editTransform(t -> {
    t.position().set(x, y);
    t.scale().set(scale, scale);
    t.setRotation(rotation);
});
```

Для render commands нельзя хранить живые ссылки на mutable widget state. Команда должна получать snapshot значений.

Плохо:

```java
command.transform = widget.transform();
```

Лучше:

```java
command.transform.copyFrom(widget.transform());
```

Или ещё лучше для hot path:

```text
DrawCommandBuffer:
  float[] positions
  float[] colors
  float[] uvs
  int[] materials
```

То есть widget state может быть mutable, но render queue должен быть snapshot/command-buffer based.

---

## Invalidation contract

Каждое изменение должно явно помечать нужный тип dirty state.

```text
layout dirty:
  width
  height
  min/max size
  flex properties
  margin
  padding
  child add/remove

visual dirty:
  position
  scale
  rotation
  opacity
  color
  texture
  shader uniforms

texture dirty:
  cached subtree changed
  render-to-texture target resized
  visual child changed inside cached subtree
```

Пример:

```java
public void setWidth(float width) {
    if (style.width() == width) return;
    style.setWidth(width);
    invalidateLayout();
}

public void setOpacity(float opacity) {
    if (this.opacity == opacity) return;
    this.opacity = opacity;
    invalidateVisual();
}
```

Для mutable objects invalidation должен быть встроен в сам объект или в owning property wrapper.

---

## Deferred widget mutations

PanelWidget и любые контейнеры не должны менять список children немедленно во время layout, input dispatch или render traversal.

Иначе возможны:

- `ConcurrentModificationException`;
- пропуск widget-а в traversal;
- двойной render;
- inconsistent hit-test;
- проблемы при изменениях из других потоков.

Нужен deferred mutation model.

```java
public interface WidgetContainer {
    void addChild(Widget child);

    void removeChild(Widget child);

    void clearChildren();

    List<Widget> childrenView();
}
```

`addChild/removeChild/clearChildren` не обязаны менять список сразу. Они должны ставить операцию в очередь.

```text
Current frame traversal:
  children snapshot is stable

Mutation requested:
  enqueue AddChild / RemoveChild / MoveChild

Safe point:
  apply queued mutations
  update parent links
  invalidate layout
  update event routing
```

Предлагаемые safe points:

```text
Frame start:
  apply mutations from previous frame

Input phase:
  dispatch events

Before layout:
  apply mutations queued before layout

Layout phase:
  stable tree traversal

Render build phase:
  stable tree traversal

Render submit phase:
  stable draw list

Frame end:
  apply or defer mutations queued during render
```

Если mutation вызвана во время render traversal, безопаснее применить её в конце текущего кадра или в начале следующего.

Для простоты MVP:

```text
Все mutations применяются в начале следующего кадра.
```

Для более responsive UI можно позже добавить:

```text
Mutations before layout are applied in current frame.
Mutations during layout/render are applied next frame.
```

Нужен `UiDispatcher`:

```java
public interface UiDispatcher {
    boolean isUiThread();

    void execute(Runnable action);

    void executeNextFrame(Runnable action);
}
```

Правило потоков:

```text
Widget state is owned by UI thread.
Other threads may request changes only through UiDispatcher / mutation queue.
```

То есть framework не обязан делать каждый widget полностью thread-safe. Вместо этого он должен дать безопасный способ попросить UI thread изменить tree/state.

Пример:

```java
ui.dispatcher().executeNextFrame(() -> {
    panel.addChild(new Button());
});
```

Для cross-thread producer-ов можно использовать MPSC queue:

```text
worker thread
   ↓
MPSC UI mutation queue
   ↓
UI frame safe point
   ↓
apply mutation
```

---

## Layout V3

Layout V3 — это Java-owned layout abstraction с Yoga/Taffy-like semantics, но без прямой публичной привязки UniGUI widgets к конкретному Yoga/Taffy binding.

Текущий pipeline:

```text
Widget tree
   ↓
LayoutTreeBuilder
   ↓
LayoutNode tree + LayoutStyleSnapshot
   ↓
LayoutEngine.compute(LayoutInput)
   ↓
LayoutOutput / LayoutResult
   ↓
LayoutApplier или container-specific V3 adapter
   ↓
Widget.layoutBounds()
```

Базовый backend contract:

```java
public interface LayoutEngine {
    LayoutOutput compute(LayoutNode root, LayoutInput input);
}
```

Текущий internal backend — `TaffyLayoutEngine`. Он покрывает flex row/column, wrap, gap, margin, padding, min/max, percent, grow/shrink, absolute children, content/overflow size и per-pass measure cache. External Yoga/Taffy остаются возможной будущей заменой backend-а, но не должны протекать в публичный widget API.

Миграция контейнеров теперь default-on для уже перенесённых slices; flags остаются для rollback и точечного отключения:

- global rollback: `runtime compatibility/reference code are removed`;
- slice rollback: `-Dremoved runtime slice flag=false`, `wrappanel=false`, `stackpanel=false`, `overlay=false`, `scrollview=false`, `splitpanel=false`, `dockpanel=false`, `gridbox=false`.

V3 layout path используется по умолчанию. V2 layout path остаётся fallback-ом для rollback/debug. V3 adapters сейчас есть для `LinearBox`/`HBox`/`VBox`, `WrapPanel`, `StackPanel`, `ScrollView`, `SplitPanel`, `DockPanel` и `GridBox`.

Overlay часть вынесена в `OverlayLayoutResolver`: popup/dropdown/tooltip-like floating widgets позиционируются через root overlay host, не участвуют в normal layout, получают явную draw/hit-test priority policy и могут не обрезаться промежуточным `ScrollView`/`Panel` scissor.

Важно: Yoga и Taffy dependencies пока подключены как `compileOnly`. Runtime layout не должен требовать их, пока backend остаётся internal Java. Если появится внешний backend, его нужно будет явно шейдить или подключать как runtime dependency.

---

## Transform model

Виджеты должны уметь менять размер не только через width/height, но и через scale, rotation и pivot.

Поэтому нужно разделить layout bounds и visual transform.

```text
Layout bounds:
  x
  y
  width
  height

Visual transform:
  translate
  scale
  rotation
  pivot

World transform:
  parentWorldTransform * localTransform

Hit-test transform:
  inverse(worldTransform)
```

Пример:

```java
public final class Transform {
    public Vec2 position;
    public Vec2 scale;
    public float rotation;
    public Vec2 pivot;
}
```

Layout остаётся прямоугольным, а visual transform применяется поверх результата Layout V3.

Это важно для:

- animated scale;
- rotated widgets;
- zoomable GraphView;
- drag previews;
- modal transitions;
- shader effects;
- корректного hit-test.

---

## Render commands

Виджеты должны отдавать не draw calls, а команды:

```text
DrawCommand:
  type
  material
  texture
  shader
  scissor
  transform
  zIndex / layer
  renderTarget
```

Основные типы команд:

```text
RECT
ROUNDED_RECT
IMAGE
TEXT
MESH
ITEM
BLOCK
ENTITY
CUSTOM
```

Команды складываются в `DrawList`, после чего проходят через `Batcher`.

---

## Батчинг

Batcher должен группировать команды по безопасным ключам:

```text
RenderTarget
Layer
Scissor
Shader / Material
Texture
VertexFormat
```

Но UI порядок нельзя ломать. Поэтому нужны render barriers.

Примеры barriers:

- смена clipping/scissor;
- stencil/mask;
- custom shader pass;
- item/entity render;
- transparent overlap, где порядок важен;
- render-to-texture boundary;
- explicit flush command.

То есть система должна батчить безопасные участки, но не пытаться любой ценой объединить всё в один draw call.

---

## Render-to-texture

Render-to-texture должен быть first-class feature.

Нужные интерфейсы:

```java
public interface RenderTarget {
    int width();

    int height();

    TextureHandle colorTexture();
}
```

```java
public interface RenderBackend {
    RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options);

    void begin(RenderTarget target);

    void submit(Batch batch);

    void end();
}
```

Тогда любой виджет или subtree можно рендерить так:

```java
TextureHandle texture = uiRenderer.renderToTexture(widget, width, height);
```

Для оптимизации нужен dirty/invalidation механизм:

```text
Widget dirty
   ↓
Subtree texture dirty
   ↓
Rerender subtree only when needed
```

Это позволит делать:

- blur;
- masks;
- shader effects;
- cached panels;
- complex graph nodes;
- render previews;
- node thumbnails;
- expensive Minecraft item/entity previews.

---

## RenderBackend

Нужна абстракция backend-а:

```java
public interface RenderBackend {
    TextureHandle createTexture(TextureDescriptor descriptor);

    RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options);

    ShaderProgram createShader(ShaderDescriptor descriptor);

    void beginFrame(FrameContext frame);

    void submit(Batch batch);

    void endFrame();
}
```

Minecraft-specific код должен быть спрятан за backend-ом.

Например:

```text
common:
  RenderBackend
  DrawCommand
  Batcher
  Shader API

fabric/forge:
  init hooks
  screen wrapper
  resource reload bridge
  platform-specific lifecycle

minecraft backend:
  GuiGraphics / PoseStack bridge
  BufferBuilder bridge
  RenderSystem bridge
  Minecraft texture/shader integration
```

Если в будущем поменяется версия Minecraft, основной UI engine должен остаться почти без изменений.

---

## Embedding и wrappers для других модов

Framework должен позволять использовать UniGUI render не только внутри собственного Screen, но и внутри UI другого мода или чужого widget-а.

Пример задачи:

~~~text
OtherMod IconWidget
   ↓
хочет нарисовать UniGUI widget внутри своей области
   ↓
UniGUI должен отдать render bridge / wrapper
~~~

Для этого нужен отдельный interop layer.

~~~java
public interface WidgetRenderer {
    void renderWidget(Widget widget, ExternalRenderContext externalContext, Rect viewport);

    TextureHandle renderWidgetToTexture(Widget widget, int width, int height);
}
~~~

ExternalRenderContext должен быть адаптером над чужим render context:

~~~java
public interface ExternalRenderContext {
    float partialTick();

    Matrix3x2 currentTransform();

    ClipState clip();

    Object nativeContext();
}
~~~

Minecraft-specific adapter может хранить GuiGraphics, PoseStack или другой version-specific объект, но core API не должен от них зависеть напрямую.

~~~java
public final class MinecraftExternalRenderContext implements ExternalRenderContext {
    private final Object nativeContext;

    public Object nativeContext() {
        return nativeContext;
    }
}
~~~

Для интеграции в чужой widget лучше иметь wrapper:

~~~java
public final class ExternalWidgetWrapper {
    private final Widget widget;
    private final WidgetRenderer renderer;

    public void render(ExternalRenderContext context, Rect bounds) {
        renderer.renderWidget(widget, context, bounds);
    }
}
~~~

Нужно поддержать два режима embedding-а.

### Direct embedded render

~~~text
foreign widget render
   ↓
UniGUI builds draw list for subtree
   ↓
UniGUI submits draw list into current backend/context
~~~

Подходит для простых случаев.

Минусы:

- больше риска конфликтов render state;
- нужно аккуратно восстанавливать scissor/blend/shader/texture state;
- сложнее гарантировать порядок с чужими draw calls.

### Texture embedded render

~~~text
UniGUI subtree
   ↓
render-to-texture
   ↓
foreign widget draws resulting texture
~~~

Подходит для сложных случаев:

- чужие UI frameworks;
- shader usage;
- graph node previews;
- item/entity previews;
- иконки/карточки/миниатюры;
- safe composition без протекания render state.

API может выглядеть так:

~~~java
TextureHandle icon = uiRenderer.renderWidgetToTexture(widget, width, height);
~~~

Для чужих модов можно дать маленький facade:

~~~java
public interface UniGuiEmbed {
    ExternalWidgetWrapper wrap(Widget widget);

    TextureHandle renderToTexture(Widget widget, int width, int height);
}
~~~

Главное правило:

~~~text
UniGUI core не зависит от чужого мода.
Чужой мод получает adapter/facade.
Minecraft-specific детали живут в bridge/backend layer.
~~~

---

## Shader API

Shader API не должен быть завязан только на Minecraft resource loader.

Нужен абстрактный источник shader-а:

```java
public interface ShaderSource {
    String vertex();

    String fragment();

    Map<String, String> includes();
}
```

```java
public interface ShaderLoader {
    ShaderProgram load(ShaderDescriptor descriptor);
}
```

Возможные источники:

```text
ClasspathShaderSource
MinecraftResourceShaderSource
FileShaderSource
StringShaderSource
GeneratedShaderSource
```

Это позволит загружать shader-ы:

- из Minecraft resources;
- из jar/classpath;
- из внешнего файла;
- из строки;
- из generated shader sources.

Также shader API должен поддерживать uniform-ы:

```java
public interface UniformSet {
    void setFloat(String name, float value);

    void setVec2(String name, float x, float y);

    void setVec4(String name, float x, float y, float z, float w);

    void setTexture(String name, TextureHandle texture);
}
```

Uniform-ы должны быть применимы как к обычным draw commands, так и к offscreen texture passes.

---

## Minecraft item/block/entity rendering

Minecraft item, block и entity rendering лучше не смешивать напрямую с обычным UI batching.

Нужны отдельные команды:

```text
DrawItemCommand
DrawBlockCommand
DrawEntityCommand
```

Возможны два режима.

### 1. Immediate bridge

```text
flush UI batch
   ↓
render Minecraft item/entity/block
   ↓
continue UI batch
```

Плюсы:

- проще реализовать;
- подходит для малого количества объектов;
- меньше проблем с совместимостью Minecraft render internals.

Минусы:

- плохо батчится;
- может быть дорого на больших списках;
- чаще ломает порядок render state.

### 2. Cached/offscreen mode

```text
render item/entity/block into texture
   ↓
cache texture
   ↓
use as regular IMAGE command
```

Плюсы:

- отлично подходит для recipe UI, inventory-like списков, graph nodes;
- можно батчить как обычные image commands;
- можно применять shader effects;
- можно использовать в render-to-texture pipeline.

Ключ кэша для item:

```text
ItemStack
NBT
lighting
scale
pose
damage/foil state
```

Entity сложнее, но для статичных preview можно использовать похожий cache key:

```text
Entity type
entity state snapshot
pose
lighting
camera angle
scale
```

---

## Events, input и hit-test

UI должен работать через Events. То есть button click, hover, focus, drag, value changed и другие действия должны выражаться событиями, а не прямыми callback-ами, зашитыми в конкретный widget.

Это ближе к подходу Unigine: widget/action вызывает event, а подписчики реагируют.

Базовые интерфейсы:

~~~java
public interface Event {
    EventType type();

    boolean isCancelled();

    void cancel();
}
~~~

~~~java
public interface EventListener<T extends Event> {
    void handle(T event);
}
~~~

~~~java
public interface EventEmitter {
    <T extends Event> EventSubscription on(EventType<T> type, EventListener<T> listener);

    void emit(Event event);
}
~~~

Пример:

~~~java
button.events().on(ButtonClickEvent.TYPE, event -> {
    // handle click
});
~~~

Или через удобный shortcut:

~~~java
button.onClick(event -> {
    // handle click
});
~~~

Сами события:

~~~text
PointerPressedEvent
PointerReleasedEvent
PointerMovedEvent
PointerEnteredEvent
PointerExitedEvent
ButtonClickEvent
FocusGainedEvent
FocusLostEvent
KeyPressedEvent
ValueChangedEvent
ChildAddedEvent
ChildRemovedEvent
DragStartedEvent
DragMovedEvent
DragDroppedEvent
ModalOpenedEvent
ModalClosedEvent
~~~

Нужны event phases:

~~~text
Capture phase:
  root → target parent

Target phase:
  target widget

Bubble phase:
  target parent → root
~~~

Event должен иметь routing information:

~~~java
public interface WidgetEvent extends Event {
    Widget target();

    Widget currentTarget();

    EventPhase phase();
}
~~~

Важно разделить input events и semantic events.

~~~text
Input event:
  PointerReleasedEvent на Button

Semantic event:
  ButtonClickEvent
~~~

Так Button сам решает, что pointer press + release внутри bounds = click, и уже после этого испускает ButtonClickEvent.

Hit-test должен учитывать transform chain.

Общий процесс:

```text
Pointer screen position
   ↓
Root coordinate space
   ↓
inverse(widget.worldTransform)
   ↓
local widget bounds check
```

Нужные системы:

- hover tracking;
- capture pointer;
- focus manager;
- keyboard focus;
- bubbling/tunneling events;
- drag threshold;
- modal input blocking;
- tooltip delay.

Желательно сразу заложить event phases:

```text
Capture phase
Target phase
Bubble phase
```

Это упростит будущие DragAndDrop, ModalWindow, DockPanel и GraphView.

Event dispatch также должен уважать deferred mutation model.

Если listener добавляет или удаляет widget:

~~~java
button.onClick(event -> {
    panel.removeChild(button);
    panel.addChild(newWidget);
});
~~~

Эти изменения не должны ломать текущий event traversal. Они должны попасть в mutation queue и примениться на safe point.

### fastutil collections contract

Для hot path framework использует `it.unimi.dsi.fastutil` как внутренний performance foundation. Приоритет — specialized primitive collections из fastutil; если для конкретной задачи нет подходящего fastutil-решения или участок не является hot path, используется стандартный Java Collections API.

Общее правило выбора:

~~~text
hot path + primitive key/value      -> fastutil primitive collections
hot path + object-only storage      -> fastutil object collections, если они реально дают пользу
public API / моддинг-контракт       -> Java Collections interfaces / Iterable / собственные stable views
редкий setup/debug/tooling path     -> Java Collections
нет подходящей fastutil структуры   -> Java Collections
~~~

Правила:

- fastutil подключается как implementation detail через `it.unimi.dsi:fastutil`; публичный API не должен заставлять пользователей импортировать fastutil classes;
- в `impl`, `backend`, `widgets` и performance-sensitive subsystems fastutil является предпочтительным вариантом для maps/sets/lists с primitive keys или values: `Int2ObjectMap`, `Long2ObjectMap`, `IntArrayList`, `IntOpenHashSet` и аналоги;
- в hot path нельзя без причины использовать `HashMap<Integer, ...>`, `Map<Long, ...>`, `List<Integer>` и похожие boxed collections, если есть прямой fastutil-аналог;
- Java Collections допустимы для стабильных внешних контрактов, маленьких списков, редко изменяемых setup-структур, debug/profiler tooling, тестов и compatibility adapters;
- если fastutil используется внутри класса, наружу отдаётся `List`, `Collection`, `Iterable`, read-only view или собственный API, а не mutable fastutil container;
- конвертация fastutil -> Java Collections не должна происходить каждый кадр; если нужен внешний view, он должен быть cached/snapshot-based;
- конкретный выбор коллекции фиксируется по access pattern: array/list для dense id, primitive map для sparse id, set для membership checks, object collection только если boxing не является проблемой;
- fallback на Java Collections является осознанным решением, а не default для hot path: причина должна быть очевидна из контекста или комментария в сложном месте.

Event system решение MVP:

- `FastEventBus` является default EventBus implementation для widgets, `UIContext` и внутренних dispatchers;
- публичный контракт остаётся на уровне `api.event.EventType`, `EventEmitter`, `EventListener`, `EventSubscription`; пользовательский код не обязан напрямую зависеть от `FastEventBus`;
- `api.event.EventType` может хранить bridge на `FastEventBus.EventType`, чтобы dispatch шёл через int id + array lookup без `HashMap` на кадр;
- подписки могут быть copy-on-write, потому что они редкие; dispatch должен быть allocation-free или close to allocation-free;
- listeners не должны менять текущий traversal напрямую: изменения widget tree идут через mutation queue / `UiDispatcher`; FastEventBus отвечает только за доставку event object;
- cancellation (`event.cancel()`) считается частью event semantics; adapter поверх FastEventBus обязан не вызывать следующие user listeners после отмены, либо делать no-op guard без изменения hot-path структуры;
- альтернативные EventBus реализации допустимы только для тестов, debug tooling или compatibility adapters, но не как default runtime path.

Практическое решение MVP:

~~~text
api.event.EventType
  owns FastEventBus.EventType bridge

impl.event.FastEventEmitter
  adapts EventEmitter API to FastEventBus

WidgetBase / DefaultUIContext
  use FastEventEmitter by default
~~~

---

## Animation API

Анимации лучше делать property-based.

Пример API:

```java
button.animate()
    .property(WidgetProperty.OPACITY, 1.0f)
    .to(0.6f)
    .duration(120)
    .easing(Easing.OUT_CUBIC);
```

Анимируемые свойства:

```text
opacity
color
scale
rotation
position
size
shader uniforms
scroll offset
blur radius
clip radius
```

Animation system должен:

- обновлять property values;
- вызывать visual invalidation;
- при необходимости вызывать layout invalidation;
- поддерживать transitions между widget states;
- поддерживать interrupt/reverse;
- поддерживать grouped timelines.

Разделение invalidation:

```text
layout dirty:
  меняется width, height, flex, margin, padding

visual dirty:
  меняется color, opacity, transform, shader uniform

texture dirty:
  меняется cached subtree render
```

---

## Будущие виджеты

Framework должен иметь понятную taxonomy виджетов, иначе быстро появятся дубли, которые делают одно и то же, но называются по-разному.

Правило:

~~~text
Публичный API может иметь удобные aliases.
Внутри engine должен быть один canonical widget или один общий base class.
~~~

Например:

~~~text
TextInput / TextField:
  можно оставить оба имени как aliases/builders,
  но внутри это один LineEdit-like widget.

ScrollView / ScrollPanel:
  можно оставить оба имени,
  но внутри это один scrollable container.

TabView / TabControl:
  aliases над одним tab container.

NodeGraph / GraphView:
  NodeGraph может быть специализацией GraphView.
~~~

### Widget library layers

Список виджетов лучше делить не на один огромный пакет, а на уровни.

~~~text
Core primitives:
  минимальные building blocks, из которых собираются остальные widgets.

Standard controls:
  обычные controls, которые нужны почти любому UI.

Composite widgets:
  собраны из primitives/controls и могут жить в standard widgets package.

Optional extension widgets:
  тяжёлые или платформенно-зависимые widgets, которые лучше делать через WidgetExtern или отдельный module.
~~~

Так framework не превращается в God-Widget/God-Package, но остаётся удобным.

Общий принцип:

~~~text
Core должен быть маленьким, стабильным и быстрым.
Большая библиотека widgets может расти вокруг core.
Редкие тяжёлые widgets подключаются как extensions.
~~~

### Game UI widget set

Из предложенного game-framework списка стоит взять не всё одинаково глубоко.

Обязательные core primitives:

~~~text
Text
TextureWidget
ImageView
Shape
Border
Separator
CanvasWidget
Path
Button
TextField
ScrollBar
Slider
ProgressBar
Popup
Tooltip
Window
Viewport3D
Chart
~~~

Пояснения:

~~~text
TextureWidget:
  низкоуровневый widget для TextureHandle, atlas region, icon, item preview texture.

ImageView:
  более высокий image widget с fit/fill/stretch/tint режимами.

Shape:
  rect/circle/rounded/line primitive widget для простых декоративных элементов.

Border:
  decorator widget или primitive для рамок/outline/focus ring.

Separator:
  тонкая линия/разделитель, может быть Shape alias.

Path:
  retained vector path widget, полезен для graph edges, curves, charts.

Viewport3D:
  render target для 3D preview: block, item, entity, model, custom scene.

Chart:
  Canvas-based виджет, не отдельный rendering backend.
~~~

Standard/composite widgets, которые стоит держать в основной библиотеке:

~~~text
CheckBox
RadioButton
Switch
ComboBox
ListBox
ListView
RecyclerView
TreeView
DataGrid
TableView
PropertyGrid
ColorPicker
TabBar
TabView
Accordion
Breadcrumb
Notification
Toast
Dialog
Card
Badge
Chip
Tag
Gauge
Spinner
ActivityIndicator
RatingBar
Knob
Dial
SplitButton
ContextMenu
PopupMenu
SearchField
PasswordField
RichText
~~~

Aliases/canonical mapping:

~~~text
ActivityIndicator -> Spinner / LoadingRing family
Switch -> ToggleButton specialization
ListBox -> ListView with selection model
DataGrid -> TableView with editing/sorting/filtering options
PopupMenu -> ContextMenu or MenuPopup
SearchField -> TextField with search affordances
RichText -> TextBlock with styled spans, links and inline objects
Gauge -> CanvasWidget/Shape-based progress visualization
Knob / Dial -> rotary Slider specialization
Badge / Chip / Tag -> small label-like composite widgets
~~~

Optional extension widgets:

~~~text
Calendar
DatePicker
MarkdownView
HTMLView
PDFViewer
ImageViewer
VideoPlayer
AudioPlayer
ModelViewer
TerminalView
Avatar
~~~

Почему optional:

~~~text
Calendar / DatePicker:
  полезны, но редко нужны в Minecraft/game UI.

MarkdownView:
  можно сделать поверх RichText, но parser лучше вынести отдельно.

HTMLView / PDFViewer:
  тяжёлые viewer-ы, не должны раздувать core.

VideoPlayer / AudioPlayer:
  зависят от codec/backend и лучше живут в extension module.

ModelViewer:
  может использовать Viewport3D и Minecraft/custom model rendering.

TerminalView:
  полезен для debug/dev tools, но не для runtime core.

Avatar:
  обычно composite над TextureWidget/ImageView, можно оставить в optional design widgets.
~~~

Все optional widgets должны быть реализуемы через WidgetExtern, чтобы сторонний мод или отдельный пакет мог добавить их без переписывания core.

### Layout containers

Базовые layout widgets:

~~~text
VBox
HBox
GridBox
StackPanel
DockPanel
AbsolutePanel
OverlayLayer
CanvasPanel
~~~

Canonical design:

~~~text
VBox:
  LinearBox with vertical direction

HBox:
  LinearBox with horizontal direction

GridBox:
  equal-cell grid container now backed by a compatibility V3 adapter;
  full CSS-grid-like model stays a future decision

StackPanel:
  overlay stack: normal children fill the padded slot, absolute children use insets

DockPanel:
  sequential dock semantics through a custom V3 adapter

AbsolutePanel:
  children positioned by explicit x/y

OverlayLayer:
  normal content + floating overlays rendered and hit-tested above content

CanvasPanel:
  low-level drawable area + optional child hosting
~~~

Возможный API:

~~~java
VBox box = Widgets.vbox();
HBox row = Widgets.hbox();
GridBox grid = Widgets.grid();
~~~

Или через один configurable widget:

~~~java
LinearBox box = new LinearBox(Direction.VERTICAL);
~~~

### Buttons and state controls

Виджеты:

~~~text
Button
ToggleButton
Switch
Checkbox
RadioButton
Slider
Knob
Dial
ProgressBar
Spinner
LoadingRing
ActivityIndicator
RatingBar
SplitButton
~~~

Canonical design:

~~~text
Button:
  clickable semantic action

ToggleButton:
  button with selected/on/off state

Switch:
  ToggleButton specialization with switch visual style

Checkbox:
  boolean toggle with check mark

RadioButton:
  exclusive selection inside RadioGroup

Slider:
  numeric value editor with min/max/step

Knob / Dial:
  rotary Slider specialization

ProgressBar:
  visual progress indicator

Spinner:
  indeterminate or determinate activity indicator

LoadingRing:
  circular spinner; can be alias/specialization of Spinner

ActivityIndicator:
  alias/family name for Spinner/LoadingRing

RatingBar:
  discrete rating input, usually stars/icons over ToggleButton-like items

SplitButton:
  Button + dropdown action menu
~~~

События:

~~~text
ButtonClickEvent
ToggleChangedEvent
CheckedChangedEvent
RadioSelectedEvent
ValueChangedEvent
ProgressChangedEvent
~~~

### Text widgets and fields

Виджеты:

~~~text
Label
TextBlock
TextField
TextInput
TextArea
NumberField
SpinBox
PasswordField
KeybindingField
SearchField
RichText
~~~

Canonical design:

~~~text
Label:
  simple single-line or lightweight text display

TextBlock:
  rich/multiline/wrapping text display

TextField / TextInput:
  single-line editable text input

TextArea:
  multiline editable text input

NumberField:
  text input constrained to numeric value

SpinBox:
  NumberField + increment/decrement buttons

PasswordField:
  TextField with masked display

KeybindingField:
  captures keyboard/mouse input and stores binding

SearchField:
  TextField with search icon, clear button and search events

RichText:
  TextBlock with styled spans, links, inline images and optional markup source
~~~

Важно:

- text editing, caret, selection и clipboard лучше вынести в отдельный TextEditorModel;
- rendering текста можно сначала делать через Minecraft font renderer, потом заменить на batched text renderer;
- TextField, TextInput, PasswordField, NumberField и KeybindingField должны переиспользовать общий input/editing core.

### Scroll, tabs and collapsible containers

Виджеты:

~~~text
ScrollView
ScrollPanel
ScrollBar
SplitView
SplitPane
TabView
TabControl
TabBar
Foldout
CollapsiblePanel
Accordion
Breadcrumb
GroupBox
~~~

Canonical design:

~~~text
ScrollView / ScrollPanel:
  one scrollable viewport container

ScrollBar:
  standalone or internal scrollbar widget

SplitView / SplitPane:
  resizable two-pane or multi-pane container

TabView / TabControl:
  tab header + active content panel

TabBar:
  header-only tab selector, can be part of TabView

Foldout:
  small expandable section, often header + body

Accordion:
  group of Foldout/CollapsiblePanel items with optional single-open policy

Breadcrumb:
  navigation path widget, usually a row of buttons/separators

CollapsiblePanel:
  larger animated expandable container

GroupBox:
  visual grouping container with optional title/border
~~~

Aliases:

~~~text
ScrollPanel -> ScrollView
SplitPane -> SplitView
TabControl -> TabView
~~~

### Data and collection widgets

Виджеты:

~~~text
ListView
ListBox
RecyclerView
TreeView
ComboBox
Dropdown
DataGrid
TableView
PropertyGrid
~~~

Canonical design:

~~~text
ListView:
  vertical/horizontal item list

ListBox:
  ListView with built-in selection model

RecyclerView:
  virtualized ListView for large data

TreeView:
  hierarchical list with expand/collapse nodes

ComboBox:
  input/select field + dropdown popup

Dropdown:
  popup list selector; can be internal part of ComboBox

DataGrid / TableView:
  tabular data widget with rows/columns

PropertyGrid:
  inspector-like table for object properties, useful for editors/debug panels
~~~

Aliases:

~~~text
Dropdown:
  low-level popup selector

ComboBox:
  field-like control that uses Dropdown

DataGrid / TableView:
  likely same canonical widget unless DataGrid later gets editing/sorting/filtering features
~~~

Для больших списков и таблиц обязательно нужна virtualization:

~~~text
visible range calculation
   ↓
recycle item renderers
   ↓
stable data model index
   ↓
only visible widgets/render commands are built
~~~

Иначе complex UI будет умирать на больших коллекциях.

### Windows, popups and overlays

Виджеты:

~~~text
Window
Dialog
ContextMenu
PopupMenu
Menu
Tooltip
VanillaTooltip
Notification
Toast
ModalWindow
~~~

Canonical design:

~~~text
Window:
  movable/resizable top-level panel

Dialog / ModalWindow:
  modal window with focus trap and input barrier

ContextMenu:
  popup menu anchored to mouse/widget

PopupMenu / Menu:
  generic menu popup; ContextMenu is a pointer-anchored specialization

Tooltip:
  UniGUI-rendered tooltip

VanillaTooltip:
  adapter for Minecraft vanilla tooltip rendering

Notification / Toast:
  timed overlay message
~~~

Всё это должно жить в top-level overlay layer:

~~~text
root content layer
popup layer
tooltip layer
modal layer
toast/notification layer
debug layer
~~~

### Media, color, graph and canvas widgets

Виджеты:

~~~text
ImageView
TextureWidget
TextureRect
Shape
Border
Separator
Path
ColorPicker
Chart
Gauge
VideoPlayer
AudioPlayer
Viewport3D
ModelViewer
ImageViewer
GraphView
NodeGraph
CanvasWidget
~~~

Canonical design:

~~~text
ImageView / TextureRect:
  texture/image drawing widget with fit/fill/stretch modes

TextureWidget:
  lower-level texture/icon widget around TextureHandle and atlas regions

Shape:
  simple primitive widget for rects, circles, lines and rounded shapes

Border:
  decorator or primitive for outlines, focus rings and framed panels

Separator:
  lightweight horizontal/vertical divider, often Shape alias

Path:
  retained vector path widget for curves, graph edges and icons

ColorPicker:
  compound widget for HSV/RGB/alpha selection

Chart / Gauge:
  Canvas-based data/progress visualization widgets

Viewport3D / ModelViewer:
  offscreen 3D preview surface for models, items, blocks or entities

VideoPlayer / AudioPlayer / ImageViewer:
  optional media widgets; should be extension-friendly rather than required core dependencies

GraphView:
  generic graph/canvas viewport with pan/zoom, culling and spatial index

NodeGraph:
  specialization of GraphView for node editor workflows

CanvasWidget:
  low-level immediate-style drawing surface that still outputs DrawCommands
~~~

Aliases:

~~~text
TextureWidget -> low-level texture/icon widget
TextureRect -> ImageView
NodeGraph -> specialized GraphView
Gauge -> Chart/Canvas visualization specialization
ModelViewer -> Viewport3D specialization
~~~

### Canvas and primitive drawing API

Framework должен позволять не только компоновать готовые widgets, но и рисовать фигуры/примитивы. Для этого нужен CanvasWidget.

Важно: CanvasWidget не должен рисовать сразу через OpenGL. Он тоже должен писать команды в DrawList, чтобы сохранялись batching, render-to-texture, clipping и shader pipeline.

Пример API:

~~~java
CanvasWidget canvas = new CanvasWidget();

canvas.onDraw(ctx -> {
    ctx.rect(10, 10, 100, 30, Paint.fill(Color.rgb(40, 40, 40)));
    ctx.roundedRect(10, 50, 100, 30, 6, Paint.fill(Color.rgb(80, 80, 120)));
    ctx.line(10, 100, 200, 100, Paint.stroke(Color.white(), 1));
    ctx.circle(40, 140, 20, Paint.fill(Color.red()));
    ctx.path(path -> {
        path.moveTo(10, 180);
        path.lineTo(80, 220);
        path.quadTo(120, 120, 180, 180);
    }, Paint.stroke(Color.green(), 2));
});
~~~

Canvas drawing primitives:

~~~text
rect
roundedRect
line
polyline
polygon
circle
ellipse
arc
path
bezier / quadratic curve
image
text
mesh
gradient
shadow
clip
mask
custom shader primitive
~~~

Canvas use cases:

~~~text
custom controls
graph edges
node backgrounds
debug overlays
mini maps
shader previews
charts
progress arcs
selection rectangles
custom item slots
~~~

Canvas должен поддерживать retained и callback modes.

~~~text
Callback mode:
  canvas.onDraw(ctx -> ...)
  удобно для быстрых custom widgets

Retained mode:
  canvas.commands().add(...)
  удобно для static geometry, graph edges, cached drawings
~~~

Для производительности:

- Canvas commands должны попадать в обычный batcher;
- static Canvas geometry можно кэшировать;
- complex Canvas можно рендерить в texture;
- path tessellation лучше кэшировать до изменения geometry;
- GraphView edges должны использовать Canvas/mesh pipeline, а не тысячи отдельных widgets.

### DockPanel

DockPanel лучше делать не через Yoga напрямую, а через custom layout strategy:

```java
public interface LayoutAlgorithm {
    void measure(Widget widget, LayoutContext context);

    void arrange(Widget widget, LayoutContext context);
}
```

Так можно добавить:

- DockPanel;
- Grid;
- AbsoluteCanvas;
- OverlayPanel;
- GraphCanvas;
- VirtualizedList.

### DragAndDrop

Нужны:

- pointer capture;
- drag threshold;
- drag payload;
- drop target discovery;
- drag preview render-to-texture;
- modal/top layer rendering.

Drag preview можно рендерить через тот же subtree-to-texture механизм.

### ModalWindow

Нужны:

- top-level layer;
- modal input barrier;
- focus trap;
- dim background pass;
- transition animations;
- optional blur of background через render-to-texture.

---

## GraphView

GraphView нужно учитывать сразу. Его нельзя делать просто как ScrollView с тысячами child widgets.

Нужна отдельная canvas-like система:

```text
GraphView
  camera transform: pan + zoom
  spatial index
  virtualized nodes
  edge renderer
  node renderer
  selection layer
  interaction layer
  minimap / overview optional
```

Особенности:

- zoom/pan transform;
- culling по viewport;
- LOD для мелкого масштаба;
- cached node textures;
- batched edges;
- GPU-friendly line/curve rendering;
- hit-test через spatial index;
- selection box;
- drag nodes;
- reroute edges;
- shader effects для selected/highlighted nodes.

Graph node может быть обычным widget subtree, который кэшируется в texture.

```text
Node widget subtree
   ↓
render-to-texture
   ↓
GraphView draws node as image
```

Это позволит не пересчитывать и не перерисовывать сложные node UI каждый кадр.

---

## RenderGraph

Для сложного UI лучше иметь небольшой render graph.

Пример:

```text
Pass 1: main UI into offscreen target
Pass 2: blur modal background
Pass 3: modal UI
Pass 4: final composite to screen
```

Или:

```text
Pass 1: GraphView nodes cache
Pass 2: GraphView edges
Pass 3: GraphView nodes
Pass 4: GraphView selection overlay
Pass 5: main UI composite
```

RenderGraph не обязан быть огромным как в game engine. Достаточно lightweight abstraction:

```java
public interface RenderPass {
    RenderTarget target();

    void execute(RenderBackend backend);
}
```

---

## Зафиксированные решения для первого этапа

Эти решения принимаются как стартовые. Их можно поменять позже, но первый каркас должен строиться вокруг них.

### Frame lifecycle

Базовый порядок кадра:

~~~text
beginFrame
applyQueuedMutations
input/events
layout
animation/tick
buildDrawList
batch
render
endFrame
~~~

Правило:

- изменения widget tree применяются только в safe point;
- события могут поставить add/remove child в очередь, но не ломают текущий traversal;
- render phase получает стабильный snapshot draw commands;
- backend не читает mutable widget state напрямую.

### Coordinates and scale

Внутри engine используются logical UI pixels в `float`.

~~~text
UniGUI core:
  float logical coordinates

Scale provider:
  logical UI pixels <-> backend/Minecraft scaled pixels

Backend:
  округляет/приводит координаты только на render boundary при необходимости
~~~

Для этого нужен `UIScaleProvider`:

~~~java
public interface UIScaleProvider {
    float scale();

    float toBackendPixels(float logicalPixels);

    float toLogicalPixels(float backendPixels);
}
~~~

Minecraft GUI scale не должен быть зашит в widgets. Его должен отдавать Minecraft-specific provider/backend.

### Transform and clipping

Transform применяется после layout.

~~~text
Yoga/custom layout:
  x, y, width, height

Visual transform:
  position, scale, rotation, pivot

World transform:
  parent * local

Hit-test:
  inverse(world transform)
~~~

Clipping для MVP:

~~~text
axis-aligned scissor для обычных containers
flush/barrier при смене clip
rotated clipping позже через stencil/mask/render-to-texture
~~~

То есть rotated widget может рендериться, но сложный rotated clip не обязан быть в MVP.

### Public API and internal API

Сразу разделяем API и implementation:

~~~text
dev.sixik.unigui.api
  стабильные интерфейсы и value objects

dev.sixik.unigui.impl
  внутренние реализации, которые можно менять

dev.sixik.unigui.widgets
  стандартные widgets

dev.sixik.unigui.backend
  backend implementations
~~~

Пользовательские внешние виджеты должны предпочитать `WidgetExtern`, а не наследование от внутренних implementation classes.

### Ownership and lifecycle

Widget tree владеет своими children.

Правила:

- `addChild` передаёт widget под управление parent/root;
- `removeChild` отсоединяет widget и сбрасывает parent/context;
- `dispose` вызывается при окончательном удалении subtree/root;
- GPU resources (`TextureHandle`, `RenderTarget`, `ShaderProgram`) должны иметь явный lifecycle;
- cached render-to-texture targets принадлежат cache/subtree owner и освобождаются через resource manager.

Для MVP достаточно:

~~~text
Widget.dispose()
PanelWidget.addChild/removeChild/clearChildren
Resource owner позже в render backend
~~~

### DrawCommand snapshot

DrawCommand не хранит живые ссылки на mutable widget state.

Плохо:

~~~java
command.transform = widget.transform();
~~~

Правильно:

~~~java
command.transform().copyFrom(widget.transform());
~~~

Или через packed command buffer для hot path.

### Theme/style

Theme/style нужно заложить сразу, но не обязательно строить полный CSS-like движок в MVP.

Минимальный MVP:

~~~text
Theme
Style
StyleKey
WidgetState: normal / hovered / pressed / disabled / focused / selected / checked
~~~

Позже можно добавить:

~~~text
selectors
style inheritance
transitions between states
theme reload
per-mod theme packs
~~~

### Text system

На MVP используется Minecraft text renderer через backend/barrier.

Но public API должен быть таким, чтобы позже заменить его на batched text renderer без переписывания widgets.

### Debug and profiler

Нужно заложить debug/profiler как в игровых движках.

MVP flags:

~~~text
widget bounds
dirty flags
draw command count
batch count
overdraw debug
focused/hovered widget
profiler overlay
~~~

Profiler должен поддерживать scopes:

~~~java
try (ProfileScope ignored = profiler.scope("layout")) {
    // layout work
}
~~~

Позже можно добавить overlay в стиле game engine: frame time, layout time, render time, batch count, draw command count, texture cache stats.

## Минимальный MVP

Не стоит начинать сразу с GraphView и shader graph.

Лучший порядок:

1. Core widget tree.
2. Mutable value objects: MutableVec2, MutableRect, MutableColor, Transform.
3. Invalidation model: layout dirty, visual dirty, texture dirty.
4. Deferred mutation queue для add/remove/move children.
5. FrameScheduler / UiDispatcher для безопасных изменений между кадрами и потоками.
6. `RenderContext`, `DrawCommand`, `DrawList`.
7. Backend abstraction.
8. Простой Minecraft backend для rect/image/text.
9. Batcher для rect/image.
10. RenderTarget abstraction.
11. Render-to-texture для widget subtree.
12. WidgetRenderer / ExternalWidgetWrapper для embedding в UI других модов.
13. WidgetExtern / WidgetExternAdapter для внешних пользовательских виджетов.
14. Yoga wrapper.
15. Event system: EventBus, EventEmitter, WidgetEvent, ButtonClickEvent.
16. Input + transform-aware hit-test.
17. Basic primitives: Text, TextureWidget, ImageView, Shape, Border, Separator, Path.
18. Basic widgets: Box, Button, PanelWidget.
19. Basic layout widgets: VBox, HBox, GridBox.
20. CanvasWidget + primitive drawing API.
21. Basic controls: ToggleButton, Switch, Checkbox, Slider, ProgressBar, Spinner.
22. Basic text inputs: TextField/TextInput, NumberField, PasswordField, SearchField.
23. ScrollView + ScrollBar.
24. Window/Popup/Tooltip basics.
25. Animation property system.
26. Minecraft item/block/entity commands.
27. Cached item/entity preview.
28. Virtualized ListView/RecyclerView prototype.
29. GraphView / NodeGraph prototype.

### Текущий checkpoint реализации

В `common` уже заложен первый retained-mode слой:

- core value objects, invalidation flags, `FrameContext`, `UiDispatcher`, `UIContext`;
- `WidgetBase` и `PanelWidget` с deferred child mutations, snapshot traversal и propagated subtree dirty flags;
- `RenderContext`, `DrawCommand`, `DrawList`, backend/target/texture/clip abstractions;
- snapshot `VectorPath` и `PATH` draw command, чтобы `Path` не уходил в immediate render;
- event API, pointer/keyboard/text/scroll/focus events, минимальный `ButtonClickEvent` и routed dispatch с capture/target/bubble;
- `HitTester` + `TransformHitTester` для transform-aware hit-test;
- `SimpleDrawBatcher` для последовательного batching `RECT` и `TEXTURE` команд;
- `RenderTargetOptions`, `RenderTargetCache`, `WidgetTextureRenderer` и Minecraft `TextureTarget` wrapper для render-to-texture subtree;
- `UiDebugCounters`, `FrameDebugCounters`, `FrameProfiler` и `DebugOverlayRenderer` для общего profiler/debug overlay с draw/batch/cache counters;
- `MinecraftGuiRenderBackend`, `MinecraftTextureHandle`, `MinecraftRenderTarget` и `MinecraftWidgetScreen` для первого Minecraft GUI/offscreen render/scissor path;
- Minecraft backend теперь рендерит rounded rect/circle/path через базовую tessellation, а не только MVP rect/scanline fallback;
- primitive widgets: `Text`, `Label`, `TextBlock`, `TextureWidget`, `ImageView`, `Shape`, `Border`, `Separator`, `CanvasWidget`, `Path`;
- basic widgets/layout shells: `Box`, `Button`, `ToggleButton`, `Checkbox`, `TextInput`, `TextField`, `NumberField`, `PasswordField`, `SearchField`, `Slider`, `ProgressBar`, `ScrollBar`, `ScrollView`, `VirtualListView` с fixed-row virtualization для больших списков, `VirtualTableView`/`VirtualTableColumn` для fixed-row virtualized data-grid prototype, axis-aligned clip/scissor и встроенной scrollbar binding, `CachedSubtreeWidget` с cache hit/miss counters, общими frame counters и `DebugFlags.CACHED_SUBTREE` overlay, `VBox`, `HBox`, `GridBox`, `StackPanel`, `DockPanel`, `WrapPanel`, `Widgets` factory;
- layout/measurement: `LayoutContext`, `LayoutSize`, `Widget.desiredSize()`, `LayoutConstraints`, `EdgeInsets`, `Alignment`, `Widget.layoutConstraints()`, mutable `WidgetBase` helpers for preferred/min/max size, margins, alignment and grow weight, text/TextInput/Button intrinsic desired-size measurement, plus `LinearBox`/`GridBox` slot sizing and richer `StackPanel`/`DockPanel`/`WrapPanel` containers that aggregate desired sizes and respect collapsed children;
- Layout V3 migration layer: `api.layout.v3` backend-neutral model, `TaffyLayoutEngine`, `LayoutV3FlexAdapter`, `LayoutV3StackAdapter`, `LayoutV3ScrollAdapter`, `LayoutV3SplitAdapter`, `LayoutV3DockAdapter`, `LayoutV3GridAdapter`, `LayoutCache`, `LayoutDebugDumper`, `OverlayLayoutResolver`, default-on `direct V3 default path` compatibility/reference code, and `LayoutV3SelfTest` coverage wired into `:common:test`;
- virtualization core: `VirtualRange` and `FixedRowVirtualizer` hold shared fixed-row content extent, max scroll, overscan window and viewport-relative item offset logic used by both `VirtualListView` and `VirtualTableView`;
- selection contracts: `SelectionMode`, `IndexSelectionModel` and `SelectionChangedEvent` provide shared single/multiple index selection for virtualized list/table rows, including selected row highlight, change events and pruning when item/row count shrinks;
- table sorting contracts: `SortDirection` and `TableSortChangedEvent` expose `VirtualTableView` column sort state, sort-key provider, per-column row comparator hooks, header click cycle and header sort markers;
- table column contracts: `TableColumnResizedEvent` and `TableColumnMovedEvent` expose `VirtualTableView` column width changes, divider drag resize with pointer capture, min column width clamping and API-level column reorder/remap semantics;
- text editing core: reusable `TextEditorModel`, reusable `TextInput` shell separated from `TextField` chrome, caret, selection API, delete/replace selection, Ctrl+A/C/X/V clipboard flow, `ClipboardService` abstraction, `MemoryClipboardService` and Minecraft clipboard bridge;
- theme/style seed: `StyleKey`, `StyleKeys`, `MutableStyle`, `MutableTheme`, `WidgetState`, `DefaultTheme` and `UIContext.theme()` default lookup layer;
- runtime theme application, inheritance and invalidation: `Box.applyTheme()` reads state-aware style tokens before render, walks local style scopes from ancestors to widget, tracks `UIContext.styleVersion()` plus local scope style versions, standard controls apply background/border/text/accent/track/thumb defaults, `DefaultUIContext.theme(theme, root)` invalidates attached widget trees, and `themeEnabled(false)` keeps manual visual overrides;
- keyboard focus traversal: `Widget.focusable()`, `focusOrder()`, `focusScope()`, `FocusDirection`, `DefaultFocusManager.focusNext/focusPrevious/focusDirectional`, Minecraft `Tab` / `Shift+Tab` dispatch path, and arrow/D-pad-style directional fallback when focused widgets do not consume navigation keys;
- hover/style state tracking: `HoverManager`, `DefaultHoverManager`, `PointerEnteredEvent`, `PointerExitedEvent`, `Widget.hovered()`, `WidgetState.HOVERED` default theme tokens, and render/style invalidation on hover transitions;
- base widget state flags: `Widget.visibility()` with `Visibility.VISIBLE/HIDDEN/COLLAPSED`, backwards-compatible `visible(false) -> HIDDEN`, `Widget.enabled()`, mutable `WidgetBase.visibility/visible/enabled`, disabled style state, HIDDEN layout participation, COLLAPSED layout skip, render/input/hit-test/focus/hover traversal opt-out for non-visible/disabled widgets;
- overlay basics: `OverlayLayer` hosts render-on-top widgets above normal content without stealing layout from the content subtree, uses deterministic content/overlay ordering, and supports V3 portal-style dropdown/popup escape from parent clips;
- popup basics: `Popup` builds on `OverlayLayer` with open/close/toggle state, anchor-based positioning, interactive popup content, V3 resolver placement by default and non-modal outside-click close handled at overlay capture phase;
- window/dialog basics: `WindowWidget` builds on `OverlayLayer` with open/close/toggle state, title bar rendering, close button, draggable pointer-captured movement, bounds clamping and optional outside-click close;
- animation property system: `AnimatedProperty`, `AnimationEasing`, `TransitionSpec` and `FloatTransition` provide tick-driven opacity/position/scale transitions on `WidgetBase`, render-time opacity stacking through `RenderContext`, real frame delta from `MinecraftWidgetScreen`, and opt-in `Button` hover/pressed interaction transitions;
- Minecraft preview widgets: `MinecraftPreviewWidget`, `MinecraftItemPreviewWidget`, `MinecraftBlockPreviewWidget` and `MinecraftEntityPreviewWidget` provide backend-specific custom draw previews for item stacks, block-as-item previews and living entities, with text fallback for non-Minecraft render backends;
- reproducible Gradle launcher: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and Gradle 8.12.1 wrapper properties;
- no-dependency `CachedSubtreeInvalidationSelfTest`, который проверяет first miss, stable hit, child dirty, resize, manual dirty и общий overlay output;
- no-dependency `BasicControlsSelfTest`, который проверяет TextEditorModel core mutations, TextInput shell vs TextField chrome split, TextField focus/editing, selection/clipboard, PasswordField masking, SearchField submit/clear, theme defaults/style version invalidation, style inheritance/scopes, keyboard + directional focus traversal/scopes, hover enter/exit/style state, disabled/visible state flags, desired-size measurement для Label/TextBlock/TextField и container aggregation, layout constraints/slot sizing, StackPanel/DockPanel/WrapPanel layout contracts, `FixedRowVirtualizer` range/scroll clamp/item offset, VirtualListView/VirtualTableView selection contracts/events/highlight/pruning, VirtualTableView sorting state/events/sort-key provider/comparator/header click cycle, VirtualListView realized window/scroll clamp/clip, VirtualTableView virtual rows/headers/cells/scroll clamp, Slider pointer/key input, ScrollView bubbled wheel input, clip commands, ScrollBar drag binding, ToggleButton/Checkbox, ProgressBar и NumberField.

Следующий практический блок:

1. проверить in-game visual smoke с `Layout V3 enabled` toggle в `/unigui`;
2. обновить user-facing examples/docs под Layout V3 flags и overlay portal semantics;
3. решить, когда подключать `LayoutCache` к live widgets после появления style/content/children/visibility versions.

---

## Definition of Done для первого этапа

Первый этап можно считать успешным, если:

- виджет может добавить `rect/image/text` команды в `DrawList`;
- команды не рисуются сразу;
- position/size/scale/color можно менять без постоянного создания новых Vector/Rect/Color объектов;
- mutable changes корректно вызывают layout/visual/texture invalidation;
- PanelWidget может принимать add/remove child во время event/input/render без ConcurrentModificationException;
- mutations из других потоков проходят через UiDispatcher или mutation queue;
- event listener может изменить widget tree, не ломая текущий traversal;
- backend может отрисовать `DrawList` на экран;
- несколько image/rect команд батчатся;
- можно создать offscreen `RenderTarget`;
- можно отрендерить widget subtree в texture;
- эту texture можно потом использовать как обычную image command;
- widget subtree можно встроить в чужой widget через direct render или render-to-texture wrapper;
- WidgetExtern может быть подключён через adapter и участвовать в layout/events/render-to-texture;
- button click и другие действия проходят через Event API;
- VBox/HBox/GridBox могут раскладывать children через общий layout pipeline;
- CanvasWidget может добавлять primitive draw commands без immediate OpenGL render;
- TextureWidget/ImageView могут рисовать icon/texture/atlas region через общий batching pipeline;
- Shape/Border/Separator/Path не создают отдельный render path, а используют Canvas/DrawCommand primitives;
- базовые aliases не плодят разные реализации одного и того же widget-а;
- Minecraft-specific API не протекает в core widget classes.

---

## Важные риски

### Minecraft render state

Minecraft может менять OpenGL/render state между своими render этапами. Нужно аккуратно изолировать state:

- restore blend/depth/scissor;
- controlled shader binding;
- controlled texture binding;
- explicit flush points;
- debug validation в dev mode.

### Text rendering

Text batching может быть отдельной большой задачей.

На MVP можно использовать Minecraft font renderer через barrier/flush, а затем заменить на собственный batched text renderer.

### Yoga dependency

Если Yoga остаётся `compileOnly`, нужно проверить runtime packaging.

Возможные варианты:

- сделать `implementation`;
- shade dependency;
- предоставить optional adapter;
- сделать fallback layout без Yoga для basic widgets.

### Version portability

Нужно избегать прямых ссылок на Minecraft classes в core API.

Плохо:

```java
void render(GuiGraphics graphics);
```

Лучше:

```java
void render(RenderContext context);
```

Minecraft-specific объекты должны жить только в backend/platform packages.

---

## Общий вывод

Нужен не просто UI kit, а маленький UI rendering engine:

```text
retained widgets
+ Yoga layout
+ command-based rendering
+ batching
+ render targets
+ shader API
+ mutable value objects with controlled invalidation
+ deferred widget mutations
+ event-driven interaction model
+ WidgetExtern for external custom widgets
+ external widget embedding
+ transform-aware input
+ animation system
+ Minecraft backend bridge
```

Если эту архитектуру держать с самого начала, то DockPanel, DragAndDrop, ModalWindow, GraphView, shader effects и cached Minecraft previews можно будет добавлять без переписывания всего framework-а.

## By Aros
- Kotlin support
