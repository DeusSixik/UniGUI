package dev.sixik.unigui.demo.snapshot;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.text.AwtFontFace;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Рисует дерево виджетов в PNG без Minecraft и OpenGL: тот же цикл кадра, что у экрана (tick, measure, arrange, render),
 * но команды списка отрисовки исполняет Java2D. Нужен, чтобы вид редактора можно было посмотреть и сравнить из тестов.
 * Текстуры и шейдеры здесь не поддерживаются: вместо них рисуется серая заглушка.
 */
public final class SnapshotRenderer implements RenderBackend {
    private final float width;
    private final float height;
    private final float scale;
    private final Widget root;
    private final DefaultUIContext context = new DefaultUIContext();
    private final Font baseFont = new Font("Arial", Font.PLAIN, 10);
    private final FontFace face = new AwtFontFace("snapshot", baseFont);
    private long frame;
    private Graphics2D graphics;
    private AffineTransform base;
    private final Deque<Shape> clips = new ArrayDeque<>();

    /**
     * @param root   корень интерфейса
     * @param width  логическая ширина окна
     * @param height логическая высота окна
     * @param scale  масштаб логического пикселя в пиксели картинки
     */
    public SnapshotRenderer(Widget root, float width, float height, float scale) {
        this.root = root;
        this.width = width;
        this.height = height;
        this.scale = scale;
        if (root instanceof WidgetBase widget) widget.setUiContextInternal(context);
        Fonts.global().defaultFace(face);
    }

    /** Прогоняет несколько кадров без вывода: так очереди мутаций и отложенные раскладки успевают примениться. */
    public void settle(int frames) {
        for (int i = 0; i < frames; i++) frame(null);
    }

    /** @return контекст, к которому привязан корень */
    public DefaultUIContext context() {
        return context;
    }

    /**
     * Рисует кадр в файл.
     *
     * @param file куда записать PNG
     * @throws IOException если файл не записался
     */
    public void write(Path file) throws IOException {
        BufferedImage image = new BufferedImage(Math.round(width * scale), Math.round(height * scale),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D target = image.createGraphics();
        target.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        target.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        target.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        target.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        target.setColor(Color.BLACK);
        target.fillRect(0, 0, image.getWidth(), image.getHeight());
        frame(target);
        target.dispose();
        Files.createDirectories(file.toAbsolutePath().getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    private void frame(Graphics2D target) {
        applyAll(root);
        context.dispatcher().drain();
        root.tick(new FrameContext(frame, 1.0f / 60.0f, 0.0f, FramePhase.ANIMATION));
        root.measure(new LayoutContext(width, height));
        root.arrange(new MutableRect(0.0f, 0.0f, width, height));
        applyAll(root);
        DrawList list = new DrawList();
        root.render(new DefaultRenderContext(list).backend(this));
        frame++;
        if (target != null) {
            graphics = target;
            base = AffineTransform.getScaleInstance(scale, scale);
            clips.clear();
            render(list, null);
        }
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    // ------------------------------------------------------------------ RenderBackend

    @Override
    public float measureTextWidth(String text) {
        float total = 0.0f;
        if (text == null) return total;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            total += face.advance(codePoint, TextRun.DEFAULT_PIXEL_SIZE);
            i += Character.charCount(codePoint);
        }
        return total;
    }

    @Override
    public FontFace defaultTextFace() {
        return face;
    }

    @Override
    public void beginFrame(FrameContext frame) {
    }

    @Override
    public void endFrame() {
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        for (DrawCommand command : drawList.commands()) {
            draw(command);
        }
    }

    private void draw(DrawCommand command) {
        switch (command.type()) {
            case RECT -> shape(command, new Rectangle2D.Float(bx(command), by(command), bw(command), bh(command)));
            case ROUNDED_RECT -> {
                float radius = Math.abs(command.radius()) * 2.0f;
                shape(command, new RoundRectangle2D.Float(bx(command), by(command), bw(command), bh(command), radius, radius));
            }
            case CIRCLE -> shape(command, new Ellipse2D.Float(bx(command), by(command), bw(command), bh(command)));
            case LINE -> shape(command, new Line2D.Float(command.bounds().x(), command.bounds().y(),
                    command.bounds().x() + command.bounds().width(), command.bounds().y() + command.bounds().height()));
            case PATH -> shape(command, AffineTransform.getTranslateInstance(command.bounds().x(), command.bounds().y())
                    .createTransformedShape(path(command.path())));
            case TEXT -> text(command);
            case MESH -> mesh(command);
            case TEXTURE, SHADER -> {
                graphics.setTransform(matrix(command));
                graphics.setClip(clips.peek() == null ? null : deviceClip());
                graphics.setColor(new Color(90, 90, 90, 120));
                graphics.fill(new Rectangle2D.Float(bx(command), by(command), bw(command), bh(command)));
            }
            case PUSH_CLIP -> pushClip(command);
            case POP_CLIP -> {
                if (!clips.isEmpty()) clips.pop();
            }
            default -> {
            }
        }
    }

    private static float bx(DrawCommand command) {
        return Math.min(command.bounds().x(), command.bounds().x() + command.bounds().width());
    }

    private static float by(DrawCommand command) {
        return Math.min(command.bounds().y(), command.bounds().y() + command.bounds().height());
    }

    private static float bw(DrawCommand command) {
        return Math.abs(command.bounds().width());
    }

    private static float bh(DrawCommand command) {
        return Math.abs(command.bounds().height());
    }

    private void shape(DrawCommand command, Shape shape) {
        Paint paint = command.paint();
        prepare(command, paint.color());
        if (paint.isStroke() || command.type().name().equals("LINE")) {
            float[] dash = paint.dashed() ? new float[]{paint.dashLength(), paint.dashGap()} : null;
            graphics.setStroke(new BasicStroke(Math.max(0.1f, paint.strokeWidth()), BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND, 4.0f, dash, paint.dashOffset()));
            graphics.draw(shape);
        } else {
            graphics.fill(shape);
        }
    }

    private void prepare(DrawCommand command, ColorView color) {
        graphics.setTransform(matrix(command));
        graphics.setClip(null);
        if (!clips.isEmpty()) {
            AffineTransform current = graphics.getTransform();
            graphics.setTransform(new AffineTransform());
            graphics.setClip(deviceClip());
            graphics.setTransform(current);
        }
        graphics.setColor(awt(color));
    }

    private Shape deviceClip() {
        return clips.peek();
    }

    private void pushClip(DrawCommand command) {
        Shape rect = matrix(command).createTransformedShape(new Rectangle2D.Float(bx(command), by(command),
                bw(command), bh(command))).getBounds2D();
        Shape parent = clips.peek();
        if (parent == null) {
            clips.push(rect);
        } else {
            Area area = new Area(parent);
            area.intersect(new Area(rect));
            clips.push(area);
        }
    }

    private AffineTransform matrix(DrawCommand command) {
        AffineTransform matrix = new AffineTransform(base);
        Object[] layers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            TransformLayer layer = (TransformLayer) layers[i];
            if (layer != null) apply(matrix, layer.bounds(), layer.transform());
        }
        apply(matrix, command.bounds(), command.transform());
        return matrix;
    }

    private static void apply(AffineTransform matrix, RectView bounds, Transform transform) {
        if (bounds == null || transform == null) return;
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        matrix.translate(transform.position().x(), transform.position().y());
        matrix.translate(pivotX, pivotY);
        if (transform.rotationDegrees() != 0.0f) matrix.rotate(Math.toRadians(transform.rotationDegrees()));
        matrix.scale(transform.scale().x(), transform.scale().y());
        matrix.translate(-pivotX, -pivotY);
    }

    private static Path2D path(VectorPath source) {
        Path2D.Float path = new Path2D.Float();
        boolean open = false;
        if (source == null) return path;
        for (VectorPath.Element element : source.elements()) {
            switch (element.verb()) {
                case MOVE_TO -> {
                    path.moveTo(element.x1(), element.y1());
                    open = true;
                }
                case LINE_TO -> {
                    if (!open) {
                        path.moveTo(element.x1(), element.y1());
                        open = true;
                    } else {
                        path.lineTo(element.x1(), element.y1());
                    }
                }
                case QUADRATIC_TO -> path.quadTo(element.x1(), element.y1(), element.x2(), element.y2());
                case CUBIC_TO -> path.curveTo(element.x1(), element.y1(), element.x2(), element.y2(), element.x3(), element.y3());
                case CLOSE -> path.closePath();
                default -> {
                }
            }
        }
        return path;
    }

    private void mesh(DrawCommand command) {
        DrawMesh mesh = command.mesh();
        if (mesh == null) return;
        Object[] vertices = mesh.vertexElements();
        for (int i = 0; i + 2 < mesh.vertexCount(); i += 3) {
            DrawVertex a = (DrawVertex) vertices[i];
            DrawVertex b = (DrawVertex) vertices[i + 1];
            DrawVertex c = (DrawVertex) vertices[i + 2];
            prepare(command, a.color());
            Path2D.Float triangle = new Path2D.Float();
            triangle.moveTo(a.x(), a.y());
            triangle.lineTo(b.x(), b.y());
            triangle.lineTo(c.x(), c.y());
            triangle.closePath();
            graphics.fill(triangle);
        }
    }

    private void text(DrawCommand command) {
        RichText rich = command.richText();
        float x = command.bounds().x();
        float y = command.bounds().y();
        if (rich == null) {
            if (command.text() == null) return;
            drawRun(command, command.text(), baseFont, TextRun.DEFAULT_PIXEL_SIZE, command.paint().color(), x, y);
            return;
        }
        for (TextRun run : rich.runs()) {
            Font font = run.font() instanceof AwtFontFace awt ? awt.source() : baseFont;
            ColorView color = run.color() != null ? run.color() : command.paint().color();
            x += drawRun(command, run.text(), font, run.pixelSize(), color, x, y);
        }
    }

    private float drawRun(DrawCommand command, String text, Font font, float size, ColorView color, float x, float y) {
        if (text == null || text.isEmpty()) return 0.0f;
        prepare(command, color);
        Font sized = font.deriveFont(size);
        var metrics = graphics.getFontMetrics(sized);
        float baseline = y + face.metrics(size).ascent();
        float cursor = x;
        // Логический шрифт сам подставляет глифы, которых нет в физическом: иконочные символы рисуются как в игре.
        Font fallback = new Font(Font.DIALOG, Font.PLAIN, 10).deriveFont(size);
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int count = Character.charCount(codePoint);
            String glyph = text.substring(i, i + count);
            Font using = sized.canDisplay(codePoint) ? sized : fallback;
            graphics.setFont(using);
            graphics.drawString(glyph, cursor, baseline);
            cursor += face.advance(codePoint, size);
            i += count;
        }
        return cursor - x;
    }

    private static Color awt(ColorView color) {
        return new Color(clamp(color.r()), clamp(color.g()), clamp(color.b()), clamp(color.a()));
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
