package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.StructureNode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Compatibility-profile 3D pass shared by the GTNH and Cleanroom UI backends.
 */
public final class LegacyGlSceneRenderer implements Scene3DRenderer, AutoCloseable {
    /**
     * Number of local meshes retained between draws.
     */
    private static final int MESH_CACHE_SIZE = 256;
    /**
     * Places the scene in the top-left part of a potentially larger target.
     * For target-relative viewport extent (sx, sy), clip XY becomes
     * {@code p.xy * (sx, sy) + (sx - 1, 1 - sy) * p.w}, leaving depth and perspective unchanged.
     */
    private static final String VERTEX_SOURCE = """
            #version 120
            uniform vec2 viewport;
            varying vec4 color;
            varying vec2 uv;
            varying vec3 normal;
            void main() {
                vec4 p = gl_ModelViewProjectionMatrix * gl_Vertex;
                p.xy = p.xy * viewport + vec2(viewport.x - 1.0, 1.0 - viewport.y) * p.w;
                gl_Position = p;
                color = gl_Color;
                uv = gl_MultiTexCoord0.xy;
                normal = gl_NormalMatrix * gl_Normal;
            }
            """;
    /**
     * Applies material tint, texture, alpha mode and Lambert lighting, emitting premultiplied RGBA.
     */
    private static final String FRAGMENT_SOURCE = """
            #version 120
            uniform sampler2D surface;
            uniform vec4 tint;
            uniform vec3 ambient;
            uniform vec3 diffuse;
            uniform vec3 direction;
            uniform vec4 textureFlags;
            uniform vec3 material;
            varying vec4 color;
            varying vec2 uv;
            varying vec3 normal;
            void main() {
                vec2 coord = vec2(uv.x, textureFlags.y > 0.5 ? 1.0 - uv.y : uv.y);
                vec4 texel = textureFlags.x > 0.5 ? texture2D(surface, coord) : vec4(1.0);
                if (textureFlags.z > 0.5) texel.rgb = texel.a > 0.0 ? texel.rgb / texel.a : vec3(0.0);
                vec4 rgba = color * tint * texel;
                if (material.x == 1.0 && rgba.a < material.y) discard;
                if (material.x < 2.0) rgba.a = 1.0;
                if (material.z > 0.5) {
                    float lengthSquared = dot(normal, normal);
                    float lambert = lengthSquared > 0.0 ? max(0.0, dot(normal * inversesqrt(lengthSquared), direction)) : 0.0;
                    rgba.rgb *= ambient + diffuse * lambert;
                }
                gl_FragColor = vec4(clamp(rgba.rgb, 0.0, 1.0) * rgba.a, rgba.a);
            }
            """;
    /**
     * Resolves and validates the owned depth target before touching GL state.
     */
    private final ToIntFunction<RenderTarget> framebuffer;
    /**
     * Resolves a material's texture using its owning backend.
     */
    private final Function<TextureHandle, TextureBinding> textures;
    /**
     * Baked geometry of block nodes, shared between frames.
     */
    private final LegacyBlockModels blocks;
    /**
     * Reusable callback for traversing the visible scene.
     */
    private final Consumer<SceneNode> visitor = this::collect;
    /**
     * Visible draw slots reused between frames.
     */
    private final ArrayList<Draw> draws = new ArrayList<>();
    /**
     * Number of populated slots.
     */
    private int count;
    /**
     * Local mesh display lists, ordered by most recent use.
     */
    private final LinkedHashMap<Mesh3D, Integer> geometry = new LinkedHashMap<>(16, 0.75f, true);
    /**
     * View and projection matrices from the current camera.
     */
    private final MutableMat4 view = new MutableMat4(), projection = new MutableMat4();
    /**
     * Scratch matrix for the visible volume and draw transforms.
     */
    private final MutableMat4 matrix = new MutableMat4();
    /**
     * Camera visibility volume in world coordinates.
     */
    private final Frustum3D frustum = new Frustum3D();
    /**
     * Column-major float values used by the OpenGL matrix stack.
     */
    private final float[] elements = new float[16];
    /**
     * Camera position for the distance check of structure proxies.
     */
    private final MutableVec3 eye = new MutableVec3();
    /**
     * World transform of the structure being collected.
     */
    private final MutableMat4 structureWorld = new MutableMat4();
    /**
     * Whether the current camera is orthographic, where structure proxies are not used.
     */
    private boolean orthographic;
    /**
     * Shader program and uniform locations, created on the render thread on first use.
     */
    private Program program;
    /**
     * Vanilla renderer of items and entities, or null when the version does not draw them.
     */
    private final LegacySceneVanilla vanilla;
    /**
     * Visible item and entity slots reused between frames.
     */
    private final ArrayList<VanillaDraw> vanillaDraws = new ArrayList<>();
    /**
     * Number of populated vanilla slots.
     */
    private int vanillaCount;

    /**
     * Creates a renderer without making GL calls.
     *
     * @param framebuffer resolver returning the framebuffer name of a live compatible target.
     * @param textures    resolver returning the texture name and UV orientation.
     * @param blocks      version-specific source of {@link BlockNode} geometry. If it also implements
     *                    {@link LegacySceneVanilla}, item and entity nodes are drawn through it.
     */
    public LegacyGlSceneRenderer(ToIntFunction<RenderTarget> framebuffer, Function<TextureHandle, TextureBinding> textures,
                                 LegacyBlockModels.Baker blocks) {
        this.framebuffer = framebuffer;
        this.textures = textures;
        this.blocks = new LegacyBlockModels(blocks);
        this.vanilla = blocks instanceof LegacySceneVanilla content ? content : null;
    }

    /**
     * Texture information required for a scene material.
     *
     * @param id    GL texture name, or zero for an unavailable texture.
     * @param flipY whether the source texture starts at the bottom row.
     */
    public record TextureBinding(int id, boolean flipY) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderTarget target, Scene scene, Camera3D camera, float width, float height,
                       ColorView clear, float partialTick) {
        render(target, scene, camera, width, height, clear, partialTick, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderTarget target, Scene scene, Camera3D camera, float width, float height,
                       ColorView clear, float partialTick, Highlight highlight) {
        int name = framebuffer.applyAsInt(target);
        camera.view(view);
        camera.projection(projection);
        camera.position(eye);
        orthographic = camera.orthographic();
        frustum.set(matrix.setProduct(projection, view));
        try {
            scene.visitVisible(frustum, visitor);
            if (highlight != null) {
                Draw draw = add(highlight.mesh(), highlight.material());
                draw.world.set(highlight.world());
                draw.measureDepth(view);
            }
            draws.subList(0, count).sort(Draw.ORDER);
            TextureBinding color = count == 0 ? null : textures.apply(target.colorTexture());
            int colorTexture = color == null ? 0 : color.id();
            // Vanilla content is depth-tested against opaque blocks and stays visible through glass in front of it.
            int split = vanillaCount == 0 ? count : firstTranslucent();
            renderPass(name, colorTexture, target, scene, width, height, clear, 0, split);
            if (vanillaCount != 0) renderVanilla(name, target, width, height, partialTick);
            if (split < count) renderPass(name, colorTexture, target, scene, width, height, null, split, count);
        } finally {
            for (int i = 0; i < count; i++) draws.get(i).clear();
            for (int i = 0; i < vanillaCount; i++) {
                vanillaDraws.get(i).content = null;
                vanillaDraws.get(i).node = null;
            }
            count = 0;
            vanillaCount = 0;
        }
    }

    /**
     * Collects the mesh of a visible node with its world transform.
     *
     * @param node visible scene instance.
     */
    private void collect(SceneNode node) {
        if (node.mesh() != null) collect(node, node.mesh(), node.material());
        if (vanilla != null && node instanceof ItemNode item) {
            collectVanilla(node, item.nativeItem(blocks.platform()), false);
        } else if (vanilla != null && node instanceof EntityNode entity) {
            collectVanilla(node, entity.nativeEntity(blocks.platform()), true);
        } else if (node instanceof BlockNode block) {
            LegacyBlockModels.Surface surface = blocks.surface(block.nativeState(blocks.platform()));
            if (surface != null) collect(node, surface.mesh(), surface.material());
        } else if (node instanceof StructureNode structure && structure.platform() == blocks.platform()) {
            StructureNode.DistantMesh distant = structure.distantMesh(orthographic ? null : eye,
                    structure.worldMatrix(structureWorld));
            if (distant != null) {
                collect(node, distant.mesh(), distant.material());
                return;
            }
            int top = structure.topVisibleY();
            for (StructureNode.Section section : structure.sections()) {
                if (section.minBlockY() > top) continue;
                for (LegacyBlockModels.Surface surface : blocks.section(structure, section)) {
                    collect(node, surface.mesh(), surface.material());
                }
            }
        }
    }

    /**
     * Queues an item or entity for the vanilla part of the pass.
     *
     * @param node    owner of the transform.
     * @param content resolved native content, or null when it is not available yet.
     * @param entity  whether the content is an entity.
     */
    private void collectVanilla(SceneNode node, Object content, boolean entity) {
        if (content == null) return;
        if (vanillaCount == vanillaDraws.size()) vanillaDraws.add(new VanillaDraw());
        VanillaDraw draw = vanillaDraws.get(vanillaCount++);
        draw.content = content;
        draw.node = node;
        draw.entity = entity;
        node.worldMatrix(draw.world);
    }

    /**
     * Finds the first blended draw after sorting.
     *
     * @return index of the first translucent draw, or {@link #count} if there is none.
     */
    private int firstTranslucent() {
        for (int i = 0; i < count; i++) {
            if (draws.get(i).material.alphaMode() == Material.AlphaMode.TRANSLUCENT) return i;
        }
        return count;
    }

    /**
     * Adds one mesh of a visible node with the node's world transform.
     *
     * @param node     owner of the transform.
     * @param mesh     node-local geometry.
     * @param material surface parameters.
     */
    private void collect(SceneNode node, Mesh3D mesh, Material material) {
        Draw draw = add(mesh, material);
        node.worldMatrix(draw.world);
        draw.measureDepth(view);
    }

    /**
     * Reserves a draw slot and resolves its texture before changing raw GL state.
     *
     * @param mesh     immutable local geometry.
     * @param material surface parameters.
     * @return populated draw slot with the transform still to be set.
     */
    private Draw add(Mesh3D mesh, Material material) {
        if (count == draws.size()) draws.add(new Draw());
        Draw draw = draws.get(count++);
        draw.mesh = mesh;
        draw.material = material;
        draw.texture = material.texture() == null ? null : textures.apply(material.texture());
        return draw;
    }

    /**
     * Binds the target and isolates raw compatibility GL calls from the game's tracked state.
     *
     * @param name         GL draw framebuffer name.
     * @param colorTexture GL name of the color attachment to exclude from texture sampling.
     * @param target       target dimensions in pixels.
     * @param scene        scene providing the ambient and directional lights.
     * @param width        physical viewport width in pixels.
     * @param height       physical viewport height in pixels.
     * @param clear        straight-alpha background color, or null to keep the target contents.
     * @param from         first sorted draw of this pass.
     * @param to           index after the last draw of this pass.
     */
    private void renderPass(int name, int colorTexture, RenderTarget target, Scene scene,
                            float width, float height, ColorView clear, int from, int to) {
        if (clear == null && from == to) return;
        if (program == null && from != to) program = Program.create();
        int oldFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, name);
            GL11.glViewport(0, 0, target.width(), target.height());
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            if (clear != null) {
                float alpha = clear.a();
                GL11.glClearColor(clear.r() * alpha, clear.g() * alpha, clear.b() * alpha, alpha);
                GL11.glClearDepth(1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            }
            if (from == to) return;

            int pixelWidth = Math.min(target.width(), Math.max(1, (int) Math.ceil(width)));
            int pixelHeight = Math.min(target.height(), Math.max(1, (int) Math.ceil(height)));
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(0, target.height() - pixelHeight, pixelWidth, pixelHeight);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthRange(0, 1);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glFrontFace(GL11.GL_CCW);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL20.glUseProgram(program.id);
            GL20.glUniform1i(program.surface, 0);
            GL20.glUniform2f(program.viewport, width / target.width(), height / target.height());
            setLight(scene);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            load(projection);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            for (int i = from; i < to; i++) renderDraw(draws.get(i), colorTexture);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL20.glUseProgram(oldProgram);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldFramebuffer);
            GL11.glPopAttrib();
        }
    }

    /**
     * Draws the queued items and entities through the game's renderers into the scene target.
     *
     * <p>The projection gets the same viewport placement as the shader:
     * {@code clip.xy = p.xy * (sx, sy) + (sx - 1, 1 - sy) * p.w}, so rows 0 and 1 of the projection become
     * {@code sx * row0 + (sx - 1) * row3} and {@code sy * row1 + (1 - sy) * row3}.</p>
     *
     * @param name        GL draw framebuffer name.
     * @param target      target dimensions in pixels.
     * @param width       physical viewport width in pixels.
     * @param height      physical viewport height in pixels.
     * @param partialTick fraction of the current game tick.
     */
    private void renderVanilla(int name, RenderTarget target, float width, float height, float partialTick) {
        int oldFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT | GL11.GL_SCISSOR_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        boolean begun = false;
        try {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, name);
            GL11.glViewport(0, 0, target.width(), target.height());
            int pixelWidth = Math.min(target.width(), Math.max(1, (int) Math.ceil(width)));
            int pixelHeight = Math.min(target.height(), Math.max(1, (int) Math.ceil(height)));
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(0, target.height() - pixelHeight, pixelWidth, pixelHeight);
            float sx = width / target.width(), sy = height / target.height();
            projection.copyColumnMajor(elements, 0);
            for (int column = 0; column < 4; column++) {
                int base = column * 4;
                float w = elements[base + 3];
                elements[base] = elements[base] * sx + (sx - 1) * w;
                elements[base + 1] = elements[base + 1] * sy + (1 - sy) * w;
            }
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadMatrixf(elements);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            load(view);
            begun = true;
            vanilla.beginVanilla();
            for (int i = 0; i < vanillaCount; i++) {
                VanillaDraw draw = vanillaDraws.get(i);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                load(matrix.setProduct(view, draw.world));
                if (draw.entity) vanilla.drawEntity((EntityNode) draw.node, draw.content, partialTick);
                else vanilla.drawItem((ItemNode) draw.node, draw.content, partialTick);
            }
        } finally {
            if (begun) vanilla.endVanilla();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GL11.glPopMatrix();
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldFramebuffer);
            GL11.glPopAttrib();
        }
    }

    /**
     * Uploads scene lights. The view matrix maps the direction vector with homogeneous w = 0.
     *
     * @param scene light colors, intensities and world direction.
     */
    private void setLight(Scene scene) {
        var ambient = scene.ambientLight();
        rgb(program.ambient, ambient.colorRgb(), ambient.intensity());
        var light = scene.directionalLight();
        if (light == null) {
            GL20.glUniform3f(program.diffuse, 0, 0, 0);
            GL20.glUniform3f(program.direction, 0, 0, 0);
        } else {
            rgb(program.diffuse, light.colorRgb(), light.intensity());
            GL20.glUniform3f(program.direction,
                    (float) -view.transformComponent(0, light.x(), light.y(), light.z(), 0),
                    (float) -view.transformComponent(1, light.x(), light.y(), light.z(), 0),
                    (float) -view.transformComponent(2, light.x(), light.y(), light.z(), 0));
        }
    }

    /**
     * Draws one instance with the material's alpha and face culling modes.
     *
     * @param draw         resolved geometry, material and local-to-world matrix.
     * @param colorTexture attachment that the draw is writing to.
     */
    private void renderDraw(Draw draw, int colorTexture) {
        Material material = draw.material;
        boolean transparent = material.alphaMode() == Material.AlphaMode.TRANSLUCENT;
        GL11.glDepthMask(!transparent);
        if (transparent) GL11.glEnable(GL11.GL_BLEND);
        else GL11.glDisable(GL11.GL_BLEND);
        if (material.culling() == Material.Culling.NONE) GL11.glDisable(GL11.GL_CULL_FACE);
        else {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(material.culling() == Material.Culling.BACK ? GL11.GL_BACK : GL11.GL_FRONT);
        }
        int color = material.colorArgb();
        GL20.glUniform4f(program.tint, red(color), green(color), blue(color), (color >>> 24) / 255f);
        int texture = draw.texture == null ? 0 : draw.texture.id();
        if (texture == colorTexture) texture = 0;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL20.glUniform4f(program.textureFlags, texture == 0 ? 0 : 1,
                texture != 0 && draw.texture.flipY() ? 1 : 0,
                texture != 0 && material.texture().options().premultipliedAlpha() ? 1 : 0, 0);
        GL20.glUniform3f(program.material, material.alphaMode().ordinal(), material.alphaCutoff(),
                material.shading() == Material.Shading.LIT ? 1 : 0);
        load(matrix.setProduct(view, draw.world));
        GL11.glCallList(list(draw.mesh));
    }

    /**
     * Loads a column-major OpenGL matrix into the currently selected stack.
     *
     * @param source matrix with column vectors.
     */
    private void load(MutableMat4 source) {
        source.copyColumnMajor(elements, 0);
        GL11.glLoadMatrixf(elements);
    }

    /**
     * Caches local triangles. Instanced transforms remain per draw and are not recorded in the list.
     *
     * @param mesh immutable indexed geometry.
     * @return GL display list name.
     */
    private int list(Mesh3D mesh) {
        Integer cached = geometry.get(mesh);
        if (cached != null) return cached;
        int list = GL11.glGenLists(1);
        if (list == 0) throw new IllegalStateException("Cannot allocate a UniGUI 3D mesh display list");
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < mesh.indexCount(); i++) {
            int v = mesh.index(i);
            int color = mesh.colorArgb(v);
            GL11.glColor4f(red(color), green(color), blue(color), (color >>> 24) / 255f);
            GL11.glNormal3f(mesh.normal(v, 0), mesh.normal(v, 1), mesh.normal(v, 2));
            GL11.glTexCoord2f(mesh.uv(v, 0), mesh.uv(v, 1));
            GL11.glVertex3f(mesh.position(v, 0), mesh.position(v, 1), mesh.position(v, 2));
        }
        GL11.glEnd();
        GL11.glEndList();
        geometry.put(mesh, list);
        if (geometry.size() > MESH_CACHE_SIZE) {
            var entries = geometry.values().iterator();
            GL11.glDeleteLists(entries.next(), 1);
            entries.remove();
        }
        return list;
    }

    /**
     * Uploads RGB multiplied by the intensity of the source.
     *
     * @param location  shader uniform location.
     * @param color     packed RGB.
     * @param intensity nonnegative linear scale.
     */
    private static void rgb(int location, int color, float intensity) {
        GL20.glUniform3f(location, red(color) * intensity, green(color) * intensity, blue(color) * intensity);
    }

    /**
     * @param color packed RGB or ARGB. @return normalized red channel.
     */
    private static float red(int color) {
        return (color >>> 16 & 255) / 255f;
    }

    /**
     * @param color packed RGB or ARGB. @return normalized green channel.
     */
    private static float green(int color) {
        return (color >>> 8 & 255) / 255f;
    }

    /**
     * @param color packed RGB or ARGB. @return normalized blue channel.
     */
    private static float blue(int color) {
        return (color & 255) / 255f;
    }

    /**
     * Releases cached GPU objects on the render thread.
     */
    @Override
    public void close() {
        for (int list : geometry.values()) GL11.glDeleteLists(list, 1);
        geometry.clear();
        if (program != null) {
            GL20.glDeleteProgram(program.id);
            program = null;
        }
    }

    /**
     * One resolved mesh instance, reused after a pass.
     */
    private static final class Draw {
        /**
         * Draws depth writers first, then blended geometry from far to near.
         */
        private static final Comparator<Draw> ORDER = Comparator
                .comparing((Draw draw) -> draw.material.alphaMode() == Material.AlphaMode.TRANSLUCENT)
                .thenComparingDouble(draw -> draw.depth);
        /**
         * Local surface geometry.
         */
        private Mesh3D mesh;
        /**
         * Surface settings for the draw.
         */
        private Material material;
        /**
         * Resolved texture or null for an untextured draw.
         */
        private TextureBinding texture;
        /**
         * Local-to-world transform.
         */
        private final MutableMat4 world = new MutableMat4();
        /**
         * View-space Z of the scene instance's bounding center, or zero for depth writers.
         */
        private double depth;

        /**
         * Finds the camera-space Z of the AABB midpoint after the local-to-world transform.
         *
         * @param view camera transform, looking along negative Z.
         */
        private void measureDepth(MutableMat4 view) {
            depth = 0;
            if (material.alphaMode() != Material.AlphaMode.TRANSLUCENT) return;
            Aabb bounds = mesh.bounds();
            double x = ((double) bounds.minX() + bounds.maxX()) / 2;
            double y = ((double) bounds.minY() + bounds.maxY()) / 2;
            double z = ((double) bounds.minZ() + bounds.maxZ()) / 2;
            depth = view.transformComponent(2, world.transformComponent(0, x, y, z, 1),
                    world.transformComponent(1, x, y, z, 1), world.transformComponent(2, x, y, z, 1), 1);
        }

        /**
         * Drops scene references after a pass.
         */
        private void clear() {
            mesh = null;
            material = null;
            texture = null;
        }
    }

    /**
     * Item or entity drawn by the game's renderer, reused after a pass.
     */
    private static final class VanillaDraw {
        /**
         * Local-to-world transform of the node.
         */
        private final MutableMat4 world = new MutableMat4();
        /**
         * Native item or entity handle.
         */
        private Object content;
        /**
         * Item or entity node of the content.
         */
        private SceneNode node;
        /**
         * Whether {@link #content} is an entity.
         */
        private boolean entity;
    }

    /**
     * Linked shader and the locations of its uniforms.
     */
    private record Program(int id, int viewport, int surface, int tint, int ambient, int diffuse,
                           int direction, int textureFlags, int material) {
        /**
         * @return linked GLSL program with cached uniform locations.
         */
        private static Program create() {
            int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX_SOURCE);
            int fragment = 0;
            int id = GL20.glCreateProgram();
            try {
                fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
                GL20.glAttachShader(id, vertex);
                GL20.glAttachShader(id, fragment);
                GL20.glLinkProgram(id);
                if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                    throw new IllegalStateException("Cannot link UniGUI 3D shader: " + GL20.glGetProgramInfoLog(id));
                }
                return new Program(id, location(id, "viewport"), location(id, "surface"), location(id, "tint"),
                        location(id, "ambient"), location(id, "diffuse"), location(id, "direction"),
                        location(id, "textureFlags"), location(id, "material"));
            } catch (RuntimeException | Error failure) {
                GL20.glDeleteProgram(id);
                throw failure;
            } finally {
                GL20.glDeleteShader(vertex);
                if (fragment != 0) GL20.glDeleteShader(fragment);
            }
        }

        /**
         * @param id linked program. @param name uniform name. @return numeric location.
         */
        private static int location(int id, String name) {
            return GL20.glGetUniformLocation(id, name);
        }

        /**
         * Compiles a shader stage and removes its GL object if compilation fails.
         *
         * @param type   OpenGL vertex or fragment shader type.
         * @param source GLSL source.
         * @return compiled shader name.
         * @throws IllegalStateException if the driver rejects the source.
         */
        private static int compile(int type, String source) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE) return shader;
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Cannot compile UniGUI 3D shader: " + log);
        }
    }
}
