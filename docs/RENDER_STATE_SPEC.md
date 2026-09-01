# Render State / Blend Mode SPEC

## Context

UniGUI currently has a small BlendMode API:

- NORMAL for regular UI alpha compositing.
- ADDITIVE for glow, energy lines, highlight rings and Destiny-style map effects.

This is enough for the first map demo, but future AAA-style UI effects need a clearer render-state model so blend, alpha, depth, masks and layers do not become scattered backend-specific flags.

## Goals

- Keep the common widget/render API backend-neutral.
- Make blend modes explicit in draw commands and batching.
- Avoid promising Photoshop-style blend modes that cannot be implemented consistently without shaders.
- Preserve the simple hot-path API: Paint.fill(...), Paint.stroke(...), paint.blend(...).
- Allow future expansion into render layers, alpha modes, depth and masks without breaking existing widgets.

## Non-Goals

- Do not add full material/shader graphs.
- Do not make every draw command carry heavyweight render objects.
- Do not add blend modes that require framebuffer reads unless there is a real backend implementation.
- Do not expose premultiplied-alpha details as a normal widget concern unless needed for custom textures/render targets.

## Current Model

    public enum BlendMode {
        NORMAL,
        ADDITIVE
    }

    Paint.stroke(color, 2.0f)
            .dash(14.0f, 9.0f)
            .blend(BlendMode.ADDITIVE);

BlendMode is part of Paint, copied into each DrawCommand, and included in batch compatibility checks.

## Why BlendMode Exists

BlendMode controls how a new pixel is composited over pixels already in the target.

Examples:

- NORMAL: buttons, panels, text, icons, regular textures.
- ADDITIVE: glow lines, active marker rings, spell/energy effects, hover highlights, emissive overlays.

Without this state, additive rendering would have to be hardcoded into specific widgets or backend calls, which makes reusable map/node-editor style widgets harder to maintain.

## Proposed Blend Modes

### Phase 1: Keep

    public enum BlendMode {
        NORMAL,
        ADDITIVE
    }

This is the stable minimum. It should remain the only required backend support until real use-cases demand more.

### Phase 2: Candidate Additions

    public enum BlendMode {
        NORMAL,
        ADDITIVE,
        MULTIPLY,
        SCREEN
    }

#### MULTIPLY

Use for:

- vignette overlays;
- tactical map darkening;
- disabled/blocked regions;
- modal dim layers that should preserve underlying color detail.

Approximate GL blend:

    glBlendFuncSeparate(GL_DST_COLOR, GL_ZERO, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

Backend behavior must be visually checked because alpha handling can differ between straight and premultiplied targets.

#### SCREEN

Use for:

- softer glow than ADDITIVE;
- UI-friendly light blooms;
- selection halos that should not overburn to white as aggressively.

SCREEN is not always as trivial as ADDITIVE; add only if the Minecraft backend can implement it consistently enough for UniGUI's render targets.

### Deferred / Not Recommended Yet

- SUBTRACT
- OVERLAY
- LIGHTEN
- DARKEN
- COLOR_DODGE
- COLOR_BURN

Reason: these are either niche for UI or require shader/framebuffer behavior to be predictable. They should not be added until there is a concrete feature and backend parity plan.

## Related State Worth Adding

Blend mode should not become the only render-state escape hatch. The long-term model should introduce a lightweight RenderState / CompositeState.

    public final class RenderState {
        private BlendMode blendMode = BlendMode.NORMAL;
        private AlphaMode alphaMode = AlphaMode.AUTO;
        private DepthMode depthMode = DepthMode.OFF;
        private RenderLayer layer = RenderLayer.CONTENT;
        private MaskMode maskMode = MaskMode.NONE;
    }

This object should be immutable or copy-on-write from public APIs so draw commands remain safe snapshots.

## AlphaMode

    public enum AlphaMode {
        AUTO,
        STRAIGHT,
        PREMULTIPLIED
    }

Use for interpreting source texture/render-target alpha.

Recommended ownership:

- TextureHandle / texture metadata should normally define source alpha.
- render targets can define their expected alpha model.
- Paint should not expose alpha mode by default.

Public API should start with AUTO; explicit modes should be advanced/backend-facing.

## DepthMode

    public enum DepthMode {
        OFF,
        TEST,
        TEST_AND_WRITE
    }

Use for:

- 3D preview widgets;
- Minecraft item/entity/block previews;
- world-space overlays;
- UI elements that must participate in depth testing.

Default must be OFF for normal 2D UI.

## RenderLayer

    public enum RenderLayer {
        BACKGROUND,
        CONTENT,
        GLOW,
        OVERLAY,
        DEBUG
    }

Use for semantic ordering and debugging:

- BACKGROUND: page/map panels, distant backdrop textures.
- CONTENT: normal widgets, map texture, node editor content.
- GLOW: additive routes, halos, energy rings.
- OVERLAY: popups, tooltips, selections, modal UI.
- DEBUG: debug overlay, bounds, profiler labels.

Layering should not automatically reorder existing commands until explicitly enabled. The safe initial version is to store the layer on commands/batches and expose it in debug tools.

## MaskMode

    public enum MaskMode {
        NONE,
        WRITE,
        TEST
    }

Use later for:

- circular minimap masks;
- soft clipped map regions;
- stencil-based complex clipping;
- radar / scope overlays.

This should be delayed until clipping/masking has a concrete widget use-case.

## API Shape

### Short Path on Paint

Keep:

    Paint paint = Paint.stroke(color, 2.0f)
            .blend(BlendMode.ADDITIVE);

Add later:

    Paint paint = Paint.stroke(color, 2.0f)
            .state(RenderState.glow());

### RenderState Builder

    RenderState glow = RenderState.builder()
            .blend(BlendMode.ADDITIVE)
            .layer(RenderLayer.GLOW)
            .depth(DepthMode.OFF)
            .build();

### Convenience Presets

    RenderState.normal();
    RenderState.glow();
    RenderState.overlay();
    RenderState.debug();

Presets should be explicit aliases, not hidden backend magic.

## DrawCommand Integration

Every DrawCommand should snapshot effective render state:

    DrawCommand command = DrawCommand.line(bounds, paint);
    RenderState state = command.renderState();

Rules:

- command state must not mutate after submission;
- DrawCommand.copy() must preserve state;
- DrawBatch compatibility must include render state;
- barriers/custom callbacks must preserve enough state for debug visibility.

## Batching Rules

Two commands can merge only if all batch-relevant state matches:

- command type;
- texture / atlas page;
- shader/program;
- clip state;
- transform compatibility;
- BlendMode;
- later: DepthMode, AlphaMode, RenderLayer, MaskMode.

Important: every new state dimension can increase draw calls, so additions need debug visibility.

## Backend Mapping

### NORMAL

Default UI alpha compositing.

Current backend already differentiates straight vs premultiplied target/texture alpha.

### ADDITIVE

Recommended mapping:

    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE,
            GL11.GL_ONE);

Visual target:

- color adds light;
- alpha should not punch holes;
- useful for glow but can overburn if overused.

### MULTIPLY / SCREEN

Do not add until:

- Minecraft backend mapping is proven;
- premultiplied target behavior is tested;
- screenshots/visual tests exist for at least dark overlay + soft glow cases.

## Debug Overlay Requirements

When render state expands, debug overlay should show:

- draw commands by blend mode;
- batches by blend mode/layer;
- additive command count;
- largest batch breaks by reason;
- optional warning when additive commands are interleaved with normal commands too often.

Suggested counters:

    Batches: 42
    Blend: NORMAL 36 / ADDITIVE 6
    Layers: CONTENT 31 / GLOW 6 / OVERLAY 5
    Batch breaks: texture 12, blend 5, clip 3

## Test Plan

Required self-tests:

- Paint.copy() preserves blend/render state.
- DrawCommand.copy() preserves blend/render state.
- SimpleDrawBatcher does not merge different blend modes.
- dashed lines preserve blend mode after expansion into solid line segments.
- default paint remains NORMAL.
- future RenderState presets are immutable snapshots.

Backend smoke tests:

- additive shape batch renders without falling back to normal blending;
- additive texture batch renders without merging into normal texture batch;
- SDF/shape fallback path either honors additive or explicitly opts out.

## Migration Plan

### Step 1: Stabilize Current BlendMode

- Keep BlendMode.NORMAL and BlendMode.ADDITIVE.
- Ensure all command copy/batch paths preserve it.
- Use it in MapCanvas demo for route glow and active markers.

### Step 2: Add Debug Visibility

- Count batches/commands per blend mode.
- Show blend-mode batch breaks in debug overlay.
- Keep API unchanged.

### Step 3: Introduce RenderState Internally

- Add RenderState with only BlendMode.
- Store it on Paint or DrawCommand internally.
- Keep Paint.blend(...) as compatibility sugar.

### Step 4: Add RenderLayer

- Add RenderLayer to RenderState.
- First expose for debug and optional manual grouping.
- Do not globally reorder commands until a screen opts in.

### Step 5: Evaluate MULTIPLY / SCREEN

- Add only after real visual use-case.
- Verify Minecraft backend output.
- Add batching tests and screenshots/manual demo.

## Open Questions

- Should RenderLayer be semantic metadata only, or should it support opt-in reordering?
- Should AlphaMode stay texture/render-target metadata, or become part of RenderState for custom renderers?
- Should debug overlay expose batch-break reasons before or after RenderState lands?
- Should ADDITIVE affect text rendering, or should text glow be implemented as a separate shadow/glow pass?

## Recommended Next Implementation

Do not add new blend modes immediately.

Next best step:

1. Add debug counters for current BlendMode.
2. Add batch-break reason tracking for blend changes.
3. Then introduce internal RenderState with only BlendMode.

This keeps the public API stable while making future expansion clean.

