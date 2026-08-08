# Widget Renderer / Skin API Spec

Status: draft, implementation started

Scope: UniGUI common module, retained-mode widgets, deferred draw command rendering.

Primary pilot widget: LoadingIndicator.

First implemented renderer-backed widgets:

- LoadingIndicator
- ProgressBar
- Slider
- ScrollBar
- Button
- ToggleButton
- Checkbox
- RadioButton
- TextInput
- TextField
- SearchField
- PasswordField
- NumberField
- Shape
- Separator
- Border
- Tooltip
- TextureWidget
- ImageView
- Path
- CachedSubtreeWidget
- Box
- Popup
- WindowWidget

## Goal

Give UniGUI widgets the same kind of visual freedom that an ImGui user gets from direct DrawList calls, while keeping UniGUI retained-mode, backend-neutral, theme-aware, and safe for Minecraft render pipelines.

The API should let users do three things:

- Use default widget visuals without thinking about render internals.
- Swap a widget visual implementation with a preset renderer or a global renderer implementation.
- Write fully custom drawing logic for a widget through a safe draw-list-like facade.

The first implementation target is LoadingIndicator because it already has one state model but multiple hardcoded visual modes: spinner, dots, and bar.

## Non-goals

- Do not expose Minecraft GuiGraphics, PoseStack, OpenGL, or RenderBackend to ordinary widget customization.
- Do not convert UniGUI to immediate-mode UI.
- Do not require subclassing a widget just to change its visual style.
- Do not force all widgets to migrate at once.
- Do not remove the low-level CustomDraw escape hatch; keep it for advanced backend-specific rendering.

## Design Principles

### Widget owns behavior

The widget owns:

- layout and measured size;
- input state;
- animation state;
- theme lookup;
- invalidation;
- child rendering order.

The renderer owns only the visual representation.

### Renderer receives a snapshot

A renderer must receive a stable render-state snapshot, not live mutable widget internals. This keeps draw commands deterministic and avoids accidental mutation during render traversal.

### DrawScope is the public DrawList

Users should not need RenderBackend. They should receive a DrawScope that maps to current RenderContext primitives:

- rect
- roundedRect
- circle
- line
- path
- texture
- text
- clip

DrawScope is the UniGUI equivalent of an ImGui DrawList, but it emits backend-neutral DrawCommands.

### Global defaults and local overrides coexist

There should be two levels of customization:

- Global visual implementation through WidgetsRender / WidgetsRenderImpl.
- Per-widget renderer override through widget.renderer(...).

Per-widget renderer always wins over global default.

## Terminology

### Widget Renderer

A small strategy object or lambda that draws one widget from a state snapshot.

~~~java
@FunctionalInterface
public interface WidgetRenderer<S> {
    void render(DrawScope draw, S state);
}
~~~

### DrawScope

A safe facade over RenderContext. It should expose common draw primitives and automatically apply the widget transform and effective paint behavior.

DrawScope should not expose RenderBackend by default.

### Widget Render State

An immutable or effectively immutable object passed to renderer code for one render call.

For LoadingIndicator this state includes bounds, phase, colors, segment count, thickness, etc.

### WidgetsRender

Global facade that returns default renderers for widgets.

### WidgetsRenderImpl

Replaceable implementation that provides the project-wide default widget visual layer.

### Escape Hatch

Advanced users can still use RenderContext.custom(...) or a separate explicit DrawScope.custom(...) API, but this must be treated as backend-specific and non-portable.

## Proposed Package Layout

Preferred package layout:

~~~text
dev.sixik.unigui.api.render
  DrawScope

dev.sixik.unigui.api.widget.render
  WidgetRenderer

dev.sixik.unigui.api.widget.skin
  WidgetsRender
  WidgetsRenderImpl
  DefaultWidgetsRenderImpl

dev.sixik.unigui.widgets.render
  LoadingIndicatorRenderer
  LoadingIndicatorRenderers
  ButtonRenderer
  SliderRenderer
  ProgressBarRenderer
~~~

Alternative naming:

- WidgetSkins instead of WidgetsRender.
- WidgetPainter instead of WidgetRenderer.
- WidgetAppearance for mutable visual parameters.

Recommendation: use Renderer for code that draws, Skin for a collection of renderers, and Appearance for configurable parameters.

## Core API

### WidgetRenderer

~~~java
package dev.sixik.unigui.api.widget.render;

import dev.sixik.unigui.api.render.DrawScope;

@FunctionalInterface
public interface WidgetRenderer<S> {
    void render(DrawScope draw, S state);
}
~~~

Rules:

- Renderer should be fast and allocation-light.
- Renderer should not store DrawScope.
- Renderer should not mutate widget tree.
- Renderer should not call layout APIs.
- Renderer may read state and emit draw commands.
- Renderer may be a singleton if it has no mutable state.

### DrawScope

~~~java
package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.text.RichText;

public final class DrawScope {
    private final RenderContext context;
    private final Transform transform;

    public DrawScope(RenderContext context, Transform transform) {
        this.context = context;
        this.transform = transform;
    }

    public void rect(float x, float y, float width, float height, Paint paint) {
        context.rect(x, y, width, height, paint, transform);
    }

    public void roundedRect(float x, float y, float width, float height, float radius, Paint paint) {
        context.roundedRect(x, y, width, height, radius, paint, transform);
    }

    public void circle(float x, float y, float width, float height, Paint paint) {
        context.circle(x, y, width, height, paint, transform);
    }

    public void line(float x1, float y1, float x2, float y2, Paint paint) {
        context.line(x1, y1, x2, y2, paint, transform);
    }

    public void path(VectorPath path, float x, float y, float width, float height, Paint paint) {
        context.path(path, x, y, width, height, paint, transform);
    }

    public void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint) {
        context.texture(texture, x, y, width, height, paint, transform);
    }

    public void text(String text, float x, float y, float width, float height, Paint paint) {
        context.text(text, x, y, width, height, paint, transform);
    }

    public void text(RichText text, float x, float y, float width, float height, Paint paint) {
        context.text(text, x, y, width, height, paint, transform);
    }

    public void pushClip(float x, float y, float width, float height) {
        context.pushClip(x, y, width, height);
    }

    public void popClip() {
        context.popClip();
    }
}
~~~

Implementation notes:

- DrawScope can initially be a thin wrapper.
- Later it can add helpers like arc, polyline, ring, shadow, gradient, and cached paths.
- If transform is null, DrawScope should route to non-transform overloads or use identity semantics.
- Clip methods need careful documentation because clips are stack-based.

### WidgetsRender

~~~java
package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;

public final class WidgetsRender {
    private static WidgetsRenderImpl impl = DefaultWidgetsRenderImpl.INSTANCE;

    public static WidgetsRenderImpl current() {
        return impl;
    }

    public static void use(WidgetsRenderImpl customImpl) {
        impl = customImpl == null ? DefaultWidgetsRenderImpl.INSTANCE : customImpl;
    }

    public static LoadingIndicatorRenderer loadingSpinner() {
        return impl.loadingSpinner();
    }

    public static LoadingIndicatorRenderer loadingDots() {
        return impl.loadingDots();
    }

    public static LoadingIndicatorRenderer loadingBar() {
        return impl.loadingBar();
    }

    public static LoadingIndicatorRenderer loadingDefault() {
        return impl.loadingDefault();
    }

    private WidgetsRender() {
    }
}
~~~

Rules:

- WidgetsRender is a facade, not the renderer itself.
- It should contain no drawing algorithms.
- It should be safe to call from widget constructors.
- Global replacement should affect future default renderer lookups.

Open question:

- Should global replacement affect already-created widgets that did not explicitly set a renderer?

Recommended answer:

- Yes, if widget.renderer is null, resolve from WidgetsRender during render.
- No, if the widget stores a renderer explicitly.

This allows live theme-wide replacement while preserving local overrides.

### WidgetsRenderImpl

~~~java
package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ScrollBarRenderer;
import dev.sixik.unigui.widgets.render.SliderRenderer;

public interface WidgetsRenderImpl {
    default LoadingIndicatorRenderer loadingDefault() {
        return null;
    }

    default LoadingIndicatorRenderer loadingSpinner() {
        return null;
    }

    default LoadingIndicatorRenderer loadingDots() {
        return null;
    }

    default LoadingIndicatorRenderer loadingBar() {
        return null;
    }

    default ProgressBarRenderer progressBar() {
        return null;
    }

    default SliderRenderer slider() {
        return null;
    }

    default ScrollBarRenderer scrollBar() {
        return null;
    }

    default ButtonRenderer button() {
        return null;
    }

    default LoadingIndicatorRenderer loadingForMode(LoadingIndicatorMode mode) {
        return switch (mode == null ? LoadingIndicatorMode.SPINNER : mode) {
            case SPINNER -> loadingSpinner();
            case DOTS -> loadingDots();
            case BAR -> loadingBar();
        };
    }
}
~~~

Default methods returning null are intentional. They allow a custom WidgetsRenderImpl to override only the renderers it cares about while WidgetsRender falls back to DefaultWidgetsRenderImpl for the rest.

Note:

- If LoadingIndicator.Mode remains nested inside LoadingIndicator, avoid referencing it from api.widget.skin to prevent package coupling.
- Either move mode enum to a public small enum, or keep mode-to-renderer mapping inside LoadingIndicator.

### DefaultWidgetsRenderImpl

~~~java
package dev.sixik.unigui.api.widget.skin;

import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderer;
import dev.sixik.unigui.widgets.render.LoadingIndicatorRenderers;

public final class DefaultWidgetsRenderImpl implements WidgetsRenderImpl {
    public static final DefaultWidgetsRenderImpl INSTANCE = new DefaultWidgetsRenderImpl();

    private DefaultWidgetsRenderImpl() {
    }

    @Override
    public LoadingIndicatorRenderer loadingDefault() {
        return loadingSpinner();
    }

    @Override
    public LoadingIndicatorRenderer loadingSpinner() {
        return LoadingIndicatorRenderers.SPINNER;
    }

    @Override
    public LoadingIndicatorRenderer loadingDots() {
        return LoadingIndicatorRenderers.DOTS;
    }

    @Override
    public LoadingIndicatorRenderer loadingBar() {
        return LoadingIndicatorRenderers.BAR;
    }
}
~~~

## LoadingIndicator API

### Renderer type

~~~java
package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;

@FunctionalInterface
public interface LoadingIndicatorRenderer extends WidgetRenderer<LoadingIndicatorState> {
}
~~~

### State snapshot

~~~java
package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

public record LoadingIndicatorState(
        float x,
        float y,
        float width,
        float height,
        float phase,
        float speed,
        int segments,
        float thickness,
        ColorView accentColor,
        ColorView trackColor
) {
    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }

    public float size() {
        return Math.min(width, height);
    }

    public float phaseRadians() {
        return phase * ((float) Math.PI * 2.0f);
    }
}
~~~

Rules:

- State must be cheap to construct.
- State must not expose mutable widget-owned objects unless they are read-only views.
- If ColorView points at MutableColor, renderer must treat it as read-only.
- If later this becomes hot, replace record with reusable snapshot object owned by the widget.

### LoadingIndicator widget changes

Add fields:

~~~java
private LoadingIndicatorRenderer renderer;
~~~

Add API:

~~~java
public LoadingIndicatorRenderer renderer() {
    return renderer;
}

public LoadingIndicator renderer(LoadingIndicatorRenderer renderer) {
    if (this.renderer == renderer) return this;
    this.renderer = renderer;
    invalidate(InvalidationFlags.VISUAL);
    return this;
}

public LoadingIndicator useDefaultRenderer() {
    return renderer(null);
}
~~~

Rendering:

~~~java
@Override
protected void renderContent(RenderContext context) {
    applyTheme();

    LoadingIndicatorRenderer effectiveRenderer = renderer != null
            ? renderer
            : WidgetsRender.loadingForMode(mode);

    effectiveRenderer.render(new DrawScope(context, transform()), snapshot());

    super.renderContent(context);
}
~~~

Snapshot:

~~~java
private LoadingIndicatorState snapshot() {
    return new LoadingIndicatorState(
            layoutBounds().x(),
            layoutBounds().y(),
            layoutBounds().width(),
            layoutBounds().height(),
            phase,
            speed,
            segments,
            thickness,
            accentColor,
            trackColor);
}
~~~

Compatibility:

- Keep mode(...) initially.
- mode(...) should select default renderer behavior, not lock the widget into hardcoded rendering.
- renderer(...) should override mode rendering.
- Later, Mode can be deprecated if renderer presets become the preferred API.

### LoadingIndicatorRenderers

~~~java
package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;

public final class LoadingIndicatorRenderers {
    private static final float TAU = (float) (Math.PI * 2.0);

    public static final LoadingIndicatorRenderer SPINNER = (draw, s) -> {
        float size = Math.max(1.0f, s.size());
        float dotSize = Math.max(2.0f, Math.min(size * 0.20f, s.thickness() * 1.6f));
        float radius = Math.max(0.0f, size * 0.5f - dotSize * 0.5f);

        for (int i = 0; i < s.segments(); i++) {
            float angle = ((i / (float) s.segments()) + s.phase()) * TAU - (float) Math.PI * 0.5f;
            float fade = (i + 1.0f) / s.segments();
            MutableColor color = colorWithAlpha(s.accentColor(), 0.18f + fade * 0.82f);
            draw.circle(
                    s.centerX() + (float) Math.cos(angle) * radius - dotSize * 0.5f,
                    s.centerY() + (float) Math.sin(angle) * radius - dotSize * 0.5f,
                    dotSize,
                    dotSize,
                    Paint.fill(color));
        }
    };

    public static final LoadingIndicatorRenderer DOTS = (draw, s) -> {
        // Port current renderDots implementation here.
    };

    public static final LoadingIndicatorRenderer BAR = (draw, s) -> {
        // Port current renderBar implementation here.
    };

    private LoadingIndicatorRenderers() {
    }
}
~~~

Implementation detail:

- Existing private methods renderSpinner, renderDots, and renderBar should move into LoadingIndicatorRenderers.
- Existing color helper can move to a small RenderColors utility or stay private inside LoadingIndicatorRenderers.

## User Examples

### Default usage

~~~java
LoadingIndicator indicator = new LoadingIndicator()
        .indicatorSize(24)
        .speed(1.0f);
~~~

### Preset renderer

~~~java
LoadingIndicator indicator = new LoadingIndicator()
        .indicatorSize(32)
        .renderer(WidgetsRender.loadingSpinner())
        .segments(12)
        .thickness(4.0f);
~~~

### Fully custom renderer

~~~java
LoadingIndicator indicator = new LoadingIndicator()
        .indicatorSize(36)
        .speed(1.2f)
        .renderer((draw, s) -> {
            float radius = s.size() * 0.42f;
            float sweep = (float) Math.PI * 1.5f;
            float start = s.phaseRadians();

            for (int i = 0; i < s.segments(); i++) {
                float t0 = i / (float) s.segments();
                float t1 = (i + 1) / (float) s.segments();
                float a0 = start + t0 * sweep;
                float a1 = start + t1 * sweep;

                draw.line(
                        s.centerX() + (float) Math.cos(a0) * radius,
                        s.centerY() + (float) Math.sin(a0) * radius,
                        s.centerX() + (float) Math.cos(a1) * radius,
                        s.centerY() + (float) Math.sin(a1) * radius,
                        Paint.stroke(
                                colorWithAlpha(s.accentColor(), 0.15f + t0 * 0.85f),
                                1.0f + t0 * s.thickness()));
            }
        });
~~~

### Global visual override

~~~java
WidgetsRender.use(new MyModWidgetsRenderImpl());
~~~

Example:

~~~java
public final class MyModWidgetsRenderImpl extends DefaultWidgetsRenderImpl {
    @Override
    public LoadingIndicatorRenderer loadingDefault() {
        return MyLoadingRenderers.GLOWING_ARC;
    }
}
~~~

If DefaultWidgetsRenderImpl has a private constructor, use composition instead:

~~~java
public final class MyModWidgetsRenderImpl implements WidgetsRenderImpl {
    private final WidgetsRenderImpl fallback = DefaultWidgetsRenderImpl.INSTANCE;

    @Override
    public LoadingIndicatorRenderer loadingDefault() {
        return MyLoadingRenderers.GLOWING_ARC;
    }

    @Override
    public LoadingIndicatorRenderer loadingSpinner() {
        return loadingDefault();
    }

    @Override
    public LoadingIndicatorRenderer loadingDots() {
        return fallback.loadingDots();
    }

    @Override
    public LoadingIndicatorRenderer loadingBar() {
        return fallback.loadingBar();
    }
}
~~~

## Appearance Objects

Renderer customization and widget state should not become the same thing.

For LoadingIndicator the existing setters are enough initially:

- segments
- thickness
- speed
- accentColor
- trackColor

For more complex widgets, introduce appearance objects:

~~~java
public final class SliderAppearance {
    private float trackHeight = 4.0f;
    private float knobRadius = 6.0f;
    private boolean showTrack = true;
    private boolean showFill = true;
}
~~~

Pattern:

~~~java
slider.appearance(a -> a
        .trackHeight(3.0f)
        .knobRadius(7.0f));
~~~

Rules:

- Widget behavior state remains on the widget.
- Visual tuning state lives in Appearance.
- Renderer reads snapshot of both.
- Theme can update Appearance defaults.

## Theme Integration

Initial phase:

- Theme continues to provide colors and basic scalar values through StyleKeys.
- LoadingIndicator.applyTheme continues setting accentColor and trackColor.
- Renderer only reads the final state.

Future phase:

- Add renderer-related style keys only if needed.
- Do not store arbitrary lambdas inside Theme unless lifecycle and serialization rules are clear.

Possible future keys:

~~~java
StyleKey<Float> LOADING_THICKNESS = StyleKey.of("loading.thickness", Float.class);
StyleKey<Integer> LOADING_SEGMENTS = StyleKey.of("loading.segments", Integer.class);
StyleKey<LoadingIndicatorRenderer> LOADING_RENDERER = ...
~~~

Recommendation:

- Avoid renderer lambdas in theme for now.
- Prefer WidgetsRenderImpl for global renderer replacement.
- Prefer StyleKeys for data-like visual settings.

## Invalidations

Renderer replacement:

- Visual invalidation only.

State changes:

- speed: visual invalidation.
- phase: visual invalidation.
- segments: visual invalidation.
- thickness: visual invalidation, unless it affects measured size.
- indicatorSize: layout invalidation through preferred size.
- mode: layout + visual only if it still affects default measured size.

Appearance changes:

- Visual invalidation by default.
- Layout invalidation only when appearance affects desired size.

Global WidgetsRender.use(...):

- Open question: should this invalidate existing widgets?

Recommended implementation:

- WidgetsRenderImpl has a version counter.
- Widgets that resolve renderer dynamically can compare version during render/applyTheme and invalidate visual if changed.
- Initial implementation can skip this and rely on next frame render if the UI already redraws frequently.

## Performance Requirements

- Default renderers should be singleton lambdas or singleton objects.
- DrawScope allocation should be reviewed after first implementation.
- LoadingIndicatorState record allocation is acceptable for the first draft.
- If profiling shows GC pressure, use reusable state snapshot owned by widget.
- Renderer must not allocate a VectorPath every frame unless necessary.
- Frequently used colors with modified alpha should eventually use a reusable MutableColor scratch or helper that avoids excessive allocation.

Potential optimization:

~~~java
private final LoadingIndicatorState.Mutable state = new LoadingIndicatorState.Mutable();
private final DrawScope drawScope = new DrawScope();
~~~

This can be added later without changing public API.

## Safety Rules

Renderer code must not:

- store DrawScope outside render call;
- store RenderContext outside render call;
- mutate widget tree;
- assume Minecraft backend;
- call blocking IO;
- trigger layout recalculation;
- directly access backend-specific classes.

Renderer code may:

- emit draw primitives;
- perform lightweight math;
- use state values;
- use read-only color and bounds data;
- choose different drawing based on widget state.

## Migration Plan

### Phase 1: Infrastructure

Add:

- WidgetRenderer<S>
- DrawScope
- WidgetsRender
- WidgetsRenderImpl
- DefaultWidgetsRenderImpl

No widget behavior changes yet.

### Phase 2: LoadingIndicator

Add:

- LoadingIndicatorRenderer
- LoadingIndicatorState
- LoadingIndicatorRenderers
- LoadingIndicator.renderer(...)
- LoadingIndicator.useDefaultRenderer()

Move current hardcoded renderSpinner/renderDots/renderBar logic into LoadingIndicatorRenderers.

Keep Mode for compatibility.

### Phase 3: Documentation and examples

Add examples for:

- default loading indicator;
- preset renderer;
- custom arc renderer;
- global WidgetsRenderImpl override.

### Phase 4: Expand to simple visual widgets

Good next candidates:

- ProgressBar: implemented in first migration batch after LoadingIndicator.
- Slider: implemented in first migration batch after LoadingIndicator.
- ScrollBar: implemented in first migration batch after LoadingIndicator.
- Button: implemented in first migration batch after LoadingIndicator, with text metrics captured in ButtonState.
- ToggleButton: implemented through the shared ButtonRenderer/ButtonState family.
- Checkbox: implemented through the shared ButtonRenderer/ButtonState family with CHECKBOX render type.
- RadioButton: implemented through the shared ButtonRenderer/ButtonState family with RADIO_BUTTON render type.
- TextInput: implemented with a dedicated TextInputRenderer/TextInputState; measurement, cursor visibility, and scroll logic remain owned by TextInput.
- TextField/SearchField/PasswordField/NumberField: implemented as TextInput renderer slots. SearchField has a dedicated renderer for the clear button; PasswordField and NumberField keep their input/display logic in the widgets.
- Shape/Separator/Border: implemented as simple DrawScope primitive renderers.
- Tooltip: implemented with a dedicated TooltipRenderer/TooltipState; wrapping, placement, and line measurement remain owned by Tooltip.
- TextureWidget/ImageView: implemented with TextureWidgetRenderer/TextureWidgetState; placement remains computed from TexturePlacement.fit.
- Path: implemented with PathRenderer/PathState and a copied VectorPath snapshot.
- CachedSubtreeWidget: implemented with CachedSubtreeRenderer/CachedSubtreeState for final cached texture and debug overlay drawing; cache lifecycle and render-target updates remain owned by CachedSubtreeWidget.
- Box: implemented with BoxRenderer/BoxState for shared background, border, and background texture chrome.
- Popup: covered by BoxRenderer because Popup's visual chrome comes from Box.
- WindowWidget: implemented with WindowRenderer/WindowState for header, separator, and title drawing; dragging, close behavior, layout, and children remain owned by WindowWidget.
- ScrollView/OverlayLayer/ExpandablePanel/Accordion/TabControl: currently remain orchestration/layout widgets. Their visible child chrome is already renderer-backed through ScrollBar, ToggleButton, Box, and child widgets.

Each migration should preserve old public API.

### Phase 5: Appearance model

Introduce appearance objects only when repeated renderer settings become too many for direct widget setters.

Do not overbuild this before at least two or three widgets are migrated.

## LoadingIndicator Acceptance Criteria

Implementation is acceptable when:

- Existing LoadingIndicator default visuals still render.
- Mode.SPINNER, Mode.DOTS, and Mode.BAR remain source-compatible.
- A user can call renderer((draw, state) -> ...) and fully replace visual drawing.
- Renderer implementation does not need Minecraft classes.
- Existing theme colors still apply.
- Existing animation phase still updates through tick.
- Existing layout sizing remains compatible.
- Rendering still emits normal DrawCommands through RenderContext.
- No direct RenderBackend access is required for custom indicator visuals.

## Example End State

~~~java
LoadingIndicator indicator = new LoadingIndicator()
        .indicatorSize(32)
        .speed(1.5f)
        .segments(14)
        .thickness(4.0f)
        .renderer((draw, s) -> {
            float radius = s.size() * 0.44f;
            float start = s.phaseRadians();
            float sweep = (float) Math.PI * 1.6f;

            for (int i = 0; i < s.segments(); i++) {
                float t = i / (float) s.segments();
                float a0 = start + t * sweep;
                float a1 = start + (i + 1.0f) / s.segments() * sweep;

                draw.line(
                        s.centerX() + (float) Math.cos(a0) * radius,
                        s.centerY() + (float) Math.sin(a0) * radius,
                        s.centerX() + (float) Math.cos(a1) * radius,
                        s.centerY() + (float) Math.sin(a1) * radius,
                        Paint.stroke(colorWithAlpha(s.accentColor(), 0.2f + t * 0.8f), 1.0f + t * s.thickness()));
            }
        });
~~~

This gives users ImGui-like visual freedom while preserving the retained UniGUI model.

## Open Questions

### Naming

Current proposed names:

- WidgetsRender
- WidgetsRenderImpl
- WidgetRenderer
- DrawScope

Alternative names:

- WidgetSkins / WidgetSkinImpl
- WidgetPainters / WidgetPainterImpl
- WidgetVisuals / WidgetVisualsImpl

Recommendation:

- Use WidgetRenderer for individual strategies.
- Use WidgetsRender for global facade if that name feels natural in the project.
- Consider WidgetSkins if the global object starts representing a complete visual theme.

### Renderer state

Should renderers be strictly stateless?

Recommendation:

- Default renderers should be stateless.
- Custom renderers may hold immutable config.
- Mutable animated state should remain in the widget unless there is a strong reason otherwise.

### Backend-specific rendering

Should DrawScope expose custom(...)?

Recommendation:

- Not in the first public version.
- Keep RenderContext.custom(...) available for advanced internal code.
- Add DrawScope.custom(...) later only with clear docs that it is backend-specific.

### Versioning

Should WidgetsRender.use(...) invalidate all existing widgets?

Recommendation:

- Eventually yes through a version counter or UIContext style version.
- Initially not required for LoadingIndicator migration.
