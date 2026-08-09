# UniGUI AZDO Renderer SPEC

## Status

Draft for implementation planning.

## Context

UniGUI already has a GPU-backed renderer: widgets emit backend-neutral DrawCommands through RenderContext / DrawScope, commands are batched by SimpleDrawBatcher, and the Minecraft backend renders supported batches through GL-backed BufferBuilder, shaders, and texture/color pipelines.

The current design is good enough for normal UI, but it is not an AZDO-style renderer yet. The renderer still builds transient buffers per frame, relies on BufferBuilder / Tesselator, performs CPU-side tessellation during command rendering, and treats several states such as scissor, custom callbacks, and texture switches as hard barriers.

This SPEC defines a staged migration toward an AZDO-inspired rendering core while preserving the public draw API and Minecraft compatibility.

## Goals

- Keep RenderContext and DrawScope as the stable public rendering API.
- Compile high-level draw commands into a compact GPU command stream.
- Reduce per-frame CPU overhead, allocations, GL state churn, and draw calls.
- Support the ImGui-style draw list API: primitives, paths, polylines, polygon fills, images, meshes, channels, callbacks, and explicit draw command barriers.
- Preserve fallback rendering for unsupported GL capabilities or unstable driver paths.
- Make the renderer scalable enough for AAA-style Minecraft UI: many widgets, animated transforms, SDF text, virtualized tables/lists, gradients, vector-heavy controls, render targets, and future effects.

## Non-Goals

- Do not expose OpenGL objects through RenderContext / DrawScope.
- Do not require bindless textures as a baseline.
- Do not remove the current Minecraft renderer immediately.
- Do not make the renderer dependent on one mod loader.
- Do not require multi-draw indirect for the first version.
- Do not rewrite the widget system around GPU concepts.

## Design Principles

- Public API remains backend-neutral.
- Backends may choose their own renderer implementation based on capability checks.
- Command compilation should be deterministic and debuggable.
- CPU tessellation is allowed in phase 1, but must move out of the immediate render path.
- Runtime renderer should mostly consume already compiled packets.
- Barriers must be explicit and minimal.
- Fallback must be easy to enable for debugging.
- Visual output must match the legacy renderer within acceptable pixel tolerance.

## Target Architecture

Pipeline:

- Widget render code emits draw commands through RenderContext / DrawScope.
- DrawList stores commands and channel/path state.
- DrawCommandNormalizer merges channels, cleans invalid commands, and preserves barriers.
- DrawCommandCompiler converts normalized commands into GPU-ready frame packets.
- GpuBatchPlanner groups compatible packets by pipeline state.
- GpuBufferRing writes vertices, indices, and optional draw metadata.
- MinecraftAzdoRenderBackend submits planned batches.

## Public API Layer

The following classes remain the primary public surface:

- RenderContext
- DrawScope
- DrawCommand
- DrawList
- DrawMesh
- DrawPoint
- DrawVertex
- TextureHandle
- VectorPath
- Paint

The AZDO renderer must consume these classes without requiring widget code changes.

## Normalization Layer

Add a normalization pass before GPU compilation.

Candidate class:

- dev.sixik.unigui.impl.render.DrawCommandNormalizer

Responsibilities:

- Merge draw channels into final order if still active.
- Expand simple high-level commands into normalized command forms.
- Convert null/invalid values into no-op commands.
- Clamp negative sizes/radii where needed.
- Preserve explicit barriers:
  - push/pop clip;
  - custom callback;
  - draw cmd marker;
  - render target boundaries;
  - unsupported texture source;
  - unsupported blend/effect state.

## Compilation Layer

Add a compiler from DrawCommand to GPU-ready vertices, indices, and batch metadata.

Candidate classes:

- dev.sixik.unigui.impl.render.gpu.GpuDrawCompiler
- dev.sixik.unigui.impl.render.gpu.GpuFramePacket
- dev.sixik.unigui.impl.render.gpu.GpuBatch
- dev.sixik.unigui.impl.render.gpu.GpuVertex
- dev.sixik.unigui.impl.render.gpu.GpuIndexRange

Responsibilities:

- Tessellate shape/path commands into triangle/index data.
- Copy DrawMesh directly into the frame packet.
- Generate textured vertices for texture/image commands.
- Apply command transforms either on CPU during compilation or in shader through per-draw transform data in later phases.
- Calculate clip rectangles per draw.
- Emit stable PipelineKeys for batch planning.

## Pipeline Keys

Every GPU batch should be grouped by a compact pipeline key.

Suggested fields:

- shader kind;
- texture key;
- blend mode;
- clip mode;
- sampler mode.

Initial shader kinds:

- COLOR
- TEXTURE
- SDF_TEXT
- MSDF_TEXT

Future shader kinds:

- BLUR
- SHADOW
- MASK
- GRADIENT

## Vertex Format

Phase 1 unified vertex:

- vec2 position
- vec2 uv
- u8vec4 color
- uint flags

Optional later fields:

- uint drawId
- uint textureSlot
- vec2 extra0
- vec2 extra1

Initial Java-side representation should be equivalent to:

- float x
- float y
- float u
- float v
- int rgba
- int flags

The renderer may pack this into a native byte buffer for upload.

## Buffer Strategy

Phase 1:

- Use one dynamic vertex buffer and one dynamic index buffer per frame.
- Prefer direct byte buffers and fewer large uploads.
- Avoid per-command BufferBuilder.

Phase 2:

- Introduce ring buffers:
  - vertex ring;
  - index ring;
  - optional draw-data ring.
- Use orphaning or unsynchronized mapping where persistent mapping is unavailable.

Phase 3:

- Use persistent mapped buffers when supported:
  - GL_ARB_buffer_storage;
  - GL_MAP_PERSISTENT_BIT;
  - GL_MAP_COHERENT_BIT when safe;
  - fence-based region reuse.

## Draw Submission

Phase 1:

- Submit one glDrawElements call per planned batch.
- Bind shader and texture only when PipelineKey changes.

Phase 2:

- Merge compatible batches more aggressively.
- Use texture arrays or atlas slots for common UI textures where possible.

Phase 3:

- If available, use glMultiDrawElements or glMultiDrawElementsIndirect.
- Treat multi-draw indirect as an optimization path, not a baseline requirement.

## Capability Detection

Add a renderer capability object.

Candidate class:

- dev.sixik.unigui.backend.minecraft.MinecraftGpuCapabilities

Fields:

- OpenGL version;
- GL_ARB_buffer_storage;
- GL_ARB_multi_draw_indirect;
- GL_ARB_timer_query;
- maximum texture units;
- maximum texture size;
- supports persistent mapping;
- supports indirect draw;
- supports debug labels.

Renderer selection:

- If AZDO is enabled and capabilities are safe, use MinecraftAzdoRenderBackend.
- Otherwise use the legacy MinecraftGuiRenderBackend path.

Debug options:

- Force legacy renderer.
- Force AZDO renderer.
- Disable persistent mapping.
- Disable multidraw.
- Log batch plan.
- Show render stats overlay.

## Clip / Scissor Model

The current scissor stack is GL-state based and creates barriers.

Phase 1:

- Keep scissor as a barrier.
- Compile clip push/pop into batch boundaries.

Phase 2:

- Store clip rectangle per batch.
- Apply scissor only when clip changes.

Phase 3:

- Store clip rectangle per draw/vertex and clip in shader where useful.
- Use shader clipping for high-frequency nested UI clips.
- Keep hardware scissor for coarse top-level clipping.

## Text Rendering

Text should remain compatible with the existing SDF / mixed renderer.

Phase 1:

- Keep existing SDF and mixed text renderer.
- Treat text batches as separate pipeline segments.

Phase 2:

- Compile glyph runs into GpuFramePacket.
- Reuse atlas texture pages.
- Batch glyphs by font page and shader kind.

Phase 3:

- Unify SDF text with the AZDO batch planner.
- Add per-glyph effects:
  - outline;
  - shadow;
  - gradient;
  - underline/strike;
  - animated color.

## Texture Handling

Baseline:

- Keep TextureHandle as the public texture abstraction.
- Resolve native Minecraft resource locations / GL texture ids during compilation or batch planning.

Phase 1:

- Batch by same texture id/resource.

Phase 2:

- Add texture slot allocator for common UI textures.
- Allow several textures per draw submission when hardware texture unit count is safe.

Phase 3:

- Optional atlas/array strategy for framework-owned textures.
- Optional bindless path only if it can be safely isolated and disabled.

## Render Targets

Render targets must remain compatible with:

- cached subtree rendering;
- blur/shadow effects;
- preview widgets;
- future post-processing.

Phase 1:

- Treat render target begin/end as hard barriers.
- Flush active GPU frame packet before switching framebuffer.

Phase 2:

- Add render-pass packets containing target plus planned GPU batches.

Phase 3:

- Build a UI frame graph:
  - main pass;
  - offscreen cache pass;
  - effect pass;
  - composite pass.

## Error Handling and Fallback

AZDO backend must fail soft.

If compilation or GPU submission fails:

- Log error once per failure type.
- Disable AZDO for the current frame or session.
- Render with legacy MinecraftGuiRenderBackend.
- Keep UI functional.

Required fallback triggers:

- unsupported GL capability;
- persistent mapping failure;
- shader compile failure;
- buffer allocation failure;
- unsupported texture handle;
- custom draw callback in the middle of a packet;
- invalid render target state.

## Debugging and Metrics

Expose render metrics through existing debug counters or a new stats object.

Candidate class:

- dev.sixik.unigui.api.debug.RenderStats

Metrics:

- draw commands;
- normalized commands;
- vertices;
- indices;
- GPU batches;
- GL draw calls;
- texture binds;
- shader binds;
- clip changes;
- buffer uploads;
- ring buffer stalls;
- fallback count;
- CPU compile time;
- GPU frame time where timer query is available.

Debug overlay should show:

- renderer: AZDO / Legacy;
- commands;
- vertices;
- indices;
- batches;
- draw calls;
- texture binds;
- CPU compile time;
- GPU UI time;
- fallback count.

## Implementation Plan

### Phase 0 — Baseline Cleanup

- Keep current renderer working.
- Add tests for the new ImGui-style command surface.
- Add a small debug screen/widget that exercises:
  - lines;
  - rects;
  - rounded rects;
  - gradients;
  - triangles;
  - quads;
  - circles;
  - ellipses;
  - convex/concave polygons;
  - image quad;
  - image rounded;
  - path stroke/fill;
  - channels.

Definition of done:

- gradlew build passes.
- Visual test screen renders with legacy backend.
- Existing widgets are unchanged.

### Phase 1 — GPU Frame Packet Compiler

- Add GpuDrawCompiler.
- Add GpuFramePacket.
- Compile color primitives and DrawMesh.
- Compile texture/image commands.
- Keep text as legacy fallback segment.
- Submit with one dynamic VBO/IBO upload per frame.
- Submit with regular glDrawElements per batch.

Definition of done:

- AZDO backend renders basic widgets and primitive test screen.
- Legacy renderer remains selectable.
- Batch count and draw call count are visible in debug overlay.
- No public API changes required for widgets.

### Phase 2 — Ring Buffers and State Reduction

- Add GpuBufferRing.
- Avoid per-frame buffer object creation.
- Avoid per-command BufferBuilder.
- Group batches by PipelineKey.
- Reduce texture/shader binds.
- Move clip handling to batch-level planning.

Definition of done:

- Large virtual table/list benchmark produces fewer allocations and lower CPU render time than legacy.
- Renderer survives window resize and render target switches.
- No visible regressions in standard widgets.

### Phase 3 — Persistent Mapping

- Add capability-gated persistent mapped buffers.
- Add fence sync for ring buffer region reuse.
- Add fallback to phase 2 upload path.
- Add debug metric for buffer stalls.

Definition of done:

- Persistent path runs when GL_ARB_buffer_storage is supported.
- Disabling persistent mapping uses upload path without visual changes.
- No driver crash or corruption on repeated UI stress tests.

### Phase 4 — Text Integration

- Compile SDF glyph runs into GpuFramePacket.
- Batch by font texture page and shader kind.
- Preserve mixed vanilla/SDF behavior where needed.
- Add glyph-level clipping and transform support.

Definition of done:

- Text-heavy screens batch efficiently.
- Existing text widgets match previous output.
- SDF text still supports transform, scale, opacity, and clipping.

### Phase 5 — Multi-Draw / Indirect Optional Path

- Add optional indirect command buffer.
- Use glMultiDrawElementsIndirect when available and stable.
- Keep regular draw path as default fallback.

Definition of done:

- Indirect path is capability-gated.
- Renderer can toggle indirect on/off at runtime or startup.
- Debug stats show draw call reduction.

## Proposed Package Layout

Common GPU compiler package:

- common/src/main/java/dev/sixik/unigui/impl/render/gpu/GpuDrawCompiler.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/GpuFramePacket.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/GpuBatch.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/GpuVertex.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/GpuIndexBuffer.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/PipelineKey.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/ShaderKind.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/BlendMode.java
- common/src/main/java/dev/sixik/unigui/impl/render/gpu/ClipState.java

Minecraft AZDO backend package:

- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftAzdoRenderBackend.java
- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftGpuCapabilities.java
- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftGpuBufferRing.java
- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftGpuProgram.java
- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftGpuPipelineCache.java
- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftTextureResolver.java
- common/src/main/java/dev/sixik/unigui/backend/minecraft/azdo/MinecraftAzdoRenderStats.java

## Compatibility Requirements

- Must compile on Java 17.
- Must support Minecraft 1.20.1 target environment.
- Must not require client mods outside existing project dependencies.
- Must support both Fabric and Forge outputs.
- Must preserve existing public widget renderers.
- Must preserve current RenderBackend contract.

## Testing Strategy

Unit tests:

- command normalization;
- path tessellation;
- concave polygon triangulation;
- batch key grouping;
- ring buffer allocation math;
- clip stack conversion;
- texture key resolution.

Integration tests:

- render simple widget tree;
- render nested clips;
- render transformed widgets;
- render mixed texture/color shapes;
- render cached subtree;
- render render target preview;
- render large virtual table/list.

Visual tests:

- primitive zoo;
- path zoo;
- text zoo;
- texture zoo;
- clipping zoo;
- transform zoo;
- stress table;
- animated opacity/scale/rotation.

Performance benchmarks:

- CPU frame render time;
- GPU UI time where timer query exists;
- Java allocation rate;
- draw calls;
- texture binds;
- vertices/indices;
- frame packet compile time.

Target improvements for phase 2+:

- fewer allocations than legacy renderer;
- fewer GL calls than legacy renderer on complex screens;
- lower CPU time on large tables/lists;
- stable frame time under animated UI stress.

## Risks

- Minecraft render state is shared and fragile.
- Some drivers may behave badly with persistent mapping.
- Shader clipping may differ subtly from hardware scissor.
- Text integration can become complex due to mixed vanilla/SDF behavior.
- Render target switching can break batching if not modeled as explicit passes.
- Debugging GPU buffer corruption is harder than legacy BufferBuilder rendering.

## Mitigations

- Keep legacy renderer as fallback.
- Make AZDO capability-gated and toggleable.
- Add render stats early.
- Add visual test screen before deep optimization.
- Introduce persistent mapping only after upload-based AZDO path is stable.
- Keep barriers explicit in the command stream.
- Log renderer selection and fallback reasons.

## Open Questions

- Should AZDO be enabled by default once phase 2 is complete, or remain opt-in until phase 3?
- Should path tessellation live in common code or Minecraft backend code?
- Should transforms be applied on CPU in phase 1, or should we immediately introduce per-draw transform data?
- Should framework-owned textures be atlased automatically?
- Should shader clipping replace nested scissor in phase 2 or later?
- Should render stats become part of public debug API or remain backend-internal?

## Initial Acceptance Criteria

The first useful AZDO milestone is complete when:

- MinecraftAzdoRenderBackend can render:
  - rects;
  - rounded rects;
  - lines;
  - circles/ellipses;
  - paths;
  - meshes;
  - texture quads;
  - gradients;
  - clips;
  - render target boundaries through barriers.
- Existing widgets require no render-code changes.
- gradlew build passes for common, Fabric, and Forge.
- Debug overlay reports AZDO batches, vertices, indices, draw calls, and fallback count.
- A runtime flag can switch between legacy and AZDO backend.
- Visual output is equivalent to legacy renderer for the primitive test screen.

