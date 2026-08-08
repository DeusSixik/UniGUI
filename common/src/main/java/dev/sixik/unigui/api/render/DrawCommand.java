package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.text.RichText;

public final class DrawCommand {
    private DrawCommandType type;
    private final MutableRect bounds = new MutableRect();
    private final MutableRect uv = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final Transform transform = new Transform();
    private Paint paint = new Paint();
    private VectorPath path;
    private DrawMesh mesh;
    private TextureHandle texture;
    private String text;
    private RichText richText;
    private CustomDraw customDraw;
    private float radius;

    public static DrawCommand rect(RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.RECT).bounds(bounds).paint(paint);
    }

    public static DrawCommand texture(TextureHandle texture, RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.TEXTURE).texture(texture).bounds(bounds).paint(paint);
    }

    public static DrawCommand text(String text, RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.TEXT).text(text).bounds(bounds).paint(paint);
    }

    public static DrawCommand path(VectorPath path, RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.PATH).path(path).bounds(bounds).paint(paint);
    }

    public static DrawCommand circle(RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.CIRCLE).bounds(bounds).paint(paint);
    }

    public static DrawCommand line(RectView bounds, Paint paint) {
        return new DrawCommand(DrawCommandType.LINE).bounds(bounds).paint(paint);
    }

    public static DrawCommand custom(CustomDraw customDraw) {
        return new DrawCommand(DrawCommandType.CUSTOM).customDraw(customDraw);
    }

    public static DrawCommand mesh(DrawMesh mesh, TextureHandle texture) {
        return new DrawCommand(DrawCommandType.MESH).mesh(mesh).texture(texture);
    }

    public static DrawCommand drawCmd() {
        return new DrawCommand(DrawCommandType.DRAW_CMD);
    }

    public static DrawCommand pushClip(RectView bounds) {
        return new DrawCommand(DrawCommandType.PUSH_CLIP).bounds(bounds);
    }

    public static DrawCommand popClip() {
        return new DrawCommand(DrawCommandType.POP_CLIP);
    }

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

    public CustomDraw customDraw() {
        return customDraw;
    }

    public DrawCommand customDraw(CustomDraw customDraw) {
        this.customDraw = customDraw;
        return this;
    }

    public float radius() {
        return radius;
    }

    public DrawCommand radius(float radius) {
        this.radius = radius;
        return this;
    }

    public DrawCommand copy() {
        DrawCommand copy = new DrawCommand(type);
        copy.bounds.set(bounds);
        copy.uv.set(uv);
        copy.transform.copyFrom(transform);
        copy.paint = paint.copy();
        copy.path = path == null ? null : path.copy();
        copy.mesh = mesh == null ? null : mesh.copy();
        copy.texture = texture;
        copy.text = text;
        copy.richText = richText;
        copy.customDraw = customDraw;
        copy.radius = radius;
        return copy;
    }
}
