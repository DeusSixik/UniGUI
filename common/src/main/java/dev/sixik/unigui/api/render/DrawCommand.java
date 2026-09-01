package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.ColorView;
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
    /** Пул слоёв, которые временно не входят в активный стек команды. */
    private final ObjectArrayList<TransformLayer> recycledTransformLayers = new ObjectArrayList<>();
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
    private int segments;
    private float quadX1;
    private float quadY1;
    private float quadX2;
    private float quadY2;
    private float quadX3;
    private float quadY3;
    private float quadX4;
    private float quadY4;
    private float quadU1;
    private float quadV1;
    private float quadU2;
    private float quadV2;
    private float quadU3;
    private float quadV3;
    private float quadU4;
    private float quadV4;

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

    DrawCommand bounds(float x, float y, float width, float height) {
        this.bounds.set(x, y, width, height);
        return this;
    }

    public MutableRect uv() {
        return uv;
    }

    public DrawCommand uv(RectView uv) {
        this.uv.set(uv);
        return this;
    }

    DrawCommand uv(float x, float y, float width, float height) {
        this.uv.set(x, y, width, height);
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
        copyTransformStackFrom(transformStack);
        return this;
    }

    public DrawCommand prependTransformStack(List<TransformLayer> transformStack) {
        if (transformStack == null || transformStack.isEmpty()) return this;
        if (transformStack == this.transformStack || transformStack == transformStackView) {
            int oldSize = this.transformStack.size();
            ensureTransformStackSize(oldSize * 2);
            for (int i = oldSize - 1; i >= 0; i--) {
                this.transformStack.get(oldSize + i).copyFrom(this.transformStack.get(i));
            }
            return this;
        }

        int prefixSize = countNonNullLayers(transformStack);
        if (prefixSize == 0) return this;
        int oldSize = this.transformStack.size();
        ensureTransformStackSize(oldSize + prefixSize);
        for (int i = oldSize - 1; i >= 0; i--) {
            this.transformStack.set(i + prefixSize, this.transformStack.get(i));
        }

        int targetIndex = 0;
        for (int i = 0, size = transformStack.size(); i < size; i++) {
            TransformLayer layer = transformStack.get(i);
            if (layer != null) {
                this.transformStack.get(targetIndex++).copyFrom(layer);
            }
        }
        return this;
    }

    private void copyTransformStackFrom(List<TransformLayer> source) {
        if (source == this.transformStack || source == transformStackView) return;
        int targetSize = source == null ? 0 : countNonNullLayers(source);
        ensureTransformStackSize(targetSize);

        int targetIndex = 0;
        if (source != null) {
            for (int i = 0, size = source.size(); i < size; i++) {
                TransformLayer layer = source.get(i);
                if (layer != null) {
                    transformStack.get(targetIndex++).copyFrom(layer);
                }
            }
        }
        trimTransformStack(targetIndex);
    }

    private void ensureTransformStackSize(int size) {
        while (transformStack.size() < size) {
            int recycledSize = recycledTransformLayers.size();
            TransformLayer layer = recycledSize == 0
                    ? new TransformLayer(null, null)
                    : recycledTransformLayers.remove(recycledSize - 1);
            transformStack.add(layer);
        }
    }

    private void trimTransformStack(int size) {
        while (transformStack.size() > size) {
            recycledTransformLayers.add(transformStack.remove(transformStack.size() - 1));
        }
    }

    private void clearTransformStack() {
        trimTransformStack(0);
    }

    private static int countNonNullLayers(List<TransformLayer> source) {
        int count = 0;
        for (int i = 0, size = source.size(); i < size; i++) {
            if (source.get(i) != null) count++;
        }
        return count;
    }
    public Paint paint() {
        return paint;
    }

    public DrawCommand paint(Paint paint) {
        this.paint = paint == null ? new Paint() : paint.copy();
        return this;
    }

    /** Записывает paint во внутренний объект команды без создания копии paint. */
    DrawCommand paintOwned(Paint paint) {
        this.paint.copyFrom(paint);
        return this;
    }

    /** Записывает обычный цвет заливки без временного создания {@link Paint}. */
    DrawCommand paintColorOwned(ColorView color, float opacity) {
        if (color == null) {
            paint.reset();
            return this;
        }
        float multiplier = Float.isFinite(opacity) ? Math.max(0.0f, opacity) : 1.0f;
        paint.color().set(color.r(), color.g(), color.b(), color.a() * multiplier);
        paint.stroke(false).strokeWidth(0.0f).blend(BlendMode.NORMAL).clearDash();
        return this;
    }

    public VectorPath path() {
        return path;
    }

    public DrawCommand path(VectorPath path) {
        this.path = path == null ? null : path.copy();
        return this;
    }

    /** Передаёт path во внутреннее владение команды без копирования. */
    DrawCommand pathOwned(VectorPath path) {
        this.path = path;
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

    /** Передаёт mesh во внутреннее владение команды без копирования. */
    DrawCommand meshOwned(DrawMesh mesh) {
        this.mesh = mesh;
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

    /** @return количество сегментов для геометрии окружности; {@code 0} означает значение backend по умолчанию */
    public int segments() {
        return segments;
    }

    /** Задаёт количество сегментов для окружности или эллипса. */
    public DrawCommand segments(int segments) {
        this.segments = Math.max(0, segments);
        return this;
    }

    /** Записывает текстурированный quad без создания объектов позиций и вершин. */
    public DrawCommand texturedQuad(float x1, float y1, float x2, float y2,
                                    float x3, float y3, float x4, float y4,
                                    float u1, float v1, float u2, float v2,
                                    float u3, float v3, float u4, float v4) {
        quadX1 = x1;
        quadY1 = y1;
        quadX2 = x2;
        quadY2 = y2;
        quadX3 = x3;
        quadY3 = y3;
        quadX4 = x4;
        quadY4 = y4;
        quadU1 = u1;
        quadV1 = v1;
        quadU2 = u2;
        quadV2 = v2;
        quadU3 = u3;
        quadV3 = v3;
        quadU4 = u4;
        quadV4 = v4;
        return this;
    }

    public float quadX1() { return quadX1; }
    public float quadY1() { return quadY1; }
    public float quadX2() { return quadX2; }
    public float quadY2() { return quadY2; }
    public float quadX3() { return quadX3; }
    public float quadY3() { return quadY3; }
    public float quadX4() { return quadX4; }
    public float quadY4() { return quadY4; }
    public float quadU1() { return quadU1; }
    public float quadV1() { return quadV1; }
    public float quadU2() { return quadU2; }
    public float quadV2() { return quadV2; }
    public float quadU3() { return quadU3; }
    public float quadV3() { return quadV3; }
    public float quadU4() { return quadU4; }
    public float quadV4() { return quadV4; }

    /**
     * Подготавливает команду, полученную из retained-пула, к новой записи.
     * Метод предназначен для внутреннего кадрового пути DrawList.
     */
    DrawCommand resetForReuse(DrawCommandType type) {
        this.type = type;
        bounds.set(0.0f, 0.0f, 0.0f, 0.0f);
        uv.set(0.0f, 0.0f, 1.0f, 1.0f);
        transform.position().set(0.0f, 0.0f);
        transform.scale().set(1.0f, 1.0f);
        transform.pivot().set(0.0f, 0.0f);
        transform.setRotationDegrees(0.0f);
        clearTransformStack();
        paint.reset();
        path = null;
        mesh = null;
        texture = null;
        text = null;
        richText = null;
        textPixelSnap = true;
        customDraw = null;
        shader = null;
        shaderUniforms.clear();
        shaderOptions.builtinUniforms(true).blend(true).squareVertexOffset(-0.25f);
        shaderTextures.clear();
        radius = 0.0f;
        segments = 0;
        quadX1 = quadY1 = quadX2 = quadY2 = quadX3 = quadY3 = quadX4 = quadY4 = 0.0f;
        quadU1 = quadV1 = quadU2 = quadV2 = quadU3 = quadV3 = quadU4 = quadV4 = 0.0f;
        return this;
    }

    /** Освобождает внешние ссылки перед помещением команды в retained-пул. */
    void releaseForPool() {
        path = null;
        mesh = null;
        texture = null;
        text = null;
        richText = null;
        customDraw = null;
        shader = null;
        shaderUniforms.clear();
        shaderTextures.clear();
        clearTransformStack();
        quadX1 = quadY1 = quadX2 = quadY2 = quadX3 = quadY3 = quadX4 = quadY4 = 0.0f;
        quadU1 = quadV1 = quadU2 = quadV2 = quadU3 = quadV3 = quadU4 = quadV4 = 0.0f;
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
        copy.segments = segments;
        copy.texturedQuad(quadX1, quadY1, quadX2, quadY2, quadX3, quadY3, quadX4, quadY4,
                quadU1, quadV1, quadU2, quadV2, quadU3, quadV3, quadU4, quadV4);
        return copy;
    }

    /**
     * Копирует сохранённую команду в объект из кадрового пула.
     *
     * <p>Метод используется для воспроизведения retained render-фрагментов без
     * создания нового {@code DrawCommand}. Снимки path и mesh безопасно
     * переиспользуются, потому что команда уже владеет их копиями.</p>
     */
    void copyForReplayTo(DrawCommand target) {
        if (target == null) return;
        target.resetForReuse(type);
        target.bounds.set(bounds);
        target.uv.set(uv);
        target.transform.copyFrom(transform);
        target.transformStack(transformStack);
        target.paint.copyFrom(paint);
        target.path = path;
        target.mesh = mesh;
        target.texture = texture;
        target.text = text;
        target.richText = richText;
        target.textPixelSnap = textPixelSnap;
        target.customDraw = customDraw;
        target.shader = shader;
        target.shaderUniforms.copyFrom(shaderUniforms);
        target.shaderOptions.copyFrom(shaderOptions);
        target.shaderTextures.putAll(shaderTextures);
        target.radius = radius;
        target.segments = segments;
        target.quadX1 = quadX1;
        target.quadY1 = quadY1;
        target.quadX2 = quadX2;
        target.quadY2 = quadY2;
        target.quadX3 = quadX3;
        target.quadY3 = quadY3;
        target.quadX4 = quadX4;
        target.quadY4 = quadY4;
        target.quadU1 = quadU1;
        target.quadV1 = quadV1;
        target.quadU2 = quadU2;
        target.quadV2 = quadV2;
        target.quadU3 = quadU3;
        target.quadV3 = quadV3;
        target.quadU4 = quadU4;
        target.quadV4 = quadV4;
    }
}
