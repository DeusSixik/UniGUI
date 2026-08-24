package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.text.RichText;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Одна backend-neutral команда отрисовки в {@link DrawList}.
 *
 * <p>Команда хранит тип, bounds, paint, transform, optional texture/text/path/mesh/shader данные и
 * используется как промежуточный формат между виджетами и {@link RenderBackend}. Большинство setter'ов
 * возвращают саму команду, чтобы renderer мог собирать её fluent-цепочкой.</p>
 *
 * <p>Команда копирует mutable данные при записи: {@link Paint}, {@link VectorPath}, {@link DrawMesh},
 * {@link Transform} и shader options не должны протекать наружу после попадания в draw list.</p>
 */
public final class DrawCommand {
    private DrawCommandType type;
    private final MutableRect bounds = new MutableRect();
    private final MutableRect uv = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final Transform transform = new Transform();
    private final ObjectArrayList<TransformLayer> transformStack = new ObjectArrayList<>();
    private final List<TransformLayer> transformStackView = Collections.unmodifiableList(transformStack);
    private Paint paint = new Paint();
    private VectorPath path;
    private DrawMesh mesh;
    private TextureHandle texture;
    private String text;
    private RichText richText;
    private boolean textPixelSnap = true;
    private CustomDraw customDraw;
    private ShaderHandle shader;
    private ShaderUniforms shaderUniforms = ShaderUniforms.empty();
    private ShaderDrawOptions shaderOptions = ShaderDrawOptions.defaults();
    private final LinkedHashMap<String, TextureHandle> shaderTextures = new LinkedHashMap<>();
    private final Map<String, TextureHandle> shaderTexturesView = Collections.unmodifiableMap(shaderTextures);
    private float radius;

    /**
     * Создаёт команду прямоугольника.
     *
     * @param bounds bounds команды
     * @param paint параметры заливки/обводки
     * @return новая команда
     */
    public static DrawCommand rect(RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.RECT).bounds(bounds).paint(paint);
    }

    /**
     * Создаёт команду текстуры.
     *
     * @param texture handle текстуры
     * @param bounds bounds назначения
     * @param paint tint/blend параметры
     * @return новая команда
     */
    public static DrawCommand texture(TextureHandle texture, RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.TEXTURE).texture(texture).bounds(bounds).paint(paint);
    }

    /**
     * Создаёт команду plain text.
     *
     * @param text текст
     * @param bounds bounds текстового блока
     * @param paint цвет и blend параметры
     * @return новая команда
     */
    public static DrawCommand text(String text, RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.TEXT).text(text).bounds(bounds).paint(paint);
    }

    /**
     * Создаёт команду vector path.
     *
     * @param path path для рендера
     * @param bounds bounds path-команды
     * @param paint параметры заливки/обводки
     * @return новая команда
     */
    public static DrawCommand path(VectorPath path, RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.PATH).path(path).bounds(bounds).paint(paint);
    }

    /**
     * Создаёт команду окружности/эллипса.
     *
     * @param bounds bounds фигуры
     * @param paint параметры рендера
     * @return новая команда
     */
    public static DrawCommand circle(RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.CIRCLE).bounds(bounds).paint(paint);
    }

    /**
     * Создаёт команду линии.
     *
     * @param bounds хранит start point и delta до end point
     * @param paint параметры stroke
     * @return новая команда
     */
    public static DrawCommand line(RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.LINE).bounds(bounds).paint(paint);
    }

    /**
     * Создаёт команду custom callback.
     *
     * @param customDraw callback backend-specific рендера
     * @return новая команда
     */
    public static DrawCommand custom(CustomDraw customDraw) {
        return new DrawCommand(DrawCommandType.CUSTOM).customDraw(customDraw);
    }

    /**
     * Создаёт shader-команду.
     *
     * @param shader handle шейдера
     * @param bounds bounds fullscreen/quad области
     * @param uniforms uniforms команды
     * @return новая команда
     */
    public static DrawCommand shader(ShaderHandle shader, RectView bounds, ShaderUniforms uniforms) {
        return new DrawCommand(DrawCommandType.SHADER).shader(shader).bounds(bounds).shaderUniforms(uniforms);
    }

    /**
     * Создаёт mesh-команду.
     *
     * @param mesh треугольная mesh-геометрия
     * @param texture необязательная текстура
     * @return новая команда
     */
    public static DrawCommand mesh(DrawMesh mesh, TextureHandle texture) {
        return new DrawCommand(DrawCommandType.MESH).mesh(mesh).texture(texture);
    }

    /**
     * Создаёт marker-команду {@link DrawCommandType#DRAW_CMD}.
     *
     * @return новая команда
     */
    public static DrawCommand drawCmd() {
        return new DrawCommand(DrawCommandType.DRAW_CMD);
    }

    /**
     * Создаёт команду открытия clip/scissor области.
     *
     * @param bounds bounds clip области
     * @return новая команда
     */
    public static DrawCommand pushClip(RectView bounds) {
        return new DrawCommand(DrawCommandType.PUSH_CLIP).bounds(bounds);
    }

    /**
     * Создаёт команду закрытия последней clip/scissor области.
     *
     * @return новая команда
     */
    public static DrawCommand popClip() {
        return new DrawCommand(DrawCommandType.POP_CLIP);
    }

    /**
     * Создаёт команду указанного типа.
     *
     * @param type тип draw-команды
     */
    public DrawCommand(DrawCommandType type) {
        this.type = type;
    }

    public DrawCommandType type() {
        return type;
    }

    public MutableRect bounds() {
        return bounds;
    }

    public DrawCommand bounds(RectView bounds) {
        this.bounds.set(bounds);
        return this;
    }

    public MutableRect uv() {
        return uv;
    }

    public DrawCommand uv(RectView uv) {
        this.uv.set(uv);
        return this;
    }

    public Transform transform() {
        return transform;
    }

    public DrawCommand transform(Transform transform) {
        this.transform.copyFrom(transform);
        return this;
    }

    public List<TransformLayer> transformStack() {
        return transformStackView;
    }

    public Object[] transformStackElements() {
        return transformStack.elements();
    }

    public int transformStackSize() {
        return transformStack.size();
    }

    public DrawCommand transformStack(List<TransformLayer> transformStack) {
        this.transformStack.clear();
        copyTransformLayers(this.transformStack, transformStack);
        return this;
    }

    public DrawCommand prependTransformStack(List<TransformLayer> transformStack) {
        if (transformStack == null || transformStack.isEmpty()) return this;
        ObjectArrayList<TransformLayer> combined = new ObjectArrayList<>(transformStack.size() + this.transformStack.size());
        copyTransformLayers(combined, transformStack);
        copyTransformLayers(combined, this.transformStack);
        this.transformStack.clear();
        this.transformStack.addAll(combined);
        return this;
    }

    private static void copyTransformLayers(ObjectArrayList<TransformLayer> target, List<TransformLayer> source) {
        if (target == null || source == null || source.isEmpty()) return;
        if (source instanceof ObjectArrayList<?> objectSource) {
            Object[] rawLayers = objectSource.elements();
            for (int i = 0, size = objectSource.size(); i < size; i++) {
                Object value = rawLayers[i];
                if (value instanceof TransformLayer layer) {
                    target.add(layer.copy());
                }
            }
            return;
        }
        for (int i = 0, size = source.size(); i < size; i++) {
            TransformLayer layer = source.get(i);
            if (layer != null) {
                target.add(layer.copy());
            }
        }
    }
    public Paint paint() {
        return paint;
    }

    public DrawCommand paint(Paint paint) {
        this.paint = paint == null ? new Paint() : paint.copy();
        return this;
    }

    public VectorPath path() {
        return path;
    }

    public DrawCommand path(VectorPath path) {
        this.path = path == null ? null : path.copy();
        return this;
    }

    public TextureHandle texture() {
        return texture;
    }

    public DrawMesh mesh() {
        return mesh;
    }

    public DrawCommand mesh(DrawMesh mesh) {
        this.mesh = mesh == null ? null : mesh.copy();
        return this;
    }

    public DrawCommand texture(TextureHandle texture) {
        this.texture = texture;
        return this;
    }

    public String text() {
        return text;
    }

    public DrawCommand text(String text) {
        this.text = text;
        this.richText = text == null ? null : RichText.plain(text);
        return this;
    }

    public RichText richText() {
        return richText;
    }

    public DrawCommand richText(RichText richText) {
        this.richText = richText;
        this.text = richText == null ? null : richText.plainText();
        return this;
    }

    public boolean textPixelSnap() {
        return textPixelSnap;
    }

    public DrawCommand textPixelSnap(boolean textPixelSnap) {
        this.textPixelSnap = textPixelSnap;
        return this;
    }

    public CustomDraw customDraw() {
        return customDraw;
    }

    public DrawCommand customDraw(CustomDraw customDraw) {
        this.customDraw = customDraw;
        return this;
    }

    public ShaderHandle shader() {
        return shader;
    }

    public DrawCommand shader(ShaderHandle shader) {
        this.shader = shader == null ? null : shader.copy();
        return this;
    }

    public ShaderUniforms shaderUniforms() {
        return shaderUniforms;
    }

    public DrawCommand shaderUniforms(ShaderUniforms shaderUniforms) {
        this.shaderUniforms = shaderUniforms == null ? ShaderUniforms.empty() : shaderUniforms.copy();
        return this;
    }

    public ShaderDrawOptions shaderOptions() {
        return shaderOptions;
    }

    public DrawCommand shaderOptions(ShaderDrawOptions shaderOptions) {
        this.shaderOptions = shaderOptions == null ? ShaderDrawOptions.defaults() : shaderOptions.copy();
        return this;
    }

    public Map<String, TextureHandle> shaderTextures() {
        return shaderTexturesView;
    }

    public DrawCommand shaderTexture(String uniformName, TextureHandle texture) {
        String name = uniformName == null ? "" : uniformName.trim();
        if (name.isEmpty()) return this;
        if (texture == null) shaderTextures.remove(name);
        else shaderTextures.put(name, texture);
        return this;
    }

    public DrawCommand shaderTextures(Map<String, TextureHandle> textures) {
        shaderTextures.clear();
        if (textures == null || textures.isEmpty()) return this;
        for (Map.Entry<String, TextureHandle> entry : textures.entrySet()) {
            shaderTexture(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public float radius() {
        return radius;
    }

    public DrawCommand radius(float radius) {
        this.radius = radius;
        return this;
    }

    /**
     * Создаёт глубокую копию команды для безопасного хранения в draw list.
     *
     * @return независимая копия команды
     */
    public DrawCommand copy() {
        DrawCommand copy = new DrawCommand(type);
        copy.bounds.set(bounds);
        copy.uv.set(uv);
        copy.transform.copyFrom(transform);
        copy.transformStack(transformStack);
        copy.paint = paint.copy();
        copy.path = path == null ? null : path.copy();
        copy.mesh = mesh == null ? null : mesh.copy();
        copy.texture = texture;
        copy.text = text;
        copy.richText = richText;
        copy.textPixelSnap = textPixelSnap;
        copy.customDraw = customDraw;
        copy.shader = shader == null ? null : shader.copy();
        copy.shaderUniforms = shaderUniforms == null ? ShaderUniforms.empty() : shaderUniforms.copy();
        copy.shaderOptions = shaderOptions == null ? ShaderDrawOptions.defaults() : shaderOptions.copy();
        copy.shaderTextures(shaderTextures);
        copy.radius = radius;
        return copy;
    }
}
