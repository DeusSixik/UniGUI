package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneMaterialEffect;
import dev.sixik.unigui.api.scene.SceneWireframe;
import dev.sixik.unigui.impl.render.SceneMeshData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Renders scene meshes and vanilla content through a shared OpenGL 3.2 pipeline.
 */
public final class GlSceneRenderer implements Scene3DRenderer, AutoCloseable {
    /**
     * Binding point for the SceneMesh std140 uniform block.
     */
    private static final int UNIFORM_BINDING = 0;
    /**
     * Binding point for the std140 instance array.
     */
    private static final int INSTANCE_BINDING = 1;
    /**
     * Binding point for the advanced surface uniform block.
     */
    private static final int SURFACE_BINDING = 2;
    /**
     * Maximum number of geometry buffers retained between passes.
     */
    private static final int CACHE_SIZE = 512;
    /**
     * Layer ordering and back-to-front ordering for translucent meshes.
     */
    private static final Comparator<Draw> ORDER = Comparator
            .comparingInt((Draw draw) -> draw.material.alphaMode().ordinal())
            .thenComparingInt(draw -> draw.glint == null ? 0 : 1)
            .thenComparingDouble(draw -> draw.depth)
            .thenComparingInt(draw -> draw.order);

    /**
     * Resolves content and the OpenGL target for the current client.
     */
    private final GlScenePlatform platform;
    /**
     * Game-tracked OpenGL operations.
     */
    private final GlScenePlatform.State gl;
    /**
     * Buffers of recently used geometry, keyed by Mesh3D identity.
     */
    private final LinkedHashMap<Mesh3D, GpuMesh> meshes = new LinkedHashMap<>(32, 0.75f, true);
    /**
     * Reusable graph traversal callback.
     */
    private final Consumer<SceneNode> collect = this::collect;
    /**
     * Local lights and persistent shadow maps for this renderer.
     */
    private final GlSceneLighting lighting;
    /**
     * Color-only view shared by fog and sprite passes.
     */
    private final GlSceneOverlay overlay;
    /**
     * Selected volumes and fog OpenGL resources.
     */
    private final GlSceneFog fog;
    /**
     * Native particle paths and billboards.
     */
    private final GlSceneSprites particles;
    /**
     * Canonical camera view-projection saved before rendering light depth.
     */
    private final MutableMat4 cameraClip = new MutableMat4();
    /**
     * Cached surface programs, including failed user shader variants.
     */
    private final LinkedHashMap<SurfaceProgram, Integer> surfaces = new LinkedHashMap<>(16, .75f, true);
    /**
     * Scene and partial tick used by the current light depth callback.
     */
    private Scene renderScene;
    /**
     * Current vanilla render partial tick.
     */
    private float renderPartialTick;
    /**
     * Whether collection and drawing currently produce light depth.
     */
    private boolean shadowPass;
    /**
     * Whether the color pass captures vanilla models for lights and shadows.
     */
    private boolean relightVanilla;
    /**
     * Byte offset of the surface block in the streaming uniform buffer.
     */
    private int surfaceOffset;
    /**
     * Wireframe settings of the current color pass, or null for filled surfaces only.
     */
    private SceneWireframe wire;
    /**
     * Flat-color program of wireframe edges, zero until first use.
     */
    private int wireProgram;
    /**
     * Visible vanilla nodes of the current frame.
     */
    private SceneNode[] models = new SceneNode[16];
    /**
     * Number of visible nodes queued for the vanilla pass.
     */
    private int modelCount;
    /**
     * View frustum of the current camera.
     */
    private final Frustum3D frustum = new Frustum3D();
    /**
     * View matrix used for translucency sorting.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * Projection matrix multiplied by the view matrix.
     */
    private final MutableMat4 viewProjection = new MutableMat4();
    /**
     * World matrix of the current node.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * Matrix mapping local coordinates of the current node to clip space.
     */
    private final MutableMat4 clip = new MutableMat4();
    /**
     * Camera position in world coordinates.
     */
    private final MutableVec3 eye = new MutableVec3();
    /**
     * Disables distant model selection for an orthographic pass.
     */
    private boolean orthographic;
    /**
     * Program IDs indexed by texturing and instancing flags.
     */
    private final int[] programs = new int[4];
    /**
     * Reusable draw commands.
     */
    private Draw[] draws = new Draw[16];
    /**
     * Number of draw commands in the current pass.
     */
    private int drawCount;
    /**
     * Whether layer sorting is required.
     */
    private boolean sort;
    /**
     * GL buffer for material uniforms and instance matrices. Zero until allocated.
     */
    private int uniforms;
    /**
     * Aligned byte offset of the instance array inside the uniform buffer.
     */
    private int instanceOffset;

    /**
     * Creates a scene renderer, allocating GPU resources on first use.
     *
     * @param platform client adapter
     */
    public GlSceneRenderer(GlScenePlatform platform) {
        this.platform = platform;
        gl = platform.state();
        lighting = new GlSceneLighting(platform);
        overlay = new GlSceneOverlay(gl);
        fog = new GlSceneFog(gl);
        particles = platform.particles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderTarget target, Scene scene, Camera3D camera, float viewportWidth, float viewportHeight,
                       ColorView clearColor, float partialTick) {
        render(target, scene, camera, viewportWidth, viewportHeight, clearColor, partialTick, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderTarget target, Scene scene, Camera3D camera, float viewportWidth, float viewportHeight,
                       ColorView clearColor, float partialTick, Highlight highlight) {
        GlScenePlatform.Target output = platform.output(target);
        platform.flush();
        camera.viewProjection(cameraClip);
        camera.view(view);
        camera.position(eye);
        orthographic = camera.orthographic();
        try {
            renderTarget(output, scene, camera, viewportWidth, viewportHeight, clearColor, partialTick, highlight);
            while (meshes.size() > Math.max(drawCount, CACHE_SIZE)) {
                var oldest = meshes.entrySet().iterator();
                oldest.next().getValue().close();
                oldest.remove();
            }
        } finally {
            clearDraws();
            Arrays.fill(models, 0, modelCount, null);
            modelCount = 0;
            renderScene = null;
            platform.endFrame(this::retire);
            fog.reset();
            if (particles != null) particles.reset();
        }
    }

    /**
     * Isolates the GL target and state used by the rest of the UI commands.
     *
     * @param target      destination color and depth target
     * @param scene       scene graph with materials and lights
     * @param camera      scene camera
     * @param width       scene viewport width in pixels
     * @param height      scene viewport height in pixels
     * @param color       background color without premultiplied alpha
     * @param partialTick fractional tick for vanilla animations
     * @param highlight   optional independent highlight
     */
    private void renderTarget(GlScenePlatform.Target target, Scene scene, Camera3D camera,
                              float width, float height, ColorView color, float partialTick, Highlight highlight) {
        var shaderState = GlSceneState.capture(gl);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int uniformBuffer = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            IntBuffer scissor = stack.mallocInt(4);
            ByteBuffer colorMask = stack.malloc(4);
            var bindings = stack.mallocInt(6);
            var offsets = stack.mallocLong(6);
            var sizes = stack.mallocLong(6);
            for (int i = 0; i < bindings.capacity(); i++) {
                bindings.put(i, GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, i));
                offsets.put(i, GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, i));
                sizes.put(i, GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, i));
            }
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
            try {
                renderScene = scene;
                renderPartialTick = partialTick;
                relightVanilla = false;
                wire = scene.wireframe();
                gl.activeTexture(GL13.GL_TEXTURE0);
                lighting.prepare(scene, target, this::drawShadowMap);
                relightVanilla = lighting.hasLights() || scene.directionalLight() != null
                        || !AmbientLight.WHITE.equals(scene.ambientLight());
                frustum.set(viewProjection.set(cameraClip));
                scene.visitVisible(frustum, collect);
                if (highlight != null) addDraw(null, highlight.mesh(), highlight.material(), highlight, null);
                if (sort) Arrays.sort(draws, 0, drawCount, ORDER);
                target.bind();
                gl.disableScissor();
                gl.depthMask(true);
                gl.colorMask(true, true, true, true);
                float alpha = channel(color.a());
                GL30.glClearBufferfv(GL11.GL_COLOR, 0,
                        stack.floats(channel(color.r()) * alpha, channel(color.g()) * alpha, channel(color.b()) * alpha, alpha));
                GL30.glClearBufferfv(GL11.GL_DEPTH, 0, stack.floats(1, 0, 0, 0));
                int pixelsWide = Math.max(1, Math.min((int) Math.ceil(width), target.width()));
                int pixelsHigh = Math.max(1, Math.min((int) Math.ceil(height), target.height()));
                gl.scissor(0, target.height() - pixelsHigh, pixelsWide, pixelsHigh);
                gl.depthTest(true);
                gl.depthFunc(GL11.GL_LEQUAL);
                gl.cull(false);
                gl.activeTexture(GL13.GL_TEXTURE0);
                float scaleX = width / target.width(), scaleY = height / target.height();
                int opaqueEnd = drawCount;
                while (opaqueEnd > 0 && draws[opaqueEnd - 1].material.alphaMode() == Material.AlphaMode.TRANSLUCENT)
                    opaqueEnd--;
                ByteBuffer bytes = stack.malloc(Math.max(SceneMeshData.UNIFORM_BYTES, UiMesh3DShaders.INSTANCE_BYTES));
                if (wire != null && !wire.shaded()) {
                    // Edges only: every edge stays visible, hidden ones included, and no surface is filled.
                    for (int i = 0; i < drawCount; i++) {
                        if (draws[i].highlight != null) draw(scene, target, scaleX, scaleY, bytes, i, i + 1);
                    }
                    drawWire(scene, scaleX, scaleY, bytes, false);
                    return;
                }
                lighting.bind();
                draw(scene, target, scaleX, scaleY, bytes, 0, opaqueEnd);
                if (modelCount != 0) {
                    lighting.unbind();
                    for (int i = 0; i < bindings.capacity(); i++) {
                        restoreUniformBinding(i, bindings.get(i), offsets.get(i), sizes.get(i));
                    }
                    GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniformBuffer);
                    platform.drawNative(models, modelCount, target, camera, view, scaleX, scaleY, partialTick);
                }
                gl.depthTest(true);
                gl.depthFunc(GL11.GL_LEQUAL);
                gl.cull(false);
                gl.activeTexture(GL13.GL_TEXTURE0);
                if (modelCount != 0) lighting.bind();
                draw(scene, target, scaleX, scaleY, bytes, opaqueEnd, drawCount);
                if (particles != null) particles.render(overlay, target, camera, width, height, pixelsWide, pixelsHigh);
                fog.render(overlay, target, viewProjection, camera, scene.materialTime(), lighting,
                        width, height, pixelsWide, pixelsHigh);
                if (wire != null) {
                    target.bind();
                    gl.scissor(0, target.height() - pixelsHigh, pixelsWide, pixelsHigh);
                    drawWire(scene, scaleX, scaleY, bytes, true);
                }
            } finally {
                wire = null;
                platform.invalidate();
                GL30.glBindVertexArray(vao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
                lighting.reset();
                for (int i = 0; i < bindings.capacity(); i++) {
                    restoreUniformBinding(i, bindings.get(i), offsets.get(i), sizes.get(i));
                }
                GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniformBuffer);
                gl.depthFunc(depthFunction);
                gl.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
                shaderState.restore(gl);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
                gl.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                if (scissorEnabled)
                    gl.scissor(scissor.get(0), scissor.get(1), scissor.get(2), scissor.get(3));
                else gl.disableScissor();
            }
        }
    }

    /**
     * Draws caster geometry with reverse depth, leaving the color pass free to use vanilla depth.
     *
     * @param target    square light framebuffer
     * @param lightClip canonical world-to-light clip transform
     * @return whether the depth map contains geometry
     */
    private boolean drawShadowMap(GlScenePlatform.Target target, MutableMat4 lightClip) {
        target.bind();
        gl.disableScissor();
        shadowPass = true;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            frustum.set(viewProjection.set(lightClip));
            renderScene.visitVisible(frustum, collect);
            if (drawCount == 0) return false;
            target.bind();
            gl.depthTest(true);
            gl.depthMask(true);
            gl.depthFunc(GL11.GL_GEQUAL);
            gl.cull(false);
            gl.colorMask(false, false, false, false);
            GL30.glClearBufferfv(GL11.GL_DEPTH, 0, stack.floats(0, 0, 0, 0));
            draw(renderScene, target, 1, 1, stack.malloc(UiMesh3DShaders.INSTANCE_BYTES), 0, drawCount);
            return true;
        } finally {
            shadowPass = false;
            clearDraws();
        }
    }

    /**
     * Clears per-pass commands and releases references for reuse.
     */
    private void clearDraws() {
        for (int i = 0; i < drawCount; i++) draws[i].clear();
        drawCount = 0;
        sort = false;
    }

    /**
     * Restores a uniform buffer binding used by the preceding GL pass.
     *
     * @param index  binding point index
     * @param buffer buffer name, or zero for an empty binding
     * @param offset range start in bytes
     * @param size   range length in bytes, or zero when binding the entire buffer
     */
    private static void restoreUniformBinding(int index, int buffer, long offset, long size) {
        if (size > 0) GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, index, buffer, offset, size);
        else GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, buffer);
    }

    /**
     * Collects a visible node's mesh. For a translucent mesh, depth equals the view-space Z of its AABB center.
     *
     * @param node visible scene node
     */
    private void collect(SceneNode node) {
        if (shadowPass && !node.castsShadow()) return;
        if (!shadowPass && node instanceof FogVolume volume) fog.collect(volume, frustum);
        if (!shadowPass && particles != null) particles.collect(node, view);
        int first = drawCount;
        Aabb bounds = collectGeometry(node, node.materialEffect() != null);
        if (bounds != null) {
            for (int i = first; i < drawCount; i++) draws[i].effectBounds = bounds;
        }
    }

    /**
     * Collects the node's geometry with the same local effect bounds in the camera and light passes.
     *
     * @param node       visible mesh or native node
     * @param needBounds whether to compute shader effect bounds
     * @return complete local bounds for the effect, or null if no bounds are needed
     */
    private Aabb collectGeometry(SceneNode node, boolean needBounds) {
        Mesh3D mesh = node.mesh();
        Aabb bounds = needBounds && mesh != null ? mesh.bounds() : null;
        if (mesh != null) addDraw(node, mesh, node.material());
        if (node instanceof BlockNode block) {
            Object state = block.nativeState(platform.scenePlatform());
            if (state == null) return bounds;
            bounds = addSurfaces(node, platform.block(state, renderScene.minecraftBlockLighting()), bounds, needBounds);
            if (!platform.hasModel(block)) return bounds;
        } else if (node instanceof StructureNode structure && structure.platform() == platform.scenePlatform()) {
            if (needBounds) bounds = union(bounds, structure.bounds());
            StructureNode.DistantMesh distant = shadowPass ? null
                    : structure.distantMesh(orthographic ? null : eye, structure.worldMatrix(world));
            if (distant != null) {
                addDraw(node, distant.mesh(), distant.material());
                return bounds;
            }
            int top = structure.topVisibleY();
            boolean hasBlockEntities = false;
            for (StructureNode.Section section : structure.sections()) {
                if (section.minBlockY() > top) continue;
                addSurfaces(node, platform.section(structure, section, renderScene.minecraftBlockLighting()), null, false);
                hasBlockEntities |= section.blockEntityCount() != 0;
            }
            if (!hasBlockEntities || !platform.hasModel(structure)) return bounds;
        } else if (!(node instanceof ItemNode) && !(node instanceof EntityNode)) {
            return bounds;
        }
        // A wireframe needs the vanilla model as geometry to draw its edges.
        if (shadowPass || relightVanilla || needBounds || wire != null) {
            var surfaces = platform.capture(node, renderPartialTick);
            if (surfaces != null) return addSurfaces(node, surfaces, bounds, needBounds);
        }
        // Content that cannot be captured has no edges; an edges-only wireframe leaves it out.
        if (!shadowPass && (wire == null || wire.shaded())) {
            if (modelCount == models.length) models = Arrays.copyOf(models, models.length * 2);
            models[modelCount++] = node;
        }
        return bounds;
    }

    /**
     * Appends visible layers and optionally accumulates bounds before per-layer frustum or alpha filtering.
     *
     * @param node       owner of the world's transformation
     * @param surfaces   baked and captured local surfaces
     * @param bounds     bounds of the surfaces already collected, or null
     * @param needBounds whether the scene shader reads the local bounds
     * @return combined local bounds
     */
    private Aabb addSurfaces(SceneNode node, List<GlScenePlatform.Surface> surfaces,
                             Aabb bounds, boolean needBounds) {
        node.worldMatrix(world);
        for (int i = 0; i < surfaces.size(); i++) {
            var layer = surfaces.get(i);
            if (needBounds) bounds = union(bounds, layer.mesh().bounds());
            if (frustum.intersects(layer.mesh().bounds(), world)) {
                addDraw(node, layer.mesh(), layer.material(), null, layer);
            }
        }
        return bounds;
    }

    /**
     * Encloses the two local bounds by taking the minimum and maximum of each axis.
     *
     * @param a existing bounds, or null
     * @param b additional bounds, or null
     * @return the combined local bounds, or null when both are absent
     */
    private static Aabb union(Aabb a, Aabb b) {
        if (a == null) return b;
        if (b == null) return a;
        return new Aabb(Math.min(a.minX(), b.minX()), Math.min(a.minY(), b.minY()), Math.min(a.minZ(), b.minZ()),
                Math.max(a.maxX(), b.maxX()), Math.max(a.maxY(), b.maxY()), Math.max(a.maxZ(), b.maxZ()));
    }

    /**
     * Enqueues a visible node's mesh.
     *
     * @param node     owner of the world transform
     * @param mesh     geometry
     * @param material color, lighting and layer settings
     */
    private void addDraw(SceneNode node, Mesh3D mesh, Material material) {
        addDraw(node, mesh, material, null, null);
    }

    /**
     * Enqueues a scene mesh or an independent highlight mesh.
     *
     * @param node      owner of the world transform for a regular mesh
     * @param mesh      indexed geometry
     * @param material  surface settings
     * @param highlight optional highlight with its own world transform
     * @param layer     captured vanilla render state, or null for a custom mesh or highlight
     */
    private void addDraw(SceneNode node, Mesh3D mesh, Material material, Highlight highlight, GlScenePlatform.Surface layer) {
        if (shadowPass && material.alphaMode() == Material.AlphaMode.TRANSLUCENT) {
            if (layer == null || !layer.translucentDepthWrite() || !layer.translucentCutout()) return;
            material = new Material(material.colorArgb(), material.texture(), material.shading(),
                    Material.AlphaMode.CUTOUT, material.alphaCutoff(), material.culling());
        }
        if (drawCount == draws.length) draws = Arrays.copyOf(draws, draws.length * 2);
        Draw draw = draws[drawCount];
        if (draw == null) draws[drawCount] = draw = new Draw();
        draw.node = node;
        draw.mesh = mesh;
        draw.material = material;
        draw.highlight = highlight;
        draw.glint = layer == null ? null : layer.glint();
        draw.translucentCutout = layer != null && layer.translucentCutout();
        draw.translucentDepthWrite = layer != null && layer.translucentDepthWrite();
        draw.effect = node == null ? null : node.materialEffect();
        draw.order = drawCount++;
        var mode = material.alphaMode();
        sort |= mode != Material.AlphaMode.OPAQUE;
        draw.depth = 0;
        if (mode == Material.AlphaMode.TRANSLUCENT) {
            Aabb bounds = mesh.bounds();
            clip.setProduct(view, draw.worldMatrix(world));
            draw.depth = clip.transformComponent(2, ((double) bounds.minX() + bounds.maxX()) * 0.5,
                    ((double) bounds.minY() + bounds.maxY()) * 0.5, ((double) bounds.minZ() + bounds.maxZ()) * 0.5, 1);
        }
    }

    /**
     * Renders layers sharing a depth buffer. The translucent layer reads depth and blends premultiplied RGBA.
     *
     * @param scene  lighting source
     * @param target destination target, which must not be read as a texture simultaneously
     * @param scaleX fraction of target width covered by the scene
     * @param scaleY fraction of target height covered by the scene
     * @param bytes  staging buffer large enough for either std140 uniform block
     * @param start  index of the first draw command
     * @param end    index past the last draw command
     */
    private void draw(Scene scene, GlScenePlatform.Target target, float scaleX, float scaleY, ByteBuffer bytes, int start, int end) {
        if (start == end) return;
        allocateUniforms();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniforms);
        GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, UNIFORM_BINDING, uniforms, 0, SceneMeshData.UNIFORM_BYTES);
        int colorTexture = target.color();
        for (int i = start; i < end; ) {
            Draw draw = draws[i];
            int instanceCount = countInstances(i, end);
            Material material = draw.material;
            var binding = material.texture() == null ? null : platform.texture(material.texture());
            boolean blend = material.alphaMode() == Material.AlphaMode.TRANSLUCENT;
            gl.depthMask(!blend || draw.translucentDepthWrite);
            gl.depthFunc(shadowPass ? GL11.GL_GEQUAL : draw.glint != null ? GL11.GL_EQUAL : GL11.GL_LEQUAL);
            if (draw.glint != null) {
                gl.blend(true);
                gl.blendFuncSeparate(GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            } else if (blend) {
                gl.blend(true);
                gl.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else gl.blend(false);
            if (binding != null && (binding.id() == 0 || binding.id() == colorTexture)) binding = null;
            boolean receiver = receivesDirectional(draw);
            boolean local = usesLocalLights(draw);
            boolean surface = shadowPass || draw.effect != null || receiver || local || draw.glint != null
                    || draw.translucentCutout;
            int program = surface ? surfaceProgram(binding != null, receiver, local, draw.glint != null, draw.translucentCutout, draw.effect) :
                    program(binding != null, instanceCount > 1);
            GL20.glUseProgram(program);
            int samplerDepth = gl.samplerDepth();
            try {
                if (binding != null) {
                    gl.bindTexture(binding.id());
                    gl.applySampler(0, binding.options());
                }
                clip.setProduct(viewProjection, draw.worldMatrix(world));
                bytes.clear();
                SceneMeshData.writeUniform(bytes, clip, world, material, scene.ambientLight(), scene.directionalLight(),
                        scaleX, scaleY, false, binding != null && binding.flipY(), binding != null && binding.premultipliedAlpha());
                GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes.flip());
                if (surface) {
                    writeSurface(bytes.clear(), scene, draw, receiver);
                    GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, SURFACE_BINDING, uniforms,
                            surfaceOffset, UiMesh3DShaders.SURFACE_BYTES);
                    GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, surfaceOffset, bytes.flip());
                }
                GpuMesh mesh = meshes.computeIfAbsent(draw.mesh, GpuMesh::upload);
                GL30.glBindVertexArray(mesh.vao());
                if (instanceCount > 1) {
                    bytes.clear();
                    for (int index = i; index < i + instanceCount; index++) {
                        SceneMeshData.writeClipMatrix(bytes, clip.setProduct(viewProjection, draws[index].worldMatrix(world)));
                    }
                    GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, INSTANCE_BINDING, uniforms, instanceOffset,
                            UiMesh3DShaders.INSTANCE_BYTES);
                    GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, instanceOffset, bytes.flip());
                    GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, mesh.indexCount(), GL11.GL_UNSIGNED_INT, 0L, instanceCount);
                } else {
                    GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indexCount(), GL11.GL_UNSIGNED_INT, 0);
                }
            } finally {
                gl.restoreSamplers(samplerDepth);
            }
            i += instanceCount;
        }
    }

    /**
     * Draws the triangle edges of every collected surface in one flat color per node, with the polygon mode set to
     * lines. Over shaded surfaces the edges are depth-tested and pulled slightly towards the camera with a polygon
     * offset, so only visible edges show and coplanar faces do not hide them. Without surfaces every edge is drawn.
     * Foil layers repeat their base geometry and hover highlights are not scene content, so both are skipped.
     *
     * @param scene  source of the uniform lighting fields, unused by the edge shader
     * @param scaleX share of the target width covered by the viewport
     * @param scaleY share of the target height covered by the viewport
     * @param bytes  staging buffer for the mesh uniform block
     * @param shaded whether surfaces were drawn first
     */
    private void drawWire(Scene scene, float scaleX, float scaleY, ByteBuffer bytes, boolean shaded) {
        if (drawCount == 0) return;
        allocateUniforms();
        if (wireProgram == 0) {
            wireProgram = GlScenePrograms.link(UiMesh3DShaders.vertex(150, false, false),
                    UiMesh3DShaders.wireFragment(150), "Position", "UV0", "Color", "Normal");
            GL31.glUniformBlockBinding(wireProgram, GL31.glGetUniformBlockIndex(wireProgram, "SceneMesh"),
                    UNIFORM_BINDING);
        }
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniforms);
        GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, UNIFORM_BINDING, uniforms, 0, SceneMeshData.UNIFORM_BYTES);
        GL20.glUseProgram(wireProgram);
        gl.depthTest(shaded);
        gl.depthFunc(GL11.GL_LEQUAL);
        gl.depthMask(false);
        gl.cull(false);
        gl.blend(true);
        gl.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
        GL11.glPolygonOffset(-1, -1);
        Material normal = null, highlighted = null;
        try {
            for (int i = 0; i < drawCount; i++) {
                Draw draw = draws[i];
                if (draw.highlight != null || draw.glint != null || draw.node == null) continue;
                int argb = wire.colorFor(draw.node);
                Material material;
                if (argb == wire.colorArgb()) {
                    if (normal == null) normal = wireMaterial(argb);
                    material = normal;
                } else {
                    if (highlighted == null) highlighted = wireMaterial(argb);
                    material = highlighted;
                }
                clip.setProduct(viewProjection, draw.worldMatrix(world));
                bytes.clear();
                SceneMeshData.writeUniform(bytes, clip, world, material, scene.ambientLight(), null,
                        scaleX, scaleY, false, false, false);
                GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes.flip());
                GpuMesh mesh = meshes.computeIfAbsent(draw.mesh, GpuMesh::upload);
                GL30.glBindVertexArray(mesh.vao());
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indexCount(), GL11.GL_UNSIGNED_INT, 0);
            }
        } finally {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
            GL11.glPolygonOffset(0, 0);
            gl.depthMask(true);
            gl.depthTest(true);
        }
    }

    /**
     * Allocates the streaming uniform buffer on first use: the mesh block, the instance array and the surface block,
     * each at an offset aligned to {@code GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT}.
     */
    private void allocateUniforms() {
        if (uniforms != 0) return;
        uniforms = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniforms);
        int alignment = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
        instanceOffset = (SceneMeshData.UNIFORM_BYTES + alignment - 1) / alignment * alignment;
        surfaceOffset = (instanceOffset + UiMesh3DShaders.INSTANCE_BYTES + alignment - 1) / alignment * alignment;
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, surfaceOffset + UiMesh3DShaders.SURFACE_BYTES, GL15.GL_STREAM_DRAW);
    }

    /**
     * @param argb edge color
     * @return unlit opaque material whose tint is the edge color
     */
    private static Material wireMaterial(int argb) {
        return new Material(argb, null, Material.Shading.UNLIT, Material.AlphaMode.OPAQUE, 0, Material.Culling.NONE);
    }

    /**
     * Counts consecutive instances of the same mesh with matching material and depth write state.
     *
     * @param start index of the first instance
     * @param end   index past the last command in the pass
     * @return number of instances for a single GPU draw call
     */
    private int countInstances(int start, int end) {
        Draw first = draws[start];
        if (shadowPass || first.effect != null || receivesDirectional(first) || usesLocalLights(first)
                || first.material.shading() != Material.Shading.UNLIT
                || first.material.alphaMode() == Material.AlphaMode.TRANSLUCENT) return 1;
        int limit = Math.min(end - start, UiMesh3DShaders.MAX_INSTANCES);
        int count = 1;
        while (count < limit) {
            Draw next = draws[start + count];
            if (next.mesh != first.mesh || next.material != first.material || next.effect != null || receivesDirectional(next))
                break;
            count++;
        }
        return count;
    }

    /**
     * @param draw draw candidate
     * @return whether to apply directional shadow depth
     */
    private boolean receivesDirectional(Draw draw) {
        return !shadowPass && draw.glint == null && lighting.directional() != null && draw.node != null && draw.node.receivesShadow();
    }

    /**
     * @param draw draw candidate
     * @return whether the material uses local lighting
     */
    private boolean usesLocalLights(Draw draw) {
        return !shadowPass && lighting.hasLights() && draw.material.shading() == Material.Shading.LIT;
    }

    /**
     * Writes world and light transforms and the selected material's parameters.
     *
     * @param bytes    staging buffer positioned at the start of the surface block
     * @param scene    source of global effect time
     * @param draw     mesh command with its world transform already resolved
     * @param receiver whether to supply directional receiver data
     */
    private void writeSurface(ByteBuffer bytes, Scene scene, Draw draw, boolean receiver) {
        SceneMeshData.writeClipMatrix(bytes, world);
        SceneMeshData.writeClipMatrix(bytes, receiver ? clip.setProduct(lighting.directionalClip(), world) : world);
        float seconds = scene.materialTime();
        var effect = draw.effect;
        if (effect != null) seconds = draw.node.materialEffectTime(seconds);
        bytes.putFloat(receiver ? lighting.directional().bias() : 0)
                .putFloat(receiver ? lighting.directional().strength() : 0).putFloat(seconds).putFloat(draw.glint == null ? 0 : 1);
        if (effect == null) bytes.putLong(0).putLong(0);
        else bytes.putFloat(effect.x()).putFloat(effect.y()).putFloat(effect.z()).putFloat(effect.w());
        var glint = draw.glint;
        if (glint == null) {
            for (int i = 0; i < 4; i++) bytes.putLong(0);
        } else {
            bytes.putFloat(glint.ux()).putFloat(glint.uy()).putFloat(glint.uz()).putFloat(0);
            bytes.putFloat(glint.vx()).putFloat(glint.vy()).putFloat(glint.vz()).putFloat(0);
        }
        SceneMeshData.writeEffectUniform(bytes, draw.effectBounds == null ? draw.mesh.bounds() : draw.effectBounds,
                effect, seconds, draw.node != null && draw.node.receivesShadow());
    }

    /**
     * Selects a linked advanced shader. Invalid custom GLSL falls back to the corresponding built-in variant.
     *
     * @param textured     whether a base texture can be sampled
     * @param receiver     whether to sample directional shadow depth
     * @param local        whether to evaluate local lighting
     * @param glint        whether to draw a native foil overlay
     * @param nativeCutout whether a native blended layer applies the vanilla alpha cutoff
     * @param effect       user effect or null
     * @return OpenGL program name
     */
    private int surfaceProgram(boolean textured, boolean receiver, boolean local, boolean glint, boolean nativeCutout,
                               SceneMaterialEffect effect) {
        var key = new SurfaceProgram(textured, shadowPass, receiver, local, glint, nativeCutout, local ? lighting.masks() : 0,
                local ? lighting.spots() : 0, effect == null ? null : effect.shader().fragmentSource());
        int id = surfaces.computeIfAbsent(key, GlSceneRenderer::linkSurface);
        if (surfaces.size() > 128) {
            var oldest = surfaces.entrySet().iterator();
            int expired = oldest.next().getValue();
            if (expired != 0) GL20.glDeleteProgram(expired);
            oldest.remove();
        }
        if (id == 0 && effect != null) return surfaceProgram(textured, receiver, local, glint, nativeCutout, null);
        if (id == 0) throw new IllegalStateException("Cannot link the UniGUI OpenGL surface shader");
        return id;
    }

    /**
     * Compiles the variant and assigns shared UBO bindings and sampler units.
     *
     * @param key immutable surface shader inputs
     * @return linked name or zero on compilation failure
     */
    private static int linkSurface(SurfaceProgram key) {
        int id;
        try {
            id = GlScenePrograms.link(UiMesh3DShaders.surfaceVertex(150, key.shadow),
                    UiMesh3DShaders.surfaceFragment(150, key.textured, key.shadow, key.receiver, key.nativeCutout,
                            false, key.glint, key.local, key.masks, key.spots, key.source),
                    "Position", "UV0", "Color", "Normal");
        } catch (IllegalStateException failure) {
            if (key.source == null) throw failure;
            System.getLogger(GlSceneRenderer.class.getName()).log(System.Logger.Level.WARNING,
                    "Cannot compile a UniGUI scene material effect", failure);
            return 0;
        }
        GlScenePrograms.block(id, "SceneMesh", UNIFORM_BINDING);
        GlScenePrograms.block(id, "SceneSurface", SURFACE_BINDING);
        if (key.local) GlScenePrograms.block(id, "SceneLocalLights", GlSceneLighting.BINDING);
        GL20.glUseProgram(id);
        GlSceneLighting.linkSamplers(id, key.masks, key.spots);
        return id;
    }

    /**
     * Identifies an advanced GL program.
     *
     * @param textured     whether the material has a texture
     * @param shadow       reverse-depth caster pass
     * @param receiver     directional shadow lookup
     * @param local        local light lookup
     * @param glint        foil overlay
     * @param nativeCutout alpha cutoff on a blended vanilla layer
     * @param masks        number of projected images
     * @param spots        number of spotlight depth textures
     * @param source       optional GLSL source
     */
    private record SurfaceProgram(boolean textured, boolean shadow, boolean receiver,
                                  boolean local, boolean glint, boolean nativeCutout, int masks, int spots,
                                  String source) {
    }

    /**
     * Finds or links the required OpenGL 3.2 shader program.
     *
     * @param textured  whether texture sampling is required
     * @param instanced whether to read instance matrices
     * @return GL program ID
     */
    private int program(boolean textured, boolean instanced) {
        int slot = (textured ? 1 : 0) + (instanced ? 2 : 0);
        if (programs[slot] == 0) {
            int program = GlScenePrograms.link(UiMesh3DShaders.vertex(150, false, instanced),
                    UiMesh3DShaders.fragment(150, textured), "Position", "UV0", "Color", "Normal");
            GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SceneMesh"), UNIFORM_BINDING);
            if (instanced) {
                GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SceneInstances"), INSTANCE_BINDING);
            }
            programs[slot] = program;
        }
        return programs[slot];
    }

    /**
     * Clamps a background color component to the range writable to a texture.
     *
     * @param value color channel value
     * @return value clamped to [0, 1]
     */
    private static float channel(float value) {
        return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }

    /**
     * @param mesh temporary native geometry to release after rendering
     */
    private void retire(Mesh3D mesh) {
        var gpu = meshes.remove(mesh);
        if (gpu != null) gpu.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        for (GpuMesh mesh : meshes.values()) mesh.close();
        meshes.clear();
        lighting.close();
        if (particles != null) particles.close();
        fog.close();
        overlay.close();
        for (int program : surfaces.values()) if (program != 0) GL20.glDeleteProgram(program);
        surfaces.clear();
        for (int program : programs) if (program != 0) GL20.glDeleteProgram(program);
        Arrays.fill(programs, 0);
        if (wireProgram != 0) GL20.glDeleteProgram(wireProgram);
        wireProgram = 0;
        if (uniforms != 0) GL15.glDeleteBuffers(uniforms);
        uniforms = 0;
        platform.close();
    }

    /**
     * Visible node draw command, ordered by material and depth.
     */
    private static final class Draw {
        /**
         * Node providing the world transform.
         */
        SceneNode node;
        /**
         * Indexed geometry of the command.
         */
        Mesh3D mesh;
        /**
         * Material of the current surface.
         */
        Material material;
        /**
         * Highlight transform when the command does not belong to a graph node.
         */
        Highlight highlight;
        /**
         * Optional user-defined scene material effect.
         */
        SceneMaterialEffect effect;
        /**
         * Combined local bounds of all surfaces captured for this effect.
         */
        Aabb effectBounds;
        /**
         * Native foil UV transform, or null for a base material.
         */
        GlScenePlatform.Glint glint;
        /**
         * Whether the original vanilla layer writes depth even with blending enabled.
         */
        boolean translucentDepthWrite;
        /**
         * Whether low-alpha fragments of a blended vanilla layer are discarded.
         */
        boolean translucentCutout;
        /**
         * View-space Z of the mesh center.
         */
        double depth;
        /**
         * Original traversal order for tie-breaking equal depths.
         */
        int order;

        /**
         * Releases references to scene objects after the frame.
         */
        void clear() {
            node = null;
            mesh = null;
            material = null;
            highlight = null;
            effect = null;
            effectBounds = null;
            glint = null;
        }

        /**
         * Copies the world transform into a scratch matrix.
         *
         * @param out destination matrix for the current draw
         * @return world transform of the mesh
         */
        MutableMat4 worldMatrix(MutableMat4 out) {
            return highlight == null ? node.worldMatrix(out) : out.set(highlight.world());
        }
    }

    /**
     * OpenGL resources of a single immutable mesh.
     *
     * @param vao        vertex array object ID
     * @param vertices   vertex buffer object
     * @param indices    32-bit index buffer object
     * @param indexCount number of triangle indices
     */
    private record GpuMesh(int vao, int vertices, int indices, int indexCount) implements AutoCloseable {
        /**
         * Uploads the geometry and configures Position, UV0, Color and Normal attributes.
         *
         * @param mesh immutable geometry
         * @return allocated GPU mesh resources
         */
        static GpuMesh upload(Mesh3D mesh) {
            int stride = SceneMeshData.VERTEX_BYTES;
            int vertexBytes = Math.multiplyExact(mesh.vertexCount(), stride);
            int indexBytes = Math.multiplyExact(mesh.indexCount(), Integer.BYTES);
            ByteBuffer bytes = MemoryUtil.memAlloc(Math.max(vertexBytes, indexBytes));
            GpuMesh gpu = new GpuMesh(GL30.glGenVertexArrays(), GL15.glGenBuffers(), GL15.glGenBuffers(), mesh.indexCount());
            try {
                GL30.glBindVertexArray(gpu.vao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.vertices);
                SceneMeshData.writeVertices(mesh, bytes);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bytes.flip(), GL15.GL_STATIC_DRAW);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
                GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 12);
                GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 20);
                GL20.glVertexAttribPointer(3, 4, GL11.GL_BYTE, true, stride, 24);
                for (int i = 0; i < 4; i++) GL20.glEnableVertexAttribArray(i);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, gpu.indices);
                SceneMeshData.writeIndices(mesh, bytes.clear());
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bytes.flip(), GL15.GL_STATIC_DRAW);
                return gpu;
            } catch (RuntimeException | Error failure) {
                gpu.close();
                throw failure;
            } finally {
                MemoryUtil.memFree(bytes);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            GL30.glDeleteVertexArrays(vao);
            GL15.glDeleteBuffers(vertices);
            GL15.glDeleteBuffers(indices);
        }
    }
}
