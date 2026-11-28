package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.impl.render.SceneMeshData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Collects and renders native sprite paths from a version-specific particle adapter.
 */
public abstract class GlSceneSprites implements AutoCloseable {
    /**
     * Camera and per-batch uniform binding points.
     */
    private static final int CAMERA_BINDING = 4, SPRITES_BINDING = 5;
    /**
     * Back-to-front order for blended billboards in camera units.
     */
    private static final Comparator<Sprite> ORDER = Comparator.comparingDouble(sprite -> sprite.values[2]);

    /**
     * Combined emitter-local to view transform.
     */
    private final MutableMat4 modelView = new MutableMat4();
    /**
     * Canonical camera projection.
     */
    private final MutableMat4 projection = new MutableMat4();
    /**
     * Working local-to-view particle center in blocks.
     */
    private final MutableVec3 center = new MutableVec3();
    /**
     * Reused billboard records, active prefix sorted during rendering.
     */
    private Sprite[] sprites = new Sprite[32];
    /**
     * Native-order camera and batch staging buffer.
     */
    private final ByteBuffer bytes = ByteBuffer.allocateDirect(SceneSoftParticleShaders.BATCH_BYTES).order(ByteOrder.nativeOrder());
    /**
     * Number of collected sprites for the current frame.
     */
    private int count;
    /**
     * OpenGL object names, zero before initialization.
     */
    private int program, vao, quad, cameraUniforms, spriteUniforms;


    /**
     * Client's tracked GL operations.
     */
    private final GlScenePlatform.State gl;
    /**
     * Whether this backend also draws procedural cloud billboards.
     */
    private final boolean proceduralClouds;

    /**
     * @param gl native state adapter
     */
    protected GlSceneSprites(GlScenePlatform.State gl) {
        this(gl, false);
    }

    /**
     * @param gl               native state adapter
     * @param proceduralClouds whether to compile the shader with procedural cloud masks
     */
    protected GlSceneSprites(GlScenePlatform.State gl, boolean proceduralClouds) {
        this.gl = gl;
        this.proceduralClouds = proceduralClouds;
    }

    /**
     * @return OpenGL texture name of the native sprite atlas
     */
    protected abstract int atlas();

    /**
     * @param node scene node that may be an emitter
     * @param view world-to-camera transform
     */
    public abstract void collect(SceneNode node, Mat4View view);

    /**
     * Queues the smoke with the modern demo's depth fade and aspect. The feathered mask uses a
     * slightly higher peak opacity to give the floor similar coverage.
     *
     * @param smoke emitter with the current ignition time
     * @param view  world-to-camera transform
     */
    protected final void collectCampfireSmoke(LegacyCampfireSmoke smoke, Mat4View view) {
        int limit = smoke.limit();
        if (limit == 0) return;
        transform(smoke, view);
        for (int i = smoke.first(limit); i < limit; i++) {
            int seed = smoke.seed(i);
            var frames = smoke.path(seed);
            double ticks = smoke.ageTicks(i);
            int tick = (int) ticks;
            if (ticks >= 0 && tick < frames.length - 1) {
                appendCloud(frames[tick], frames[tick + 1], (float) (ticks - tick), 0.36f, 2.1f, 0.72f, 0.33f, seed);
            }
        }
    }

    /**
     * @param emitter node supplying the local-to-world transform
     * @param view    world-to-camera transform
     */
    protected final void transform(SceneNode emitter, Mat4View view) {
        modelView.setProduct(view, emitter.worldMatrix(modelView));
    }

    /**
     * Interpolates {@code a + fraction * (b - a)} within one recorded tick.
     *
     * @param fraction fractional tick in [0,1]
     * @param a        initial value
     * @param b        final value
     * @return sampled value in the same units
     */
    private static float lerp(float fraction, float a, float b) {
        return a + fraction * (b - a);
    }

    /**
     * Interpolates native sprite position, roll, size and color within a tick and keeps its current atlas frame.
     *
     * @param current      start of the native tick
     * @param next         end of the native tick
     * @param part         fractional tick in [0, 1)
     * @param fadeDistance depth fade range in scene units, zero for a hard intersection
     * @param widthScale   multiplier on native sprite half-width
     * @param heightScale  multiplier on native sprite half-height
     * @param opacity      multiplier on native alpha
     */
    public void append(Frame current, Frame next, float part, float fadeDistance, float widthScale, float heightScale, float opacity) {
        append(current, next, part, fadeDistance, widthScale, heightScale, opacity, -1);
    }

    /**
     * Queues a sprite with an optional override for its interpolated RGB. The packed RGB channels
     * are decoded to shader inputs by dividing the byte values by 255.
     *
     * @param current      start of the native tick
     * @param next         end of the native tick
     * @param part         fractional tick in [0, 1)
     * @param fadeDistance depth fade range in scene units, zero for a hard intersection
     * @param widthScale   multiplier on native sprite half-width
     * @param heightScale  multiplier on native sprite half-height
     * @param opacity      multiplier on native alpha
     * @param tintRgb      packed 24-bit RGB, or -1 to use the sampled native color
     */
    protected final void append(Frame current, Frame next, float part, float fadeDistance, float widthScale,
                                float heightScale, float opacity, int tintRgb) {
        append(current, next, part, fadeDistance, widthScale, heightScale, opacity, tintRgb, 0);
    }

    /**
     * Queues a soft cloud whose mask uses the full [0, 1] UV rectangle.
     *
     * @param current      frame at the start of the tick
     * @param next         frame at the end of the tick
     * @param part         fractional tick in [0, 1)
     * @param fadeDistance depth fade distance in blocks
     * @param widthScale   multiplier on the billboard half-width
     * @param heightScale  multiplier on the billboard half-height
     * @param opacity      multiplier on frame alpha
     * @param cloudSeed    positive seed for the mask shape
     */
    protected final void appendCloud(Frame current, Frame next, float part, float fadeDistance, float widthScale,
                                     float heightScale, float opacity, int cloudSeed) {
        append(current, next, part, fadeDistance, widthScale, heightScale, opacity, -1, cloudSeed);
    }

    /**
     * @param current      first frame
     * @param next         second frame
     * @param part         interpolation fraction in [0, 1)
     * @param fadeDistance depth fade distance in blocks, or zero
     * @param widthScale   multiplier on billboard half-width
     * @param heightScale  multiplier on billboard half-height
     * @param opacity      multiplier on frame alpha
     * @param tintRgb      packed tint, or -1 to interpolate the frame RGB
     * @param cloudSeed    procedural mask seed, or zero for the native atlas
     */
    private void append(Frame current, Frame next, float part, float fadeDistance, float widthScale,
                        float heightScale, float opacity, int tintRgb, int cloudSeed) {
        center.set(lerp(part, current.x, next.x), lerp(part, current.y, next.y), lerp(part, current.z, next.z));
        if (!modelView.transformPoint(center, center)) return;
        float size = lerp(part, current.size, next.size);
        float w = size * widthScale;
        float h = size * heightScale;
        if (!(w > 0 && h > 0 && Float.isFinite(w) && Float.isFinite(h))) return;
        if (count == sprites.length) sprites = Arrays.copyOf(sprites, count * 2);
        if (sprites[count] == null) sprites[count] = new Sprite();
        float[] data = sprites[count++].values;
        data[0] = center.x();
        data[1] = center.y();
        data[2] = center.z();
        data[3] = fadeDistance;
        data[4] = w;
        data[5] = h;
        data[6] = cloudSeed;
        float halfRoll = lerp(part, current.roll, next.roll) * 0.5f;
        data[10] = (float) Math.sin(halfRoll);
        data[11] = (float) Math.cos(halfRoll);
        data[12] = current.u0;
        data[13] = current.v0;
        data[14] = current.u1;
        data[15] = current.v1;
        if (tintRgb == -1) {
            data[16] = lerp(part, current.red, next.red);
            data[17] = lerp(part, current.green, next.green);
            data[18] = lerp(part, current.blue, next.blue);
        } else {
            data[16] = (tintRgb >>> 16 & 255) / 255f;
            data[17] = (tintRgb >>> 8 & 255) / 255f;
            data[18] = (tintRgb & 255) / 255f;
        }
        data[19] = lerp(part, current.alpha, next.alpha) * opacity;
    }

    /**
     * Renders sorted particles into a color-only target, using a shader depth test and optional soft fade.
     *
     * @param overlay    color-only view that binds the scene depth texture
     * @param target     offscreen scene color and depth
     * @param camera     canonical projection and scene clip planes
     * @param width      active viewport width before pixel rounding
     * @param height     active viewport height before pixel rounding
     * @param pixelsWide scissor width in pixels
     * @param pixelsHigh scissor height in pixels
     */
    void render(GlSceneOverlay overlay, GlScenePlatform.Target target, Camera3D camera, float width, float height,
                int pixelsWide, int pixelsHigh) {
        if (count == 0) return;
        if (program == 0) initialize();
        Arrays.sort(sprites, 0, count, ORDER);
        gl.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int samplerDepth = gl.samplerDepth();
        try {
            overlay.begin(target, pixelsWide, pixelsHigh);
            gl.bindTexture(atlas());
            gl.applySampler(0, TextureOptions.linear());
            gl.blend(true);
            gl.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);
            bytes.clear();
            SceneMeshData.writeClipMatrix(bytes, camera.projection(projection));
            bytes.putFloat(width / target.width()).putFloat(height / target.height()).putFloat(camera.near()).putFloat(camera.far());
            bytes.putFloat(camera.orthographic() ? 1 : 0).putFloat(0).putLong(0);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, cameraUniforms);
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes.flip());
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, CAMERA_BINDING, cameraUniforms);
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, SPRITES_BINDING, spriteUniforms);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, spriteUniforms);
            int batch = SceneSoftParticleShaders.BATCH_SIZE;
            for (int i = 0; i < count; i += batch) {
                bytes.clear();
                int end = Math.min(i + batch, count);
                for (int index = i; index < end; index++) {
                    for (float value : sprites[index].values) bytes.putFloat(value);
                }
                GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes.flip());
                GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 6, end - i);
            }
        } finally {
            gl.restoreSamplers(samplerDepth);
            gl.bindTexture(previousTexture);
            overlay.end();
        }
    }

    /**
     * Links the shared billboard program and allocates the quad and stream buffers.
     */
    private void initialize() {
        program = GlScenePrograms.link(SceneSoftParticleShaders.vertex(150, true, proceduralClouds),
                SceneSoftParticleShaders.fragment(150, true, proceduralClouds), "Position");
        GL20.glUseProgram(program);
        GlSceneOverlay.sampler(program, "SoftAtlas", 0);
        GlSceneOverlay.sampler(program, "SoftSceneDepth", GlSceneOverlay.DEPTH);
        GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SoftCamera"), CAMERA_BINDING);
        GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SoftSprites"), SPRITES_BINDING);
        vao = GL30.glGenVertexArrays();
        quad = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quad);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                    stack.floats(-1, -1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, 1, 1, 0, -1, 1, 0), GL15.GL_STATIC_DRAW);
        }
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        cameraUniforms = uniformBuffer(SceneSoftParticleShaders.SCENE_BYTES);
        spriteUniforms = uniformBuffer(SceneSoftParticleShaders.BATCH_BYTES);
    }

    /**
     * @param size required bytes in a uniform block
     * @return allocated streaming uniform buffer name
     */
    private static int uniformBuffer(int size) {
        int id = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL15.GL_STREAM_DRAW);
        return id;
    }

    /**
     * Clears collected billboards between frames.
     */
    void reset() {
        count = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (program != 0) GL20.glDeleteProgram(program);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (quad != 0) GL15.glDeleteBuffers(quad);
        if (cameraUniforms != 0) GL15.glDeleteBuffers(cameraUniforms);
        if (spriteUniforms != 0) GL15.glDeleteBuffers(spriteUniforms);
        program = vao = quad = cameraUniforms = spriteUniforms = 0;
        reset();
    }

    /**
     * @param x     local X in blocks
     * @param y     local Y in blocks
     * @param z     local Z in blocks
     * @param size  native billboard half-size in blocks
     * @param roll  native billboard rotation about the view axis in radians
     * @param u0    atlas left edge
     * @param v0    atlas top edge
     * @param u1    atlas right edge
     * @param v1    atlas bottom edge
     * @param red   red tint in [0, 1]
     * @param green green tint in [0, 1]
     * @param blue  blue tint in [0, 1]
     * @param alpha opacity in [0, 1]
     */
    public record Frame(float x, float y, float z, float size, float roll, float u0, float v0, float u1, float v1,
                        float red, float green, float blue, float alpha) {
    }

    /**
     * Reusable billboard in the shared GLSL std140 field order.
     */
    private static final class Sprite {
        /**
         * Center, fade, half extents, quaternion, atlas rectangle and RGBA.
         */
        private final float[] values = new float[20];
    }
}
